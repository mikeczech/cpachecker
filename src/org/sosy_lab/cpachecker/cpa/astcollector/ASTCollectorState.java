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
package org.sosy_lab.cpachecker.cpa.astcollector;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;


public class ASTCollectorState
    implements Serializable, AbstractState, Graphable {

  public class CFAEdgeInfo {
    private final int source;
    private final int target;

    private final boolean assumption;

    public CFAEdgeInfo(int pSource, int pTarget, boolean pAssumption) {
      source = pSource;
      target = pTarget;
      assumption = pAssumption;
    }

    public int getTarget() {
      return target;
    }

    public int getSource() {
      return source;
    }

    public boolean getAssumption() {
      return assumption;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }

      CFAEdgeInfo that = (CFAEdgeInfo)pO;

      if (source != that.source) {
        return false;
      }
      if (target != that.target) {
        return false;
      }
      if (assumption != that.assumption) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = source;
      result = 31 * result + target;
      result = 31 * result + (assumption ? 1 : 0);
      return result;
    }
  }

  private ASTree tree;

  private Set<String> variables = new HashSet<>();

  private Set<CFAEdgeInfo> cfaEdgeInfoSet = new HashSet<>();

  private Table<Integer, Integer, Boolean> assumptions = HashBasedTable.create();

  public final static ASTCollectorState TOP = new ASTCollectorState();

  private ASTCollectorState() {
    this.tree = new ASTree();
  }

  public ASTCollectorState(CFAEdge pEdge, ASTree pTree) {
    this(pEdge, pTree, new HashSet<String>());
  }

  public ASTCollectorState(CFAEdge pEdge, ASTree pTree, Set<String> pVars) {
    addEdge(pEdge);
    this.tree = pTree;
    this.variables.addAll(pVars);
  }

  public ASTCollectorState(CFAEdge pEdge, ASTree pTree, Set<String> pVars, boolean assumption) {
    addEdge(pEdge, assumption);
    for(int i = 0; i < pEdge.getPredecessor().getNumLeavingEdges(); i++) {
      CFAEdge altEdge = pEdge.getPredecessor().getLeavingEdge(i);
      if(altEdge != pEdge) {
        addEdge(altEdge, !assumption);
      }
    }
    assert cfaEdgeInfoSet.size() == 2;
    this.tree = pTree;
    this.variables.addAll(pVars);
  }

  public Set<String> getVariables() {
    return variables;
  }

  public Table<Integer, Integer, Boolean> getAssumptions() {
    return assumptions;
  }

  private void addEdge(CFAEdge pCFAEdge, boolean assumption) {
    int pre = pCFAEdge.getPredecessor().getNodeNumber();
    int succ = pCFAEdge.getSuccessor().getNodeNumber();
    CFAEdgeInfo cfaEdgeInfo = new CFAEdgeInfo(
        pCFAEdge.getPredecessor().getNodeNumber(),
        pCFAEdge.getSuccessor().getNodeNumber(), assumption);
    this.cfaEdgeInfoSet.add(cfaEdgeInfo);
    this.assumptions.put(pre, succ, assumption);
  }

  private void addEdge(CFAEdge pCFAEdge) {
    addEdge(pCFAEdge, true);
  }

  public Set<CFAEdgeInfo> getCfaEdgeInfoSet() {
    return cfaEdgeInfoSet;
  }

  public ASTree getTree() {
    return tree;
  }

  public boolean isInit() {
    return this.cfaEdgeInfoSet.isEmpty();
  }

  @Override
  public String toString() {
    StringWriter strWriter = new StringWriter();
    DOTExporter<ASTNode, ASTEdge> dotExp = new DOTExporter<>(
        new IntegerNameProvider(),
        new VertexNameProvider<ASTNode>() {
          @Override
          public String getVertexName(ASTNode o) {
            return o.toString();
          }
        },
        new EdgeNameProvider<ASTEdge>() {
          @Override
          public String getEdgeName(ASTEdge o) {
            return o.toString();
          }
        });
    dotExp.export(strWriter, this.tree.asGraph());
    return strWriter.toString();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }

    ASTCollectorState that = (ASTCollectorState)pO;

    if (!cfaEdgeInfoSet.equals(that.cfaEdgeInfoSet)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return cfaEdgeInfoSet.hashCode();
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
