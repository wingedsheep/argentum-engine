package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityResolutionCountThisTurnComponent
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import kotlin.reflect.KClass

/**
 * Executor for IncrementAbilityResolutionCountEffect.
 * Increments the ability resolution count on the source permanent.
 */
class IncrementAbilityResolutionCountExecutor : EffectExecutor<IncrementAbilityResolutionCountEffect> {

    override val effectType: KClass<IncrementAbilityResolutionCountEffect> =
        IncrementAbilityResolutionCountEffect::class

    override fun execute(
        state: GameState,
        effect: IncrementAbilityResolutionCountEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.success(state)

        // Only increment if the source is still on the battlefield
        state.getEntity(sourceId)
            ?: return ExecutionResult.success(state)

        val current = state.getEntity(sourceId)?.get<AbilityResolutionCountThisTurnComponent>()
            ?: AbilityResolutionCountThisTurnComponent()

        val newState = state.updateEntity(sourceId) { container ->
            container.with(current.incremented())
        }

        return ExecutionResult.success(newState)
    }
}
