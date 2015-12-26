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
package org.sosy_lab.cpachecker.cpa.astcollector.visitors;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTNode;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTNodeLabel;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTree;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Created by zenscr on 01/10/15.
 */
public class CStatementASTVisitor implements CStatementVisitor<ASTree, CPATransferException> {

  private final CFAEdge cfaEdge;

  public CStatementASTVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  @Override
  public ASTree visit(CExpressionStatement pIastExpressionStatement)
      throws CPATransferException {
    return pIastExpressionStatement.getExpression().accept(
        new CExpressionASTVisitor(this.cfaEdge));
  }

  @Override
  public ASTree visit(
      CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.ASSIGNMENT));
    ASTree leftTree = pIastExpressionAssignmentStatement.getLeftHandSide().accept(
        new CExpressionASTVisitor(this.cfaEdge));
    tree.addTree(leftTree);
    ASTree rightTree = pIastExpressionAssignmentStatement.getRightHandSide().accept(
        new CExpressionASTVisitor(this.cfaEdge));
    tree.addTree(rightTree);
    return tree;
  }

  @Override public ASTree visit(
      CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.FUNC_CALL_ASSIGN));
    ASTree leftTree = pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(
        new CExpressionASTVisitor(this.cfaEdge));
    tree.addTree(leftTree);
    if(pIastFunctionCallAssignmentStatement.getRightHandSide().getParameterExpressions().size() > 0) {
      ASTree paramsTree = new ASTree(new ASTNode(
          ASTNodeLabel.PARAMS));
      for(CExpression paramExp : pIastFunctionCallAssignmentStatement.getRightHandSide().getParameterExpressions()) {
        ASTree paramExpTree = paramExp.accept(new CExpressionASTVisitor(this.cfaEdge));
        paramsTree.addTree(paramExpTree);
      }
      tree.addTree(paramsTree);
    }
    return tree;
  }

  @Override
  public ASTree visit(CFunctionCallStatement pIastFunctionCallStatement)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.FUNC_CALL));
    // add labels for arguments as well
    if(pIastFunctionCallStatement.getFunctionCallExpression().getParameterExpressions().size() > 0) {
      ASTree paramsTree = new ASTree(new ASTNode(
          ASTNodeLabel.PARAMS));
      for (CExpression paramExp : pIastFunctionCallStatement
          .getFunctionCallExpression().getParameterExpressions()) {
        ASTree paramExpTree =
            paramExp.accept(new CExpressionASTVisitor(this.cfaEdge));
        paramsTree.addTree(paramExpTree);
      }
      tree.addTree(paramsTree);
    }
    return tree;
  }
}
