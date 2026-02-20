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
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.PreventLifeGain
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
            if (isLifeGainPrevented(state, playerId, context.controllerId)) continue

            val currentLife = newState.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: continue
            val newLife = currentLife + amount
            newState = newState.updateEntity(playerId) { container ->
                container.with(LifeTotalComponent(newLife))
            }
            events.add(LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.LIFE_GAIN))
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Check if life gain is prevented for a player by any PreventLifeGain replacement effect
     * on the battlefield (e.g., Sulfuric Vortex, Erebos).
     */
    private fun isLifeGainPrevented(state: GameState, playerId: EntityId, controllerId: EntityId?): Boolean {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is PreventLifeGain) continue

                val lifeGainEvent = effect.appliesTo
                if (lifeGainEvent !is com.wingedsheep.sdk.scripting.GameEvent.LifeGainEvent) continue

                when (lifeGainEvent.player) {
                    Player.Each -> return true
                    Player.You -> {
                        val sourceControllerId = container.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
                        if (playerId == sourceControllerId) return true
                    }
                    Player.Opponent -> {
                        val sourceControllerId = container.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
                        if (playerId != sourceControllerId) return true
                    }
                    else -> {}
                }
            }
        }
        return false
    }
}
