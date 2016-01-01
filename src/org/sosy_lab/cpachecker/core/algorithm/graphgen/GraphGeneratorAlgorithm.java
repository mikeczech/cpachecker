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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.GraphMLExporter;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.graphgen.utils.Dominators;
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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;

/**
 * Created by zenscr on 26/11/15.
 */
@Options(prefix = "graphgen")
public class GraphGeneratorAlgorithm implements Algorithm {

  private final LogManager logger;

  private final Algorithm algorithm;

  private final CFA cfa;

  private final Set<ASTNodeLabel> programEndLabels = ImmutableSet.of(ASTNodeLabel.FUNC_CALL, ASTNodeLabel.VERIFIER_ERROR);

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

  @Option(secure=true, name = "postDomTree", description = "Output file of the post dominator tree")
  @FileOption(Type.OUTPUT_FILE)
  private Path postDomTreeOutputFile = Paths.get("output/postdomtree.dot");

  public GraphGeneratorAlgorithm(Algorithm pAlgorithm, LogManager pLogger,
      ConfigurableProgramAnalysis pCpa, CFA pCfa) {
    logger = pLogger;
    algorithm = pAlgorithm;
    cfa = pCfa;
  }

  /**
   * Prunes all nodes from a graph, which are associated with the label BLANK
   * @param pGraph
   */
  private void pruneBlankNodes(DirectedPseudograph<ASTNode, ASTEdge> pGraph) {
    // add control flow edge between sources and targets of blank nodes
    // hacky solution. Todo fix this
    while(true) {
      ASTNode nodeToRemove = null;
      for(ASTNode node : pGraph.vertexSet()) {
        if(node.isBlank()) {
          nodeToRemove = node;
          Set<ASTEdge> incomingEdges = pGraph.incomingEdgesOf(node);
          Set<ASTEdge> outgoingEdges = pGraph.outgoingEdgesOf(node);

          for(ASTEdge out : outgoingEdges) {
            ASTNode target = out.getTargetNode();
            for(ASTEdge incoming : incomingEdges) {
              ASTNode source = incoming.getSourceNode();
              ASTEdge newEdge = new ASTEdge(source, target, incoming.getAstEdgeLabel());
              newEdge.setTruthValue(incoming.getTruthValue());
              pGraph.addEdge(source, target, newEdge);
            }
          }
          break;
        }
      }
      if(nodeToRemove != null)
        pGraph.removeVertex(nodeToRemove);
      else
        break;
    }
    Set<ASTNode> floatingNodes = new HashSet<>();
    // remove all nodes which have an outgoing edge, but no incoming (except the start node)
    for(ASTNode node : pGraph.vertexSet()) {
      if(!node.isStart() && pGraph.inDegreeOf(node) == 0)
        floatingNodes.add(node);
    }
    pGraph.removeAllVertices(floatingNodes);
  }

  /**
   * Takes a set of ASTCollector states and constructs a graph representation
   * containing statement ASTs ans control-flow edges between root nodes.
   * @param states
   * @return
   */
  private DirectedPseudograph<ASTNode, ASTEdge> generateCFGFromStates(Table<Integer, Integer, ASTCollectorState> edgeToState) {
    DirectedPseudograph<ASTNode, ASTEdge> result = new DirectedPseudograph<>(ASTEdge.class);
    Set<CFAEdge> allCfaEdges = new HashSet<>();
    for(CFANode n : cfa.getAllNodes()) {
      for(int i = 0; i < n.getNumLeavingEdges(); i++)
        allCfaEdges.add(n.getLeavingEdge(i));
      for(int i = 0; i < n.getNumEnteringEdges(); i++)
        allCfaEdges.add(n.getEnteringEdge(i));
    }
    // add nodes
    Map<Integer, Set<ASTCollectorState>> sourceToStates = new HashMap<>();
    for(CFAEdge e : allCfaEdges) {
      int source = e.getPredecessor().getNodeNumber();
      int target = e.getSuccessor().getNodeNumber();

      if(!sourceToStates.containsKey(source))
        sourceToStates.put(source, new HashSet<ASTCollectorState>());

      if(edgeToState.contains(source, target)) {
        ASTCollectorState state = edgeToState.get(source, target);
        result.addVertex(state.getTree().getRoot());
        sourceToStates.get(source).add(state);
      } else {
        ASTNode blank = new ASTNode(ASTNodeLabel.BLANK);
        result.addVertex(blank);
        ASTCollectorState newState = new ASTCollectorState(e, new ASTree(blank));
        sourceToStates.get(source).add(newState);
        edgeToState.put(source, target, newState);
      }
    }
    // add control flow edges
    for(CFAEdge e : allCfaEdges) {
      int source = e.getPredecessor().getNodeNumber();
      int target = e.getSuccessor().getNodeNumber();
      ASTNode sourceNode = edgeToState.get(source, target).getTree().getRoot();
      if(!sourceNode.getLabels().containsAll(programEndLabels)) {
        if(sourceToStates.containsKey(target)) {
          for(ASTCollectorState s : sourceToStates.get(target)) {
            ASTNode targetNode = s.getTree().getRoot();
            ASTEdge edge = new ASTEdge(sourceNode, targetNode,
                ASTEdgeLabel.CONTROL_FLOW);
            edge.setTruthValue(edgeToState.get(source, target).getAssumptions().get(source, target));
            result.addEdge(sourceNode, targetNode, edge);
          }
        }
      }
    }

    return result;
  }


  private void addDummyEdges(DirectedPseudograph<ASTNode, ASTEdge> graph) {
    // find nodes with out degree zero or error label and connect them with an end node
    ASTNode endNode = new ASTNode(ASTNodeLabel.END);
    graph.addVertex(endNode);
    Set<ASTEdge> edgesToDelete = new HashSet<>();
    for(ASTNode n : graph.vertexSet()) {
      if((graph.outDegreeOf(n) == 0 || n.getLabels().containsAll(programEndLabels)) && n != endNode) {

        for(ASTEdge e : graph.outgoingEdgesOf(n))
          edgesToDelete.add(e);

        ASTEdge edge = new ASTEdge(n, endNode,
            ASTEdgeLabel.CONTROL_FLOW);
        graph.addEdge(n, endNode, edge);
        // Add dummy edge to enforce multigraph in graphml (workaround) Todo find a better solution!
        ASTEdge dummyEdge = new ASTEdge(n, endNode,
            ASTEdgeLabel.DUMMY);
        graph.addEdge(n, endNode, dummyEdge);
      }
    }
    graph.removeAllEdges(edgesToDelete);

    // If there are infinite cycles in the cfg, then find sccs and connect some node from them with the end node (dummy edge)
    StrongConnectivityInspector<ASTNode, ASTEdge> sccInsp =
        new StrongConnectivityInspector<>(graph);
    for(Set<ASTNode> scc : sccInsp.stronglyConnectedSets()) {

      if(scc.size() < 2)
        continue;

      ASTNode source = null;
      boolean terminate = false;
      for(ASTNode n : scc) {

        for(ASTEdge e : graph.outgoingEdgesOf(n)) {
          if(!scc.contains(e.getTargetNode())) {
            terminate = true;
            break;
          }
        }
        if(terminate)
          break;

        if(graph.outDegreeOf(n) < 2) {
          source = n;
        }
      }
      if(!terminate) {
        assert source != null;
        ASTEdge edge = new ASTEdge(source, endNode,
            ASTEdgeLabel.DUMMY);
        graph.addEdge(source, endNode, edge);
      }
    }

    assert graph.inDegreeOf(endNode) != 0;
  }

  /**
   * Writes a file containing a list of node id - label pairs
   * @param graph
   */
  private void exportNodeLabels(DirectedPseudograph<ASTNode, ASTEdge> graph) {
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
  private void exportEdgeTypeLabels(DirectedPseudograph<ASTNode, ASTEdge> graph) {
    PrintWriter wtr = null;
    try {
      wtr = new PrintWriter(edgeTypesOutputFile.getPath());
      for(ASTEdge edge : graph.edgeSet()) {
        wtr.println(String.format("%s,%s,%s,%s", edge.getSourceNode().getId(), edge.getTargetNode().getId(), edge.getId(), edge.getAstEdgeLabel().getValue()));
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
  private void exportEdgeTruthLabels(DirectedPseudograph<ASTNode, ASTEdge> graph) {
    PrintWriter wtr = null;
    try {
      wtr = new PrintWriter(edgeTruthOutputFile.getPath());
      for(ASTEdge edge : graph.edgeSet()) {
        wtr.println(String.format("%s,%s,%s,%s", edge.getSourceNode().getId(), edge.getTargetNode().getId(), edge.getId(), edge.getTruthLabel().getValue()));
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
  private void exportNodeDepthLabels(DirectedPseudograph<ASTNode, ASTEdge> graph) {
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

  private void addControlDependencies(DirectedPseudograph<ASTNode, ASTEdge> pInputGraph) {
    DirectedGraph<ASTNode, ASTEdge> graph = new EdgeReversedGraph<>(pInputGraph);
    // Find entry node
    Set<ASTNode> zeroOutNodes = new HashSet<>();
    for(ASTNode node : graph.vertexSet()) {
      if(graph.inDegreeOf(node) == 0) {
        zeroOutNodes.add(node);
      }
    }
    assert zeroOutNodes.size() == 1;
    ASTNode entry = zeroOutNodes.iterator().next();
    assert entry.getLabels().contains(ASTNodeLabel.END);

    Set<ASTEdge> controlDependences = new HashSet<>();
    Dominators<ASTNode, ASTEdge> dominators = new Dominators<>(graph, entry);
    DirectedGraph<ASTNode, DefaultEdge> pdt = dominators.getDominatorTree();
    for(ASTEdge e : pInputGraph.edgeSet()) {
      if(!dominators.dominates(e.getTargetNode(), e.getSourceNode())
          && pdt.incomingEdgesOf(e.getTargetNode()).iterator().hasNext()) {

        Set<DefaultEdge> sourceIncoming =
            pdt.incomingEdgesOf(e.getSourceNode());
        ASTNode parent = null;
        if (sourceIncoming.isEmpty()) {
          // set root as parent
          for (ASTNode n : pdt.vertexSet()) {
            if (pdt.inDegreeOf(n) == 0) {
              parent = n;
              break;
            }
          }
        } else {
          parent = pdt.getEdgeSource(sourceIncoming.iterator().next());
        }
        assert parent != null;
        // traverse backwards in tree
        ASTNode current = e.getTargetNode();
        ASTNode pred =
            pdt.getEdgeSource(pdt.incomingEdgesOf(current).iterator().next());
        ASTEdge cdEdge = new ASTEdge(e.getSourceNode(), current,
            ASTEdgeLabel.CONTROL_DEPENDENCE);
        cdEdge.setTruthValue(e.getTruthValue());
        controlDependences.add(cdEdge);
        while(pred != parent) {
          current = pred;
          pred =
              pdt.getEdgeSource(pdt.incomingEdgesOf(current).iterator().next());
          cdEdge = new ASTEdge(e.getSourceNode(), current,
              ASTEdgeLabel.CONTROL_DEPENDENCE);
          cdEdge.setTruthValue(e.getTruthValue());
          controlDependences.add(cdEdge);
        }
      }
    }
    for(ASTEdge cdEdge : controlDependences) {
      pInputGraph.addEdge(cdEdge.getSourceNode(), cdEdge.getTargetNode(), cdEdge);
    }
  }

  private void addASTsToGraph(
      DirectedPseudograph<ASTNode, ASTEdge> graph,
      Set<ASTCollectorState> states) {
    for(ASTCollectorState s : states) {
      if(graph.containsVertex(s.getTree().getRoot())) {
        Graphs.addGraph(graph, s.getTree().asGraph());
      }
    }
  }

  private void pruneUnusedGlobalDeclarations(DirectedPseudograph<ASTNode, ASTEdge> graph, Set<ASTCollectorState> states) {
    Set<ASTree> globalTrees = new HashSet<>();
    Set<ASTree> localTrees = new HashSet<>();
    for(ASTCollectorState s : states) {
      ASTree tree = s.getTree();
      if(tree.getRoot().isGlobal())
        globalTrees.add(tree);
      else
        localTrees.add(tree);
    }

    Map<String, ASTree> idToASTree = new HashMap<>();
    Set<ASTree> noRemove = new HashSet<>();
    for(ASTree tree : globalTrees) {
      String declId = tree.getIdentifiers().iterator().next();
      assert declId != null;
      idToASTree.put(declId, tree);
    }

    // look for local usage of global declarations
    for(ASTree tree : localTrees) {
      for(String id : tree.getIdentifiers()) {
        if(idToASTree.containsKey(id))
          noRemove.add(idToASTree.get(id));
      }
    }

    // look for dependencies between global declarations
    boolean foundDependencies = true;
    while(foundDependencies) {
      Set<ASTree> dependencies = new HashSet<>();
      for(ASTree tree : noRemove) {
        for(String id : tree.getIdentifiers()) {
          assert idToASTree.containsKey(id);
          dependencies.add(idToASTree.get(id));
        }
      }
      foundDependencies = noRemove.addAll(dependencies);
    }

    // remove all unused global declarations
    Set<ASTNode> nodesToRemove = new HashSet<>();

    final Collection<ASTNode> noRemoveNodes = Collections2.transform(noRemove,
        new Function<ASTree, ASTNode>() {
          @Override
          public ASTNode apply(ASTree pASTree) {
            return pASTree.getRoot();
          }
        });

    for(ASTNode n : graph.vertexSet()) {
      if(n.isGlobal() && !noRemoveNodes.contains(n))
        nodesToRemove.add(n);
    }
    for(ASTNode n : nodesToRemove) {
      assert graph.inDegreeOf(n) == 1 && graph.outDegreeOf(n) == 1;
      ASTEdge incoming = graph.incomingEdgesOf(n).iterator().next();
      ASTEdge outgoing = graph.outgoingEdgesOf(n).iterator().next();
      ASTEdge newEdge = new ASTEdge(incoming.getSourceNode(), outgoing.getTargetNode(),
          ASTEdgeLabel.CONTROL_FLOW);
      graph.addEdge(incoming.getSourceNode(), outgoing.getTargetNode(), newEdge);
      graph.removeEdge(incoming);
      graph.removeEdge(outgoing);
      graph.removeVertex(n);
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
    Table<Integer, Integer, ASTCollectorState> edgeToState = HashBasedTable.create();

    for(AbstractState absState : reachedSet.asCollection()) {

      ARGState state = (ARGState)absState;
      CompositeState compState = (CompositeState)state.getWrappedState();
      for(AbstractState child : compState.getWrappedStates()) {

        if(child instanceof ASTCollectorState) {
          ASTCollectorState gmState = (ASTCollectorState)child;
          for(CFAEdgeInfo e : gmState.getCfaEdgeInfoSet())
            edgeToState.put(e.getSource(), e.getTarget(), gmState);
        }

      }
    }
    Set<ASTCollectorState> states = new HashSet<>(edgeToState.values());


    // Create graph representation
    DirectedPseudograph<ASTNode, ASTEdge> graph = generateCFGFromStates(edgeToState);
    pruneUnusedGlobalDeclarations(graph, states);
    pruneBlankNodes(graph);
    addDummyEdges(graph);
    //addDataDependenceEdges(astLocStates, gm, statesPerNode);
    addControlDependencies(graph);
    addASTsToGraph(graph, states);

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
        new EdgeNameProvider<ASTEdge>() {
          @Override
          public String getEdgeName(ASTEdge pASTEdge) {
            return String.valueOf(pASTEdge.getId());
          }
        },
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
