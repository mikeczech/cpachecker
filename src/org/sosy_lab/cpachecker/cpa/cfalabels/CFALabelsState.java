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
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

import org.jgrapht.ext.DOTExporter;


public class CFALabelsState
    implements Serializable, AbstractState, Graphable {

  public class EdgeInfo {
    private int source;
    private int target;
    private Set<GMEdgeLabel> labels = new HashSet<>();

    public EdgeInfo(int pSource, int pTarget) {
      source = pSource;
      target = pTarget;
    }

    public Set<GMEdgeLabel> getLabels() {
      return labels;
    }

    public void addLabel(GMEdgeLabel pLabel) {
      this.labels.add(pLabel);
    }

    public int getTarget() {
      return target;
    }

    public int getSource() {
      return source;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      EdgeInfo edgeInfo = (EdgeInfo)o;

      if (source != edgeInfo.source) {
        return false;
      }
      if (target != edgeInfo.target) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = source;
      result = 31 * result + target;
      return result;
    }
  }

  private ASTree tree;

  private Set<EdgeInfo> edgeInfoSet= new HashSet<>();

  private EdgeInfo lastAddedEdgeInfo = null;

  public final static CFALabelsState TOP = new CFALabelsState();

  private CFALabelsState() {
    this.tree = new ASTree();
  }

  public CFALabelsState(CFAEdge pEdge, ASTree pTree) {
    EdgeInfo edgeInfo = new EdgeInfo(
        pEdge.getPredecessor().getNodeNumber(),
        pEdge.getSuccessor().getNodeNumber());
    this.lastAddedEdgeInfo = edgeInfo;
    this.edgeInfoSet.add(edgeInfo);
    this.tree = pTree;
  }

  public void addEdge(CFAEdge pCFAEdge) {
    EdgeInfo edgeInfo = new EdgeInfo(
        pCFAEdge.getPredecessor().getNodeNumber(),
        pCFAEdge.getSuccessor().getNodeNumber());
    this.edgeInfoSet.add(edgeInfo);
    this.lastAddedEdgeInfo = edgeInfo;
  }

  public EdgeInfo getLastAddedEdgeInfo() {
    return lastAddedEdgeInfo;
  }


  public Set<EdgeInfo> getEdgeInfoSet() {
    return edgeInfoSet;
  }

  public ASTree getTree() {
    return tree;
  }

  public boolean isInit() {
    return this.edgeInfoSet.isEmpty();
  }

  @Override
  public String toString() {
    StringWriter strWriter = new StringWriter();
    DOTExporter<GMNode, GMEdge> dotExp = new DOTExporter<>(
        new IntegerNameProvider(),
        new VertexNameProvider<GMNode>() {
          @Override
          public String getVertexName(GMNode o) {
            return o.toString();
          }
        },
        new EdgeNameProvider<GMEdge>() {
          @Override
          public String getEdgeName(GMEdge o) {
            return o.toString();
          }
        });
    dotExp.export(strWriter, this.tree.asGraph());
    return strWriter.toString();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CFALabelsState that = (CFALabelsState)o;

    if (edgeInfoSet != null ? !edgeInfoSet.equals(that.edgeInfoSet)
        : that.edgeInfoSet != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return edgeInfoSet != null ? edgeInfoSet.hashCode() : 0;
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
