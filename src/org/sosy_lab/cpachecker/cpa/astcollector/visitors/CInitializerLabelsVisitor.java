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

import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.cfalabels.ASTree;
import org.sosy_lab.cpachecker.cpa.cfalabels.GMNode;
import org.sosy_lab.cpachecker.cpa.cfalabels.GMNodeLabel;
import org.sosy_lab.cpachecker.cpa.cfalabels.visitors.CExpressionLabelVisitor;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

/**
 * Created by zenscr on 01/12/15.
 */
public class CInitializerLabelsVisitor implements CInitializerVisitor<ASTree, CPATransferException> {

  private final CFAEdge cfaEdge;

  public CInitializerLabelsVisitor(
      CFAEdge pCfaEdge) {
    cfaEdge = pCfaEdge;
  }

  @Override
  public ASTree visit(CInitializerExpression pInitializerExpression)
      throws CPATransferException {
    return pInitializerExpression.getExpression().accept(new CExpressionLabelVisitor(this.cfaEdge));
  }

  @Override
  public ASTree visit(CInitializerList pInitializerList)
      throws CPATransferException {
    ASTree tree = new ASTree(new GMNode(GMNodeLabel.INITIALIZER_LIST));
    for(CInitializer initializer : pInitializerList.getInitializers()) {
      tree.addTree(initializer.accept(this));
    }
    return tree;
  }

  @Override
  public ASTree visit(CDesignatedInitializer pCStructInitializerPart)
      throws CPATransferException {
    throw new UnsupportedCCodeException("Designed initializers are not supported", this.cfaEdge);
  }
}
