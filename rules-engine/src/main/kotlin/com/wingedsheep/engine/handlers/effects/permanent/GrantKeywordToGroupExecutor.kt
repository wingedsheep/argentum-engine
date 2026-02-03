package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.KeywordGrantedEvent
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
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantKeywordToGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantKeywordToGroupEffect.
 * "Creatures you control gain [keyword] until end of turn" and similar group keyword effects.
 */
class GrantKeywordToGroupExecutor : EffectExecutor<GrantKeywordToGroupEffect> {

    override val effectType: KClass<GrantKeywordToGroupEffect> = GrantKeywordToGroupEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: GrantKeywordToGroupEffect,
        context: EffectContext
    ): ExecutionResult {
        val events = mutableListOf<EngineGameEvent>()
        val affectedEntities = mutableSetOf<EntityId>()
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"

        val filter = effect.filter
        val predicateContext = PredicateContext.fromEffectContext(context)

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isCreature) continue

            // Check excludeSelf
            if (filter.excludeSelf && entityId == context.sourceId) continue

            // Apply unified filter
            if (!predicateEvaluator.matches(state, entityId, filter.baseFilter, predicateContext)) {
                continue
            }

            affectedEntities.add(entityId)

            events.add(
                KeywordGrantedEvent(
                    targetId = entityId,
                    targetName = cardComponent.name,
                    keyword = effect.keyword.displayName,
                    sourceName = sourceName
                )
            )
        }

        if (affectedEntities.isEmpty()) {
            return ExecutionResult.success(state)
        }

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.GrantKeyword(effect.keyword.name),
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

        return ExecutionResult.success(newState, events)
    }
}
