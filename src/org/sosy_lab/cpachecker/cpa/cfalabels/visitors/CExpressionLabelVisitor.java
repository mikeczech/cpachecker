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
import org.sosy_lab.cpachecker.cpa.cfalabels.CFAEdgeLabel;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

/**
 * Created by zenscr on 30/09/15.
 */
public class CExpressionLabelVisitor implements CExpressionVisitor<Void, CPATransferException> {

  private final List<CFAEdgeLabel> labels = new ArrayList<>();

  private CFAEdge cfaEdge;

  public CExpressionLabelVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  @Override
  public Void visit(CBinaryExpression pIastBinaryExpression)
      throws CPATransferException {
    switch(pIastBinaryExpression.getOperator()) {
      case MULTIPLY:
      case DIVIDE:
      case MODULO:
      case PLUS:
      case MINUS:
        this.labels.add(CFAEdgeLabel.ARITHMETIC);
        break;
      case EQUALS:
      case NOT_EQUALS:
      case LESS_THAN:
      case GREATER_THAN:
      case LESS_EQUAL:
      case GREATER_EQUAL:
        this.labels.add(CFAEdgeLabel.COMPARISON);
        break;
      case BINARY_AND:
      case BINARY_XOR:
      case BINARY_OR:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
        this.labels.add(CFAEdgeLabel.BIT_OPERATION);
        break;
    }
    pIastBinaryExpression.getOperand1().accept(this);
    pIastBinaryExpression.getOperand2().accept(this);
    return null;
  }

  @Override
  public Void visit(CCastExpression pIastCastExpression)
      throws CPATransferException {
    this.labels.add(CFAEdgeLabel.CAST);
    pIastCastExpression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws CPATransferException {
    this.labels.add(CFAEdgeLabel.LITERAL);
    return null;
  }

  @Override
  public Void visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws CPATransferException {
    this.labels.add(CFAEdgeLabel.LITERAL);
    return null;
  }

  @Override
  public Void visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws CPATransferException {
    this.labels.add(CFAEdgeLabel.LITERAL);
    return null;
  }

  @Override
  public Void visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws CPATransferException {
    this.labels.add(CFAEdgeLabel.LITERAL);
    return null;
  }

  @Override
  public Void visit(CTypeIdExpression pIastTypeIdExpression)
      throws CPATransferException {
    this.labels.add(CFAEdgeLabel.TYPE_ID);
    return null;
  }

  @Override
  public Void visit(CUnaryExpression pIastUnaryExpression)
      throws CPATransferException {
    switch(pIastUnaryExpression.getOperator()) {
      case MINUS:
        this.labels.add(CFAEdgeLabel.ARITHMETIC);
        break;
      case AMPER:
        this.labels.add(CFAEdgeLabel.ADDRESS);
        break;
      case TILDE:
        this.labels.add(CFAEdgeLabel.BIT_OPERATION);
        break;
      case SIZEOF:
        this.labels.add(CFAEdgeLabel.SIZEOF);
        break;
      case ALIGNOF:
        throw new UnsupportedCCodeException("ALIGNOF is not supported", this.cfaEdge);
    }
    pIastUnaryExpression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws CPATransferException {
    this.labels.add(CFAEdgeLabel.COMPLEX);
    return null;
  }

  @Override
  public Void visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws CPATransferException {
    this.labels.add(CFAEdgeLabel.ADDRESS);
    return null;
  }

  @Override
  public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws CPATransferException {
    this.labels.add(CFAEdgeLabel.ARRAY_SUBSCRIPT);
    return null;
  }

  @Override
  public Void visit(CFieldReference pIastFieldReference)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Field Reference", this.cfaEdge);
  }

  @Override
  public Void visit(CIdExpression pIastIdExpression)
      throws CPATransferException {
    this.labels.add(CFAEdgeLabel.ID);
    return null;
  }

  @Override
  public Void visit(CPointerExpression pointerExpression)
      throws CPATransferException {
    this.labels.add(CFAEdgeLabel.PTR);
    pointerExpression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CComplexCastExpression complexCastExpression)
      throws CPATransferException {
    this.labels.add(CFAEdgeLabel.COMPLEX);
    return null;
  }
}
