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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.cfalabels.CFAEdgeLabel;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.collect.Sets;

/**
 * Created by zenscr on 30/09/15.
 */
public class CTypeLabelVisitor implements CTypeVisitor<Set<CFAEdgeLabel>, CPATransferException> {

  private final CFAEdge cfaEdge;

  public CTypeLabelVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  @Override
  public Set<CFAEdgeLabel> visit(CArrayType pArrayType) throws CPATransferException {
    Set<CFAEdgeLabel> labels = Sets.newHashSet(CFAEdgeLabel.ARRAY);
    labels.addAll(pArrayType.getType().accept(this));
    return Sets.immutableEnumSet(labels);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CCompositeType pCompositeType)
      throws CPATransferException {
    Set<CFAEdgeLabel> labels = Sets.newHashSet();
    switch (pCompositeType.getKind()) {
      case ENUM:
        labels.add(CFAEdgeLabel.ENUM);
        break;
      case STRUCT:
        labels.add(CFAEdgeLabel.STRUCT);
        break;
      case UNION:
        labels.add(CFAEdgeLabel.UNION);
    }
    for(CCompositeTypeMemberDeclaration decl : pCompositeType.getMembers()) {
      CTypeLabelVisitor typeVisitor = new CTypeLabelVisitor(this.cfaEdge);
      labels.addAll(decl.getType().accept(typeVisitor));
    }
    return Sets.immutableEnumSet(labels);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CElaboratedType pElaboratedType)
      throws CPATransferException {
    Set<CFAEdgeLabel> labels = Sets.newHashSet();
    switch (pElaboratedType.getKind()) {
      case ENUM:
        labels.add(CFAEdgeLabel.ENUM);
        break;
      case STRUCT:
        labels.add(CFAEdgeLabel.STRUCT);
        break;
      case UNION:
        labels.add(CFAEdgeLabel.UNION);
    }
    return Sets.immutableEnumSet(labels);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CEnumType pEnumType) throws CPATransferException {
    return Sets.immutableEnumSet(CFAEdgeLabel.ENUM);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CFunctionType pFunctionType)
      throws CPATransferException {
    Set<CFAEdgeLabel> labels = Sets.newHashSet(CFAEdgeLabel.FUNCTION_TYPE);
    CTypeLabelVisitor typeVisitor = new CTypeLabelVisitor(this.cfaEdge);
    labels.addAll(pFunctionType.getReturnType().accept(typeVisitor));
    pFunctionType.getCanonicalType().getReturnType();
    for(CType type : pFunctionType.getParameters()) {
      CTypeLabelVisitor paramTypeVisitor = new CTypeLabelVisitor(this.cfaEdge);
      labels.addAll(type.accept(paramTypeVisitor));
    }
    return Sets.immutableEnumSet(labels);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CPointerType pPointerType)
      throws CPATransferException {
    return Sets.union(pPointerType.getType().accept(this), Sets.immutableEnumSet(CFAEdgeLabel.PTR));
  }

  @Override
  public Set<CFAEdgeLabel> visit(CProblemType pProblemType)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type: ProblemType", this.cfaEdge);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CSimpleType pSimpleType)
      throws CPATransferException {
    Set<CFAEdgeLabel> labels = Sets.newHashSet();
    if(pSimpleType.isUnsigned())
      labels.add(CFAEdgeLabel.UNSIGNED);
    switch(pSimpleType.getType()) {
      case BOOL:
      case CHAR:
      case INT:
        labels.add(CFAEdgeLabel.INT);
        break;
      case FLOAT:
      case DOUBLE:
        labels.add(CFAEdgeLabel.FLOAT);
        break;
      default:
        if(pSimpleType.isLong()) {
          labels.add(CFAEdgeLabel.LONG);
          break;
        }
        throw new UnsupportedCCodeException("Unspecified declaration type: CSimpleType", this.cfaEdge);
    }
    return Sets.immutableEnumSet(labels);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CTypedefType pTypedefType)
      throws CPATransferException {
    return pTypedefType.getRealType().accept(this);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CVoidType pVoidType)
      throws CPATransferException {
    return Sets.immutableEnumSet(CFAEdgeLabel.VOID);
  }

}
