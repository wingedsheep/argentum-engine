package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GainControlOfGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for GainControlOfGroupEffect.
 * "Gain control of all creatures until end of turn" and similar mass-control effects.
 *
 * Creates individual floating effects for each creature not already controlled by the caster.
 */
class GainControlOfGroupExecutor : EffectExecutor<GainControlOfGroupEffect> {

    override val effectType: KClass<GainControlOfGroupEffect> = GainControlOfGroupEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: GainControlOfGroupEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()
        val newControllerId = context.controllerId

        val filter = effect.filter
        val predicateContext = PredicateContext.fromEffectContext(context)

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down permanents are always creatures (Rule 707.2)
            if (!cardComponent.typeLine.isCreature && !container.has<FaceDownComponent>()) continue

            // Check excludeSelf
            if (filter.excludeSelf && entityId == context.sourceId) continue

            // Apply unified filter
            if (!predicateEvaluator.matches(state, entityId, filter.baseFilter, predicateContext)) {
                continue
            }

            // Skip if already controlled by the caster
            val currentControllerId = container.get<ControllerComponent>()?.playerId
            if (currentControllerId == newControllerId) continue

            // Remove any previous Layer.CONTROL floating effects from the same source on the same target
            val filteredEffects = newState.floatingEffects.filter { floating ->
                !(floating.sourceId == context.sourceId &&
                  floating.effect.layer == Layer.CONTROL &&
                  entityId in floating.effect.affectedEntities)
            }

            // Create new floating effect for this creature
            val floatingEffect = ActiveFloatingEffect(
                id = EntityId.generate(),
                effect = FloatingEffectData(
                    layer = Layer.CONTROL,
                    sublayer = null,
                    modification = SerializableModification.ChangeController(newControllerId),
                    affectedEntities = setOf(entityId)
                ),
                duration = effect.duration,
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { newState.getEntity(it)?.get<CardComponent>()?.name },
                controllerId = newControllerId,
                timestamp = System.currentTimeMillis()
            )

            newState = newState.copy(
                floatingEffects = filteredEffects + floatingEffect
            )

            events.add(
                ControlChangedEvent(
                    permanentId = entityId,
                    permanentName = cardComponent.name,
                    oldControllerId = currentControllerId ?: newControllerId,
                    newControllerId = newControllerId
                )
            )
        }

        return ExecutionResult.success(newState, events)
    }
}
