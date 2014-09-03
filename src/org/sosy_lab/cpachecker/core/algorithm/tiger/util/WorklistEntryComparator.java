/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import java.util.Comparator;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;


public class WorklistEntryComparator implements Comparator<Entry<Integer, Goal>> {

  public static final WorklistEntryComparator ORDER_RESPECTING_COMPARATOR = new WorklistEntryComparator(true);
  public static final WorklistEntryComparator ORDER_INVERTING_COMPARATOR = new WorklistEntryComparator(false);

  private int factor = 1;

  public WorklistEntryComparator(Boolean doNotInvertOrder) {
    if (!doNotInvertOrder) {
      factor = -1;
    }
  }

  @Override
  public int compare(Entry<Integer, Goal> pArg0, Entry<Integer, Goal> pArg1) {
    return (factor * (pArg0.getKey() - pArg1.getKey()));
  }

}