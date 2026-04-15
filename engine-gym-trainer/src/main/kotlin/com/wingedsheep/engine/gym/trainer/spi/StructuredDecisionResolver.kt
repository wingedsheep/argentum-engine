package com.wingedsheep.engine.gym.trainer.spi

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.state.GameState

/**
 * Resolves "complex" engine decisions — `ChooseTargetsDecision`,
 * `DistributeDecision`, `OrderObjectsDecision`, `SplitPilesDecision`,
 * `SearchLibraryDecision`, `ReorderLibraryDecision`, `AssignDamageDecision`,
 * `SelectManaSourcesDecision`, multi-select `SelectCardsDecision`, and
 * multi-mode `ChooseModeDecision` — that the gym can't fold into a single
 * numeric action space.
 *
 * MCTS treats the result as a *forced* single edge (no exploration), which
 * keeps the self-play loop running but does not train the network on
 * structured decisions. A production project can either:
 *
 *  - supply a smarter resolver that samples from a heuristic distribution, or
 *  - pre-flatten structured decisions into the action space via a custom
 *    [ActionFeaturizer] and an `Evaluator` that knows how to emit priors for
 *    them.
 */
fun interface StructuredDecisionResolver {
    fun resolve(state: GameState, decision: PendingDecision): DecisionResponse
}
