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
package org.sosy_lab.cpachecker.cpa.cfalabels;

import java.io.Serializable;
import java.io.StringWriter;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

import org.jgrapht.ext.DOTExporter;


public class CFALabelsState
    implements Serializable, AbstractState, Graphable {

  private int source;

  private int target;

  private ASTree tree;

  public final static CFALabelsState TOP = new CFALabelsState();

  private CFALabelsState() {
    this.source = -1;
    this.target = -1;
    this.tree = new ASTree();
  }

  public CFALabelsState(CFAEdge pEdge, ASTree pTree) {
    this.source = pEdge.getPredecessor().getNodeNumber();
    this.target = pEdge.getSuccessor().getNodeNumber();
    this.tree = pTree;
  }

  @Override
  public String toString() {
    StringWriter strWriter = new StringWriter();
    DOTExporter<GMNode, GMEdge> dotExp = new DOTExporter<>();
    dotExp.export(strWriter, this.tree.asGraph());
    return strWriter.toString();
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof CFALabelsState))
      return false;
    return ((CFALabelsState) pObj).source == this.source
        && ((CFALabelsState) pObj).target == this.target;
  }

  @Override
  public int hashCode() {
    int result = source;
    result = 31 * result + target;
    return result;
  }

  @Override
  public String toDOTLabel() {
    return this.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
