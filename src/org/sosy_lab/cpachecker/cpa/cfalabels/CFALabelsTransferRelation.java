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

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

//  @Override
//  protected CFALabelsState handleAssumption(CAssumeEdge cfaEdge,
//      CExpression expression, boolean truthAssumption)
//      throws CPATransferException {
//    Set<GMNodeLabel> labels = Sets.newHashSet(truthAssumption ? GMNodeLabel.ASSUME_TRUE : GMNodeLabel.ASSUME_FALSE);
//    CExpressionLabelVisitor expLabelVisitor = new CExpressionLabelVisitor(cfaEdge);
//    return state.addEdgeLabel(cfaEdge, Sets.union(labels, expression.accept(expLabelVisitor)));
//  }
//
//  @Override
//  protected CFALabelsState handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
//      List<CExpression> arguments, List<CParameterDeclaration> parameters,
//      String calledFunctionName) throws CPATransferException {
//    Set<GMNodeLabel> labels = Sets.newHashSet(GMNodeLabel.FUNC_CALL);
//    if(SPECIAL_FUNCTIONS.containsKey(calledFunctionName)) {
//      labels.add(SPECIAL_FUNCTIONS.get(calledFunctionName));
//    }
//    for(String key : SPECIAL_FUNCTIONS.keySet()) {
//      if(calledFunctionName.startsWith(key))
//        labels.add(SPECIAL_FUNCTIONS.get(key));
//    }
//    for(CExpression arg : arguments) {
//      CExpressionLabelVisitor expLabelVisitor = new CExpressionLabelVisitor(cfaEdge);
//      labels.addAll(arg.accept(expLabelVisitor));
//    }
//    return state.addEdgeLabel(cfaEdge, Sets.immutableEnumSet(labels));
//  }
//
//  @Override
//  protected CFALabelsState handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
//      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr,
//      String callerFunctionName) throws CPATransferException {
//    return state.addEdgeLabel(cfaEdge, Sets.immutableEnumSet(GMNodeLabel.FUNC_RETURN));
//  }

  @Override
  protected CFALabelsState handleDeclarationEdge(CDeclarationEdge cfaEdge,
      CDeclaration decl) throws CPATransferException {
    CSimpleDeclLabelVisitor declVisitor = new CSimpleDeclLabelVisitor(cfaEdge);
    Set<GMNodeLabel> labels = Sets.newHashSet(GMNodeLabel.DECL);
    return state.addEdgeLabel(cfaEdge, Sets.union(labels, decl.accept(declVisitor)));
  }
//
//  @Override
//  protected CFALabelsState handleStatementEdge(CStatementEdge cfaEdge,
//      CStatement statement) throws CPATransferException {
//    CStatementLabelVisitor statementLabelVisitor = new CStatementLabelVisitor(cfaEdge);
//    Set<GMNodeLabel> labels = Sets.newHashSet();
//    return state.addEdgeLabel(cfaEdge, Sets.union(labels,
//        statement.accept(statementLabelVisitor)));
//  }

//  @Override
//  protected CFALabelsState handleReturnStatementEdge(
//      CReturnStatementEdge cfaEdge) throws CPATransferException {
//    return state.addEdgeLabel(cfaEdge, Sets.immutableEnumSet(GMNodeLabel.RETURN));
//  }
//
//  @Override
//  protected CFALabelsState handleBlankEdge(BlankEdge cfaEdge) {
//    return state.addEdgeLabel(cfaEdge, Sets.immutableEnumSet(GMNodeLabel.BLANK));
//  }

  @Override
  protected CFALabelsState handleFunctionSummaryEdge(
      CFunctionSummaryEdge cfaEdge) throws CPATransferException {
    throw new UnsupportedCodeException("SummaryEdge", cfaEdge);
  }
}
