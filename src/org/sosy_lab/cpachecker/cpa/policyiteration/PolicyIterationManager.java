package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Main logic in a single class.
 */
@Options(prefix="cpa.stator.policy")
public class PolicyIterationManager implements IPolicyIterationManager {

  @Option(secure=true, name="pathFocusing",
      description="Perform abstraction only at the nodes from the cut-set.")
  private boolean pathFocusing = true;

  @Option(secure=true, name="epsilon",
      description="Value to substitute for the epsilon")
  private int EPSILON = 1;

  @SuppressWarnings("unused, FieldCanBeLocal")
  private final FormulaManagerView fmgr;

  @SuppressWarnings("unused, FieldCanBeLocal")
  private final CFA cfa;
  private final PathFormulaManager pfmgr;
  private final LinearConstraintManager lcmgr;
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerFactory formulaManagerFactory;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final NumeralFormulaManagerView<NumeralFormula, NumeralFormula.RationalFormula>
      rfmgr;
  private final TemplateManager templateManager;
  private final ValueDeterminationFormulaManager vdfmgr;
  private final PolicyIterationStatistics statistics;

  public PolicyIterationManager(
      Configuration config,
      FormulaManagerView pFormulaManager,
      CFA pCfa,
      PathFormulaManager pPfmgr,
      LinearConstraintManager pLcmgr,
      BooleanFormulaManager pBfmgr,
      FormulaManagerFactory pFormulaManagerFactory,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      NumeralFormulaManagerView<NumeralFormula, NumeralFormula.RationalFormula> pRfmgr,
      TemplateManager pTemplateManager,
      ValueDeterminationFormulaManager pValueDeterminationFormulaManager,
      PolicyIterationStatistics pStatistics)
      throws InvalidConfigurationException {
    config.inject(this, PolicyIterationManager.class);
    fmgr = pFormulaManager;
    cfa = pCfa;
    pfmgr = pPfmgr;
    lcmgr = pLcmgr;
    bfmgr = pBfmgr;
    formulaManagerFactory = pFormulaManagerFactory;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    rfmgr = pRfmgr;
    templateManager = pTemplateManager;
    vdfmgr = pValueDeterminationFormulaManager;
    statistics = pStatistics;

    /** Compute the cache for nodes */
    ImmutableMap.Builder<Integer, CFANode> nodeMapBuilder = ImmutableMap.builder();
    for (CFANode node : pCfa.getAllNodes()) {
      nodeMapBuilder.put(node.getNodeNumber(), node);
    }
    nodeMap = nodeMapBuilder.build();

    /** Compute the cache for loops */
    ImmutableMap.Builder<CFANode, LoopStructure.Loop> loopStructureBuilder =
        ImmutableMap.builder();
    LoopStructure loopStructure1 = pCfa.getLoopStructure().get();
    for (LoopStructure.Loop l : loopStructure1.getAllLoops()) {
      for (CFANode n : l.getLoopHeads()) {
        loopStructureBuilder.put(n, l);
      }
    }
    loopStructure = loopStructureBuilder.build();
    abstractStates = new HashMap<>(pCfa.getAllNodes().size());
  }

  /** Static caches */
  private final ImmutableMap<Integer, CFANode> nodeMap;

  // Mapping from loop-heads to the associated loops.
  private final ImmutableMap<CFANode, LoopStructure.Loop> loopStructure;

  /** Scary-hairy global, contains all abstract states. */
  private final Map<CFANode, PolicyState> abstractStates;

  /** Constants */

  // Temporary variable for edge selection.
  private static final String SELECTION_VAR_TEMPLATE = "__BRANCH_SELECTION_(%d)";

  /**
   * @param pNode Initial node
   * @return Initial state for the analysis, assuming the first node
   * is {@code pNode}
   */
  @Override
  public PolicyState getInitialState(CFANode pNode) {
    PolicyState initial = PolicyState.empty(pNode);
    abstractStates.put(pNode, initial);
    return initial;
  }

  @Override
  public Collection<PolicyState> getAbstractSuccessors(PolicyState oldState, CFAEdge edge)
      throws CPATransferException, InterruptedException {

    CFANode node = edge.getSuccessor();
    PathFormula prev;

    NumeralFormula branchVar =  rfmgr.makeVariable(
        String.format(SELECTION_VAR_TEMPLATE, node.getNodeNumber()));
    BooleanFormula branchConstraint = rfmgr.equal(
        branchVar,
        rfmgr.makeNumber(edge.getPredecessor().getNodeNumber())
    );
    if (oldState.isAbstract()) {

      PathFormula empty = pfmgr.makeEmptyPathFormula();
      prev = pfmgr.makeAnd(
          empty,
          abstractStateToFormula(oldState, empty.getSsa())
      );
    } else {
      prev = oldState.getPathFormula();
    }
    prev = pfmgr.makeAnd(prev, branchConstraint);

    PolicyState out = PolicyState.ofIntermediate(
        ImmutableList.<AbstractState>of(),
        ImmutableSet.of(edge),
        edge.getSuccessor(),
        templateManager.templatesForNode(node),
        pfmgr.makeAnd(prev, edge)
    );

    // NOTE: the abstraction computation and the global update is delayed
    // until the {@code strengthen} call.
    return Collections.singleton(out);
  }

  @Override
  public Collection<PolicyState> strengthen(
      PolicyState state,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

    state = state.withOtherStates(otherStates);

    statistics.abstractionTimer.start();
    try {
      // Perform the abstraction, if necessary.
      CFANode toNode = state.getNode();
      if (shouldPerformAbstraction(toNode) && !state.isAbstract()) {
        Optional<PolicyState> abstraction = performAbstraction(state);
        if (!abstraction.isPresent()) {
          abstractStates.put(toNode, state);
          return Collections.emptyList();
        }
        state = abstraction.get();
      }
      abstractStates.put(toNode, state);
    } finally {
      statistics.abstractionTimer.stop();
    }

    // Perform the reachability check for the target states.
    statistics.strengthenTimer.start();
    try {
      if (state.isAbstract()) {
        return Collections.singleton(state);
      }

      boolean hasTargetState = false;
      for (AbstractState oState : otherStates) {
        if (AbstractStates.isTargetState(oState)) {
          hasTargetState = true;
        }
      }

      if (hasTargetState) {
        boolean isSat = checkSatisfiability(state);
        if (!isSat) {
          return Collections.emptyList();
        } else {
          return Collections.singleton(state);
        }
      }
      return Collections.singleton(state);

    } finally {
      statistics.strengthenTimer.stop();
    }
  }

  /**
   * @return {@code true} if and only if {@code newState} represents a state
   * which is smaller-or-equal (with respect to the abstract lattice)
   * than the {@code oldState}.
   */
  @Override
  public boolean isLessOrEqual(PolicyState newState, PolicyState oldState) {
    Preconditions.checkState(newState.isAbstract() == oldState.isAbstract(),
        "Abstraction state of two states associated with the same node should " +
            "match");

    boolean isAbstract = newState.isAbstract();

    if (!isAbstract) {

      // TODO: compare the path formulas.
      // but that won't make things better!

//      return false;
      return oldState.getPathFormula().equals(newState.getPathFormula());
    } else {

      for (Entry<LinearExpression, PolicyBound> entry : oldState) {
        LinearExpression template = entry.getKey();
        PolicyBound newBound = entry.getValue();
        Optional<PolicyBound> oldBound = newState.getBound(template);

        if (!oldBound.isPresent()) {

          // Old bound is infinity, new bound is not, not covered.
          return false;
        } else if (oldBound.get().bound.compareTo(newBound.bound) > 0) {

          // Note that the trace obtained is irrelevant.
          return false;
        }
      }
      return true;
    }
  }

  @Override
  public PolicyState join(PolicyState newState, PolicyState oldState)
      throws CPATransferException, InterruptedException, SolverException {
    statistics.timeInMerge.start();
    try {
      PolicyState out = join0(newState, oldState);
      abstractStates.put(out.getNode(), out);

      // Note: returning an exactly same state is important, due to the issues
      // with .equals() handling.
      if (out.equals(oldState)) {
        return oldState;
      }
      return out;
    } finally {
      statistics.timeInMerge.stop();
    }
  }

  PolicyState join0(PolicyState newState, PolicyState oldState)
      throws CPATransferException, InterruptedException, SolverException {
    Preconditions.checkState(oldState.getNode() == newState.getNode());
    Preconditions.checkState(oldState.isAbstract() == newState.isAbstract());
    Preconditions.checkState(oldState.getTemplates().equals(newState.getTemplates()));

    final boolean isAbstract = oldState.isAbstract();
    final CFANode node = oldState.getNode();

    Set<Template> allTemplates = oldState.getTemplates();

    if (!isAbstract) {

      PathFormula newPath = newState.getPathFormula();
      PathFormula oldPath = oldState.getPathFormula();

      // No value determination, no abstraction, simply join incoming edges
      // and the tracked templates.
      return PolicyState.ofIntermediate(
          Iterables.concat(
              oldState.getOtherStates(), newState.getOtherStates()),
          Sets.union(oldState.getIncomingEdges(), newState.getIncomingEdges()),
          node, allTemplates,
          pfmgr.makeOr(newPath, oldPath)
      );
    }

    Map<LinearExpression, PolicyBound> updated = new HashMap<>();
    Set<LinearExpression> unbounded = new HashSet<>();

    // Simple join:
    // Pick the biggest bound, and keep the biggest trace to match.
    for (Template templateWrapper : allTemplates) {
      LinearExpression template = templateWrapper.linearExpression;
      Optional<PolicyBound> oldValue, newValue;
      oldValue = oldState.getBound(template);
      newValue = newState.getBound(template);

      if (!oldValue.isPresent()) {

        // Can't do better than unbounded.
        continue;
      } else if (!newValue.isPresent()) {

        // {@code template} became unbounded.
        unbounded.add(template);
        continue;
      } else if (newValue.get().bound.compareTo(oldValue.get().bound) > 0) {
        updated.put(template, newValue.get());
      }
    }

    PolicyState stateWithUpdates =
        oldState.withUpdates(updated, unbounded, allTemplates);
    logger.log(Level.FINE, "# State with updates: ", stateWithUpdates);

    if (!shouldPerformValueDetermination(node, updated)) {
      return stateWithUpdates;

    } else {
      logger.log(Level.FINE, "# Value Determination launched");

      Map<CFANode, PolicyState> related =
          findRelated(stateWithUpdates, node, updated);

      // Note: this formula contains no disjunctions, as the policy entails
      // the edge selection. Hence it can be used safely for the maximization.
      List<BooleanFormula> constraints = vdfmgr.valueDeterminationFormula(
          related, node, updated);

      return vdfmgr.valueDeterminationMaximization(
          oldState,
          allTemplates,
          updated, node, constraints, EPSILON);
    }
  }

  /**
   * @return Whether to perform the value determination on <code>node</code>.
   *
   * Returns true iff the <code>node</code> is a loophead and at least one of
   * the bounds in <code>updated</code> has an associated edge coming from
   * outside of the loop.
   */
  private boolean shouldPerformValueDetermination(
      CFANode node,
      Map<LinearExpression, PolicyBound> updated) {
    if (!node.isLoopStart()) {
      return false;
    }

    // At least one of updated values comes from inside the loop.
    LoopStructure.Loop l = loopStructure.get(node);
    for (PolicyBound bound : updated.values()) {

      CFAEdge edge = bound.trace;
      assert edge.getSuccessor() == node;
      if (l.getLoopNodes().contains(edge.getPredecessor())) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return Whether the <code>state</code> is reachable.
   */
  private boolean checkSatisfiability(PolicyState state)
        throws CPATransferException, InterruptedException {

    PathFormula p = allPathsToNode(state);

    try (ProverEnvironment solver
            = formulaManagerFactory.newProverEnvironment(false, false)) {
      solver.push(p.getFormula());
      if (solver.isUnsat()) {
        return false;
      }
    } catch (SolverException e) {
      throw new CPATransferException("Failed solving", e);
    }

    return true;
  }

  /**
   * Perform the abstract operation on a new state
   *
   * @param state State to abstract
   * @return Abstracted state if the state is reachable, empty optional
   * otherwise.
   */
  private Optional<PolicyState> performAbstraction(PolicyState state)
      throws CPATransferException, InterruptedException {
    final CFANode node = state.getNode();
    final PathFormula p = allPathsToNode(state);

    ImmutableMap.Builder<LinearExpression, PolicyBound> abstraction
        = ImmutableMap.builder();

    for (Template templateWrapper : state.getTemplates()) {
      LinearExpression template = templateWrapper.linearExpression;

      // Optimize for the template subject to the
      // constraints introduced by {@code p}.
      try (OptEnvironment solver = formulaManagerFactory.newOptEnvironment()) {
        shutdownNotifier.shutdownIfNecessary();

        solver.addConstraint(p.getFormula());
        NumeralFormula objective =
            lcmgr.linearExpressionToFormula(template, p.getSsa());
        solver.maximize(objective);

        // Generate the trace for the single template.
        OptEnvironment.OptStatus result = solver.check();
        switch (result) {
          case OPT:
            Rational bound = solver.value(EPSILON);
            Model model = solver.getModel();
            logger.log(Level.FINE, "# Model =" + model);

            // Re-arrange the unique policy into the multi-edge.
            // Make sure that the edges meet.
            final List<CFAEdge> traceReversed = new ArrayList<>();
            final Set<CFANode> visitedNodes = new HashSet<>();
            visitedNodes.add(node);

            CFANode successor = node;
            while (true) {
              int toNodeNo = successor.getNodeNumber();
              Object o = model.get(
                  new Model.Constant(
                      String.format(SELECTION_VAR_TEMPLATE, toNodeNo),
                      Model.TermType.Real
                  )
              );
              if (o == null) {

                // Trace has finished.
                break;
              }
              int fromNodeNo = Integer.parseInt(o.toString());
              CFAEdge edge = edgeFromIdentifier(fromNodeNo, toNodeNo);
              assert edge != null;
              traceReversed.add(edge);
              successor = nodeMap.get(fromNodeNo);
              if (visitedNodes.contains(successor)) {

                // Don't loop.
                break;
              }
              visitedNodes.add(successor);
            }

            // Last successor is the ultimate "predecessor"
            CFANode predecessor = successor;
            assert !traceReversed.isEmpty() : model;
            MultiEdge edge = new MultiEdge(predecessor, node,
                Lists.reverse(traceReversed));
            logger.log(Level.FINE, "# Constructed edge: ", edge);
            abstraction.put(template, new PolicyBound(edge, bound));

            break;
          case UNSAT:
            // Short circuit: this point is infeasible.
            return Optional.absent();
          case UNBOUNDED:
            // Skip the constraint: it does not return any additional
            // information.
            continue;
          case UNDEF:
            throw new CPATransferException("Solver returned undefined status");
        }
      } catch (SolverException e) {
        throw new CPATransferException("Solver error: ", e);
      }
    }

    return Optional.of(PolicyState.ofAbstraction(
        abstraction.build(),
        state.getTemplates(),
        node,
        state.getIncomingEdges(),
        state.getOtherStates()));
  }

  /**
   * @return {@link CFAEdge} object connecting the node <code>fromNodeNo</code>
   * to the node <code>toNodeNo</code>.
   */
  private CFAEdge edgeFromIdentifier(int fromNodeNo, int toNodeNo) {
    CFANode toNode = nodeMap.get(toNodeNo);
    for (int idx=0; idx<toNode.getNumEnteringEdges(); idx++) {
      CFAEdge enteringEdge = toNode.getEnteringEdge(idx);
      if (enteringEdge.getPredecessor().getNodeNumber() == fromNodeNo) {
        return enteringEdge;
      }
    }
    throw new IllegalArgumentException(
        "Pair of nodes corresponds to the non-existent edge: " +
    fromNodeNo + "->" + toNodeNo);
  }

  /**
   *
   * @return Whether to compute the abstraction when creating a new
   * state associated with <code>node</code>.
   */
  private boolean shouldPerformAbstraction(CFANode node) {
    return !pathFocusing || node.isLoopStart();
  }

  /**
  * Recursively compute the formula representing all possible paths
  * to the node <code>current</code> from the already resolved nodes.
  */
  private PathFormula allPathsToNode(PolicyState toState)
      throws CPATransferException, InterruptedException {
    return toState.getPathFormula();
  }

  private BooleanFormula abstractStateToFormula(
      PolicyState abstractState, SSAMap ssa) {

    List<BooleanFormula> tokens = new ArrayList<>();
    for (Entry<LinearExpression, PolicyBound> entry : abstractState) {
      LinearExpression template = entry.getKey();
      PolicyBound bound = entry.getValue();
      BooleanFormula constraint = rfmgr.lessOrEquals(
          lcmgr.linearExpressionToFormula(template, ssa),
          rfmgr.makeNumber(bound.bound.toString())
      );
      tokens.add(constraint);
    }
    return bfmgr.and(tokens);
  }

  /**
   * @return the subset of {@code abstractStates} required for the update
   * {@code updated}.
   */
  private Map<CFANode, PolicyState> findRelated(
      PolicyState newState,
      CFANode focusedNode,
      Map<LinearExpression, PolicyBound> updated) {

    Map<CFANode, PolicyState> out = new HashMap<>();
    Set<CFANode> visited = Sets.newHashSet();

    LinkedHashSet<CFANode> queue = new LinkedHashSet<>();
    queue.add(focusedNode);
    while (!queue.isEmpty()) {
      Iterator<CFANode> it = queue.iterator();
      CFANode node = it.next();
      it.remove();
      visited.add(node);

      PolicyState state;
      if (node == focusedNode) {
        state = newState;

      } else {
        state = abstractStates.get(node);
      }

      out.put(node, state);

      for (Map.Entry<LinearExpression, PolicyBound> entry : state) {
        LinearExpression template = entry.getKey();
        PolicyBound bound = entry.getValue();

        // Do not follow the edges which are associated with the focused node
        // but are not in <updated>.
        if (!(state == newState && !updated.containsKey(template))) {
          CFANode toVisit = bound.trace.getPredecessor();
          if (!visited.contains(toVisit)) {
            queue.add(toVisit);
          }
        }
      }
    }
    return out;
  }
}