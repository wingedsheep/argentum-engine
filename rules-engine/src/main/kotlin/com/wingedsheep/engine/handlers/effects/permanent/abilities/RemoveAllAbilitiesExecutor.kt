package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect
import kotlin.reflect.KClass

/**
 * Executor for RemoveAllAbilitiesEffect.
 * "Target creature loses all abilities until end of turn"
 */
class RemoveAllAbilitiesExecutor : EffectExecutor<RemoveAllAbilitiesEffect> {

    override val effectType: KClass<RemoveAllAbilitiesEffect> = RemoveAllAbilitiesEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveAllAbilitiesEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return ExecutionResult.error(state, "No valid target for remove abilities")

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
            modification = SerializableModification.RemoveAllAbilities,
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        return ExecutionResult.success(newState, emptyList())
    }
}
