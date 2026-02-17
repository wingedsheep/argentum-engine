package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId

class ColorChoiceContinuationResumer(
    private val ctx: ContinuationContext
) {

    fun resumeChooseColorProtection(
        state: GameState,
        continuation: ChooseColorProtectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is ColorChosenResponse) {
            return ExecutionResult.error(state, "Expected color choice response for protection effect")
        }

        val chosenColor = response.color
        val controllerId = continuation.controllerId
        val events = mutableListOf<GameEvent>()
        val affectedEntities = mutableSetOf<EntityId>()
        val predicateEvaluator = PredicateEvaluator()
        val predicateContext = PredicateContext(controllerId = controllerId, sourceId = continuation.sourceId)
        val projected = StateProjector().project(state)

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (continuation.filter.excludeSelf && entityId == continuation.sourceId) continue

            if (!predicateEvaluator.matchesWithProjection(state, projected, entityId, continuation.filter.baseFilter, predicateContext)) {
                continue
            }

            affectedEntities.add(entityId)

            val displayName = if (container.has<FaceDownComponent>()) "Face-down creature" else cardComponent.name
            events.add(
                KeywordGrantedEvent(
                    targetId = entityId,
                    targetName = displayName,
                    keyword = "Protection from ${chosenColor.displayName.lowercase()}",
                    sourceName = continuation.sourceName ?: "Unknown"
                )
            )
        }

        if (affectedEntities.isEmpty()) {
            return checkForMore(state, events)
        }

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.GrantProtectionFromColor(
                    chosenColor.name
                ),
                affectedEntities = affectedEntities
            ),
            duration = continuation.duration,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return checkForMore(newState, events)
    }

    fun resumeChooseColorProtectionTarget(
        state: GameState,
        continuation: ChooseColorProtectionTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is ColorChosenResponse) {
            return ExecutionResult.error(state, "Expected color choice response for protection effect")
        }

        val chosenColor = response.color
        val targetEntityId = continuation.targetEntityId
        val events = mutableListOf<GameEvent>()

        val container = state.getEntity(targetEntityId)
        val cardComponent = container?.get<CardComponent>()
        if (container == null || cardComponent == null || !state.getBattlefield().contains(targetEntityId)) {
            return checkForMore(state, events)
        }

        val displayName = if (container.has<FaceDownComponent>()) "Face-down creature" else cardComponent.name
        events.add(
            KeywordGrantedEvent(
                targetId = targetEntityId,
                targetName = displayName,
                keyword = "Protection from ${chosenColor.displayName.lowercase()}",
                sourceName = continuation.sourceName ?: "Unknown"
            )
        )

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.GrantProtectionFromColor(
                    chosenColor.name
                ),
                affectedEntities = setOf(targetEntityId)
            ),
            duration = continuation.duration,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return checkForMore(newState, events)
    }
}
