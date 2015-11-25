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

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
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
import org.sosy_lab.cpachecker.cpa.cfalabels.ASTree;
import org.sosy_lab.cpachecker.cpa.cfalabels.GMNode;
import org.sosy_lab.cpachecker.cpa.cfalabels.GMNodeLabel;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Sets;

/**
 * Created by zenscr on 30/09/15.
 */
public class CExpressionLabelVisitor implements CExpressionVisitor<ASTree, CPATransferException> {

  private CFAEdge cfaEdge;

  public CExpressionLabelVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  // TODO put this in one file
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
  public ASTree visit(CBinaryExpression pIastBinaryExpression)
      throws CPATransferException {

    ASTree tree = new ASTree(new GMNode());
    GMNode root = tree.getRoot();

    switch(pIastBinaryExpression.getOperator()) {
      case MULTIPLY:
        root.addLabel(GMNodeLabel.MULTIPLY);
        break;
      case DIVIDE:
        root.addLabel(GMNodeLabel.DIVIDE);
        break;
      case PLUS:
        root.addLabel(GMNodeLabel.PLUS);
        break;
      case MINUS:
        root.addLabel(GMNodeLabel.MINUS);
        break;
      case EQUALS:
        root.addLabel(GMNodeLabel.EQUALS);
        break;
      case NOT_EQUALS:
        root.addLabel(GMNodeLabel.NOT_EQUALS);
        break;
      case LESS_THAN:
        root.addLabel(GMNodeLabel.LESS_THAN);
        break;
      case GREATER_THAN:
        root.addLabel(GMNodeLabel.GREATER_THAN);
        break;
      case LESS_EQUAL:
        root.addLabel(GMNodeLabel.LESS_EQUAL);
        break;
      case GREATER_EQUAL:
        root.addLabel(GMNodeLabel.GREATER_EQUAL);
        break;
      case BINARY_AND:
        root.addLabel(GMNodeLabel.BINARY_AND);
        break;
      case BINARY_XOR:
        root.addLabel(GMNodeLabel.BINARY_XOR);
        break;
      case BINARY_OR:
        root.addLabel(GMNodeLabel.BINARY_OR);
        break;
      case SHIFT_LEFT:
        root.addLabel(GMNodeLabel.SHIFT_LEFT);
        break;
      case SHIFT_RIGHT:
        root.addLabel(GMNodeLabel.SHIFT_RIGHT);
        break;
      case MODULO:
        root.addLabel(GMNodeLabel.MODULO);
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
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.CAST_EXPRESSION));
    ASTree castTypeTree = pIastCastExpression.getCastType().accept(new CTypeLabelVisitor(this.cfaEdge));
    tree.addTree(castTypeTree, new GMNode(GMNodeLabel.CAST_TYPE));
    ASTree operandTree = pIastCastExpression.getOperand().accept(this);
    tree.addTree(operandTree, new GMNode(GMNodeLabel.OPERAND));
    return tree;
  }

  @Override
  public ASTree visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws CPATransferException {
    return new ASTree(new GMNode(GMNodeLabel.CHAR_LITERAL));
  }

  @Override
  public ASTree visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws CPATransferException {
    return new ASTree(new GMNode(GMNodeLabel.FLOAT_LITERAL));
  }

  @Override
  public ASTree visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws CPATransferException {
    BigInteger value = pIastIntegerLiteralExpression.getValue();
    if(value.compareTo(new BigInteger("256")) == -1)
      return new ASTree(new GMNode(GMNodeLabel.INT_LITERAL_SMALL));
    if(value.compareTo(new BigInteger("1024")) == -1)
      return new ASTree(new GMNode(GMNodeLabel.INT_LITERAL_MEDIUM));
    return new ASTree(new GMNode(GMNodeLabel.INT_LITERAL_LARGE));
  }

  @Override
  public ASTree visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws CPATransferException {
    return new ASTree(new GMNode(GMNodeLabel.STRING_LITERAL));
  }

  @Override
  public ASTree visit(CTypeIdExpression pIastTypeIdExpression)
      throws CPATransferException {
    return new ASTree(new GMNode(GMNodeLabel.VARIABLE_ID));
  }

  @Override
  public ASTree visit(CUnaryExpression pIastUnaryExpression)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode());
    GMNode root = tree.getRoot();
    switch(pIastUnaryExpression.getOperator()) {
      case MINUS:
        root.addLabel(GMNodeLabel.MINUS);
        break;
      case AMPER:
        root.addLabel(GMNodeLabel.AMPER);
        break;
      case TILDE:
        root.addLabel(GMNodeLabel.TILDE);
        break;
      case SIZEOF:
        root.addLabel(GMNodeLabel.SIZEOF);
        break;
      case ALIGNOF:
        root.addLabel(GMNodeLabel.ALIGNOF);
    }
    ASTree operandTree = pIastUnaryExpression.getOperand().accept(this);
    tree.addTree(operandTree);
    return tree;
  }

  @Override
  public ASTree visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws CPATransferException {
    return new ASTree(new GMNode(GMNodeLabel.COMPLEX_LITERAL));
  }

  @Override
  public ASTree visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws CPATransferException {
    return new ASTree(new GMNode(GMNodeLabel.LABEL_ADDRESS));
  }

  @Override
  public ASTree visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.ARRAY_SUBSCRIPT_EXPRESSION));
    ASTree arrayExp = pIastArraySubscriptExpression.getArrayExpression().accept(this);
    tree.addTree(arrayExp, new GMNode(GMNodeLabel.ARRAY_EXPRESSION));
    ASTree subscriptExp = pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
    tree.addTree(subscriptExp, new GMNode(GMNodeLabel.SUBSCRIPT_EXPRESSION));
    return tree;
  }

  @Override
  public ASTree visit(CFieldReference pIastFieldReference)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode());
    GMNode root = tree.getRoot();
    if(pIastFieldReference.isPointerDereference())
      root.addLabel(GMNodeLabel.FIELD_POINTER_DEREF);
    else
      root.addLabel(GMNodeLabel.FIELD_REF);
    ASTree ownerTree = pIastFieldReference.getFieldOwner().accept(this);
    tree.addTree(ownerTree);
    return tree;
  }

  @Override
  public ASTree visit(CIdExpression pIastIdExpression)
      throws CPATransferException {
    if(SPECIAL_FUNCTIONS.containsKey(pIastIdExpression.getName())) {
      return new ASTree(new GMNode(SPECIAL_FUNCTIONS.get(pIastIdExpression.getName())));
    }
    for(String key : SPECIAL_FUNCTIONS.keySet()) {
      if(pIastIdExpression.getName().startsWith(key))
        return new ASTree(new GMNode(SPECIAL_FUNCTIONS.get(key)));
    }
    return new ASTree(new GMNode(GMNodeLabel.VARIABLE_ID));
  }

  @Override
  public ASTree visit(CPointerExpression pointerExpression)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.POINTER_EXPRESSION));
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
