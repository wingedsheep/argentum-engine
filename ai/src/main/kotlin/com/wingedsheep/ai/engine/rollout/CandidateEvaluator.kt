package com.wingedsheep.ai.engine.rollout

import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * How the Strategist scores the position a candidate action leads to.
 *
 * This is the seam Phase 7 of `backlog/engine-ai-improvement.md` opens: the AI's leaf score used to
 * be one `BoardEvaluator.evaluate` call on the state right after the action resolved, and the whole
 * phase is about replacing that one call with the mean of several short playouts *without* touching
 * candidate generation, target selection, the `CardAdvisor` override path or the hold policy.
 *
 * **Scores are in raw evaluator units**, the same ones [BoardEvaluator] produces, because everything
 * above the leaf is written in them: the pass comparison, the hold policy's timing delta, and a
 * `CardAdvisor` returning `defaultScore + 2.0`. [RolloutCandidateEvaluator] averages its playouts in
 * probability space — see [WinProbability] for why it must — and converts back at the boundary, so
 * the new mechanism plugs in underneath the old vocabulary instead of replacing it.
 */
interface CandidateEvaluator {

    /**
     * Score one candidate's post-action state.
     *
     * @param root the state the decision is being made from, before any candidate was applied.
     *   Rollouts key their common-random-number grid off it, so every candidate in one decision
     *   plays out against the same sampled futures.
     * @param afterAction the quiet state the simulator reached after applying the candidate.
     */
    fun score(root: GameState, afterAction: GameState, playerId: EntityId, budget: DecisionBudget): Double

    /**
     * Score every candidate of one decision together.
     *
     * The batch entry point is not a convenience: it is what lets an evaluator *allocate* effort
     * across candidates rather than spending a fixed amount on each. [RolloutCandidateEvaluator]
     * overrides it with sequential halving, which is both the reason the rollout budget goes to the
     * contenders and the reason the search is anytime.
     *
     * The default maps [score], which is exactly what a per-candidate evaluator wants and is why
     * [StaticCandidateEvaluator] needs no override — routing the Strategist through the batch API
     * therefore changes nothing at all for `AiProfile.LEGACY_V0`.
     */
    fun scoreAll(
        root: GameState,
        afterActions: List<GameState>,
        playerId: EntityId,
        budget: DecisionBudget,
    ): List<Double> = afterActions.map { score(root, it, playerId, budget) }
}

/**
 * The pre-Phase-7 leaf: one static evaluation of the post-action state.
 *
 * What `AiProfile.LEGACY_V0` uses, and what every profile that does not opt into rollouts keeps
 * using. `root` is unused — a greedy 1-ply evaluator has no use for where it came from — and that
 * asymmetry is the point of the interface.
 */
class StaticCandidateEvaluator(private val evaluator: BoardEvaluator) : CandidateEvaluator {

    override fun score(
        root: GameState,
        afterAction: GameState,
        playerId: EntityId,
        budget: DecisionBudget,
    ): Double = evaluator.evaluate(afterAction, afterAction.projectedState, playerId)

    override fun toString(): String = "static"
}
