package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
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
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        // Verify target still exists
        state.getEntity(targetId)
            ?: return ExecutionResult.success(state)

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                modification = SerializableModification.RedirectCombatDamageToController,
                affectedEntities = setOf(targetId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = state.timestamp
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return ExecutionResult.success(newState)
    }
}
