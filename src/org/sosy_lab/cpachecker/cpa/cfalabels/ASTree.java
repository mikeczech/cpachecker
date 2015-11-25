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

import org.eclipse.jdt.core.dom.AST;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

/**
 * Created by zenscr on 24/11/15.
 */
public class ASTree {

  private DirectedGraph<GMNode, GMEdge> tree = new DefaultDirectedGraph<>(GMEdge.class);

  private GMNode root = null;

  public ASTree(GMNode pRoot) {
    this.tree.addVertex(pRoot);
    this.root = pRoot;
  }

  public ASTree() { }

  public DirectedGraph<GMNode, GMEdge> asGraph() {
    return this.tree;
  }

  private void appendTree(ASTree pTree) {
    for(GMNode node : pTree.tree.vertexSet())
      this.tree.addVertex(node);
    for(GMEdge edge : pTree.tree.edgeSet())
      this.tree.addEdge(edge.getV1(), edge.getV2(), edge);
  }

  public void addTree(ASTree pTree) {
    appendTree(pTree);
    this.tree.addEdge(pTree.getRoot(), root,
        new GMEdge(pTree.getRoot(), root, GMEdgeLabel.SYNTACTIC));
  }

  public void addTree(ASTree pTree, GMNode connectorNode) {
    appendTree(pTree);
    this.tree.addVertex(connectorNode);
    this.tree.addEdge(connectorNode, root,
        new GMEdge(connectorNode, root, GMEdgeLabel.SYNTACTIC));
    this.tree.addEdge(pTree.getRoot(), connectorNode,
        new GMEdge(pTree.getRoot(), connectorNode, GMEdgeLabel.SYNTACTIC));
  }

  public GMNode getRoot() {
    return this.root;
  }

}
