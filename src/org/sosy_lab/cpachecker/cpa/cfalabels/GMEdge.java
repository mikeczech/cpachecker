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

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.graph.DefaultEdge;

/**
 * Created by zenscr on 24/11/15.
 */
public class GMEdge extends DefaultEdge {

  private GMNode v1;

  private GMNode v2;

  private List<GMEdgeLabel> gmEdgeLabels = new ArrayList<>();

  public GMEdge(GMNode pV1, GMNode pV2, List<GMEdgeLabel> pEdgeLabels) {
    this.v1 = pV1;
    this.v2 = pV2;
    this.gmEdgeLabels.addAll(pEdgeLabels);
  }

  public GMEdge(GMNode pV1, GMNode pV2, GMEdgeLabel pEdgeLabel) {
    this.v1 = pV1;
    this.v2 = pV2;
    this.gmEdgeLabels.add(pEdgeLabel);
  }

  public GMNode getV1() {
    return v1;
  }

  public GMNode getV2() {
    return v2;
  }

  public void addLabel(GMEdgeLabel pLabel) {
    gmEdgeLabels.add(pLabel);
  }

  public String toString() {
    StringBuilder labelList = new StringBuilder();
    for (GMEdgeLabel label : gmEdgeLabels) {
      labelList.append(label.name() + ",");
    }
    return new String(labelList.deleteCharAt(labelList.length() - 1));
  }

}
