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
import com.wingedsheep.sdk.scripting.effects.FlipCoinEffect
import kotlin.reflect.KClass

/**
 * Executor for [FlipCoinEffect].
 *
 * Flips one coin through [CoinFlipService] — which applies any coin-flip replacement the flipper
 * controls — and executes the matching sub-effect. Because a
 * [com.wingedsheep.sdk.scripting.FlipAdditionalCoins] replacement (Krark's Thumb) turns that coin
 * into a batch the flipper must choose from, the flip can pause; the sub-effect then runs from
 * [CoinFlipChoiceContinuation] on the resume path instead of here.
 *
 * @param cardRegistry Used to look up the coin-flip replacements the flipping player controls.
 * @param effectExecutor Function to execute a sub-effect (provided by registry)
 */
class FlipCoinExecutor(
    private val cardRegistry: CardRegistry,
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<FlipCoinEffect> {

    override val effectType: KClass<FlipCoinEffect> = FlipCoinEffect::class

    override fun execute(
        state: GameState,
        effect: FlipCoinEffect,
        context: EffectContext
    ): EffectResult {
        val resolution = CoinFlipService.flip(
            state = state,
            flipperId = context.controllerId,
            count = 1,
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

        /**
         * Which half of [effect] the settled coin calls for, or null when that half is empty.
         *
         * Shared with the resume path so a flip that paused for a Krark's Thumb choice runs exactly
         * the same branch it would have run without one.
         */
        fun subEffectFor(effect: FlipCoinEffect, results: List<Boolean>): Effect? =
            if (results.firstOrNull() == true) effect.wonEffect else effect.lostEffect
    }
}
