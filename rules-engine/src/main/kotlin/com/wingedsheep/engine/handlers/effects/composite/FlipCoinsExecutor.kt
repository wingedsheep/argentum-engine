package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.CoinFlipChoiceContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.CoinFlipService
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.FlipCoinsEffect
import kotlin.reflect.KClass

/**
 * Executor for [FlipCoinsEffect].
 *
 * Flips [FlipCoinsEffect.count] coins through [CoinFlipService] and publishes the number that came
 * up heads into the pipeline (`storedNumbers[storeHeadsAs]`) via
 * [EffectResult.updatedStoredNumbers] so a later sub-effect in the same composite can scale off it
 * with [com.wingedsheep.sdk.scripting.values.DynamicAmount.VariableReference] — exactly how
 * [StoreNumberExecutor][com.wingedsheep.engine.handlers.effects.library.StoreNumberExecutor]
 * surfaces a value.
 *
 * "Heads" is modelled as a won flip, matching the rest of the coin-flip plumbing
 * ([FlipCoinExecutor]). Each of the [FlipCoinsEffect.count] coins is its own flip for replacement
 * purposes, so a [com.wingedsheep.sdk.scripting.FlipAdditionalCoins] replacement (Krark's Thumb)
 * turns "flip five coins" into five pairs with one kept from each, not into ten coins with any five
 * ignored — the distinction the card's own ruling draws.
 */
class FlipCoinsExecutor(
    private val cardRegistry: CardRegistry,
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<FlipCoinsEffect> {

    override val effectType: KClass<FlipCoinsEffect> = FlipCoinsEffect::class

    override fun execute(
        state: GameState,
        effect: FlipCoinsEffect,
        context: EffectContext
    ): EffectResult {
        val resolution = CoinFlipService.flip(
            state = state,
            flipperId = context.controllerId,
            count = effect.count,
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

            is CoinFlipService.Resolution.Resolved -> EffectResult(
                state = resolution.state,
                events = resolution.events,
                updatedStoredNumbers = mapOf(effect.storeHeadsAs to resolution.results.count { it })
            )
        }
    }
}
