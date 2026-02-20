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
import com.wingedsheep.sdk.scripting.effects.PreventAllDamageDealtByTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for PreventAllDamageDealtByTargetEffect.
 *
 * Prevents all damage target creature or spell would deal this turn
 * by creating a PreventAllDamageDealtBy floating effect on the target entity.
 */
class PreventAllDamageDealtByTargetExecutor : EffectExecutor<PreventAllDamageDealtByTargetEffect> {

    override val effectType: KClass<PreventAllDamageDealtByTargetEffect> = PreventAllDamageDealtByTargetEffect::class

    override fun execute(
        state: GameState,
        effect: PreventAllDamageDealtByTargetEffect,
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
                modification = SerializableModification.PreventAllDamageDealtBy,
                affectedEntities = setOf(targetId)
            ),
            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "damage prevention",
            controllerId = context.controllerId,
            timestamp = state.timestamp
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return ExecutionResult.success(newState)
    }
}
