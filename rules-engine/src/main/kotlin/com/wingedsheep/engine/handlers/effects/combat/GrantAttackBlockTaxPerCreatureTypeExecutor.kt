package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantAttackBlockTaxPerCreatureTypeEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantAttackBlockTaxPerCreatureTypeEffect.
 * Creates a floating effect that marks a creature as having a combat tax
 * based on the count of a specific creature type on the battlefield.
 */
class GrantAttackBlockTaxPerCreatureTypeExecutor : EffectExecutor<GrantAttackBlockTaxPerCreatureTypeEffect> {

    override val effectType: KClass<GrantAttackBlockTaxPerCreatureTypeEffect> =
        GrantAttackBlockTaxPerCreatureTypeEffect::class

    override fun execute(
        state: GameState,
        effect: GrantAttackBlockTaxPerCreatureTypeEffect,
        context: EffectContext
    ): ExecutionResult {
        val entityId = context.resolveTarget(effect.target)
            ?: return ExecutionResult.success(state)
        val container = state.getEntity(entityId) ?: return ExecutionResult.success(state)
        container.get<CardComponent>() ?: return ExecutionResult.success(state)

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.AttackBlockTaxPerCreatureType(
                creatureType = effect.creatureType,
                manaCostPer = effect.manaCostPer
            ),
            affectedEntities = setOf(entityId),
            duration = effect.duration,
            context = context
        )

        return ExecutionResult.success(newState)
    }
}
