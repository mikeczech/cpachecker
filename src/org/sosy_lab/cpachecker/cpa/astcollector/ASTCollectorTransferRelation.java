/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.astcollector;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTCollectorState.CFAEdgeInfo;
import org.sosy_lab.cpachecker.cpa.astcollector.visitors.CExpressionASTVisitor;
import org.sosy_lab.cpachecker.cpa.astcollector.visitors.CSimpleDeclASTVisitor;
import org.sosy_lab.cpachecker.cpa.astcollector.visitors.CStatementASTVisitor;
import org.sosy_lab.cpachecker.cpa.astcollector.visitors.CStatementVariablesCollectingVisitor;
import org.sosy_lab.cpachecker.cpa.astcollector.visitors.CVariablesCollectingVisitor;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;


public class ASTCollectorTransferRelation extends ForwardingTransferRelation<ASTCollectorState, ASTCollectorState, SingletonPrecision> {

  private LogManager logger;

  public ASTCollectorTransferRelation(LogManager pLogger) {
    logger = pLogger;
  }

  static final Map<String, ASTNodeLabel> SPECIAL_FUNCTIONS;

  static {
    Builder<String, ASTNodeLabel> builder = ImmutableMap.builder();
    builder.put("pthread_create", ASTNodeLabel.PTHREAD);
    builder.put("pthread_exit", ASTNodeLabel.PTHREAD);
    builder.put("__VERIFIER_error", ASTNodeLabel.VERIFIER_ERROR);
    builder.put("__VERIFIER_assert", ASTNodeLabel.VERIFIER_ASSERT);
    builder.put("__VERIFIER_assume", ASTNodeLabel.VERIFIER_ASSUME);
    builder.put("__VERIFIER_atomic_begin", ASTNodeLabel.VERIFIER_ATOMIC_BEGIN);
    builder.put("__VERIFIER_atomic_end", ASTNodeLabel.VERIFIER_ATOMIC_END);
    builder.put("__VERIFIER_nondet", ASTNodeLabel.INPUT);
    builder.put("malloc", ASTNodeLabel.MALLOC);
    builder.put("free", ASTNodeLabel.FREE);
    SPECIAL_FUNCTIONS = builder.build();
  }

  public static ASTNodeLabel extractControlLabel(CFAEdge pCFAEdge) {
    if(pCFAEdge.getPredecessor().isLoopStart()) {
      return ASTNodeLabel.LOOP;
    }
    return ASTNodeLabel.BRANCH;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

  @Override
  protected ASTCollectorState handleAssumption(CAssumeEdge cfaEdge,
      CExpression expression, boolean truthAssumption)
      throws CPATransferException {
    if(truthAssumption) {
      ASTree tree = new ASTree(new ASTNode(extractControlLabel(cfaEdge)));
      ASTree assumeExpTree = expression.accept(new CExpressionASTVisitor(cfaEdge));
      tree.addTree(assumeExpTree);
      ASTCollectorState state = new ASTCollectorState(cfaEdge, tree, expression.accept(
          new CVariablesCollectingVisitor(cfaEdge.getPredecessor())), truthAssumption);
      return state;
    }
    return this.getState();
  }

  @Override
  protected ASTCollectorState handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {
    ASTree
        tree = new ASTree(new ASTNode(ASTNodeLabel.FUNC_CALL));
    ASTNode root = tree.getRoot();
    for(String key : SPECIAL_FUNCTIONS.keySet()) {
      if(calledFunctionName.startsWith(key))
        root.addLabel(SPECIAL_FUNCTIONS.get(key));
    }
    if(arguments.size() > 0) {
      ASTree
          argsTree = new ASTree(new ASTNode(ASTNodeLabel.ARGUMENTS));
      for (CExpression arg : arguments) {
        argsTree.addTree(arg.accept(new CExpressionASTVisitor(cfaEdge)));
      }
      tree.addTree(argsTree);
    }
    Set<String> vars = new HashSet<>();
    for(CExpression exp : arguments)
      vars.addAll(exp.accept(new CVariablesCollectingVisitor(cfaEdge.getPredecessor())));
    return new ASTCollectorState(cfaEdge, tree);
  }

  @Override
  protected ASTCollectorState handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr,
      String callerFunctionName) throws CPATransferException {
    return new ASTCollectorState(cfaEdge, new ASTree(new ASTNode(ASTNodeLabel.RETURN)));
  }

  @Override
  protected ASTCollectorState handleDeclarationEdge(CDeclarationEdge cfaEdge,
      CDeclaration decl) throws CPATransferException {
    ASTree
        tree = new ASTree(new ASTNode(ASTNodeLabel.DECL));
    ASTNode root = tree.getRoot();
    if(decl.isGlobal())
      root.addLabel(ASTNodeLabel.GLOBAL);
    ASTree
        declTree = decl.accept(new CSimpleDeclASTVisitor(cfaEdge));
    tree.addTree(declTree);
    return new ASTCollectorState(cfaEdge, tree);
  }

  @Override
  protected ASTCollectorState handleStatementEdge(CStatementEdge cfaEdge,
      CStatement statement) throws CPATransferException {
    ASTree
        tree = statement.accept(new CStatementASTVisitor(cfaEdge));
    return new ASTCollectorState(cfaEdge, tree,
        statement.accept(new CStatementVariablesCollectingVisitor(cfaEdge.getPredecessor())));
  }

  @Override
  protected ASTCollectorState handleReturnStatementEdge(
      CReturnStatementEdge cfaEdge) throws CPATransferException {
    return new ASTCollectorState(cfaEdge, new ASTree(new ASTNode(ASTNodeLabel.RETURN)));
  }

  @Override
  protected ASTCollectorState handleBlankEdge(BlankEdge cfaEdge) {
    ASTree
        blankTree = new ASTree(new ASTNode(ASTNodeLabel.BLANK));
    return new ASTCollectorState(cfaEdge, blankTree);
  }

  @Override
  protected ASTCollectorState handleFunctionSummaryEdge(
      CFunctionSummaryEdge cfaEdge) throws CPATransferException {
    throw new UnsupportedCodeException("SummaryEdge", cfaEdge);
  }
}
