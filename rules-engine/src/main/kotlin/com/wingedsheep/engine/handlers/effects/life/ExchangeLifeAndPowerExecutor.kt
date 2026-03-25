package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.resolveTarget
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ExchangeLifeAndPowerEffect
import kotlin.reflect.KClass

/**
 * Executor for ExchangeLifeAndPowerEffect.
 * Exchanges a player's life total with a creature's power.
 *
 * Per MTG Rule 118.7, the exchange is simultaneous:
 * 1. Read both the player's life total and the creature's projected power
 * 2. Set the creature's base power to the player's former life total (floating effect at Layer 7b)
 * 3. Set the player's life total to the creature's former power (gain/lose per Rule 118.5)
 *
 * If the creature isn't on the battlefield when the ability resolves, the exchange doesn't happen.
 */
class ExchangeLifeAndPowerExecutor : EffectExecutor<ExchangeLifeAndPowerEffect> {

    override val effectType: KClass<ExchangeLifeAndPowerEffect> = ExchangeLifeAndPowerEffect::class

    override fun execute(
        state: GameState,
        effect: ExchangeLifeAndPowerEffect,
        context: EffectContext
    ): ExecutionResult {
        val creatureId = resolveTarget(effect.target, context, state)
            ?: return ExecutionResult.success(state)

        // Creature must be on the battlefield
        if (creatureId !in state.getBattlefield()) {
            return ExecutionResult.success(state)
        }

        val controllerId = context.controllerId
            ?: return ExecutionResult.success(state)

        // Read both values before making any changes (simultaneous exchange)
        val currentLife = state.getEntity(controllerId)?.get<LifeTotalComponent>()?.life
            ?: return ExecutionResult.success(state)

        val projected = state.projectedState
        val currentPower = projected.getPower(creatureId)
            ?: return ExecutionResult.success(state)

        val events = mutableListOf<EngineGameEvent>()

        // Set creature's base power to the player's former life total
        var newState = state.addFloatingEffect(
            layer = Layer.POWER_TOUGHNESS,
            modification = SerializableModification.SetPower(currentLife),
            affectedEntities = setOf(creatureId),
            duration = Duration.Permanent,
            context = context,
            sublayer = Sublayer.SET_VALUES
        )

        // Set player's life total to the creature's former power
        if (currentPower != currentLife) {
            newState = newState.updateEntity(controllerId) { container ->
                container.with(LifeTotalComponent(currentPower))
            }

            val reason = if (currentPower > currentLife) LifeChangeReason.LIFE_GAIN else LifeChangeReason.LIFE_LOSS
            events.add(LifeChangedEvent(controllerId, currentLife, currentPower, reason))
            if (currentPower < currentLife) {
                newState = DamageUtils.markLifeLostThisTurn(newState, controllerId)
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
