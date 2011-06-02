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
/**
 *
 */
package org.sosy_lab.cpachecker.core.algorithm.cbmctools;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.ProcessExecutor;

import com.google.common.base.Preconditions;

class CBMCExecutor extends ProcessExecutor<RuntimeException> {

  // TODO function name
  private static final String[] CBMC_ARGS = {"cbmc", "--function", "main_0", "--32"};

  private Boolean result = null;

  public CBMCExecutor(LogManager logger, File file, String mainFunctionName) throws IOException {
    super(logger, RuntimeException.class, getCmdline(file, mainFunctionName));
  }

  private static String[] getCmdline(File file, String mainFunctionName) {
    Preconditions.checkArgument(file.canRead());

    CBMC_ARGS[2] = mainFunctionName + "_0";

    String[] result = Arrays.copyOf(CBMC_ARGS, CBMC_ARGS.length + 1);
    result[result.length-1] = file.getAbsolutePath();
    return result;
  }

  @Override
  protected void handleExitCode(int pCode) {
    switch (pCode) {
    case 0: // Verification successful (Path is infeasible)
      result = false;
      break;

    case 10: // Verification failed (Path is feasible)
      result = true;
      break;

    default:
      super.handleExitCode(pCode);
    }
  }

  @Override
  protected void handleErrorOutput(String pLine) throws RuntimeException {
    if (!(pLine.startsWith("Verified ") && pLine.endsWith("original clauses."))) {
      // exclude the normal status output of CBMC
      super.handleErrorOutput(pLine);
    }
  }

  public Boolean getResult() {
    checkState(isFinished());
    return result;
  }
}