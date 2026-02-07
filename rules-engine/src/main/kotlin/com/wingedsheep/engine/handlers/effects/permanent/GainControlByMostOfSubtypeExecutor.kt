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
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GainControlByMostOfSubtypeEffect
import kotlin.reflect.KClass

/**
 * Executor for GainControlByMostOfSubtypeEffect.
 *
 * Finds the player who controls the most creatures of the given subtype
 * and gives that player control of the target permanent.
 */
class GainControlByMostOfSubtypeExecutor : EffectExecutor<GainControlByMostOfSubtypeEffect> {

    override val effectType: KClass<GainControlByMostOfSubtypeEffect> = GainControlByMostOfSubtypeEffect::class

    override fun execute(
        state: GameState,
        effect: GainControlByMostOfSubtypeEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for control change")

        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target permanent no longer exists")

        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")

        // Count creatures of the subtype per player
        val counts = state.turnOrder.associateWith { playerId ->
            state.entities.count { (_, container) ->
                container.get<ControllerComponent>()?.playerId == playerId &&
                container.get<CardComponent>()?.typeLine?.hasSubtype(effect.subtype) == true
            }
        }

        val maxCount = counts.values.maxOrNull() ?: return ExecutionResult.success(state)
        if (maxCount == 0) return ExecutionResult.success(state)

        val playersWithMax = counts.filter { it.value == maxCount }
        if (playersWithMax.size != 1) return ExecutionResult.success(state)

        val newControllerId = playersWithMax.keys.first()

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
            duration = Duration.Permanent,
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
