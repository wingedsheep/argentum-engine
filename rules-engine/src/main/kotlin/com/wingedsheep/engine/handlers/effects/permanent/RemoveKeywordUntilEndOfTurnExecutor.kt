package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
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
import com.wingedsheep.sdk.scripting.effects.RemoveKeywordUntilEndOfTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for RemoveKeywordUntilEndOfTurnEffect.
 * "All other creatures lose flying until end of turn."
 */
class RemoveKeywordUntilEndOfTurnExecutor : EffectExecutor<RemoveKeywordUntilEndOfTurnEffect> {

    override val effectType: KClass<RemoveKeywordUntilEndOfTurnEffect> = RemoveKeywordUntilEndOfTurnEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveKeywordUntilEndOfTurnEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for keyword removal")

        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target creature no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")
        val projected = StateProjector().project(state)
        if (!projected.isCreature(targetId)) {
            return ExecutionResult.error(state, "Target is not a creature")
        }

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.RemoveKeyword(effect.keyword.name),
                affectedEntities = setOf(targetId)
            ),
            duration = effect.duration,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return ExecutionResult.success(newState)
    }
}
