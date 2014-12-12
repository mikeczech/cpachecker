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
package org.sosy_lab.cpachecker.util.precondition.segkro;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.RuleEngine;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.Lists;

public class ExtractNewPredsTest0 extends SolverBasedTest0 {

  private ExtractNewPreds enp;
  private RuleEngine rulesEngine;

  private FormulaManagerView mgrv;
  private ArrayFormulaManagerView afm;
  private BooleanFormulaManagerView bfm;
  private NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifm;

  private BooleanFormula _i1_EQUAL_0;
  private BooleanFormula _b0_at_i1_NOTEQUAL_0;
  private IntegerFormula _a0_at_i1;
  private IntegerFormula _b0_at_i1;
  private BooleanFormula _i1_GEQ_al0;
  private BooleanFormula _a0_at_i1_EQUAL_b0_at_i1;
  private IntegerFormula _i1_plus_1;
  private BooleanFormula _i0_EQUAL_i1_plus_1;
  private BooleanFormula _b0_at_i0_EQUAL_0;
  private IntegerFormula _b0_at_i0;
  private BooleanFormula _i2_EQUAL_0;
  private IntegerFormula _b0_at_i2;
  private BooleanFormula _b0_at_i2_NOTEQUAL_0;
  private BooleanFormula _i2_GEQ_al0;
  private BooleanFormula _a1_at_i2_EQUAL_b0_at_i2;
  private IntegerFormula _i2_plus_1;
  private BooleanFormula _i1_EQUAL_i2_plus_1;
  private BooleanFormula _b0_at_i0_NOTEQUAL_0;
  private BooleanFormula _i0_LESS_al0;
  private IntegerFormula _a1_at_i2;

  private BooleanFormula _safeTrace;
  private BooleanFormula _errorTrace;
  private IntegerFormula _0;
  private IntegerFormula _1;
  private IntegerFormula _i;
  private IntegerFormula _al;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b0;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b;


  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  @Before
  public void setUp() throws Exception {
    mgrv = new FormulaManagerView(mgr, config, logger);
    afm = mgrv.getArrayFormulaManager();
    bfm = mgrv.getBooleanFormulaManager();
    ifm = mgrv.getIntegerFormulaManager();

    Solver solver = new Solver(mgrv, factory);
    rulesEngine = new RuleEngine(logger, mgr, mgrv, solver);
    enp = new ExtractNewPreds(mgr, mgrv, rulesEngine);

    setupTestdata();
  }

  public void setupTestdata() {
    _0 = ifm.makeNumber(0);
    _1 = ifm.makeNumber(1);

    _i = mgrv.makeVariable(NumeralType.IntegerType, "i");
    _al = mgrv.makeVariable(NumeralType.IntegerType, "al");
    _b = afm.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);

    IntegerFormula _i0 = mgrv.makeVariable(NumeralType.IntegerType, "i0");
    IntegerFormula _i1 = mgrv.makeVariable(NumeralType.IntegerType, "i1");
    IntegerFormula _i2 = mgrv.makeVariable(NumeralType.IntegerType, "i2");
    IntegerFormula _al0 = mgrv.makeVariable(NumeralType.IntegerType, "al0");

    ArrayFormula<IntegerFormula, IntegerFormula> _a0 = afm.makeArray("a0", NumeralType.IntegerType, NumeralType.IntegerType);
    ArrayFormula<IntegerFormula, IntegerFormula> _a1 = afm.makeArray("a1", NumeralType.IntegerType, NumeralType.IntegerType);
    ArrayFormula<IntegerFormula, IntegerFormula> _b0 = afm.makeArray("b0", NumeralType.IntegerType, NumeralType.IntegerType);

    _i0_LESS_al0 = ifm.lessThan(_i0, _al0);
    _i1_plus_1 = ifm.add(_i1, _1);
    _i2_plus_1 = ifm.add(_i2, _1);
    _a0_at_i1 = afm.select(_a0, _i1);
    _a1_at_i2 = afm.select(_a1, _i2);
    _b0_at_i1 = afm.select(_b0, _i1);
    _b0_at_i0 = afm.select(_b0, _i0);
    _i1_EQUAL_0 = ifm.equal(_i1, _0);
    _b0_at_i1_NOTEQUAL_0 = ifm.equal(_b0_at_i1, _0);
    _i1_GEQ_al0 = ifm.greaterOrEquals(_i1, _al0);
    _a0_at_i1_EQUAL_b0_at_i1 = ifm.equal(_a0_at_i1, _b0_at_i1);
    _i0_EQUAL_i1_plus_1 = ifm.equal(_i0, _i1_plus_1);
    _b0_at_i0_EQUAL_0 = ifm.equal(_b0_at_i0, _0);
    _b0_at_i0_NOTEQUAL_0 = bfm.not(_b0_at_i0_EQUAL_0);
    _i2_EQUAL_0 = ifm.equal(_i2, _0);
    _b0_at_i2 = afm.select(_b0, _i2);
    _b0_at_i2_NOTEQUAL_0 = bfm.not(ifm.equal(_b0_at_i2, _0));
    _i2_GEQ_al0 = ifm.greaterOrEquals(_i2, _al0);
    _a1_at_i2_EQUAL_b0_at_i2 = ifm.equal(_a1_at_i2, _b0_at_i2);
    _i1_EQUAL_i2_plus_1 = ifm.equal(_i1, _i2_plus_1);

    _safeTrace = bfm.and(Lists.newArrayList(
        _i1_EQUAL_0,
        _b0_at_i1_NOTEQUAL_0,
        _i1_GEQ_al0,
        _a0_at_i1_EQUAL_b0_at_i1,
        _i0_EQUAL_i1_plus_1,
        _b0_at_i0_EQUAL_0));

    _errorTrace = bfm.and(Lists.newArrayList(
        _i2_EQUAL_0,
        _b0_at_i2_NOTEQUAL_0,
        _i2_GEQ_al0,
        _a1_at_i2_EQUAL_b0_at_i2,
        _i1_EQUAL_i2_plus_1,
        _b0_at_i1_NOTEQUAL_0,
        _i1_GEQ_al0,
        _a0_at_i1_EQUAL_b0_at_i1,
        _i0_EQUAL_i1_plus_1,
        _b0_at_i0_NOTEQUAL_0,
        _i0_LESS_al0));
  }

  @Test(timeout=5000)
  public void testOnSafeTrace1() throws SolverException, InterruptedException {
    ArrayFormula<IntegerFormula, IntegerFormula> _b = afm.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
    IntegerFormula _i = ifm.makeVariable("i");
    BooleanFormula _b_at_i_NOTEQ_0 = bfm.not(ifm.equal(afm.select(_b, _i), _0));
    BooleanFormula _i_plus_1_LESSEQ_al = ifm.lessOrEquals(ifm.add(_i, _1), _0);
    BooleanFormula _b_at_i_plus_1_EQ_0 = ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0);

    BooleanFormula _safeWp1 = bfm.and(Lists.newArrayList(
        _b_at_i_NOTEQ_0,        // b[i] != 0
        _i_plus_1_LESSEQ_al,    // i+1 <= al
        _b_at_i_plus_1_EQ_0));  // b[i+1] == 0

    //  (and
    //      (not (= (select b i) 0))
    //      (<= (+ i 1) 0)
    //      (= (select b (+ i 1)) 0))

    // Application of rules EXISTS and EXTEND_RIGHT?

    List<BooleanFormula> result = enp.extractNewPreds(_safeWp1);
    assertThat(result).isNotEmpty();
  }

  @Test(timeout=5000)
  public void testOnErrorTrace() throws SolverException, InterruptedException {
    //   (and (= i2 0)
    //        (not (= (select b0 i2) 0))
    //        (>= i2 al0)
    //        (= (select a1 i2) (select b0 i2))
    //        (= i1 (+ i2 1))
    //        (= (select b0 i1) 0)
    //        (>= i1 al0)

    // This is not supposed to terminate.
    // The algorithm is designed to work on WPs

    List<BooleanFormula> result = enp.extractNewPreds(_errorTrace);
    assertThat(result).isNotEmpty();
  }

  @Test(timeout=5000)
  public void testOnErrorWp1() throws SolverException, InterruptedException {
    BooleanFormula wpError = bfm.and(Lists.newArrayList(
        ifm.greaterOrEquals(ifm.add(_i, _1), _al),
        bfm.not(ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0)),
        ifm.lessThan(_i, _al),
        bfm.not(ifm.equal(afm.select(_b, _i), _0))
        ));

    List<BooleanFormula> result = enp.extractNewPreds(wpError);
    assertThat(result).isNotEmpty();
  }



}
