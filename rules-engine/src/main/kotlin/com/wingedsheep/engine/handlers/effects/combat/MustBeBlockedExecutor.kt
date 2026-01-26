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
import com.wingedsheep.sdk.scripting.MustBeBlockedEffect
import kotlin.reflect.KClass

/**
 * Executor for MustBeBlockedEffect.
 * "All creatures able to block target creature this turn do so."
 *
 * This creates a floating effect that marks the target creature as needing to be
 * blocked by all creatures that can block it. The CombatManager checks for this
 * restriction during declare blockers validation.
 */
class MustBeBlockedExecutor : EffectExecutor<MustBeBlockedEffect> {

    override val effectType: KClass<MustBeBlockedEffect> = MustBeBlockedEffect::class

    override fun execute(
        state: GameState,
        effect: MustBeBlockedEffect,
        context: EffectContext
    ): ExecutionResult {
        // Resolve the target creature
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for must-be-blocked effect")

        // Verify target exists and is a creature
        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target creature no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")
        if (!cardComponent.typeLine.isCreature) {
            return ExecutionResult.error(state, "Target is not a creature")
        }

        // Create a floating effect marking this creature as "must be blocked by all"
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,  // Layer doesn't matter for this effect
                sublayer = null,
                modification = SerializableModification.MustBeBlockedByAll,
                affectedEntities = setOf(targetId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        // Add the floating effect to game state
        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return ExecutionResult.success(newState)
    }
}
