package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.Effect
import com.wingedsheep.sdk.scripting.FlipCoinEffect
import kotlin.reflect.KClass

/**
 * Executor for FlipCoinEffect.
 * Flips a coin (50/50 random) and executes the appropriate sub-effect.
 *
 * @param effectExecutor Function to execute a sub-effect (provided by registry)
 */
class FlipCoinExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<FlipCoinEffect> {

    override val effectType: KClass<FlipCoinEffect> = FlipCoinEffect::class

    override fun execute(
        state: GameState,
        effect: FlipCoinEffect,
        context: EffectContext
    ): ExecutionResult {
        val won = kotlin.random.Random.nextBoolean()

        val sourceId = context.sourceId
        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val event = CoinFlipEvent(context.controllerId, won, sourceId ?: context.controllerId, sourceName)

        val subEffect = if (won) effect.wonEffect else effect.lostEffect

        if (subEffect == null) {
            return ExecutionResult.success(state, listOf(event))
        }

        val result = effectExecutor(state, subEffect, context)
        return result.copy(events = listOf(event) + result.events)
    }
}
