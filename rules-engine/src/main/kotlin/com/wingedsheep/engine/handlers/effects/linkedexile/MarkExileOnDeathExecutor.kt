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
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import kotlin.reflect.KClass

/**
 * Executor for MarkExileOnDeathEffect.
 * Adds an ExileOnDeath floating effect so that if the target creature would die
 * this turn, it goes to exile instead of the graveyard.
 *
 * If the target is a player (not a creature), this effect does nothing.
 */
class MarkExileOnDeathExecutor : EffectExecutor<MarkExileOnDeathEffect> {

    override val effectType: KClass<MarkExileOnDeathEffect> = MarkExileOnDeathEffect::class

    override fun execute(
        state: GameState,
        effect: MarkExileOnDeathEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return ExecutionResult.error(state, "No valid target for exile-on-death marker")

        // Only applies to creatures, not players
        val isPlayer = state.getEntity(targetId)?.get<LifeTotalComponent>() != null
        if (isPlayer) {
            return ExecutionResult.success(state)
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.ExileOnDeath,
            affectedEntities = setOf(targetId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return ExecutionResult.success(newState)
    }
}
