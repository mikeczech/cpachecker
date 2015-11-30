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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
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
import org.sosy_lab.cpachecker.cpa.cfalabels.CFALabelsState.EdgeInfo;
import org.sosy_lab.cpachecker.cpa.cfalabels.GMEdge;
import org.sosy_lab.cpachecker.cpa.cfalabels.GMEdgeLabel;
import org.sosy_lab.cpachecker.cpa.cfalabels.GMNode;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.DefinitionPoint;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
import org.sosy_lab.cpachecker.cpa.seplogic.interfaces.Handle;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * Created by zenscr on 26/11/15.
 */
@Options(prefix = "gmgen")
public class GMGeneratorAlgorithm implements Algorithm {

  private final LogManager logger;

  private final Algorithm algorithm;

  @Option(secure=true, name = "gmOutputFile", description = "Output file of Graph Model")
  @FileOption(Type.OUTPUT_FILE)
  private Path gmOutputFile = Paths.get("output/gm.dot");

  public GMGeneratorAlgorithm(Algorithm pAlgorithm, LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) {
    logger = pLogger;
    algorithm = pAlgorithm;
  }

  private void pruneBlankNodes(DirectedGraph<GMNode, GMEdge> pGMGraph) {
    Set<GMEdge> edgesToRemove = new HashSet<>();
    Set<GMNode> nodesToRemove = new HashSet<>();
    for(GMNode node : pGMGraph.vertexSet()) {
      if(node.isBlank()) {
        assert pGMGraph.outDegreeOf(node) == 1;
        for(GMEdge targetEdge : pGMGraph.outgoingEdgesOf(node)) {
          GMNode target = targetEdge.getV2();
          edgesToRemove.add(targetEdge);
          for(GMEdge sourceEdge : pGMGraph.incomingEdgesOf(node)) {
            GMNode source = sourceEdge.getV1();
            Set<GMEdgeLabel> labels = new HashSet<>();
            labels.addAll(targetEdge.getGmEdgeLabels());
            labels.addAll(sourceEdge.getGmEdgeLabels());
            pGMGraph.addEdge(source, target,
                new GMEdge(source, target, new ArrayList<>(labels)));
            edgesToRemove.add(sourceEdge);
          }
        }
        nodesToRemove.add(node);
      }
    }
    pGMGraph.removeAllEdges(edgesToRemove);
    pGMGraph.removeAllVertices(nodesToRemove);
  }

  private DirectedGraph<GMNode, GMEdge> generateCFGFromStates(Collection<CFALabelsState> states) {
    DirectedGraph<GMNode, GMEdge> result = new DefaultDirectedGraph<>(GMEdge.class);
    Map<Integer, List<GMNode>> stateTable = new HashMap<>();
    for(CFALabelsState s : states) {
      if(s.isInit())
        continue;
      for(EdgeInfo e : s.getEdgeInfoSet()) {
        int source = e.getSource();
        if(!stateTable.containsKey(source))
          stateTable.put(source, new ArrayList<GMNode>());
        stateTable.get(source).add(s.getTree().getRoot());
      }
      boolean modified = Graphs.addGraph(result, s.getTree().asGraph());
      assert modified;
    }
    for(CFALabelsState s : states) {
      if(s.isInit())
        continue;
      for(EdgeInfo e : s.getEdgeInfoSet()) {
        int target = e.getTarget();
        if(stateTable.containsKey(target)) {
          GMNode sourceRoot = s.getTree().getRoot();
          for(GMNode targetRoot : stateTable.get(target)) {
            GMEdge edge = new GMEdge(sourceRoot, targetRoot,
                GMEdgeLabel.CONTROL_FLOW);
            for(GMEdgeLabel l : e.getLabels())
              edge.addLabel(l);
            result.addEdge(sourceRoot, targetRoot, edge);
          }
        }
      }
    }
    return result;
  }

  private void addDataDependenceEdges(Table<Integer, Integer, CFALabelsState> states,
      DirectedGraph<GMNode, GMEdge> pGM, Map<Integer, Set<AbstractState>> statesPerNode) {
    Map<Integer, ReachingDefState> reachDef = collectReachDef(statesPerNode);
    for(CFALabelsState s : states.values()) {
      GMNode targetRoot = s.getTree().getRoot();
      for(EdgeInfo e : s.getEdgeInfoSet()) {
        ReachingDefState reachDefState = reachDef.get(e.getSource());
        for(String var : s.getVariables()) {

          Set<DefinitionPoint> local = reachDefState.getLocalReachingDefinitions().get(var);
          Set<DefinitionPoint> global = reachDefState.getGlobalReachingDefinitions().get(var);
          List<DefinitionPoint> defPoints = new ArrayList<>();
          if(local != null)
            defPoints.addAll(local);
          if(global != null)
            defPoints.addAll(global);

          // Create for each program definition point a data dependence edge
          for(DefinitionPoint p : defPoints) {
            if(p instanceof ProgramDefinitionPoint) {
              ProgramDefinitionPoint pdp = (ProgramDefinitionPoint)p;
              GMNode sourceRoot = states.get(pdp.getDefinitionEntryLocation().getNodeNumber(),
                  pdp.getDefinitionExitLocation().getNodeNumber()).getTree().getRoot();
              pGM.addEdge(sourceRoot, targetRoot, new GMEdge(sourceRoot, targetRoot, GMEdgeLabel.DATA_DEPENDENCE));
            }
          }
        }
      }
    }
  }

  private Map<Integer, ReachingDefState> collectReachDef(Map<Integer, Set<AbstractState>> statesPerNode) {
    Map<Integer, ReachingDefState> result = new HashMap<>();
    for(Integer nodeNum : statesPerNode.keySet()) {
      Set<ReachingDefState> reachDefStates = new HashSet<>();
      // Collect ReachDef states for node
      for(AbstractState absState : statesPerNode.get(nodeNum)) {
        ARGState state = (ARGState)absState;
        CompositeState compState = (CompositeState)state.getWrappedState();
        for(AbstractState child : compState.getWrappedStates()) {
          if (child instanceof ReachingDefState) {
            ReachingDefState reachDef = (ReachingDefState)child;
            reachDefStates.add(reachDef);
          }
        }
      }
      // Merge ReachDef states for node
      Map<String, Set<DefinitionPoint>> localReachDef = new HashMap<>();
      Map<String, Set<DefinitionPoint>> globalReachDef = new HashMap<>();
      for(ReachingDefState rdState : reachDefStates) {
        for(String var : rdState.getLocalReachingDefinitions().keySet()) {
          if(!localReachDef.containsKey(var))
            localReachDef.put(var, new HashSet<DefinitionPoint>());
          localReachDef.get(var).addAll(rdState.getLocalReachingDefinitions().get(var));
        }
        for(String var : rdState.getGlobalReachingDefinitions().keySet()) {
          if(!globalReachDef.containsKey(var))
            globalReachDef.put(var, new HashSet<DefinitionPoint>());
          globalReachDef.get(var).addAll(rdState.getGlobalReachingDefinitions().get(var));
        }
      }
      result.put(nodeNum, new ReachingDefState(localReachDef, globalReachDef, null));
    }
    return result;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    AlgorithmStatus result = algorithm.run(reachedSet);
    logger.log(Level.INFO, "GM generator algorithm started.");

    Table<Integer, Integer, CFALabelsState> astLocStates = HashBasedTable.create();
    Map<Integer, Set<AbstractState>> statesPerNode = new HashMap<>();
    for(AbstractState absState : reachedSet.asCollection()) {
      ARGState state = (ARGState)absState;
      CompositeState compState = (CompositeState)state.getWrappedState();
      for(AbstractState child : compState.getWrappedStates()) {
        if(child instanceof CFALabelsState) {
          CFALabelsState gmState = (CFALabelsState)child;
          for(EdgeInfo e : gmState.getEdgeInfoSet())
            astLocStates.put(e.getSource(), e.getTarget(), gmState);
        }
        if(child instanceof LocationState) {
          LocationState locState = (LocationState)child;
          int nodeNum = locState.getLocationNode().getNodeNumber();
          if(!statesPerNode.containsKey(nodeNum))
            statesPerNode.put(nodeNum, new HashSet<AbstractState>());
          statesPerNode.get(nodeNum).add(absState);
        }
      }
    }
    DirectedGraph<GMNode, GMEdge> gm = generateCFGFromStates(astLocStates.values());
    addDataDependenceEdges(astLocStates, gm, statesPerNode);
    pruneBlankNodes(gm);

    DOTExporter<GMNode, GMEdge> dotExp = new DOTExporter<>(
        new IntegerNameProvider<GMNode>(),
        new VertexNameProvider<GMNode>() {
          @Override
          public String getVertexName(GMNode o) {
            return o.toString();
          }
        },
        new EdgeNameProvider<GMEdge>() {
          @Override
          public String getEdgeName(GMEdge o) {
            return o.toString();
          }
        });
    try {
      dotExp.export(new FileWriter(gmOutputFile.getPath()), gm);
    } catch (IOException e) {
      logger.logException(Level.ALL, e, "Cannot write DOT");
    }

    logger.log(Level.INFO, "GM generator algorithm finished.");
    return result;
  }
}
