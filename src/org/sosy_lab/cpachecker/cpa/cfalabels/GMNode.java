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

/**
 * Created by zenscr on 24/11/15.
 */
public class GMNode {

  private static int idCounter = 0;

  private final int id;

  private List<GMNodeLabel> labels = new ArrayList<>();

  public GMNode(GMNodeLabel pLabel) {
    this();
    this.labels.add(pLabel);
  }

  public GMNode() {
    id = idCounter;
    idCounter++;
  }

  public boolean isBlank() {
    if(labels.contains(GMNodeLabel.BLANK)) {
      assert labels.size() == 1;
      return true;
    }
    return false;
  }

  public List<GMNodeLabel> getLabels() {
    return this.labels;
  }

  public void addLabel(GMNodeLabel pLabel) {
    this.labels.add(pLabel);
  }

  @Override
  public String toString() {
    StringBuilder labelList = new StringBuilder();
    for (GMNodeLabel label : labels) {
      labelList.append(label.name() + ",");
    }
    return new String(labelList.deleteCharAt(labelList.length() - 1));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    GMNode gmNode = (GMNode)o;

    if (id != gmNode.id) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return id;
  }
}
