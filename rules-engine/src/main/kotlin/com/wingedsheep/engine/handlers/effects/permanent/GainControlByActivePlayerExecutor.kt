package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ControlChangedEvent
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
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GainControlByActivePlayerEffect
import kotlin.reflect.KClass

/**
 * Executor for GainControlByActivePlayerEffect.
 *
 * Gives control of target permanent to the active player (whose turn it is),
 * rather than to the ability's controller.
 *
 * Used by Risky Move: "At the beginning of each player's upkeep, that player
 * gains control of Risky Move."
 */
class GainControlByActivePlayerExecutor : EffectExecutor<GainControlByActivePlayerEffect> {

    override val effectType: KClass<GainControlByActivePlayerEffect> = GainControlByActivePlayerEffect::class

    override fun execute(
        state: GameState,
        effect: GainControlByActivePlayerEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for control change")

        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target permanent no longer exists")

        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")

        val newControllerId = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

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
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.CONTROL,
                sublayer = null,
                modification = SerializableModification.ChangeController(newControllerId),
                affectedEntities = setOf(targetId)
            ),
            duration = com.wingedsheep.sdk.scripting.Duration.Permanent,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = newControllerId,
            timestamp = System.currentTimeMillis()
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
