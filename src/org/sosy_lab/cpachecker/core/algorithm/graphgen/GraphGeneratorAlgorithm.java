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

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.GraphMLExporter;
import org.jgrapht.ext.IntegerEdgeNameProvider;
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
import org.sosy_lab.cpachecker.cpa.astcollector.ASTNodeLabel;
import org.sosy_lab.cpachecker.cpa.astcollector.ASTree;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
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

  @Option(secure=true, name = "graphOutputFile", description = "Output file of Graph Representation (DOT)")
  @FileOption(Type.OUTPUT_FILE)
  private Path graphOutputFile = Paths.get("output/vtask_graph.dot");

  @Option(secure=true, name = "graphMLOutputFile", description = "Output file of Graph Representation (GraphML)")
  @FileOption(Type.OUTPUT_FILE)
  private Path graphMLOutputFile = Paths.get("output/vtask_graph.graphml");

  @Option(secure=true, name = "nodeLabels", description = "Output file of labels of nodes")
  @FileOption(Type.OUTPUT_FILE)
  private Path nodeLabelsOutputFile = Paths.get("output/nodes.labels");

  @Option(secure=true, name = "edgeTypeLabels", description = "Output file of types of edges")
  @FileOption(Type.OUTPUT_FILE)
  private Path edgeTypesOutputFile = Paths.get("output/edge_types.labels");

  @Option(secure=true, name = "edgeTruthLabels", description = "Output file of truth values of edges")
  @FileOption(Type.OUTPUT_FILE)
  private Path edgeTruthOutputFile = Paths.get("output/edge_truth.labels");

  @Option(secure=true, name = "nodeDepthLabels", description = "Output file of depth values of nodes")
  @FileOption(Type.OUTPUT_FILE)
  private Path nodeDepthOutputFile = Paths.get("output/node_depth.labels");

  public GraphGeneratorAlgorithm(Algorithm pAlgorithm, LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) {
    logger = pLogger;
    algorithm = pAlgorithm;
  }

  /**
   * Prunes all nodes from a graph, which are associated with the label BLANK
   * @param pGraph
   */
  private void pruneBlankNodes(DirectedMultigraph<ASTNode, ASTEdge> pGraph) {
    Set<ASTNode> nodesToRemove = new HashSet<>();
    // add control flow edge between sources and targets of blank nodes
    for(ASTNode node : pGraph.vertexSet()) {
      if(node.isBlank()) {
        nodesToRemove.add(node);
      }
    }
    for(ASTNode node : nodesToRemove)
      removeASTFromGraph(pGraph, new ASTree(node), true);
  }

  /**
   * Takes a set of ASTCollector states and constructs a graph representation
   * containing statement ASTs ans control-flow edges between root nodes.
   * @param states
   * @return
   */
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

  /**
   * Writes a file containing a list of node id - label pairs
   * @param graph
   */
  private void exportNodeLabels(DirectedMultigraph<ASTNode, ASTEdge> graph) {
    PrintWriter wtr = null;
    try {
      wtr = new PrintWriter(nodeLabelsOutputFile.getPath());
      for(ASTNode node : graph.vertexSet()) {
        StringBuilder label = new StringBuilder();
        for (ASTNodeLabel l : node.getLabels()) {
          label.append(l.name() + "_");
        }
        wtr.println(String.format("%s,%s", node.getId(), new String(label.deleteCharAt(label.length() - 1))));
      }
    } catch (FileNotFoundException pE) {
      pE.printStackTrace();
    } finally {
      wtr.close();
    }
  }

  /**
   * Writes a file containing edge - type pairs
   * @param graph
   */
  private void exportEdgeTypeLabels(DirectedMultigraph<ASTNode, ASTEdge> graph) {
    PrintWriter wtr = null;
    try {
      wtr = new PrintWriter(edgeTypesOutputFile.getPath());
      for(ASTEdge edge : graph.edgeSet()) {
        wtr.println(String.format("%s,%s,%s", edge.getSourceNode().getId(), edge.getTargetNode().getId(), edge.getAstEdgeLabel().getValue()));
      }
    } catch (FileNotFoundException pE) {
      pE.printStackTrace();
    } finally {
      wtr.close();
    }
  }

  /**
   * Writes a file containing edge - truth value (CFG) pairs
   * @param graph
   */
  private void exportEdgeTruthLabels(DirectedMultigraph<ASTNode, ASTEdge> graph) {
    PrintWriter wtr = null;
    try {
      wtr = new PrintWriter(edgeTruthOutputFile.getPath());
      for(ASTEdge edge : graph.edgeSet()) {
        wtr.println(String.format("%s,%s,%s", edge.getSourceNode().getId(), edge.getTargetNode().getId(), edge.getTruthLabel().getValue()));
      }
    } catch (FileNotFoundException pE) {
      pE.printStackTrace();
    } finally {
      wtr.close();
    }
  }

  /**
   * Writes a file containing node - depth pairs
   * @param graph
   */
  private void exportNodeDepthLabels(DirectedMultigraph<ASTNode, ASTEdge> graph) {
    PrintWriter wtr = null;
    try {
      wtr = new PrintWriter(nodeDepthOutputFile.getPath());
      for(ASTNode node : graph.vertexSet()) {
        wtr.println(String.format("%s,%s", node.getId(), node.getDepth()));
      }
    } catch (FileNotFoundException pE) {
      pE.printStackTrace();
    } finally {
      wtr.close();
    }
  }

  /**
   * Removes an AST from a graph and reconnects incoming/outgoing edges
   * @param graph
   * @param tree
   */
  private void removeASTFromGraph(DirectedMultigraph<ASTNode, ASTEdge> graph, ASTree tree, boolean ignoreOutgoing) {

    ASTNode root = tree.getRoot();
    Set<ASTEdge> incomingEdges = graph.incomingEdgesOf(root);
    Set<ASTEdge> outgoingEdges = graph.outgoingEdgesOf(root);

    for(ASTEdge incoming : incomingEdges) {
      ASTNode source = incoming.getSourceNode();
      for(ASTEdge outgoing : outgoingEdges) {

        ASTNode target = outgoing.getTargetNode();
        if(ignoreOutgoing || incoming.equalAttributes(outgoing)) {
          ASTEdge newEdge = new ASTEdge(source, target, incoming.getAstEdgeLabel());
          newEdge.setTruthValue(incoming.getTruthValue());
          graph.addEdge(source, target, newEdge);
        } else {
          // If both edges do not have equal attrbutes, create two new edges
          ASTEdge newEdgeA = new ASTEdge(source, target, incoming.getAstEdgeLabel());
          newEdgeA.setTruthValue(incoming.getTruthValue());
          graph.addEdge(source, target, newEdgeA);

          ASTEdge newEdgeB = new ASTEdge(source, target, outgoing.getAstEdgeLabel());
          newEdgeB.setTruthValue(outgoing.getTruthValue());
          graph.addEdge(source, target, newEdgeB);
        }

      }
    }
    // Remove AST
    graph.removeAllEdges(tree.asGraph().edgeSet());
    graph.removeAllVertices(tree.asGraph().vertexSet());
    graph.removeAllEdges(incomingEdges);
    graph.removeAllEdges(outgoingEdges);
  }

  /**
   * Prunes global declarations which are neither used locally nor within other
   * global declarations. (it slightly reduces some graphs, but its not perfect though)
   * @param graph
   * @param globalStates
   * @param nonGlobalStates
   */
  private void pruneUnusedGlobalDeclarations(
      DirectedMultigraph<ASTNode, ASTEdge> graph,
      Set<ASTCollectorState> globalStates,
      Set<ASTCollectorState> nonGlobalStates) {

    Set<String> localIdentifiers = new HashSet<>();
    for(ASTCollectorState s : nonGlobalStates) {
        localIdentifiers.addAll(s.getTree().getIdentifiers());
    }

    for(ASTCollectorState s : globalStates) {
      boolean dependOnGlobal = false;
      for(ASTCollectorState ss : globalStates) {
        if(ss != s) {
          for(String id : s.getTree().getIdentifiers()) {
            if(ss.getTree().getIdentifiers().contains(id)) {
              dependOnGlobal = true;
              break;
            }
          }
        }
        if(dependOnGlobal)
          break;
      }
      if(dependOnGlobal)
        continue;
      Set<String> globalIdentifiers = s.getTree().getIdentifiers();
      if(!localIdentifiers.containsAll(globalIdentifiers)) {
        removeASTFromGraph(graph, s.getTree(), false);
      }
    }

  }

//  private void addDataDependenceEdges(Table<Integer, Integer, ASTCollectorState> states,
//      DirectedMultigraph<ASTNode, ASTEdge> pGM, Map<Integer, Set<AbstractState>> statesPerNode) {
//    Map<Integer, ReachingDefState> reachDef = collectReachDef(statesPerNode);
//    for(ASTCollectorState s : states.values()) {
//      ASTNode targetRoot = s.getTree().getRoot();
//      for(CFAEdgeInfo e : s.getCfaEdgeInfoSet()) {
//        ReachingDefState reachDefState = reachDef.get(e.getSource());
//        for(String var : s.getVariables()) {
//
//          Set<DefinitionPoint> local = reachDefState.getLocalReachingDefinitions().get(var);
//          Set<DefinitionPoint> global = reachDefState.getGlobalReachingDefinitions().get(var);
//          List<DefinitionPoint> defPoints = new ArrayList<>();
//          if(local != null)
//            defPoints.addAll(local);
//          if(global != null)
//            defPoints.addAll(global);
//
//          // Create for each program definition point a data dependence edge
//          for(DefinitionPoint p : defPoints) {
//            if(p instanceof ProgramDefinitionPoint) {
//              ProgramDefinitionPoint pdp = (ProgramDefinitionPoint)p;
//              ASTNode sourceRoot = states.get(pdp.getDefinitionEntryLocation().getNodeNumber(),
//                  pdp.getDefinitionExitLocation().getNodeNumber()).getTree().getRoot();
//              pGM.addEdge(sourceRoot, targetRoot, new ASTEdge(sourceRoot, targetRoot, ASTEdgeLabel.DATA_DEPENDENCE));
//            }
//          }
//        }
//      }
//    }
//  }
//
//  private Map<Integer, ReachingDefState> collectReachDef(Map<Integer, Set<AbstractState>> statesPerNode) {
//    Map<Integer, ReachingDefState> result = new HashMap<>();
//    for(Integer nodeNum : statesPerNode.keySet()) {
//      Set<ReachingDefState> reachDefStates = new HashSet<>();
//      // Collect ReachDef states for node
//      for(AbstractState absState : statesPerNode.get(nodeNum)) {
//        ARGState state = (ARGState)absState;
//        CompositeState compState = (CompositeState)state.getWrappedState();
//        for(AbstractState child : compState.getWrappedStates()) {
//          if (child instanceof ReachingDefState) {
//            ReachingDefState reachDef = (ReachingDefState)child;
//            reachDefStates.add(reachDef);
//          }
//        }
//      }
//      // Merge ReachDef states for node
//      Map<String, Set<DefinitionPoint>> localReachDef = new HashMap<>();
//      Map<String, Set<DefinitionPoint>> globalReachDef = new HashMap<>();
//      for(ReachingDefState rdState : reachDefStates) {
//        for(String var : rdState.getLocalReachingDefinitions().keySet()) {
//          if(!localReachDef.containsKey(var))
//            localReachDef.put(var, new HashSet<DefinitionPoint>());
//          localReachDef.get(var).addAll(rdState.getLocalReachingDefinitions().get(var));
//        }
//        for(String var : rdState.getGlobalReachingDefinitions().keySet()) {
//          if(!globalReachDef.containsKey(var))
//            globalReachDef.put(var, new HashSet<DefinitionPoint>());
//          globalReachDef.get(var).addAll(rdState.getGlobalReachingDefinitions().get(var));
//        }
//      }
//      result.put(nodeNum, new ReachingDefState(localReachDef, globalReachDef, null));
//    }
//    return result;
//  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    AlgorithmStatus result = algorithm.run(reachedSet);
    logger.log(Level.INFO, "Graph generator algorithm started.");

    // Fill data structures
    Set<ASTCollectorState> states = new HashSet<>();
    Set<ASTCollectorState> globalDeclStates = new HashSet<>();
    Set<ASTCollectorState> nonGlobalDeclStates = new HashSet<>();

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
          if(gmState.getTree().isGlobal())
            globalDeclStates.add(gmState);
          else
            nonGlobalDeclStates.add(gmState);
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
    DirectedMultigraph<ASTNode, ASTEdge> graph = generateCFGFromStates(states);
    pruneUnusedGlobalDeclarations(graph, globalDeclStates, nonGlobalDeclStates);
    //addDataDependenceEdges(astLocStates, gm, statesPerNode);
    pruneBlankNodes(graph);


    // Export graph
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
          }
        },
        new EdgeNameProvider<ASTEdge>() {
          @Override
          public String getEdgeName(ASTEdge o) {
            return o.toString();
          }
        });
    try {
      dotExp.export(new FileWriter(graphOutputFile.getPath()), graph);
    } catch (IOException e) {
      logger.logException(Level.ALL, e, "Cannot write DOT");
    }

    GraphMLExporter<ASTNode, ASTEdge> gmlExp = new GraphMLExporter<>(
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
          }
        },
        new IntegerEdgeNameProvider(),
        new EdgeNameProvider<ASTEdge>() {
          @Override
          public String getEdgeName(ASTEdge o) {
            return o.toString();
          }
        });
    try {
      gmlExp.export(new FileWriter(graphMLOutputFile.getPath()), graph);
    } catch (Exception e) {
      logger.logException(Level.ALL, e, "Cannot write GraphML");
    }

    exportNodeLabels(graph);
    exportEdgeTypeLabels(graph);
    exportEdgeTruthLabels(graph);
    exportNodeDepthLabels(graph);

    logger.log(Level.INFO, "graph generator algorithm finished.");
    return result;
  }
}
