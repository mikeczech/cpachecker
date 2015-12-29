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

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.graph.DefaultEdge;

/**
 * Created by zenscr on 24/11/15.
 */
public class ASTEdge extends DefaultEdge {

  private final ASTNode sourceNode;

  private final ASTNode targetNode;

  private final ASTEdgeLabel astEdgeLabel;

  private boolean truthValue = true;

  public ASTEdge(ASTNode pSourceNode, ASTNode pTargetNode, ASTEdgeLabel pEdgeLabel) {
    this.sourceNode = pSourceNode;
    this.targetNode = pTargetNode;
    this.astEdgeLabel = pEdgeLabel;
  }

  public ASTEdgeLabel getAstEdgeLabel() {
    return astEdgeLabel;
  }

  public ASTNode getSourceNode() {
    return sourceNode;
  }

  public ASTNode getTargetNode() {
    return targetNode;
  }

  public boolean getTruthValue() {
    return truthValue;
  }

  public ASTEdgeLabel getTruthLabel() {
    if(truthValue)
      return ASTEdgeLabel.TRUE;
    else
      return ASTEdgeLabel.FALSE;
  }

  public boolean equalAttributes(ASTEdge edge) {
    return astEdgeLabel == edge.getAstEdgeLabel() && truthValue == edge.getTruthValue();
  }

  public void setTruthValue(boolean pTruthValue) {
    truthValue = pTruthValue;
  }

  public String toString() {
    return astEdgeLabel.name() + " (" + (truthValue ? "T" : "F") + ")";
  }

}
