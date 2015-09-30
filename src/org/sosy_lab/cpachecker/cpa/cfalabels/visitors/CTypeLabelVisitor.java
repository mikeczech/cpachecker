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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.cfalabels.CFAEdgeLabel;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

/**
 * Created by zenscr on 30/09/15.
 */
public class CTypeLabelVisitor implements CTypeVisitor<Void, CPATransferException> {

  private final CFAEdge cfaEdge;

  public List<CFAEdgeLabel> getTypeLabels() {
    return typeLabels;
  }

  private final List<CFAEdgeLabel> typeLabels = new ArrayList<>();

  public CTypeLabelVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  @Override
  public Void visit(CArrayType pArrayType) throws CPATransferException {
    this.typeLabels.add(CFAEdgeLabel.ARRAY);
    pArrayType.getType().accept(this);
    return null;
  }

  @Override
  public Void visit(CCompositeType pCompositeType)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }

  @Override
  public Void visit(CElaboratedType pElaboratedType)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }

  @Override
  public Void visit(CEnumType pEnumType) throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }

  @Override
  public Void visit(CFunctionType pFunctionType)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }

  @Override
  public Void visit(CPointerType pPointerType)
      throws CPATransferException {
    this.typeLabels.add(CFAEdgeLabel.PTR);
    pPointerType.getType().accept(this);
    return null;
  }

  @Override
  public Void visit(CProblemType pProblemType)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }

  @Override
  public Void visit(CSimpleType pSimpleType)
      throws CPATransferException {
    if(pSimpleType.isUnsigned())
      this.typeLabels.add(CFAEdgeLabel.UNSIGNED);
    switch(pSimpleType.getType()) {
      case BOOL:
      case CHAR:
      case INT:
        this.typeLabels.add(CFAEdgeLabel.INT);
        break;
      case FLOAT:
      case DOUBLE:
        this.typeLabels.add(CFAEdgeLabel.FLOAT);
        break;
      default:
        throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
    }
    return null;
  }

  @Override
  public Void visit(CTypedefType pTypedefType)
      throws CPATransferException {
    // We ignore typedefs and use the real type.
    pTypedefType.getRealType().accept(this);
    return null;
  }

  @Override
  public Void visit(CVoidType pVoidType) throws CPATransferException {
    this.typeLabels.add(CFAEdgeLabel.VOID);
    return null;
  }
}
