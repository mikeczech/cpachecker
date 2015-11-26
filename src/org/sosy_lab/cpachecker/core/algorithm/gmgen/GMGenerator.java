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
package org.sosy_lab.cpachecker.core.algorithm.gmgen;

import java.util.logging.Level;

import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.cfalabels.CFALabelsCPA;
import org.sosy_lab.cpachecker.cpa.cfalabels.CFALabelsState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Created by zenscr on 26/11/15.
 */
@Options(prefix = "gmgen")
public class GMGenerator implements Algorithm {

  private final LogManager logger;

  private final Algorithm algorithm;

  @Option(secure=true, name = "gmOutputFile", description = "Output file of Graph Model")
  @FileOption(Type.OUTPUT_FILE)
  private Path gmOutputFile = Paths.get("gm.dot");

  public GMGenerator(Algorithm pAlgorithm, LogManager pLogger, ConfigurableProgramAnalysis pCpa) {
    logger = pLogger;
    algorithm = pAlgorithm;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    AlgorithmStatus result = algorithm.run(reachedSet);
    logger.log(Level.INFO, "GM generator algorithm started.");

    for(AbstractState absState : reachedSet.asCollection()) {
      ARGState state = (ARGState)absState;
      CompositeState compState = (CompositeState)state.getWrappedState();
      for(AbstractState child : compState.getWrappedStates()) {
        if(child instanceof CFALabelsState) {
          CFALabelsState gmState = (CFALabelsState)child;

        }
      }
    }

    logger.log(Level.INFO, "GM generator algorithm finished.");
    return result;
  }
}
