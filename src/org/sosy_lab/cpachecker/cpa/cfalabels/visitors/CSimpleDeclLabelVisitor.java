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

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cpa.cfalabels.ASTree;
import org.sosy_lab.cpachecker.cpa.cfalabels.GMEdge;
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
public class CSimpleDeclLabelVisitor
    implements CSimpleDeclarationVisitor<ASTree, CPATransferException> {

  private final CFAEdge cfaEdge;

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

  public CSimpleDeclLabelVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  @Override
  public ASTree visit(CFunctionDeclaration pDecl)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.FUNCTION_DECL));
    GMNode root = tree.getRoot();
    for(String key : SPECIAL_FUNCTIONS.keySet()) {
      if(pDecl.getName().startsWith(key))
        root.addLabel(SPECIAL_FUNCTIONS.get(key));
    }
    ASTree returnTypeTree = pDecl.getType().getReturnType().accept(new CTypeLabelVisitor(this.cfaEdge));
    tree.addTree(returnTypeTree, new GMNode(GMNodeLabel.RETURN_TYPE));

    if(pDecl.getParameters().size() > 0) {
      ASTree paramTypesTree = new ASTree(new GMNode(GMNodeLabel.PARAM_TYPES));
      for (CParameterDeclaration param : pDecl.getParameters()) {
        ASTree typeTree =
            param.getType().accept(new CTypeLabelVisitor(this.cfaEdge));
        paramTypesTree.addTree(typeTree);
      }
      tree.addTree(paramTypesTree);
    }
    return tree;
  }

  @Override
  public ASTree visit(CComplexTypeDeclaration pDecl)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.COMPLEX_TYPE_DECL));
    ASTree typeTree = pDecl.getType().accept(new CTypeLabelVisitor(this.cfaEdge));
    tree.addTree(typeTree);
    return tree;
  }

  @Override
  public ASTree visit(CTypeDeclaration pDecl)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.TYPE_DECL));
    ASTree typeTree = pDecl.getType().accept(new CTypeLabelVisitor(this.cfaEdge));
    tree.addTree(typeTree);
    return tree;
  }

  @Override
  public ASTree visit(CVariableDeclaration pDecl)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.VARIABLE_DECL));
    ASTree typeTree = pDecl.getType().accept(new CTypeLabelVisitor(this.cfaEdge));
    tree.addTree(typeTree);
    // Todo: add initializer
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
