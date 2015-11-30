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
import org.sosy_lab.cpachecker.cpa.cfalabels.ASTree;
import org.sosy_lab.cpachecker.cpa.cfalabels.GMEdge;
import org.sosy_lab.cpachecker.cpa.cfalabels.GMEdgeLabel;
import org.sosy_lab.cpachecker.cpa.cfalabels.GMNode;
import org.sosy_lab.cpachecker.cpa.cfalabels.GMNodeLabel;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.collect.Sets;

/**
 * Created by zenscr on 30/09/15.
 */
public class CTypeLabelVisitor implements CTypeVisitor<ASTree, CPATransferException> {

  private final CFAEdge cfaEdge;

  public CTypeLabelVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  @Override
  public ASTree visit(CArrayType pArrayType) throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.ARRAY));
    GMNode root = tree.getRoot();

    if(pArrayType.isConst())
      root.addLabel(GMNodeLabel.CONST);
    if(pArrayType.isVolatile())
      root.addLabel(GMNodeLabel.VOLATILE);

    ASTree typeTree = pArrayType.getType().accept(
        new CTypeLabelVisitor(this.cfaEdge));
    tree.addTree(typeTree, new GMNode(GMNodeLabel.TYPE));

    ASTree lengthTree = pArrayType.getLength().accept(
        new CExpressionLabelVisitor(this.cfaEdge));
    tree.addTree(lengthTree, new GMNode(GMNodeLabel.LENGTH));

    return tree;
  }

  @Override
  public ASTree visit(CCompositeType pCompositeType)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.COMPOSITE_TYPE));
    GMNode root = tree.getRoot();

    if(pCompositeType.isConst())
      root.addLabel(GMNodeLabel.CONST);
    if(pCompositeType.isVolatile())
      root.addLabel(GMNodeLabel.VOLATILE);

    switch (pCompositeType.getKind()) {
      case ENUM:
        root.addLabel(GMNodeLabel.ENUM);
        break;
      case STRUCT:
        root.addLabel(GMNodeLabel.STRUCT);
        break;
      case UNION:
        root.addLabel(GMNodeLabel.UNION);
    }
    for(CCompositeTypeMemberDeclaration decl : pCompositeType.getMembers()) {
      ASTree compTypeMemberTypeTree = decl.getType().accept(new CTypeLabelVisitor(this.cfaEdge));
      tree.addTree(compTypeMemberTypeTree);
    }
    return tree;
  }

  @Override
  public ASTree visit(CElaboratedType pElaboratedType)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.ELABORATED_TYPE));
    GMNode root = tree.getRoot();

    if(pElaboratedType.isConst())
      root.addLabel(GMNodeLabel.CONST);
    if(pElaboratedType.isVolatile())
      root.addLabel(GMNodeLabel.VOLATILE);

    switch (pElaboratedType.getKind()) {
      case ENUM:
        root.addLabel(GMNodeLabel.ENUM);
        break;
      case STRUCT:
        root.addLabel(GMNodeLabel.STRUCT);
        break;
      case UNION:
        root.addLabel(GMNodeLabel.UNION);
    }
    return tree;
  }

  @Override
  public ASTree visit(CEnumType pEnumType) throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type: CEnumType", this.cfaEdge);
  }

  @Override
  public ASTree visit(CFunctionType pFunctionType)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.FUNCTION_TYPE));
    GMNode root = tree.getRoot();

    if(pFunctionType.isConst())
      root.addLabel(GMNodeLabel.CONST);
    if(pFunctionType.isVolatile())
      root.addLabel(GMNodeLabel.VOLATILE);

    if(pFunctionType.getParameters().size() > 0) {
      ASTree paramTypeTree = new ASTree(new GMNode(GMNodeLabel.PARAM_TYPES));
      for (CType type : pFunctionType.getParameters()) {
        ASTree typeTree = type.accept(new CTypeLabelVisitor(this.cfaEdge));
        paramTypeTree.addTree(typeTree);
      }
      tree.addTree(paramTypeTree);
    }
    ASTree returnTypeTree = pFunctionType.getReturnType().accept(
        new CTypeLabelVisitor(this.cfaEdge));
    tree.addTree(returnTypeTree, new GMNode(GMNodeLabel.RETURN_TYPE));

    return tree;
  }

  @Override
  public ASTree visit(CPointerType pPointerType)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.POINTER_TYPE));
    GMNode root = tree.getRoot();

    if(pPointerType.isConst())
      root.addLabel(GMNodeLabel.CONST);
    if(pPointerType.isVolatile())
      root.addLabel(GMNodeLabel.VOLATILE);

    ASTree typeTree = pPointerType.getType().accept(new CTypeLabelVisitor(this.cfaEdge));
    tree.addTree(typeTree);

    return tree;
  }

  @Override
  public ASTree visit(CProblemType pProblemType)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type: ProblemType", this.cfaEdge);
  }

  @Override
  public ASTree visit(CSimpleType pSimpleType)
      throws CPATransferException {

    ASTree tree = new ASTree(new GMNode());
    GMNode root = tree.getRoot();

    if(pSimpleType.isConst())
      root.addLabel(GMNodeLabel.CONST);
    if(pSimpleType.isVolatile())
      root.addLabel(GMNodeLabel.VOLATILE);

    if(pSimpleType.isUnsigned())
      root.addLabel(GMNodeLabel.UNSIGNED);
    switch(pSimpleType.getType()) {
      case BOOL:
        root.addLabel(GMNodeLabel.BOOL);
        break;
      case CHAR:
        root.addLabel(GMNodeLabel.CHAR);
        break;
      case INT:
        root.addLabel(GMNodeLabel.INT);
        break;
      case FLOAT:
        root.addLabel(GMNodeLabel.FLOAT);
        break;
      case DOUBLE:
        root.addLabel(GMNodeLabel.DOUBLE);
        break;
      default:
        if(pSimpleType.isLong()) {
          root.addLabel(GMNodeLabel.LONG);
          break;
        }
        if(pSimpleType.isLongLong()) {
          root.addLabel(GMNodeLabel.LONGLONG);
          break;
        }
        if(pSimpleType.isShort()) {
          root.addLabel(GMNodeLabel.SHORT);
          break;
        }
        if(pSimpleType.isVolatile()) {
          root.addLabel(GMNodeLabel.VOLATILE);
          break;
        }
        // Can be used as standalone type?
        if(pSimpleType.isUnsigned()) {
          root.addLabel(GMNodeLabel.UNSIGNED);
          break;
        }
        throw new UnsupportedCCodeException("Unspecified declaration type: CSimpleType", this.cfaEdge);
    }
    return tree;
  }

  @Override
  public ASTree visit(CTypedefType pTypedefType)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.TYPEDEF_TYPE));
    ASTree realTypeTree = pTypedefType.getRealType().accept(new CTypeLabelVisitor(this.cfaEdge));
    tree.addTree(realTypeTree, new GMNode(GMNodeLabel.REAL_TYPE));
    return tree;
  }

  @Override
  public ASTree visit(CVoidType pVoidType)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.VOID_TYPE));
    GMNode root = tree.getRoot();

    if(pVoidType.isConst())
      root.addLabel(GMNodeLabel.CONST);
    if(pVoidType.isVolatile())
      root.addLabel(GMNodeLabel.VOLATILE);

    return tree;
  }

}
