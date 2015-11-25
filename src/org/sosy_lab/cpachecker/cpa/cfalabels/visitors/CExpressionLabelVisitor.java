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

import java.util.Map;
import java.util.Set;

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
    ASTree labels = Sets.newHashSet();
    switch(pIastBinaryExpression.getOperator()) {
      case MULTIPLY:
      case DIVIDE:
      case PLUS:
      case MINUS:
        labels.add(GMNodeLabel.ARITHMETIC);
        break;
      case EQUALS:
      case NOT_EQUALS:
      case LESS_THAN:
      case GREATER_THAN:
      case LESS_EQUAL:
      case GREATER_EQUAL:
        labels.add(GMNodeLabel.COMPARISON);
        break;
      case BINARY_AND:
      case BINARY_XOR:
      case BINARY_OR:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
        labels.add(GMNodeLabel.BIT_OPERATION);
        break;
      case MODULO:
        labels.add(GMNodeLabel.MODULO);
        break;
    }
    labels.addAll(pIastBinaryExpression.getOperand1().accept(this));
    labels.addAll(pIastBinaryExpression.getOperand2().accept(this));
    return Sets.immutableEnumSet(labels);
  }

  @Override
  public ASTree visit(CCastExpression pIastCastExpression)
      throws CPATransferException {
    ASTree labels = Sets.newHashSet();
    labels.add(GMNodeLabel.CAST);
    CTypeLabelVisitor typeLabelVisitor = new CTypeLabelVisitor(this.cfaEdge);
    labels.addAll(pIastCastExpression.getCastType().accept(typeLabelVisitor));
    labels.addAll(pIastCastExpression.getOperand().accept(this));
    return Sets.immutableEnumSet(labels);
  }

  @Override
  public ASTree visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws CPATransferException {
    return Sets.immutableEnumSet(GMNodeLabel.LITERAL);
  }

  @Override
  public ASTree visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws CPATransferException {
    return Sets.immutableEnumSet(GMNodeLabel.LITERAL);
  }

  @Override
  public ASTree visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws CPATransferException {
    return Sets.immutableEnumSet(GMNodeLabel.LITERAL);
  }

  @Override
  public ASTree visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws CPATransferException {
    return Sets.immutableEnumSet(GMNodeLabel.LITERAL);
  }

  @Override
  public ASTree visit(CTypeIdExpression pIastTypeIdExpression)
      throws CPATransferException {
    return Sets.immutableEnumSet(GMNodeLabel.TYPE_ID);
  }

  @Override
  public ASTree visit(CUnaryExpression pIastUnaryExpression)
      throws CPATransferException {
    ASTree labels = Sets.newHashSet();
    switch(pIastUnaryExpression.getOperator()) {
      case MINUS:
        labels.add(GMNodeLabel.ARITHMETIC);
        break;
      case AMPER:
        labels.add(GMNodeLabel.ADDRESS);
        break;
      case TILDE:
        labels.add(GMNodeLabel.BIT_OPERATION);
        break;
      case SIZEOF:
        labels.add(GMNodeLabel.SIZEOF);
        break;
      case ALIGNOF:
        throw new UnsupportedCCodeException("ALIGNOF is not supported", this.cfaEdge);
    }
    return Sets.union(labels, pIastUnaryExpression.getOperand().accept(this));
  }

  @Override
  public ASTree visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws CPATransferException {
    return Sets.immutableEnumSet(GMNodeLabel.COMPLEX_NUMBER);
  }

  @Override
  public ASTree visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws CPATransferException {
    return Sets.immutableEnumSet(GMNodeLabel.ADDRESS);
  }

  @Override
  public ASTree visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws CPATransferException {
    return Sets.immutableEnumSet(GMNodeLabel.ARRAY_SUBSCRIPT);
  }

  @Override
  public ASTree visit(CFieldReference pIastFieldReference)
      throws CPATransferException {
    return Sets.immutableEnumSet(GMNodeLabel.FIELD_REFERENCE);
  }

  @Override
  public ASTree visit(CIdExpression pIastIdExpression)
      throws CPATransferException {
    if(SPECIAL_FUNCTIONS.containsKey(pIastIdExpression.getName())) {
      return Sets.immutableEnumSet(SPECIAL_FUNCTIONS.get(pIastIdExpression.getName()));
    }
    for(String key : SPECIAL_FUNCTIONS.keySet()) {
      if(pIastIdExpression.getName().startsWith(key))
        return Sets.immutableEnumSet(SPECIAL_FUNCTIONS.get(key));
    }
    return Sets.immutableEnumSet(GMNodeLabel.ID);
  }

  @Override
  public ASTree visit(CPointerExpression pointerExpression)
      throws CPATransferException {
    return Sets.union(Sets.immutableEnumSet(GMNodeLabel.PTR), pointerExpression.getOperand().accept(this));
  }

  @Override
  public ASTree visit(CComplexCastExpression complexCastExpression)
      throws CPATransferException {
    return Sets.immutableEnumSet(GMNodeLabel.COMPLEX_NUMBER);
  }
}
