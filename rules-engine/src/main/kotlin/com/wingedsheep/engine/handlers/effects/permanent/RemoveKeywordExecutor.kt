package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RemoveKeywordEffect
import kotlin.reflect.KClass

/**
 * Executor for RemoveKeywordEffect.
 * "All other creatures lose flying until end of turn."
 */
class RemoveKeywordExecutor : EffectExecutor<RemoveKeywordEffect> {

    override val effectType: KClass<RemoveKeywordEffect> = RemoveKeywordEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveKeywordEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return ExecutionResult.error(state, "No valid target for keyword removal")

        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target creature no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")
        val projected = state.projectedState
        if (!projected.isCreature(targetId)) {
            return ExecutionResult.error(state, "Target is not a creature")
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.RemoveKeyword(effect.keyword),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        return ExecutionResult.success(newState)
    }
}
