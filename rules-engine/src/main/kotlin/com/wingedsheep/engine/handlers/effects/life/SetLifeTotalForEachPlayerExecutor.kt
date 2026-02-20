package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.effects.SetLifeTotalForEachPlayerEffect
import kotlin.reflect.KClass

/**
 * Executor for SetLifeTotalForEachPlayerEffect.
 * Sets each player's life total to a per-player dynamic amount.
 *
 * Per MTG Rule 118.5, setting a life total causes the player to gain or lose
 * the necessary amount of life. This executor emits the appropriate LifeChangedEvent
 * for each player.
 */
class SetLifeTotalForEachPlayerExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<SetLifeTotalForEachPlayerEffect> {

    override val effectType: KClass<SetLifeTotalForEachPlayerEffect> = SetLifeTotalForEachPlayerEffect::class

    override fun execute(
        state: GameState,
        effect: SetLifeTotalForEachPlayerEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (playerId in state.turnOrder) {
            val currentLife = newState.getEntity(playerId)?.get<LifeTotalComponent>()?.life
                ?: continue

            // Evaluate the amount with this player as the "controller" so Player.You resolves to them
            val perPlayerContext = context.copy(controllerId = playerId)
            val newLife = amountEvaluator.evaluate(newState, effect.perPlayerAmount, perPlayerContext)

            if (newLife != currentLife) {
                newState = newState.updateEntity(playerId) { container ->
                    container.with(LifeTotalComponent(newLife))
                }

                val reason = if (newLife > currentLife) LifeChangeReason.LIFE_GAIN else LifeChangeReason.LIFE_LOSS
                events.add(LifeChangedEvent(playerId, currentLife, newLife, reason))
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
