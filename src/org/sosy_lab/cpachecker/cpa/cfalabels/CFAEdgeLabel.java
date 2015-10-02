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
  TYPE,
  STRUCT,
  UNION,
  ENUM,
  COMPLEX,

  // Types
  UNSIGNED,
  INT,
  FLOAT,
  PTR,
  VOID,
  ARRAY,
  LONG,
  FUNCTION_TYPE,

  // Expression
  COMPARISON,
  ARITHMETIC,
  LOGICAL,
  BIT_OPERATION,
  LITERAL,
  ADDRESS,
  FIELD_REFERENCE,
  SIZEOF,
  ID,
  COMPLEX_NUMBER,
  ARRAY_SUBSCRIPT,
  VERIFIER_ASSERT_CALL_ID,
  VERIFIER_ERROR_CALL_ID,
  VERIFIER_NONDET_ID,
  TYPE_ID

}