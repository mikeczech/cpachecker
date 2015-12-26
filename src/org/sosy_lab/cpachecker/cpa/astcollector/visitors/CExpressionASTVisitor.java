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
import org.sosy_lab.cpachecker.cpa.astcollector.ASTNode;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTNodeLabel;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTree;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

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

  // TODO put this in one file
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

  @Override
  public ASTree visit(CBinaryExpression pIastBinaryExpression)
      throws CPATransferException {

    ASTree tree = new ASTree(new ASTNode());
    ASTNode root = tree.getRoot();

    switch(pIastBinaryExpression.getOperator()) {
      case MULTIPLY:
        root.addLabel(ASTNodeLabel.MULTIPLY);
        break;
      case DIVIDE:
        root.addLabel(ASTNodeLabel.DIVIDE);
        break;
      case PLUS:
        root.addLabel(ASTNodeLabel.PLUS);
        break;
      case MINUS:
        root.addLabel(ASTNodeLabel.MINUS);
        break;
      case EQUALS:
        root.addLabel(ASTNodeLabel.EQUALS);
        break;
      case NOT_EQUALS:
        root.addLabel(ASTNodeLabel.NOT_EQUALS);
        break;
      case LESS_THAN:
        root.addLabel(ASTNodeLabel.LESS_THAN);
        break;
      case GREATER_THAN:
        root.addLabel(ASTNodeLabel.GREATER_THAN);
        break;
      case LESS_EQUAL:
        root.addLabel(ASTNodeLabel.LESS_EQUAL);
        break;
      case GREATER_EQUAL:
        root.addLabel(ASTNodeLabel.GREATER_EQUAL);
        break;
      case BINARY_AND:
        root.addLabel(ASTNodeLabel.BINARY_AND);
        break;
      case BINARY_XOR:
        root.addLabel(ASTNodeLabel.BINARY_XOR);
        break;
      case BINARY_OR:
        root.addLabel(ASTNodeLabel.BINARY_OR);
        break;
      case SHIFT_LEFT:
        root.addLabel(ASTNodeLabel.SHIFT_LEFT);
        break;
      case SHIFT_RIGHT:
        root.addLabel(ASTNodeLabel.SHIFT_RIGHT);
        break;
      case MODULO:
        root.addLabel(ASTNodeLabel.MODULO);
        break;
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
    ASTree tree = new ASTree(new ASTNode());
    ASTNode root = tree.getRoot();
    switch(pIastUnaryExpression.getOperator()) {
      case MINUS:
        root.addLabel(ASTNodeLabel.MINUS);
        break;
      case AMPER:
        root.addLabel(ASTNodeLabel.AMPER);
        break;
      case TILDE:
        root.addLabel(ASTNodeLabel.TILDE);
        break;
      case SIZEOF:
        root.addLabel(ASTNodeLabel.SIZEOF);
        break;
      case ALIGNOF:
        root.addLabel(ASTNodeLabel.ALIGNOF);
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
    ASTree tree = new ASTree(new ASTNode());
    ASTNode root = tree.getRoot();
    if(pIastFieldReference.isPointerDereference())
      root.addLabel(ASTNodeLabel.FIELD_POINTER_DEREF);
    else
      root.addLabel(ASTNodeLabel.FIELD_REF);
    ASTree ownerTree = pIastFieldReference.getFieldOwner().accept(this);
    tree.addTree(ownerTree);
    return tree;
  }

  @Override
  public ASTree visit(CIdExpression pIastIdExpression)
      throws CPATransferException {
    if(SPECIAL_FUNCTIONS.containsKey(pIastIdExpression.getName())) {
      return new ASTree(new ASTNode(SPECIAL_FUNCTIONS.get(pIastIdExpression.getName())));
    }
    for(String key : SPECIAL_FUNCTIONS.keySet()) {
      if(pIastIdExpression.getName().startsWith(key))
        return new ASTree(new ASTNode(SPECIAL_FUNCTIONS.get(key)));
    }
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
