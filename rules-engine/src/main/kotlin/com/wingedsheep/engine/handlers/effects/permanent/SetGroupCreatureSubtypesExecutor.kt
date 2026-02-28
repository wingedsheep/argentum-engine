package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.SetGroupCreatureSubtypesEffect
import kotlin.reflect.KClass

/**
 * Executor for SetGroupCreatureSubtypesEffect.
 * "Each creature you control becomes a Shade until end of turn" and similar group type effects.
 */
class SetGroupCreatureSubtypesExecutor : EffectExecutor<SetGroupCreatureSubtypesEffect> {

    override val effectType: KClass<SetGroupCreatureSubtypesEffect> = SetGroupCreatureSubtypesEffect::class

    private val predicateEvaluator = PredicateEvaluator()
    private val stateProjector = StateProjector()

    override fun execute(
        state: GameState,
        effect: SetGroupCreatureSubtypesEffect,
        context: EffectContext
    ): ExecutionResult {
        val affectedEntities = mutableSetOf<EntityId>()

        val filter = effect.filter
        val predicateContext = PredicateContext.fromEffectContext(context)
        val projected = stateProjector.project(state)

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            container.get<CardComponent>() ?: continue

            // Face-down permanents are always creatures (Rule 707.2)
            val isCreature = projected.isCreature(entityId) || container.has<FaceDownComponent>()
            if (!isCreature) continue

            // Check excludeSelf
            if (filter.excludeSelf && entityId == context.sourceId) continue

            // Apply unified filter
            if (!predicateEvaluator.matchesWithProjection(state, projected, entityId, filter.baseFilter, predicateContext)) {
                continue
            }

            affectedEntities.add(entityId)
        }

        if (affectedEntities.isEmpty()) {
            return ExecutionResult.success(state)
        }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.TYPE,
                sublayer = null,
                modification = SerializableModification.SetCreatureSubtypes(effect.subtypes),
                affectedEntities = affectedEntities
            ),
            duration = effect.duration,
            sourceId = context.sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return ExecutionResult.success(newState)
    }
}
