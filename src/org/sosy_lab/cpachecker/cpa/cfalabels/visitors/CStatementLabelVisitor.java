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
package org.sosy_lab.cpachecker.cpa.cfalabels.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.cfalabels.CFAEdgeLabel;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.Sets;

/**
 * Created by zenscr on 01/10/15.
 */
public class CStatementLabelVisitor implements CStatementVisitor<Set<CFAEdgeLabel>, CPATransferException> {

  private final CFAEdge cfaEdge;

  public CStatementLabelVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  @Override
  public Set<CFAEdgeLabel> visit(CExpressionStatement pIastExpressionStatement)
      throws CPATransferException {
    CExpressionLabelVisitor expLabelVisitor = new CExpressionLabelVisitor(this.cfaEdge);
    return pIastExpressionStatement.getExpression().accept(expLabelVisitor);
  }

  @Override
  public Set<CFAEdgeLabel> visit(
      CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
      throws CPATransferException {
    Set<CFAEdgeLabel> labels = Sets.newHashSet(CFAEdgeLabel.ASSIGN);
    CExpressionLabelVisitor leftExpLabelVisitor  = new CExpressionLabelVisitor(this.cfaEdge);
    CExpressionLabelVisitor rightExpLabelVisitor = new CExpressionLabelVisitor(this.cfaEdge);
    Set<CFAEdgeLabel> leftExpLabels = pIastExpressionAssignmentStatement.getLeftHandSide().accept(leftExpLabelVisitor);
    Set<CFAEdgeLabel> rightExpLabels = pIastExpressionAssignmentStatement.getRightHandSide().accept(
        rightExpLabelVisitor);
    labels.addAll(leftExpLabels);
    labels.addAll(rightExpLabels);
    return Sets.immutableEnumSet(labels);
  }

  @Override public Set<CFAEdgeLabel> visit(
      CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
      throws CPATransferException {
    Set<CFAEdgeLabel> edgeLabels = Sets.newHashSet(CFAEdgeLabel.ASSIGN, CFAEdgeLabel.FUNC_CALL);
    CExpressionLabelVisitor leftExpLabelVisitor  = new CExpressionLabelVisitor(this.cfaEdge);
    edgeLabels.addAll(pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(leftExpLabelVisitor));
    CExpressionLabelVisitor nameExpLabelVisitor  = new CExpressionLabelVisitor(this.cfaEdge);
    edgeLabels.addAll(pIastFunctionCallAssignmentStatement.getFunctionCallExpression().getFunctionNameExpression().accept(nameExpLabelVisitor));
    return Sets.immutableEnumSet(edgeLabels);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CFunctionCallStatement pIastFunctionCallStatement)
      throws CPATransferException {
    Set<CFAEdgeLabel> labels = Sets.newHashSet(CFAEdgeLabel.FUNC_CALL);
    CExpressionLabelVisitor expLabelVisitor = new CExpressionLabelVisitor(cfaEdge);
    labels.addAll(pIastFunctionCallStatement.getFunctionCallExpression().getFunctionNameExpression().accept(expLabelVisitor));
    // add labels for arguments as well
    for(CExpression paramExp : pIastFunctionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
      CExpressionLabelVisitor paramExpLabelVisitor = new CExpressionLabelVisitor(cfaEdge);
      labels.addAll(paramExp.accept(paramExpLabelVisitor));
    }
    return Sets.immutableEnumSet(labels);
  }
}
