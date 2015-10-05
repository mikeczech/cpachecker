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

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cpa.cfalabels.CFAEdgeLabel;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Sets;

/**
 * Created by zenscr on 30/09/15.
 */
public class CSimpleDeclLabelVisitor
    implements CSimpleDeclarationVisitor<Set<CFAEdgeLabel>, CPATransferException> {

  private final CFAEdge cfaEdge;

  // TODO put this in one file
  static final Map<String, CFAEdgeLabel> SPECIAL_FUNCTIONS;

  static {
    Builder<String, CFAEdgeLabel> builder = ImmutableMap.builder();
    builder.put("pthread_create", CFAEdgeLabel.PTHREAD);
    builder.put("pthread_exit", CFAEdgeLabel.PTHREAD);
    builder.put("__VERIFIER_error", CFAEdgeLabel.VERIFIER_ERROR);
    builder.put("__VERIFIER_assert", CFAEdgeLabel.VERIFIER_ASSERT);
    builder.put("__VERIFIER_assume", CFAEdgeLabel.VERIFIER_ASSUME);
    builder.put("__VERIFIER_atomic_begin", CFAEdgeLabel.VERIFIER_ATOMIC_BEGIN);
    builder.put("__VERIFIER_atomic_end", CFAEdgeLabel.VERIFIER_ATOMIC_END);
    builder.put("__VERIFIER_nondet", CFAEdgeLabel.INPUT);
    builder.put("malloc", CFAEdgeLabel.MALLOC);
    builder.put("free", CFAEdgeLabel.FREE);
    SPECIAL_FUNCTIONS = builder.build();
  }

  public CSimpleDeclLabelVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  @Override
  public Set<CFAEdgeLabel> visit(CFunctionDeclaration pDecl)
      throws CPATransferException {
    Set<CFAEdgeLabel> labels = Sets.newHashSet(CFAEdgeLabel.FUNC);
    if(SPECIAL_FUNCTIONS.containsKey(pDecl.getName())) {
      labels.add(SPECIAL_FUNCTIONS.get(pDecl.getName()));
    }
    for(String key : SPECIAL_FUNCTIONS.keySet()) {
      if(pDecl.getName().startsWith(key))
        labels.add(SPECIAL_FUNCTIONS.get(key));
    }
    for(CParameterDeclaration param : pDecl.getParameters()) {
      CTypeLabelVisitor typeVisitor = new CTypeLabelVisitor(this.cfaEdge);
      labels.addAll(param.getType().accept(typeVisitor));
    }
    return Sets.immutableEnumSet(labels);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CComplexTypeDeclaration pDecl)
      throws CPATransferException {
    Set<CFAEdgeLabel> labels = Sets.newHashSet(CFAEdgeLabel.COMPLEX_TYPE);
    CTypeLabelVisitor typeVisitor = new CTypeLabelVisitor(this.cfaEdge);
    labels.addAll(pDecl.getType().accept(typeVisitor));
    return Sets.immutableEnumSet(labels);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CTypeDeclaration pDecl)
      throws CPATransferException {
    Set<CFAEdgeLabel> labels = Sets.newHashSet(CFAEdgeLabel.TYPE);
    CTypeLabelVisitor typeVisitor = new CTypeLabelVisitor(this.cfaEdge);
    labels.addAll(pDecl.getType().accept(typeVisitor));
    return Sets.immutableEnumSet(labels);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CVariableDeclaration pDecl)
      throws CPATransferException {
    CTypeLabelVisitor declTypeVisitor = new CTypeLabelVisitor(this.cfaEdge);
    return Sets.union(Sets.immutableEnumSet(CFAEdgeLabel.VAR), pDecl.getType().accept(declTypeVisitor));
  }

  @Override
  public Set<CFAEdgeLabel> visit(CParameterDeclaration pDecl)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }

  @Override
  public Set<CFAEdgeLabel> visit(CEnumerator pDecl) throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }
}
