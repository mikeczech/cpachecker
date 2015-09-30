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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Table;


public class CFALabelsState
    implements Serializable, AbstractState {

  private Table<Integer, Integer, List<CFAEdgeLabel>> cfaEdgeLabelMap;

  public final static CFALabelsState TOP = new CFALabelsState();

  private CFALabelsState() {
    cfaEdgeLabelMap = ImmutableTable.of();
  }

  private CFALabelsState(Table<Integer, Integer, List<CFAEdgeLabel>> pCfaEdgeLabelMap) {
    cfaEdgeLabelMap = pCfaEdgeLabelMap;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    return builder.toString();
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof CFALabelsState))
      return false;
    return ((CFALabelsState) pObj).cfaEdgeLabelMap.equals(
        this.cfaEdgeLabelMap);
  }

  public CFALabelsState addEdgeLabel(CFAEdge pEdge, List<CFAEdgeLabel> pLabels) {
    Builder<Integer, Integer, List<CFAEdgeLabel>> b = ImmutableTable.builder();
    b.put(pEdge.getPredecessor().getNodeNumber(),
          pEdge.getSuccessor().getNodeNumber(),
          pLabels);
    b.putAll(cfaEdgeLabelMap);
    return new CFALabelsState(b.build());
  }

  @Override
  public int hashCode() {
    return cfaEdgeLabelMap.hashCode();
  }

}
