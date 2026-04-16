package com.wingedsheep.gym.trainer.defaults

import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.evaluation.CardAdvantage
import com.wingedsheep.ai.engine.evaluation.CompositeBoardEvaluator
import com.wingedsheep.ai.engine.evaluation.LifeDifferential
import com.wingedsheep.ai.engine.evaluation.Tempo
import com.wingedsheep.ai.engine.evaluation.ThreatAssessment
import com.wingedsheep.gym.trainer.spi.Evaluator
import com.wingedsheep.gym.trainer.spi.EvaluationResult
import com.wingedsheep.gym.trainer.spi.SlotEncoding
import com.wingedsheep.gym.trainer.spi.TrainerContext
import kotlin.math.tanh

/**
 * No-NN [Evaluator] — returns uniform priors and a value derived from the
 * rules-engine's built-in [BoardEvaluator].
 *
 * Two use cases:
 *  - Smoke-testing the self-play loop without any Python server running
 *  - A warm-start / bootstrap phase before the NN has learnt anything
 *    useful
 *
 * Value normalisation is `tanh(raw / scale)` so output stays in `[-1, +1]`
 * regardless of the heuristic's raw range. `scale = 20000` works for the
 * engine's default [CompositeBoardEvaluator] and matches the factor
 * MageZero's XMage fork uses.
 */
class HeuristicEvaluator<T>(
    private val boardEvaluator: BoardEvaluator = defaultEvaluator(),
    private val valueScale: Double = 20_000.0
) : Evaluator<T> {

    override fun evaluate(
        features: T,
        legalSlots: List<SlotEncoding>,
        ctx: TrainerContext
    ): EvaluationResult {
        val raw = boardEvaluator.evaluate(ctx.state, ctx.state.projectedState, ctx.playerId)
        val value = tanh(raw / valueScale).toFloat()

        // Uniform priors per head — the trainer normalises across legal slots anyway.
        val priors = legalSlots
            .groupBy { it.head }
            .mapValues { (_, slots) ->
                val max = slots.maxOf { it.slot } + 1
                FloatArray(max) { 1f }
            }

        return EvaluationResult(priors = priors, value = value)
    }

    companion object {
        fun defaultEvaluator(): BoardEvaluator = CompositeBoardEvaluator(
            listOf(
                1.0 to LifeDifferential,
                1.5 to BoardPresence,
                1.0 to CardAdvantage,
                1.2 to ThreatAssessment,
                0.6 to Tempo
            )
        )
    }
}
