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

import java.math.BigInteger;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTCollectorUtils;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTNode;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTNodeLabel;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTree;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Created by zenscr on 30/09/15.
 */
public class CExpressionASTVisitor implements CExpressionVisitor<ASTree, CPATransferException> {

  private CFAEdge cfaEdge;

  public CExpressionASTVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  @Override
  public ASTree visit(CBinaryExpression pIastBinaryExpression)
      throws CPATransferException {

    ASTree tree;
    switch(pIastBinaryExpression.getOperator()) {
      case MULTIPLY:
        tree = new ASTree(new ASTNode(ASTNodeLabel.MULTIPLY));
        break;
      case DIVIDE:
        tree = new ASTree(new ASTNode(ASTNodeLabel.DIVIDE));
        break;
      case PLUS:
        tree = new ASTree(new ASTNode(ASTNodeLabel.PLUS));
        break;
      case MINUS:
        tree = new ASTree(new ASTNode(ASTNodeLabel.MINUS));
        break;
      case EQUALS:
        tree = new ASTree(new ASTNode(ASTNodeLabel.EQUALS));
        break;
      case NOT_EQUALS:
        tree = new ASTree(new ASTNode(ASTNodeLabel.NOT_EQUALS));
        break;
      case LESS_THAN:
        tree = new ASTree(new ASTNode(ASTNodeLabel.LESS_THAN));
        break;
      case GREATER_THAN:
        tree = new ASTree(new ASTNode(ASTNodeLabel.GREATER_THAN));
        break;
      case LESS_EQUAL:
        tree = new ASTree(new ASTNode(ASTNodeLabel.LESS_EQUAL));
        break;
      case GREATER_EQUAL:
        tree = new ASTree(new ASTNode(ASTNodeLabel.GREATER_EQUAL));
        break;
      case BINARY_AND:
        tree = new ASTree(new ASTNode(ASTNodeLabel.BINARY_AND));
        break;
      case BINARY_XOR:
        tree = new ASTree(new ASTNode(ASTNodeLabel.BINARY_XOR));
        break;
      case BINARY_OR:
        tree = new ASTree(new ASTNode(ASTNodeLabel.BINARY_OR));
        break;
      case SHIFT_LEFT:
        tree = new ASTree(new ASTNode(ASTNodeLabel.SHIFT_LEFT));
        break;
      case SHIFT_RIGHT:
        tree = new ASTree(new ASTNode(ASTNodeLabel.SHIFT_RIGHT));
        break;
      case MODULO:
        tree = new ASTree(new ASTNode(ASTNodeLabel.MODULO));
        break;
      default:
        throw new UnsupportedCCodeException("Unknown operator", this.cfaEdge);
    }
    ASTree leftExpTree = pIastBinaryExpression.getOperand1().accept(this);
    ASTree rightExpTree = pIastBinaryExpression.getOperand2().accept(this);
    tree.addTree(leftExpTree);
    tree.addTree(rightExpTree);

    return tree;
  }

  @Override
  public ASTree visit(CCastExpression pIastCastExpression)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.CAST_EXPRESSION));
    ASTree castTypeTree = pIastCastExpression.getCastType().accept(new CTypeASTVisitor(this.cfaEdge));
    tree.addTree(castTypeTree, new ASTNode(
        ASTNodeLabel.CAST_TYPE));
    ASTree operandTree = pIastCastExpression.getOperand().accept(this);
    tree.addTree(operandTree, new ASTNode(
        ASTNodeLabel.OPERAND));
    return tree;
  }

  @Override
  public ASTree visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws CPATransferException {
    return new ASTree(new ASTNode(
        ASTNodeLabel.CHAR_LITERAL));
  }

  @Override
  public ASTree visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws CPATransferException {
    return new ASTree(new ASTNode(
        ASTNodeLabel.FLOAT_LITERAL));
  }

  @Override
  public ASTree visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws CPATransferException {
    BigInteger value = pIastIntegerLiteralExpression.getValue();
    if(value.compareTo(new BigInteger("256")) == -1)
      return new ASTree(new ASTNode(
          ASTNodeLabel.INT_LITERAL_SMALL));
    if(value.compareTo(new BigInteger("1024")) == -1)
      return new ASTree(new ASTNode(
          ASTNodeLabel.INT_LITERAL_MEDIUM));
    return new ASTree(new ASTNode(
        ASTNodeLabel.INT_LITERAL_LARGE));
  }

  @Override
  public ASTree visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws CPATransferException {
    return new ASTree(new ASTNode(
        ASTNodeLabel.STRING_LITERAL));
  }

  @Override
  public ASTree visit(CTypeIdExpression pIastTypeIdExpression)
      throws CPATransferException {
    return new ASTree(new ASTNode(
        ASTNodeLabel.VARIABLE_ID));
  }

  @Override
  public ASTree visit(CUnaryExpression pIastUnaryExpression)
      throws CPATransferException {
    ASTree tree;
    switch(pIastUnaryExpression.getOperator()) {
      case MINUS:
        tree = new ASTree((new ASTNode(ASTNodeLabel.MINUS)));
        break;
      case AMPER:
        tree = new ASTree((new ASTNode(ASTNodeLabel.AMPER)));
        break;
      case TILDE:
        tree = new ASTree((new ASTNode(ASTNodeLabel.TILDE)));
        break;
      case SIZEOF:
        tree = new ASTree((new ASTNode(ASTNodeLabel.SIZEOF)));
        break;
      case ALIGNOF:
        tree = new ASTree((new ASTNode(ASTNodeLabel.ALIGNOF)));
        break;
      default:
        throw new UnsupportedCCodeException("Unknown operator", this.cfaEdge);
    }
    ASTree operandTree = pIastUnaryExpression.getOperand().accept(this);
    tree.addTree(operandTree);
    return tree;
  }

  @Override
  public ASTree visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws CPATransferException {
    return new ASTree(new ASTNode(
        ASTNodeLabel.COMPLEX_LITERAL));
  }

  @Override
  public ASTree visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws CPATransferException {
    return new ASTree(new ASTNode(
        ASTNodeLabel.LABEL_ADDRESS));
  }

  @Override
  public ASTree visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.ARRAY_SUBSCRIPT_EXPRESSION));
    ASTree arrayExp = pIastArraySubscriptExpression.getArrayExpression().accept(this);
    tree.addTree(arrayExp, new ASTNode(
        ASTNodeLabel.ARRAY_EXPRESSION));
    ASTree subscriptExp = pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
    tree.addTree(subscriptExp, new ASTNode(
        ASTNodeLabel.SUBSCRIPT_EXPRESSION));
    return tree;
  }

  @Override
  public ASTree visit(CFieldReference pIastFieldReference)
      throws CPATransferException {
    ASTree tree;
    if(pIastFieldReference.isPointerDereference())
      tree = new ASTree(new ASTNode(ASTNodeLabel.FIELD_POINTER_DEREF));
    else
      tree = new ASTree(new ASTNode(ASTNodeLabel.FIELD_REF));
    ASTree ownerTree = pIastFieldReference.getFieldOwner().accept(this);
    tree.addTree(ownerTree);
    return tree;
  }

  @Override
  public ASTree visit(CIdExpression pIastIdExpression)
      throws CPATransferException {
    Optional<ASTNodeLabel> specialLabel = ASTCollectorUtils.getSpecialLabel(pIastIdExpression.getName());
    if(specialLabel.isPresent())
       return new ASTree(new ASTNode(specialLabel.get()));
    return new ASTree(new ASTNode(
        ASTNodeLabel.VARIABLE_ID));
  }

  @Override
  public ASTree visit(CPointerExpression pointerExpression)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.POINTER_EXPRESSION));
    ASTree operandTree = pointerExpression.getOperand().accept(this);
    tree.addTree(operandTree);
    return tree;
  }

  @Override
  public ASTree visit(CComplexCastExpression complexCastExpression)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified expression", this.cfaEdge);
  }
}
