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
package org.sosy_lab.cpachecker.core.algorithm.graphgen;

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
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DirectedMultigraph;
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
import org.sosy_lab.cpachecker.cpa.astcollector.ASTCollectorState;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTCollectorState.CFAEdgeInfo;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTEdge;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTEdgeLabel;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTNode;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTree;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.DefinitionPoint;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Created by zenscr on 26/11/15.
 */
@Options(prefix = "graphgen")
public class GraphGeneratorAlgorithm implements Algorithm {

  private final LogManager logger;

  private final Algorithm algorithm;

  @Option(secure=true, name = "gmOutputFile", description = "Output file of Graph Model")
  @FileOption(Type.OUTPUT_FILE)
  private Path gmOutputFile = Paths.get("output/gm.dot");

  public GraphGeneratorAlgorithm(Algorithm pAlgorithm, LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) {
    logger = pLogger;
    algorithm = pAlgorithm;
  }


//  private void pruneBlankNodes(DirectedGraph<ASTNode, ASTEdge> pGMGraph) {
//
//    Set<ASTEdge> edgesToRemove = new HashSet<>();
//    Set<ASTNode> nodesToRemove = new HashSet<>();
//
//    for(ASTNode node : pGMGraph.vertexSet()) {
//      if(node.isBlank()) {
//
//        assert pGMGraph.outDegreeOf(node) == 1;
//
//        for(ASTEdge e : pGMGraph.outgoingEdgesOf(node)) {
//          ASTNode target = e.getTargetNode();
//          edgesToRemove.add(e);
//          for(ASTEdge sourceEdge : pGMGraph.incomingEdgesOf(node)) {
//            ASTNode source = sourceEdge.getSourceNode();
//            Set<ASTEdgeLabel> labels = new HashSet<>();
//            labels.addAll(e.getAstEdgeLabels());
//            labels.addAll(sourceEdge.getAstEdgeLabels());
//            pGMGraph.addEdge(source, target,
//                new ASTEdge(source, target, new ArrayList<>(labels)));
//            edgesToRemove.add(sourceEdge);
//          }
//        }
//        nodesToRemove.add(node);
//
//      }
//    }
//    pGMGraph.removeAllEdges(edgesToRemove);
//    pGMGraph.removeAllVertices(nodesToRemove);
//  }

  private DirectedMultigraph<ASTNode, ASTEdge> generateCFGFromStates(Set<ASTCollectorState> states) {
    DirectedMultigraph<ASTNode, ASTEdge> result = new DirectedMultigraph<>(ASTEdge.class);
    Map<Integer, ASTNode> sourceNodeToRoot = new HashMap<>();
    // Add all the ASTs to the graph
    for(ASTCollectorState s : states) {
      if(s.isInit())
        continue;
      for(CFAEdgeInfo e : s.getCfaEdgeInfoSet()) {
        int source = e.getSource();
        // If there are multiple edges associated with an AST, add only one
        if(!sourceNodeToRoot.containsKey(source))
          sourceNodeToRoot.put(source, s.getTree().getRoot());
      }
      boolean modified = Graphs.addGraph(result, s.getTree().asGraph());
      assert modified;
    }
    // Add control-flow edges
    for(ASTCollectorState s : states) {
      if(s.isInit())
        continue;
      for(CFAEdgeInfo e : s.getCfaEdgeInfoSet()) {
        int target = e.getTarget();
        if(sourceNodeToRoot.containsKey(target)) {
          ASTNode sourceRoot = s.getTree().getRoot();
          ASTNode targetRoot = sourceNodeToRoot.get(target);
          ASTEdge edge = new ASTEdge(sourceRoot, targetRoot,
              ASTEdgeLabel.CONTROL_FLOW);
          edge.setTruthValue(e.getAssumption());
          result.addEdge(sourceRoot, targetRoot, edge);
        } else {
          // do nothing
        }
      }
    }
    return result;
  }

  private void addDataDependenceEdges(Table<Integer, Integer, ASTCollectorState> states,
      DirectedMultigraph<ASTNode, ASTEdge> pGM, Map<Integer, Set<AbstractState>> statesPerNode) {
    Map<Integer, ReachingDefState> reachDef = collectReachDef(statesPerNode);
    for(ASTCollectorState s : states.values()) {
      ASTNode targetRoot = s.getTree().getRoot();
      for(CFAEdgeInfo e : s.getCfaEdgeInfoSet()) {
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
              ASTNode sourceRoot = states.get(pdp.getDefinitionEntryLocation().getNodeNumber(),
                  pdp.getDefinitionExitLocation().getNodeNumber()).getTree().getRoot();
              pGM.addEdge(sourceRoot, targetRoot, new ASTEdge(sourceRoot, targetRoot, ASTEdgeLabel.DATA_DEPENDENCE));
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

    // Fill data structures
    Set<ASTCollectorState> states = new HashSet<>();
    Table<Integer, Integer, ASTCollectorState> edgeToState = HashBasedTable.create();
    Map<Integer, Set<AbstractState>> locToAbstractState = new HashMap<>();

    for(AbstractState absState : reachedSet.asCollection()) {

      ARGState state = (ARGState)absState;
      CompositeState compState = (CompositeState)state.getWrappedState();
      for(AbstractState child : compState.getWrappedStates()) {

        if(child instanceof ASTCollectorState) {
          ASTCollectorState gmState = (ASTCollectorState)child;
          for(CFAEdgeInfo e : gmState.getCfaEdgeInfoSet())
            edgeToState.put(e.getSource(), e.getTarget(), gmState);
          states.add(gmState);
        }

        if(child instanceof LocationState) {
          LocationState locState = (LocationState)child;
          int nodeNum = locState.getLocationNode().getNodeNumber();
          if(!locToAbstractState.containsKey(nodeNum))
            locToAbstractState.put(nodeNum, new HashSet<AbstractState>());
          locToAbstractState.get(nodeNum).add(absState);
        }

      }
    }

    // Create graph representation
    DirectedMultigraph<ASTNode, ASTEdge> gm = generateCFGFromStates(states);
    //addDataDependenceEdges(astLocStates, gm, statesPerNode);
    //pruneBlankNodes(gm);

    DOTExporter<ASTNode, ASTEdge> dotExp = new DOTExporter<>(
        new VertexNameProvider<ASTNode>() {
          @Override
          public String getVertexName(ASTNode o) {
            return String.valueOf(o.getId());
          }
        },
        new VertexNameProvider<ASTNode>() {
          @Override
          public String getVertexName(ASTNode o) {
            return o.toString();
            //return String.valueOf(o.getDepth());
          }
        },
        new EdgeNameProvider<ASTEdge>() {
          @Override
          public String getEdgeName(ASTEdge o) {
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
