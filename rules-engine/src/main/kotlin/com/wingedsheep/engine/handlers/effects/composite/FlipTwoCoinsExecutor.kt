package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.CoinFlipChoiceContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.CoinFlipService
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.FlipTwoCoinsEffect
import kotlin.reflect.KClass

/**
 * Executor for [FlipTwoCoinsEffect].
 *
 * Flips two coins through [CoinFlipService] and executes the sub-effect for the combined outcome.
 * The two coins are two separate flips, so a
 * [com.wingedsheep.sdk.scripting.FlipAdditionalCoins] replacement (Krark's Thumb) applies to each
 * of them independently — "you don't flip four coins and ignore any two; you flip two coins, flip
 * two coins, and then ignore one flip from each pair" — which is exactly what asking
 * [CoinFlipService] for a `count` of two does.
 */
class FlipTwoCoinsExecutor(
    private val cardRegistry: CardRegistry,
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<FlipTwoCoinsEffect> {

    override val effectType: KClass<FlipTwoCoinsEffect> = FlipTwoCoinsEffect::class

    override fun execute(
        state: GameState,
        effect: FlipTwoCoinsEffect,
        context: EffectContext
    ): EffectResult {
        val resolution = CoinFlipService.flip(
            state = state,
            flipperId = context.controllerId,
            count = 2,
            sourceId = context.sourceId,
            cardRegistry = cardRegistry,
            decisionHandler = decisionHandler
        )

        return when (resolution) {
            is CoinFlipService.Resolution.NeedsChoice -> EffectResult.paused(
                resolution.state.pushContinuation(
                    CoinFlipChoiceContinuation(
                        decisionId = resolution.decision.id,
                        effect = effect,
                        effectContext = context,
                        pending = resolution.pending
                    )
                ),
                resolution.decision,
                resolution.events
            )

            is CoinFlipService.Resolution.Resolved -> {
                val subEffect = subEffectFor(effect, resolution.results)
                    ?: return EffectResult.success(resolution.state, resolution.events)
                val result = effectExecutor(resolution.state, subEffect, context)
                result.copy(events = resolution.events + result.events)
            }
        }
    }

    companion object {

        /** Which of the three outcome branches the settled coins call for, or null when it is empty. */
        fun subEffectFor(effect: FlipTwoCoinsEffect, results: List<Boolean>): Effect? {
            val heads = results.count { it }
            return when {
                heads == results.size -> effect.bothHeadsEffect
                heads == 0 -> effect.bothTailsEffect
                else -> effect.mixedEffect
            }
        }
    }
}
