package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.RedirectCombatDamageToControllerEffect
import kotlin.reflect.KClass

/**
 * Executor for RedirectCombatDamageToControllerEffect.
 *
 * Creates a floating effect that marks a creature so that the next time it would deal
 * combat damage this turn, that damage is dealt to its controller instead.
 * Used by Goblin Psychopath and similar effects.
 */
class RedirectCombatDamageToControllerExecutor : EffectExecutor<RedirectCombatDamageToControllerEffect> {

    override val effectType: KClass<RedirectCombatDamageToControllerEffect> = RedirectCombatDamageToControllerEffect::class

    override fun execute(
        state: GameState,
        effect: RedirectCombatDamageToControllerEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        // Verify target still exists
        state.getEntity(targetId)
            ?: return EffectResult.success(state)

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.RedirectCombatDamageToController,
            affectedEntities = setOf(targetId),
            duration = Duration.EndOfTurn,
            context = context,
            timestamp = state.timestamp
        )

        return EffectResult.success(newState)
    }
}
