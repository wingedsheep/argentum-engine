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
import com.wingedsheep.sdk.scripting.effects.PreventCombatDamageToAndByEffect
import kotlin.reflect.KClass

/**
 * Executor for PreventCombatDamageToAndByEffect.
 *
 * Prevents all combat damage that would be dealt to and dealt by a creature this turn
 * by creating a PreventCombatDamageToAndBy floating effect on the target entity.
 */
class PreventCombatDamageToAndByExecutor : EffectExecutor<PreventCombatDamageToAndByEffect> {

    override val effectType: KClass<PreventCombatDamageToAndByEffect> = PreventCombatDamageToAndByEffect::class

    override fun execute(
        state: GameState,
        effect: PreventCombatDamageToAndByEffect,
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
                modification = SerializableModification.PreventCombatDamageToAndBy,
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
