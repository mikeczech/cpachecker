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

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTCollectorUtils;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTNode;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTNodeLabel;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTree;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.base.Optional;

/**
 * Created by zenscr on 30/09/15.
 */
public class CSimpleDeclASTVisitor
    implements CSimpleDeclarationVisitor<ASTree, CPATransferException> {

  private final CFAEdge cfaEdge;

  public CSimpleDeclASTVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  @Override
  public ASTree visit(CFunctionDeclaration pDecl)
      throws CPATransferException {
    Optional<ASTNodeLabel> specialLabel = ASTCollectorUtils.getSpecialLabel(pDecl.getName());

    ASTree tree = new ASTree(new ASTNode(ASTNodeLabel.FUNCTION_DECL), pDecl.getName());
    ASTNode root = tree.getRoot();
    if(specialLabel.isPresent())
      root.addLabel(specialLabel.get());

    ASTree returnTypeTree = pDecl.getType().getReturnType().accept(new CTypeASTVisitor(this.cfaEdge));
    tree.addTree(returnTypeTree, new ASTNode(ASTNodeLabel.RETURN_TYPE));

    if(pDecl.getParameters().size() > 0) {
      ASTree paramTypesTree = new ASTree(new ASTNode(ASTNodeLabel.PARAM_TYPES));
      for (CParameterDeclaration param : pDecl.getParameters()) {
        ASTree typeTree =
            param.getType().accept(new CTypeASTVisitor(this.cfaEdge));
        paramTypesTree.addTree(typeTree);
      }
      tree.addTree(paramTypesTree);
    }
    return tree;
  }

  @Override
  public ASTree visit(CComplexTypeDeclaration pDecl)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(ASTNodeLabel.COMPLEX_TYPE_DECL), pDecl.getName());
    ASTree typeTree = pDecl.getType().accept(new CTypeASTVisitor(this.cfaEdge));
    tree.addTree(typeTree);
    return tree;
  }

  @Override
  public ASTree visit(CTypeDeclaration pDecl)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(ASTNodeLabel.TYPE_DECL), pDecl.getName());
    ASTree typeTree = pDecl.getType().accept(new CTypeASTVisitor(this.cfaEdge));
    tree.addTree(typeTree);
    return tree;
  }

  @Override
  public ASTree visit(CVariableDeclaration pDecl)
      throws CPATransferException {
    ASTree tree = new ASTree(new ASTNode(ASTNodeLabel.VARIABLE_DECL), pDecl.getName());
    ASTree typeTree = pDecl.getType().accept(new CTypeASTVisitor(this.cfaEdge));
    tree.addTree(typeTree);
    if(pDecl.getInitializer() != null) {
      ASTree initializerTree = pDecl.getInitializer().accept(new CInitializerASTVisitor(this.cfaEdge));
      tree.addTree(initializerTree, new ASTNode(ASTNodeLabel.INITIALIZER));
    }
    return tree;
  }

  @Override
  public ASTree visit(CParameterDeclaration pDecl)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }

  @Override
  public ASTree visit(CEnumerator pDecl) throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }
}
