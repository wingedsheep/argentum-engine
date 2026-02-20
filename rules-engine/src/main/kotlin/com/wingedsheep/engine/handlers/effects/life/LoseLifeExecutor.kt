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
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import kotlin.reflect.KClass

/**
 * Executor for LoseLifeEffect.
 * "You lose X life" or "Target player loses X life"
 */
class LoseLifeExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<LoseLifeEffect> {

    override val effectType: KClass<LoseLifeEffect> = LoseLifeEffect::class

    override fun execute(
        state: GameState,
        effect: LoseLifeEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerIds = resolvePlayerTargets(effect.target, state, context)
        if (playerIds.isEmpty()) {
            return ExecutionResult.error(state, "No valid target for life loss")
        }

        val amount = amountEvaluator.evaluate(state, effect.amount, context)

        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (playerId in playerIds) {
            val currentLife = newState.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: continue
            val newLife = currentLife - amount
            newState = newState.updateEntity(playerId) { container ->
                container.with(LifeTotalComponent(newLife))
            }
            events.add(LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.LIFE_LOSS))
        }

        return ExecutionResult.success(newState, events)
    }
}
