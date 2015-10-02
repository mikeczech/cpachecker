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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

import com.google.common.collect.Sets;


public class CFALabelsTransferRelation extends ForwardingTransferRelation<CFALabelsState, CFALabelsState, SingletonPrecision> {

  LogManager logger;

  public CFALabelsTransferRelation(LogManager pLogger) {
    logger = pLogger;
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
    Set<CFAEdgeLabel> labels = Sets.newHashSet(CFAEdgeLabel.ASSUME);
    CExpressionLabelVisitor expLabelVisitor = new CExpressionLabelVisitor(cfaEdge);
    return state.addEdgeLabel(cfaEdge, Sets.union(labels, expression.accept(expLabelVisitor)));
  }

  @Override
  protected CFALabelsState handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {
    if(calledFunctionName.equals("__VERIFIER_assert") || calledFunctionName.equals("assert")) {
      Set<CFAEdgeLabel> labels = Sets.newHashSet(CFAEdgeLabel.VERIFIER_ASSERT_CALL_ID);
      for(CExpression arg : arguments) {
        CExpressionLabelVisitor expLabelVisitor = new CExpressionLabelVisitor(cfaEdge);
        labels.addAll(arg.accept(expLabelVisitor));
      }
      return state.addEdgeLabel(cfaEdge, Sets.immutableEnumSet(labels));
    }
    return state.addEdgeLabel(cfaEdge, Sets.immutableEnumSet(CFAEdgeLabel.FUNC_CALL));
  }

  @Override
  protected CFALabelsState handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr,
      String callerFunctionName) throws CPATransferException {
    return state.addEdgeLabel(cfaEdge, Sets.immutableEnumSet(CFAEdgeLabel.FUNC_RETURN));
  }

  @Override
  protected CFALabelsState handleDeclarationEdge(CDeclarationEdge cfaEdge,
      CDeclaration decl) throws CPATransferException {
    CSimpleDeclLabelVisitor declVisitor = new CSimpleDeclLabelVisitor(cfaEdge);
    Set<CFAEdgeLabel> labels = Sets.newHashSet(CFAEdgeLabel.DECL);
    return state.addEdgeLabel(cfaEdge, Sets.union(labels, decl.accept(declVisitor)));
  }

  @Override
  protected CFALabelsState handleStatementEdge(CStatementEdge cfaEdge,
      CStatement statement) throws CPATransferException {
    CStatementLabelVisitor statementLabelVisitor = new CStatementLabelVisitor(cfaEdge);
    Set<CFAEdgeLabel> labels = Sets.newHashSet(CFAEdgeLabel.STMT);
    return state.addEdgeLabel(cfaEdge, Sets.union(labels,
        statement.accept(statementLabelVisitor)));
  }

  @Override
  protected CFALabelsState handleReturnStatementEdge(
      CReturnStatementEdge cfaEdge) throws CPATransferException {
    return state.addEdgeLabel(cfaEdge, Sets.immutableEnumSet(CFAEdgeLabel.RETURN));
  }

  @Override
  protected CFALabelsState handleBlankEdge(BlankEdge cfaEdge) {
    return state.addEdgeLabel(cfaEdge, Sets.immutableEnumSet(CFAEdgeLabel.BLANK));
  }

  @Override
  protected CFALabelsState handleFunctionSummaryEdge(
      CFunctionSummaryEdge cfaEdge) throws CPATransferException {
    throw new UnsupportedCodeException("SummaryEdge", cfaEdge);
  }
}
