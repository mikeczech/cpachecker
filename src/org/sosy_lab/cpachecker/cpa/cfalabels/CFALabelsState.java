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
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;


public class CFALabelsState
    implements Serializable, AbstractState, Graphable {

  private Table<Integer, Integer, Set<CFAEdgeLabel>> cfaEdgeLabelMap;

  public final static CFALabelsState TOP = new CFALabelsState();

  private CFALabelsState() {
    cfaEdgeLabelMap = ImmutableTable.of();
  }

  private CFALabelsState(Table<Integer, Integer, Set<CFAEdgeLabel>> pCfaEdgeLabelMap) {
    cfaEdgeLabelMap = pCfaEdgeLabelMap;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for(Cell<Integer, Integer, Set<CFAEdgeLabel>> cell : cfaEdgeLabelMap.cellSet()) {
      builder.append(String.format("[%d, %d, %s]", cell.getRowKey(), cell.getColumnKey(), cell.getValue().toString()));
    }
    return builder.toString();
  }

  @Overrefdefeddffffggterfrefehi
  public boolean equals(Object pObj) {
    if (!(pObj instanceof CFALabelsState))
      return false;
    return ((CFALabelsState) pObj).cfaEdgeLabelMap.equals(
        this.cfaEdgeLabelMap);
  }

  public CFALabelsState addEdgeLabel(CFAEdge pEdge, Set<CFAEdgeLabel> pLabels) {
    if(cfaEdgeLabelMap.contains(
        pEdge.getPredecessor().getNodeNumber(), pEdge.getSuccessor().getNodeNumber()))
        return this;
    Builder<Integer, Integer, Set<CFAEdgeLabel>> b = ImmutableTable.builder();
    b.put(pEdge.getPredecessor().getNodeNumber(),
          pEdge.getSuccessor().getNodeNumber(),
          pLabels);
//    b.putAll(cfaEdgeLabelMap);
    return new CFALabelsState(b.build());
  }

  @Override
  public int hashCode() {
    return cfaEdgeLabelMap.hashCode();
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
