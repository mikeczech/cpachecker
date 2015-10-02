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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;

public enum CFAEdgeLabel implements Serializable {

  // Statements
  DECL,
  STMT,
  VAR,
  FUNC,
  ASSUME,
  CAST,
  ASSIGN,
  RETURN,
  BLANK,
  FUNC_CALL,
  FUNC_RETURN,


  // Types
  UNSIGNED,
  INT,
  FLOAT,
  PTR,
  VOID,
  ARRAY,

  // Expression
  COMPARISON,
  ARITHMETIC,
  LOGICAL,
  BIT_OPERATION,
  LITERAL,
  ADDRESS,
  SIZEOF,
  ID,
  COMPLEX,
  ARRAY_SUBSCRIPT,
  VERIFIER_ERROR_CALL_ID,
  TYPE_ID

}