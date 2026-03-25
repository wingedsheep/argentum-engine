package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.resolveTarget
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantKeywordEffect.
 * "Target creature gains [keyword] until end of turn"
 */
class GrantKeywordExecutor : EffectExecutor<GrantKeywordEffect> {

    override val effectType: KClass<GrantKeywordEffect> = GrantKeywordEffect::class

    override fun execute(
        state: GameState,
        effect: GrantKeywordEffect,
        context: EffectContext
    ): ExecutionResult {
        // Resolve the target creature
        val targetId = resolveTarget(effect.target, context, state)
            ?: return ExecutionResult.error(state, "No valid target for keyword grant")

        // Verify target exists and is a creature (use projected types for animated lands etc.)
        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target creature no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")
        val projected = state.projectedState
        if (!projected.isCreature(targetId)) {
            return ExecutionResult.error(state, "Target is not a creature")
        }

        // Create a floating effect for the keyword grant
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantKeyword(effect.keyword),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
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
