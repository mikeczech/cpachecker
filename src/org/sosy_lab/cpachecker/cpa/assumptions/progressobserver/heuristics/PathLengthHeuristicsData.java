/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.heuristics;

import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula.PreventingHeuristicType;

public class PathLengthHeuristicsData implements StopHeuristicsData{

  private static long threshold = -1;

  private final int noOfNodes;

  public PathLengthHeuristicsData(int noOfNodes){
    this.noOfNodes = noOfNodes;
  }

  public PathLengthHeuristicsData()
  {
    noOfNodes = 0;
  }

  public PathLengthHeuristicsData updateForEdge(StopHeuristicsData pData, int pThreshold){

    int newValue = (((PathLengthHeuristicsData)pData).noOfNodes);
    newValue++;

    if ((pThreshold > 0) && (newValue > pThreshold)){
      setThreshold(pThreshold);
      return BOTTOM;
    }
    else{
      return new PathLengthHeuristicsData(newValue);
    }
  }

  public static final PathLengthHeuristicsData TOP = new PathLengthHeuristicsData() {
    @Override
    public boolean isTop() { return true; }
    @Override
    public PathLengthHeuristicsData updateForEdge(StopHeuristicsData pData, int pThreshold) { return this; }
    @Override
    public String toString() { return "TOP"; }
  };

  public static final PathLengthHeuristicsData BOTTOM = new PathLengthHeuristicsData() {
    @Override
    public boolean isBottom() { return true; }
    @Override
    public PathLengthHeuristicsData updateForEdge(StopHeuristicsData pData, int pThreshold) { return this; }
    @Override
    public String toString() { return "BOTTOM"; }
  };

  public void setThreshold(long newThreshold)
  {
    threshold = newThreshold;
  }

  @Override
  public long getThreshold()
  {
    return threshold;
  }

  @Override
  public PreventingHeuristicType getHeuristicType() {
    return PreventingHeuristicType.PATHLENGTH;
  }

  @Override
  public boolean isBottom() {
    return false;
  }

  @Override
  public boolean isLessThan(StopHeuristicsData pD) {
    return false;
  }

  @Override
  public boolean isTop() {
    return false;
  }

  @Override
  public boolean shouldTerminateAnalysis() {
    return false;
  }

}
