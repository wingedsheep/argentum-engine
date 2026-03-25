package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffects
import com.wingedsheep.engine.mechanics.layers.createFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.CantAttackOrBlockTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for CantAttackOrBlockTargetEffect.
 * "Target creature can't attack or block this turn."
 *
 * Creates two floating effects (SetCantAttack + SetCantBlock) on the targeted creature.
 * Used by Briber's Purse and similar effects.
 */
class CantAttackOrBlockTargetExecutor : EffectExecutor<CantAttackOrBlockTargetEffect> {

    override val effectType: KClass<CantAttackOrBlockTargetEffect> = CantAttackOrBlockTargetEffect::class

    override fun execute(
        state: GameState,
        effect: CantAttackOrBlockTargetEffect,
        context: EffectContext
    ): ExecutionResult {
        val entityId = TargetResolutionUtils.resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)
        val container = state.getEntity(entityId) ?: return ExecutionResult.success(state)
        container.get<CardComponent>() ?: return ExecutionResult.success(state)

        val affectedEntities = setOf(entityId)

        val cantAttackEffect = state.createFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetCantAttack,
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        val cantBlockEffect = state.createFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetCantBlock,
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        val newState = state.addFloatingEffects(listOf(cantAttackEffect, cantBlockEffect))

        return ExecutionResult.success(newState)
    }
}
