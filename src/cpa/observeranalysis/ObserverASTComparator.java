package cpa.observeranalysis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.ParserFactory;
import org.eclipse.core.runtime.CoreException;

import cmdline.stubs.CLanguage;
import cmdline.stubs.StubCodeReaderFactory;
import cmdline.stubs.StubScannerInfo;

import com.google.common.base.Preconditions;

/**
 * Provides methods for generating, comparing and printing the ASTs generated from String.
 * The ASTs are generated by the Eclipse CDT IDE plugin.
 * @author rhein
 */
public class ObserverASTComparator {

  /**
   * Every occurrence of the joker expression $? in the pattern is substituted by JOKER_EXPR.
   * This is necessary because the C-parser cannot parse the pattern if it contains Dollar-Symbols.
   * The JOKER_EXPR must be a valid C-Identifier. It will be used to recognize the jokers in the generated AST.   
   */
  private static final String JOKER_EXPR = " CPAChecker_ObserverAnalysis_JokerExpression ";
  private static final String NUMBERED_JOKER_EXPR = " CPAChecker_ObserverAnalysis_JokerExpression_Num";
  private static final Pattern NUMBERED_JOKER_PATTERN = Pattern.compile("\\$\\d+");

  private static String replaceJokersInPattern(String pPattern) {
    String tmp = pPattern.replaceAll("\\$\\?", JOKER_EXPR);
    Matcher matcher = NUMBERED_JOKER_PATTERN.matcher(tmp);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(result, "");
      String key = tmp.substring(matcher.start()+1, matcher.end());
      try {
        int varKey = Integer.parseInt(key);
        result.append(NUMBERED_JOKER_EXPR + varKey + " ");
      } catch (NumberFormatException e) {
        // did not work, but i cant log it down here. Should not be able to happen anyway (regex captures only ints)
        result.append(matcher.group());
      }
    }
    matcher.appendTail(result);
    return result.toString();
  }
  
  static IASTNode generatePatternAST(String pPattern) {
    // $?-Jokers, $1-Jokers and function declaration
    String tmp = addFunctionDeclaration(replaceJokersInPattern(pPattern));
    
    IASTTranslationUnit ast = ObserverASTComparator.parse(tmp);
    return stripFunctionDeclaration(ast);
  }
  
  static IASTNode generateSourceAST(String pSource) {
    String tmp = addFunctionDeclaration(pSource);

    IASTTranslationUnit ast = ObserverASTComparator.parse(addFunctionDeclaration(tmp));
    return stripFunctionDeclaration(ast);
  }
  
  /**
   * Returns the Problem Message if this AST has a problem node.
   * Returns null otherwise.
   * @param pAST
   * @return
   */
  static String ASTcontatinsProblems(IASTNode pAST) {
    if (pAST instanceof IASTProblem) {
      return ((IASTProblem)pAST).getMessage();
    } else {
      String problem;
      for (IASTNode n : pAST.getChildren()) {
        problem = ASTcontatinsProblems(n);
          if (problem != null) {
            return problem;
        }
      }
    }
    return null;
  }
  
  
  /**
   * Surrounds the argument with a function declaration. 
   * This is necessary so the string can be parsed by the CDT parser. 
   * @param pBody
   * @return "void test() { " + body + ";}";
   */
  private static String addFunctionDeclaration(String pBody) {
    if (pBody.trim().endsWith(";")) {
      return "void test() { " + pBody + "}";
    } else {
      return "void test() { " + pBody + ";}";
    }
  }
  
  private static IASTNode stripFunctionDeclaration(IASTTranslationUnit ast) {
    IASTDeclaration[] declarations = ast.getDeclarations();
    assert declarations != null && declarations.length == 1;
    assert declarations[0] instanceof IASTFunctionDefinition;
    IASTFunctionDefinition func = (IASTFunctionDefinition)declarations[0];
    assert func.getDeclarator().getName().getRawSignature().equals("test");
    assert func.getBody() instanceof IASTCompoundStatement;
    IASTStatement[] body = ((IASTCompoundStatement)func.getBody()).getStatements();
    assert body.length == 2 && body[1] == null || body.length == 1;
    if (body[0] instanceof IASTExpressionStatement) {
      return ((IASTExpressionStatement)body[0]).getExpression();
    } else {
      return body[0];
    }
  }
  
  /**
   * Recursive method for comparing the ASTs.
   */
  static boolean compareASTs(IASTNode pCode, IASTNode pPattern, ObserverExpressionArguments pArgs) {
    Preconditions.checkNotNull(pCode);
    Preconditions.checkNotNull(pPattern);
    Preconditions.checkNotNull(pArgs);
    
    boolean result = true;
    if (isJoker(pPattern)) {
      result = true;
    } else if (handleNumberJoker(pCode, pPattern, pArgs)) {
      result = true;
    } else if (pCode instanceof IASTExpressionStatement) {
      result = compareASTs(((IASTExpressionStatement)pCode).getExpression(), pPattern, pArgs);
    } else if (pCode.getClass().equals(pPattern.getClass())) {
      if (pCode instanceof IASTName && ! IASTNamesAreEqual((IASTName)pCode, (IASTName)pPattern)) {
        result = false;
      } else if (pCode instanceof IASTLiteralExpression && ! IASTLiteralExpressionsAreEqual((IASTLiteralExpression)pCode, (IASTLiteralExpression)pPattern)) {
        result = false;
      } else if (pCode.getChildren().length != pPattern.getChildren().length) {
        result = false;
      } else {
        for (int i = 0; i < pCode.getChildren().length; i++) {
          if (compareASTs(pCode.getChildren()[i], pPattern.getChildren()[i], pArgs) == false)
            result = false;
          }
      }
    } else {
      result = false;
    }

    return result;
  }

  private static boolean handleNumberJoker(IASTNode pSource, IASTNode pPotentialJoker,
      ObserverExpressionArguments pArgs) {
    boolean isJoker = false;
    String number = "";
    if (pPotentialJoker instanceof IASTName) {
      IASTName name = (IASTName) pPotentialJoker;
      if (String.copyValueOf(name.getSimpleID()).startsWith(NUMBERED_JOKER_EXPR.trim())) {
        isJoker = true;
        number =  String.copyValueOf(name.getSimpleID()).substring(NUMBERED_JOKER_EXPR.trim().length());
      }
      // are there more IASTsomethings that could be Jokers?
    } else if (pPotentialJoker instanceof IASTIdExpression) {
      IASTIdExpression name = (IASTIdExpression) pPotentialJoker;
      if (name.getRawSignature().startsWith(NUMBERED_JOKER_EXPR.trim())) {
        isJoker = true;
        number =  name.getRawSignature().substring(NUMBERED_JOKER_EXPR.trim().length());
      }
    }
    if (isJoker) {
      // RawSignature returns the raw code before preprocessing.
      // This does not matter in this case because only very small sniplets, generated by method "addFunctionDeclaration" are tested, no preprocessing
      String value = pSource.getRawSignature();
      pArgs.putTransitionVariable(Integer.parseInt(number),value);
      return true;
    } else {
      return false; 
    }
  }

  private static boolean isJoker(IASTNode pNode) {
    if (pNode instanceof IASTName) {
      IASTName name = (IASTName) pNode;
      return String.copyValueOf(name.getSimpleID()).equals(JOKER_EXPR.trim());
      // are there more IASTsomethings that could be Jokers?
    } else if (pNode instanceof IASTName) {
      IASTName name = (IASTName) pNode;
      return String.copyValueOf(name.getSimpleID()).equals(JOKER_EXPR.trim());
    } else if (pNode instanceof IASTIdExpression) {
      IASTIdExpression name = (IASTIdExpression) pNode;
      return name.getRawSignature().equals(JOKER_EXPR.trim());
    } else return false;
  }

  private static boolean IASTNamesAreEqual(IASTName pA, IASTName pB) {
   return String.copyValueOf(pA.getSimpleID()).equals(String.copyValueOf(pB.getSimpleID()));
  }
  
  private static boolean IASTLiteralExpressionsAreEqual(IASTLiteralExpression pA, IASTLiteralExpression pB) {
    return String.copyValueOf(pA.getValue()).equals(String.copyValueOf(pB.getValue()));
   }

  /**
   * Parse the content of a file into an AST with the Eclipse CDT parser.
   * If an error occurs, the program is halted.
   * 
   * @param code The C code to parse.
   * @return The AST.
   */
  private static IASTTranslationUnit parse(String code) {
    CodeReader reader = new CodeReader(code.toCharArray());

    IScannerInfo scannerInfo = StubScannerInfo.getInstance();
    ICodeReaderFactory codeReaderFactory = new StubCodeReaderFactory();
    IParserLogService parserLog = ParserFactory.createDefaultLogService();

    ILanguage lang = new CLanguage("C99");

    try {
      return lang.getASTTranslationUnit(reader, scannerInfo, codeReaderFactory, null, parserLog);
    } catch (CoreException e) {
      // FIXME add error handling
      e.printStackTrace();
      assert false;
      return null;
    }
  }
}
