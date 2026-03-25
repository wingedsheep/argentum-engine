package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.resolveTarget
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
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

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.PreventAllDamageDealtBy,
            affectedEntities = setOf(targetId),
            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
            context = context,
            timestamp = state.timestamp
        )

        return ExecutionResult.success(newState)
    }
}
