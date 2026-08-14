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
}

/**
 * A single scoring dimension. Returns a raw (unnormalized) score from the AI player's perspective.
 */
fun interface BoardFeature {
    fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double
}
