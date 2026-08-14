package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/** A fitted linear model over the unaggregated Phase 9 position facts. */
@Serializable
data class RawEvaluationWeights(
    val intercept: Double,
    val weights: Map<String, Double>,
    val winProbabilityScale: Double = 1.0,
) {
    fun isValid(): Boolean =
        intercept.isFinite() &&
            winProbabilityScale.isFinite() && winProbabilityScale > 0.0 &&
            weights.keys == RawBoardFeatures.names &&
            weights.values.all(Double::isFinite)

    fun evaluate(features: RawBoardFeatures): Double = intercept + features.weightedSum(weights)

    fun toEvaluator(intents: IntentCatalog): BoardEvaluator = RawBoardEvaluator(this, intents)
}

private class RawBoardEvaluator(
    private val fitted: RawEvaluationWeights,
    private val intents: IntentCatalog,
) : BoardEvaluator {
    override fun evaluate(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        terminalScore(state, playerId)?.let { return it }
        return fitted.evaluate(RawBoardFeatures.extract(state, projected, playerId, intents))
    }
}

/** Shared terminal contract for composite and fitted evaluators. */
internal fun terminalScore(state: GameState, playerId: EntityId): Double? = when {
    state.gameOver -> when {
        state.winnerId == null -> 0.0
        state.winnerId in state.teamOf(playerId) -> Double.MAX_VALUE / 2
        else -> -(Double.MAX_VALUE / 2)
    }
    state.teamActivePlayers(playerId).isEmpty() -> -(Double.MAX_VALUE / 2)
    else -> null
}
