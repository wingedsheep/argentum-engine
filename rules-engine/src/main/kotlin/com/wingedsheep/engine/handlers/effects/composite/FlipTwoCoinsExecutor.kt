package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.FlipTwoCoinsEffect
import kotlin.reflect.KClass

/**
 * Executor for FlipTwoCoinsEffect.
 * Flips two coins and executes the appropriate sub-effect based on the combined outcome.
 */
class FlipTwoCoinsExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<FlipTwoCoinsEffect> {

    override val effectType: KClass<FlipTwoCoinsEffect> = FlipTwoCoinsEffect::class

    override fun execute(
        state: GameState,
        effect: FlipTwoCoinsEffect,
        context: EffectContext
    ): EffectResult {
        val coin1 = kotlin.random.Random.nextBoolean()
        val coin2 = kotlin.random.Random.nextBoolean()

        val sourceId = context.sourceId
        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"

        val events = listOf(
            CoinFlipEvent(context.controllerId, coin1, sourceId ?: context.controllerId, sourceName),
            CoinFlipEvent(context.controllerId, coin2, sourceId ?: context.controllerId, sourceName)
        )

        val subEffect = when {
            coin1 && coin2 -> effect.bothHeadsEffect
            !coin1 && !coin2 -> effect.bothTailsEffect
            else -> effect.mixedEffect
        }

        if (subEffect == null) {
            return EffectResult.success(state, events)
        }

        val result = effectExecutor(state, subEffect, context)
        return result.copy(events = events + result.events)
    }
}
