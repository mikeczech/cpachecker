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
package org.sosy_lab.cpachecker.cpa.cfalabels;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
import org.sosy_lab.cpachecker.cpa.cfalabels.visitors.CExpressionLabelVisitor;
import org.sosy_lab.cpachecker.cpa.cfalabels.visitors.CSimpleDeclLabelVisitor;
import org.sosy_lab.cpachecker.cpa.cfalabels.visitors.CStatementLabelVisitor;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Sets;


public class CFALabelsTransferRelation extends ForwardingTransferRelation<CFALabelsState, CFALabelsState, SingletonPrecision> {

  LogManager logger;

  public CFALabelsTransferRelation(LogManager pLogger) {
    logger = pLogger;
  }

  static final Map<String, GMNodeLabel> SPECIAL_FUNCTIONS;

  static {
    Builder<String, GMNodeLabel> builder = ImmutableMap.builder();
    builder.put("pthread_create", GMNodeLabel.PTHREAD);
    builder.put("pthread_exit", GMNodeLabel.PTHREAD);
    builder.put("__VERIFIER_error", GMNodeLabel.VERIFIER_ERROR);
    builder.put("__VERIFIER_assert", GMNodeLabel.VERIFIER_ASSERT);
    builder.put("__VERIFIER_assume", GMNodeLabel.VERIFIER_ASSUME);
    builder.put("__VERIFIER_atomic_begin", GMNodeLabel.VERIFIER_ATOMIC_BEGIN);
    builder.put("__VERIFIER_atomic_end", GMNodeLabel.VERIFIER_ATOMIC_END);
    builder.put("__VERIFIER_nondet", GMNodeLabel.INPUT);
    builder.put("malloc", GMNodeLabel.MALLOC);
    builder.put("free", GMNodeLabel.FREE);
    SPECIAL_FUNCTIONS = builder.build();
  }

  public static GMNodeLabel extractControlLabel(CFAEdge pCFAEdge) {
    if(pCFAEdge.getPredecessor().isLoopStart()) {
      return GMNodeLabel.LOOP;
    }
    return GMNodeLabel.BRANCH;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

  @Override
  protected CFALabelsState handleAssumption(CAssumeEdge cfaEdge,
      CExpression expression, boolean truthAssumption)
      throws CPATransferException {
    if(truthAssumption) {
      ASTree tree = new ASTree(new GMNode(extractControlLabel(cfaEdge)));
      ASTree assumeExpTree = expression.accept(new CExpressionLabelVisitor(cfaEdge));
      tree.addTree(assumeExpTree);
      return new CFALabelsState(cfaEdge, tree);
    }
    return state;
  }

  @Override
  protected CFALabelsState handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.FUNC_CALL));
    GMNode root = tree.getRoot();
    for(String key : SPECIAL_FUNCTIONS.keySet()) {
      if(calledFunctionName.startsWith(key))
        root.addLabel(SPECIAL_FUNCTIONS.get(key));
    }
    if(arguments.size() > 0) {
      ASTree argsTree = new ASTree(new GMNode(GMNodeLabel.ARGUMENTS));
      for (CExpression arg : arguments) {
        argsTree.addTree(arg.accept(new CExpressionLabelVisitor(cfaEdge)));
      }
      tree.addTree(argsTree);
    }
    return new CFALabelsState(cfaEdge, tree);
  }

  @Override
  protected CFALabelsState handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr,
      String callerFunctionName) throws CPATransferException {
    return new CFALabelsState(cfaEdge, new ASTree(new GMNode(GMNodeLabel.RETURN)));
  }

  @Override
  protected CFALabelsState handleDeclarationEdge(CDeclarationEdge cfaEdge,
      CDeclaration decl) throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.DECL));
    GMNode root = tree.getRoot();
    if(decl.isGlobal())
      root.addLabel(GMNodeLabel.GLOBAL);
    ASTree declTree = decl.accept(new CSimpleDeclLabelVisitor(cfaEdge));
    tree.addTree(declTree);
    return new CFALabelsState(cfaEdge, tree);
  }

  @Override
  protected CFALabelsState handleStatementEdge(CStatementEdge cfaEdge,
      CStatement statement) throws CPATransferException {
    ASTree tree = statement.accept(new CStatementLabelVisitor(cfaEdge));
    return new CFALabelsState(cfaEdge, tree);
  }

  @Override
  protected CFALabelsState handleReturnStatementEdge(
      CReturnStatementEdge cfaEdge) throws CPATransferException {
    return new CFALabelsState(cfaEdge, new ASTree(new GMNode(GMNodeLabel.RETURN)));
  }

  @Override
  protected CFALabelsState handleBlankEdge(BlankEdge cfaEdge) {
    return state;
  }

  @Override
  protected CFALabelsState handleFunctionSummaryEdge(
      CFunctionSummaryEdge cfaEdge) throws CPATransferException {
    throw new UnsupportedCodeException("SummaryEdge", cfaEdge);
  }
}
