package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTargets
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.GainLifeEffect
import kotlin.reflect.KClass

/**
 * Executor for GainLifeEffect.
 * "You gain X life" or "Target player gains X life"
 */
class GainLifeExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<GainLifeEffect> {

    override val effectType: KClass<GainLifeEffect> = GainLifeEffect::class

    override fun execute(
        state: GameState,
        effect: GainLifeEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerIds = resolvePlayerTargets(effect.target, state, context)
        if (playerIds.isEmpty()) {
            return ExecutionResult.error(state, "No valid target for life gain")
        }

        val amount = amountEvaluator.evaluate(state, effect.amount, context)

        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (playerId in playerIds) {
            val currentLife = newState.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: continue
            val newLife = currentLife + amount
            newState = newState.updateEntity(playerId) { container ->
                container.with(LifeTotalComponent(newLife))
            }
            events.add(LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.LIFE_GAIN))
        }

        return ExecutionResult.success(newState, events)
    }
}
