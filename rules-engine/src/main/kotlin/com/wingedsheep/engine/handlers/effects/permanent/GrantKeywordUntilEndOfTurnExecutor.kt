package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantKeywordUntilEndOfTurnEffect.
 * "Target creature gains [keyword] until end of turn"
 */
class GrantKeywordUntilEndOfTurnExecutor : EffectExecutor<GrantKeywordUntilEndOfTurnEffect> {

    override val effectType: KClass<GrantKeywordUntilEndOfTurnEffect> = GrantKeywordUntilEndOfTurnEffect::class

    override fun execute(
        state: GameState,
        effect: GrantKeywordUntilEndOfTurnEffect,
        context: EffectContext
    ): ExecutionResult {
        // Resolve the target creature
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for keyword grant")

        // Verify target exists and is a creature (use projected types for animated lands etc.)
        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target creature no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")
        val projected = StateProjector().project(state)
        if (!projected.isCreature(targetId)) {
            return ExecutionResult.error(state, "Target is not a creature")
        }

        // Create a floating effect for the keyword grant
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.GrantKeyword(effect.keyword),
                affectedEntities = setOf(targetId)
            ),
            duration = effect.duration,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        // Add the floating effect to game state
        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        // Emit event for visualization
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val events = listOf(
            KeywordGrantedEvent(
                targetId = targetId,
                targetName = cardComponent.name,
                keyword = effect.keyword.lowercase().replace('_', ' '),
                sourceName = sourceName
            )
        )

        return ExecutionResult.success(newState, events)
    }
}
