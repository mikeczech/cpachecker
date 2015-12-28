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
import org.sosy_lab.cpachecker.cpa.astcollector.ASTNode;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTNodeLabel;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTree;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

/**
 * Created by zenscr on 30/09/15.
 */
public class CTypeASTVisitor implements CTypeVisitor<ASTree, CPATransferException> {

  private final CFAEdge cfaEdge;

  public CTypeASTVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  @Override
  public ASTree visit(CArrayType pArrayType) throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.ARRAY));
    ASTNode root = tree.getRoot();

    if(pArrayType.isConst())
      root.addLabel(ASTNodeLabel.CONST);
    if(pArrayType.isVolatile())
      root.addLabel(ASTNodeLabel.VOLATILE);

    ASTree typeTree = pArrayType.getType().accept(
        new CTypeASTVisitor(this.cfaEdge));
    tree.addTree(typeTree, new ASTNode(
        ASTNodeLabel.TYPE));

    if(pArrayType.getLength() != null) {
      ASTree lengthTree = pArrayType.getLength().accept(
          new CExpressionASTVisitor(this.cfaEdge));
      tree.addTree(lengthTree, new ASTNode(
          ASTNodeLabel.LENGTH));
    }

    return tree;
  }

  @Override
  public ASTree visit(CCompositeType pCompositeType)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.COMPOSITE_TYPE));
    ASTNode root = tree.getRoot();

    if(pCompositeType.isConst())
      root.addLabel(ASTNodeLabel.CONST);
    if(pCompositeType.isVolatile())
      root.addLabel(ASTNodeLabel.VOLATILE);

    switch (pCompositeType.getKind()) {
      case ENUM:
        root.addLabel(ASTNodeLabel.ENUM);
        break;
      case STRUCT:
        root.addLabel(ASTNodeLabel.STRUCT);
        break;
      case UNION:
        root.addLabel(ASTNodeLabel.UNION);
    }
    for(CCompositeTypeMemberDeclaration decl : pCompositeType.getMembers()) {
      ASTree compTypeMemberTypeTree = decl.getType().accept(new CTypeASTVisitor(this.cfaEdge));
      tree.addTree(compTypeMemberTypeTree);
    }
    return tree;
  }

  @Override
  public ASTree visit(CElaboratedType pElaboratedType)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.ELABORATED_TYPE));
    ASTNode root = tree.getRoot();

    if(pElaboratedType.isConst())
      root.addLabel(ASTNodeLabel.CONST);
    if(pElaboratedType.isVolatile())
      root.addLabel(ASTNodeLabel.VOLATILE);

    switch (pElaboratedType.getKind()) {
      case ENUM:
        root.addLabel(ASTNodeLabel.ENUM);
        break;
      case STRUCT:
        root.addLabel(ASTNodeLabel.STRUCT);
        break;
      case UNION:
        root.addLabel(ASTNodeLabel.UNION);
    }
    return tree;
  }

  @Override
  public ASTree visit(CEnumType pEnumType) throws CPATransferException {
    return new ASTree(new ASTNode(
        ASTNodeLabel.ENUM_TYPE));
  }

  @Override
  public ASTree visit(CFunctionType pFunctionType)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.FUNCTION_TYPE));
    ASTNode root = tree.getRoot();

    if(pFunctionType.isConst())
      root.addLabel(ASTNodeLabel.CONST);
    if(pFunctionType.isVolatile())
      root.addLabel(ASTNodeLabel.VOLATILE);

    if(pFunctionType.getParameters().size() > 0) {
      ASTree paramTypeTree = new ASTree(new ASTNode(
          ASTNodeLabel.PARAM_TYPES));
      for (CType type : pFunctionType.getParameters()) {
        ASTree typeTree = type.accept(new CTypeASTVisitor(this.cfaEdge));
        paramTypeTree.addTree(typeTree);
      }
      tree.addTree(paramTypeTree);
    }
    ASTree returnTypeTree = pFunctionType.getReturnType().accept(
        new CTypeASTVisitor(this.cfaEdge));
    tree.addTree(returnTypeTree, new ASTNode(
        ASTNodeLabel.RETURN_TYPE));

    return tree;
  }

  @Override
  public ASTree visit(CPointerType pPointerType)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.POINTER_TYPE));
    ASTNode root = tree.getRoot();

    if(pPointerType.isConst())
      root.addLabel(ASTNodeLabel.CONST);
    if(pPointerType.isVolatile())
      root.addLabel(ASTNodeLabel.VOLATILE);

    ASTree typeTree = pPointerType.getType().accept(new CTypeASTVisitor(this.cfaEdge));
    tree.addTree(typeTree);

    return tree;
  }

  @Override
  public ASTree visit(CProblemType pProblemType)
      throws CPATransferException {
    return new ASTree(new ASTNode(ASTNodeLabel.PROBLEM_TYPE));
  }

  @Override
  public ASTree visit(CSimpleType pSimpleType)
      throws CPATransferException {

    ASTree tree = new ASTree(new ASTNode());
    ASTNode root = tree.getRoot();

    if(pSimpleType.isConst())
      root.addLabel(ASTNodeLabel.CONST);
    if(pSimpleType.isVolatile())
      root.addLabel(ASTNodeLabel.VOLATILE);

    if(pSimpleType.isUnsigned())
      root.addLabel(ASTNodeLabel.UNSIGNED);
    switch(pSimpleType.getType()) {
      case BOOL:
        root.addLabel(ASTNodeLabel.BOOL);
        break;
      case CHAR:
        root.addLabel(ASTNodeLabel.CHAR);
        break;
      case INT:
        root.addLabel(ASTNodeLabel.INT);
        break;
      case FLOAT:
        root.addLabel(ASTNodeLabel.FLOAT);
        break;
      case DOUBLE:
        root.addLabel(ASTNodeLabel.DOUBLE);
        break;
      default:
        if(pSimpleType.isLong()) {
          root.addLabel(ASTNodeLabel.LONG);
          break;
        }
        if(pSimpleType.isLongLong()) {
          root.addLabel(ASTNodeLabel.LONGLONG);
          break;
        }
        if(pSimpleType.isShort()) {
          root.addLabel(ASTNodeLabel.SHORT);
          break;
        }
        if(pSimpleType.isVolatile()) {
          root.addLabel(ASTNodeLabel.VOLATILE);
          break;
        }
        // Can be used as standalone type?
        if(pSimpleType.isUnsigned()) {
          root.addLabel(ASTNodeLabel.UNSIGNED);
          break;
        }
        throw new UnsupportedCCodeException("Unspecified declaration type: CSimpleType", this.cfaEdge);
    }
    return tree;
  }

  @Override
  public ASTree visit(CTypedefType pTypedefType)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.TYPEDEF_TYPE));
    ASTree realTypeTree = pTypedefType.getRealType().accept(new CTypeASTVisitor(this.cfaEdge));
    tree.addTree(realTypeTree, new ASTNode(
        ASTNodeLabel.REAL_TYPE));
    return tree;
  }

  @Override
  public ASTree visit(CVoidType pVoidType)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(
        ASTNodeLabel.VOID_TYPE));
    ASTNode root = tree.getRoot();

    if(pVoidType.isConst())
      root.addLabel(ASTNodeLabel.CONST);
    if(pVoidType.isVolatile())
      root.addLabel(ASTNodeLabel.VOLATILE);

    return tree;
  }

}
