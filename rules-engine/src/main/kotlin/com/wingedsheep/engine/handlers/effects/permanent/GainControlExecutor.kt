package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.createFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import kotlin.reflect.KClass

/**
 * Executor for GainControlEffect.
 *
 * Gains control of target permanent for the controller of the spell/ability.
 */
class GainControlExecutor : EffectExecutor<GainControlEffect> {

    override val effectType: KClass<GainControlEffect> = GainControlEffect::class

    override fun execute(
        state: GameState,
        effect: GainControlEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return ExecutionResult.error(state, "No valid target for control change")

        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target permanent no longer exists")

        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")

        val newControllerId = context.controllerId

        // If that player already controls the target, no-op
        val currentControllerId = targetContainer.get<ControllerComponent>()?.playerId
        if (currentControllerId == newControllerId) return ExecutionResult.success(state)

        // Remove any previous Layer.CONTROL floating effects from the same source on the same target
        val filteredEffects = state.floatingEffects.filter { floating ->
            !(floating.sourceId == context.sourceId &&
              floating.effect.layer == Layer.CONTROL &&
              targetId in floating.effect.affectedEntities)
        }

        // Create new floating effect
        val floatingEffect = state.createFloatingEffect(
            layer = Layer.CONTROL,
            modification = SerializableModification.ChangeController(newControllerId),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        val newState = state.copy(
            floatingEffects = filteredEffects + floatingEffect
        )

        val events = listOf(
            ControlChangedEvent(
                permanentId = targetId,
                permanentName = cardComponent.name,
                oldControllerId = currentControllerId ?: context.controllerId,
                newControllerId = newControllerId
            )
        )

        return ExecutionResult.success(newState, events)
    }
}
