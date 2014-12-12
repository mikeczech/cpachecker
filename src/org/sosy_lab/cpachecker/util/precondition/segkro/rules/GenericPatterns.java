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
package org.sosy_lab.cpachecker.util.precondition.segkro.rules;

import static org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstPatternBuilder.*;

import org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstPattern;
import org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstPatternSelection;

final class GenericPatterns {

  public static SmtAstPattern f_of_x (final String pBindFunctionTo, final String pBindArgTo) {
    return matchBind(pBindFunctionTo,
        or(
          match("select",
              matchAnyWithAnyArgs(),
              matchInSubtree(
                  matchAnyWithAnyArgsBind(pBindArgTo)))
                  ,
          matchAny(
              matchAny(
                  matchInSubtree(
                      matchAnyWithAnyArgsBind(pBindArgTo))))
            ));
  }

  public static SmtAstPatternSelection f_of_x_selection (final String pBindFunctionTo, final String pBindArgTo) {
    return or(
        matchBind("not", pBindFunctionTo,
            match("not",
              matchAny(
                  match("select",
                      matchAnyWithAnyArgs(),
                      matchInSubtree(
                          matchAnyWithAnyArgsBind(pBindArgTo))),
                  matchAnyWithAnyArgs()))),
        matchBind("not", pBindFunctionTo,
            matchAny(
                match("select",
                    matchAnyWithAnyArgs(),
                    matchInSubtree(
                        matchAnyWithAnyArgsBind(pBindArgTo))),
                matchAnyWithAnyArgs())),
        matchAnyBind(pBindFunctionTo,
            match("select",
                matchAnyWithAnyArgs(),
                matchInSubtree(
                    matchAnyWithAnyArgsBind(pBindArgTo))),
            matchAnyWithAnyArgs()),
        matchBind("not", pBindFunctionTo,
            matchAny(
                matchAny(
                    matchInSubtree(
                        matchAnyWithAnyArgsBind(pBindArgTo))),
                matchAnyWithAnyArgs())),
        matchAnyBind(pBindFunctionTo,
            matchAny(
                matchInSubtree(
                    matchAnyWithAnyArgsBind(pBindArgTo))))
        );
  }

}
