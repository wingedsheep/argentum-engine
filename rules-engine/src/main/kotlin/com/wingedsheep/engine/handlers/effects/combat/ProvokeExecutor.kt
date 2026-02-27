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
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ProvokeEffect
import kotlin.reflect.KClass

/**
 * Executor for ProvokeEffect.
 * "Target creature defending player controls untaps and blocks this creature if able."
 *
 * 1. Untaps the target creature
 * 2. Creates a floating effect forcing the target to block the source attacker
 */
class ProvokeExecutor : EffectExecutor<ProvokeEffect> {

    override val effectType: KClass<ProvokeEffect> = ProvokeEffect::class

    override fun execute(
        state: GameState,
        effect: ProvokeEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for provoke effect")

        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target creature no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")
        if (!cardComponent.typeLine.isCreature) {
            return ExecutionResult.error(state, "Target is not a creature")
        }

        // The source is the creature with provoke (the attacker)
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "No source for provoke effect")

        // Verify source is attacking
        val sourceContainer = state.getEntity(sourceId)
            ?: return ExecutionResult.error(state, "Source creature no longer exists")
        if (!sourceContainer.has<AttackingComponent>()) {
            return ExecutionResult.error(state, "Source creature is not attacking")
        }

        // Step 1: Untap the target creature
        var newState = state.updateEntity(targetId) { container ->
            container.without<TappedComponent>()
        }

        // Step 2: Create a floating effect forcing the target to block the source
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.MustBlockSpecificAttacker(sourceId),
                affectedEntities = setOf(targetId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = sourceId,
            sourceName = sourceContainer.get<CardComponent>()?.name,
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        newState = newState.copy(
            floatingEffects = newState.floatingEffects + floatingEffect
        )

        return ExecutionResult.success(newState)
    }
}
