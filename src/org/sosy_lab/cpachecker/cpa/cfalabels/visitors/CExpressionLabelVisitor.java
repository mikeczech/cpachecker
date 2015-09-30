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
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Created by zenscr on 30/09/15.
 */
public class CExpressionLabelVisitor implements CExpressionVisitor<Void, CPATransferException> {
  @Override
  public Void visit(CBinaryExpression pIastBinaryExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CCastExpression pIastCastExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CTypeIdExpression pIastTypeIdExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CUnaryExpression pIastUnaryExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CFieldReference pIastFieldReference)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CIdExpression pIastIdExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CPointerExpression pointerExpression)
      throws CPATransferException {
    return null;
  }

  @Override
  public Void visit(CComplexCastExpression complexCastExpression)
      throws CPATransferException {
    return null;
  }
}
