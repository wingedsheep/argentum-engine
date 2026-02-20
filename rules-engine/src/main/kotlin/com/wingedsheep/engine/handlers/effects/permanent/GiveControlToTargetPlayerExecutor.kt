package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect
import kotlin.reflect.KClass

/**
 * Executor for GiveControlToTargetPlayerEffect.
 *
 * Gives control of a permanent to a targeted player (not the ability's controller).
 * Used by Custody Battle to give control of enchanted creature to a target opponent.
 */
class GiveControlToTargetPlayerExecutor : EffectExecutor<GiveControlToTargetPlayerEffect> {

    override val effectType: KClass<GiveControlToTargetPlayerEffect> = GiveControlToTargetPlayerEffect::class

    override fun execute(
        state: GameState,
        effect: GiveControlToTargetPlayerEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.permanent, context, state)
            ?: return ExecutionResult.error(state, "No valid permanent for control change")

        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target permanent no longer exists")

        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")

        val newControllerId = resolvePlayerTarget(effect.newController, context)
            ?: return ExecutionResult.error(state, "No valid player target for control change")

        val currentControllerId = targetContainer.get<ControllerComponent>()?.playerId

        // Remove any previous Layer.CONTROL floating effects from the same source on the same target
        val filteredEffects = state.floatingEffects.filter { floating ->
            !(floating.sourceId == context.sourceId &&
              floating.effect.layer == Layer.CONTROL &&
              targetId in floating.effect.affectedEntities)
        }

        // If the base controller already matches after removing old floating effects, no new effect needed
        val newState = if (currentControllerId == newControllerId) {
            state.copy(floatingEffects = filteredEffects)
        } else {
            val floatingEffect = ActiveFloatingEffect(
                id = EntityId.generate(),
                effect = FloatingEffectData(
                    layer = Layer.CONTROL,
                    sublayer = null,
                    modification = SerializableModification.ChangeController(newControllerId),
                    affectedEntities = setOf(targetId)
                ),
                duration = effect.duration,
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                controllerId = newControllerId,
                timestamp = System.currentTimeMillis()
            )
            state.copy(floatingEffects = filteredEffects + floatingEffect)
        }

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
