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

/**
 * Created by zenscr on 30/09/15.
 */
public class CSimpleDeclLabelVisitor
    implements CSimpleDeclarationVisitor<Void,CPATransferException> {

  private final List<CFAEdgeLabel> labels = new ArrayList<>();

  public List<CFAEdgeLabel> getLabels() {
    return labels;
  }

  private final CFAEdge cfaEdge;

  public CSimpleDeclLabelVisitor(CFAEdge cfaEdge) {
    this.cfaEdge = cfaEdge;
  }

  @Override
  public Void visit(CFunctionDeclaration pDecl)
      throws CPATransferException {
    labels.add(CFAEdgeLabel.FUNC);
    return null;
  }

  @Override
  public Void visit(CComplexTypeDeclaration pDecl)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }

  @Override
  public Void visit(CTypeDeclaration pDecl)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }

  @Override
  public Void visit(CVariableDeclaration pDecl)
      throws CPATransferException {
    CTypeLabelVisitor declTypeVisitor = new CTypeLabelVisitor(this.cfaEdge);
    pDecl.getType().accept(declTypeVisitor);
    labels.add(CFAEdgeLabel.VAR);
    labels.addAll(declTypeVisitor.getTypeLabels());
    return null;
  }

  @Override
  public Void visit(CParameterDeclaration pDecl)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }

  @Override
  public Void visit(CEnumerator pDecl) throws CPATransferException {
    throw new UnsupportedCCodeException("Unspecified declaration type", this.cfaEdge);
  }
}
