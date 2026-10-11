package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Evaluates how favorable a game state is for a given player.
 *
 * Returns a score where higher is better. Positive means ahead, negative means behind.
 * [Double.MAX_VALUE] means the player has won; [Double.MIN_VALUE] means they've lost.
 */
fun interface BoardEvaluator {
    fun evaluate(state: GameState, projected: ProjectedState, playerId: EntityId): Double

    /**
     * This evaluator, for scoring leaves reached from [root] — the position a decision is being
     * made from, before any candidate was applied.
     *
     * A leaf score is a property of the leaf, with one exception that cannot be: a term that asks
     * "what did this line cost?" has to know where the line started. `BoardPresence`'s
     * land-sequencing refund is the one such term today (see [BoardFeature.rootedAt]). Callers that
     * compare sibling leaves from one root pass it; everything else gets the leaf-only score, which
     * is what the default returns.
     */
    fun rootedAt(root: GameState, playerId: EntityId): BoardEvaluator = this
}

/**
 * Combines multiple [BoardFeature]s with weights into a single evaluator.
 */
class CompositeBoardEvaluator(
    private val features: List<Pair<Double, BoardFeature>>
) : BoardEvaluator {

    override fun evaluate(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        terminalScore(state, playerId)?.let { return it }

        return features.sumOf { (weight, feature) -> weight * feature.score(state, projected, playerId) }
    }

    override fun rootedAt(root: GameState, playerId: EntityId): BoardEvaluator =
        CompositeBoardEvaluator(features.map { (weight, feature) -> weight to feature.rootedAt(root, playerId) })
}

/**
 * A single scoring dimension. Returns a raw (unnormalized) score from the AI player's perspective.
 */
fun interface BoardFeature {
    fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double

    /** This feature for leaves reached from [root]; see [BoardEvaluator.rootedAt]. Leaf-only by default. */
    fun rootedAt(root: GameState, playerId: EntityId): BoardFeature = this
}
