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

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

/**
 * Created by zenscr on 24/11/15.
 */
public class ASTree {

  private DirectedGraph<ASTNode, ASTEdge> tree = new DefaultDirectedGraph<>(ASTEdge.class);

  private ASTNode root = null;

  private Set<String> identifierList = new HashSet<>();

  public ASTree(ASTNode pRoot) {
    this.tree.addVertex(pRoot);
    this.root = pRoot;
    reinitASTNodeDepth();
  }

  public ASTree(ASTNode pRoot, String identifier) {
    this(pRoot);
    identifierList.add(identifier);
  }

  public ASTree() { }

  public DirectedGraph<ASTNode, ASTEdge> asGraph() {
    return this.tree;
  }

  public Set<String> getIdentifierList() {
    return identifierList;
  }

  // Initializes depth attribute of ASTNode objects
  private void reinitASTNodeDepth() {
    root.setDepth(0);
    for(ASTEdge e : tree.incomingEdgesOf(root))
      reinitASTNodeDepth(e);
  }

  private void reinitASTNodeDepth(ASTEdge edge) {
    ASTNode sourceNode = edge.getSourceNode();
    ASTNode targetNode = edge.getTargetNode();
    sourceNode.setDepth(targetNode.getDepth() + 1);
    for(ASTEdge e : tree.incomingEdgesOf(sourceNode))
      reinitASTNodeDepth(e);
  }
  private void appendTree(ASTree pTree) {
    for(ASTNode node : pTree.tree.vertexSet())
      this.tree.addVertex(node);
    for(ASTEdge edge : pTree.tree.edgeSet())
      this.tree.addEdge(edge.getSourceNode(), edge.getTargetNode(), edge);
    identifierList.addAll(pTree.identifierList);
  }

  public void addTree(ASTree pTree) {
    appendTree(pTree);
    this.tree.addEdge(pTree.getRoot(), root,
        new ASTEdge(pTree.getRoot(), root, ASTEdgeLabel.SYNTACTIC));
    reinitASTNodeDepth();
  }

  public void addTree(ASTree pTree, ASTNode connectorNode) {
    appendTree(pTree);
    this.tree.addVertex(connectorNode);
    this.tree.addEdge(connectorNode, root,
        new ASTEdge(connectorNode, root, ASTEdgeLabel.SYNTACTIC));
    this.tree.addEdge(pTree.getRoot(), connectorNode,
        new ASTEdge(pTree.getRoot(), connectorNode, ASTEdgeLabel.SYNTACTIC));
    reinitASTNodeDepth();
  }

  public ASTNode getRoot() {
    return this.root;
  }

}
