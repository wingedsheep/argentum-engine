package com.wingedsheep.engine.handlers.effects.linkedexile

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.MarkExileControllerGraveyardOnDeathEffect
import kotlin.reflect.KClass

/**
 * Executor for MarkExileControllerGraveyardOnDeathEffect.
 * Adds an ExileControllerGraveyardOnDeath floating effect so that when the target creature
 * dies this turn, its controller's graveyard is exiled.
 *
 * If the target is a player (not a creature), this effect does nothing.
 */
class MarkExileControllerGraveyardOnDeathExecutor : EffectExecutor<MarkExileControllerGraveyardOnDeathEffect> {

    override val effectType: KClass<MarkExileControllerGraveyardOnDeathEffect> =
        MarkExileControllerGraveyardOnDeathEffect::class

    override fun execute(
        state: GameState,
        effect: MarkExileControllerGraveyardOnDeathEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return ExecutionResult.error(state, "No valid target for exile-controller-graveyard-on-death marker")

        // Only applies to creatures, not players
        val isPlayer = state.getEntity(targetId)?.get<LifeTotalComponent>() != null
        if (isPlayer) {
            return ExecutionResult.success(state)
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.ExileControllerGraveyardOnDeath,
            affectedEntities = setOf(targetId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return ExecutionResult.success(newState)
    }
}
