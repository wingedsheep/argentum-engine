package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId

class ColorChoiceContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ChooseColorProtectionContinuation::class, ::resumeChooseColorProtection),
        resumer(ChooseColorProtectionTargetContinuation::class, ::resumeChooseColorProtectionTarget)
    )

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
        val projected = state.projectedState

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

        val context = EffectContext(
            sourceId = continuation.sourceId,
            controllerId = controllerId,
            opponentId = null
        )
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantProtectionFromColor(
                chosenColor.name
            ),
            affectedEntities = affectedEntities,
            duration = continuation.duration,
            context = context
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

        val context = EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.controllerId,
            opponentId = null
        )
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantProtectionFromColor(
                chosenColor.name
            ),
            affectedEntities = setOf(targetEntityId),
            duration = continuation.duration,
            context = context
        )

        return checkForMore(newState, events)
    }
}
