/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.featurevariables;

import java.util.logging.Level;

import javax.management.QueryExp;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableElement;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class FeatureVarsElement implements AbstractQueryableElement, Cloneable {

  static BiMap<String, Region> regionMap = HashBiMap.create();
  static RegionManager manager = BDDRegionManager.getInstance();
  
  final Region currentState;
  
  public FeatureVarsElement( Region currentState ) {
    this.currentState = currentState;
  }
  
  public Region getRegion() {
    return currentState;
  }
  
  public Region getVariableRegion(String pVarName) {
    Region ret = regionMap.get(pVarName);
    if (ret == null) {
      FeatureVarsCPA.logger.log(Level.INFO, "FeatureVars tracks: " + pVarName);
      //System.out.println("FeatureVars tracks: " + pVarName);
      ret = manager.createPredicate();
      regionMap.put(pVarName, ret);      
    }
    return ret;
  }
  
  @Override
  public String toString() {
    return FeatureVarsElement.regionToString(currentState);
  }
  
  /**
   * Returns a String representation of the parameter.
   * It is important that every predicate in the region is contained in the regionMap.
   * @param r
   * @return
   */
  static String regionToString(Region r) {
    if (regionMap.containsValue(r)) {
      return regionMap.inverse().get(r);
    } else if (manager.isFalse(r))
      return "FALSE";
    else if (manager.isTrue(r))
      return "TRUE";
    else {
      Triple<Region, Region, Region> triple = manager.getIfThenElse(r);
      String predName = regionMap.inverse().get(triple.getFirst());
      String ifTrue = "";
      if (manager.isFalse(triple.getSecond())) {
        // omit
      } else if (manager.isTrue(triple.getSecond())) {
        ifTrue = predName;
      } else {
        ifTrue = predName + " & " + regionToString(triple.getSecond());
      }
      String ifFalse = "";
      if (manager.isFalse(triple.getThird())) {
        // omit
      } else if (manager.isTrue(triple.getThird())) {
        ifFalse = "!" + predName;
      } else {
        ifFalse = "!" + predName + " & " + regionToString(triple.getThird());
      }
      if (ifTrue != "" && ifFalse != "") {
        return "(" + ifTrue + ") | (" + ifFalse + ")";
      } else if (ifTrue == "") {
        return ifFalse;
      } else if (ifFalse == "") {
        return ifTrue;
      } else {
        throw new InternalError("Both BDD Branches are empty!?");
      }
    }
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    throw new InvalidQueryException("Feature Vars Element cannot check anything");
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals("VALUES")) {
      return regionToString(this.currentState);
    } else
    throw new InvalidQueryException("Feature Vars Element can only return the current values (\"VALUES\")");
  }

  @Override
  public String getCPAName() {
    return "FeatureVars";
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    throw new InvalidQueryException("Feature Vars Element cannot be modified");
  }

}
