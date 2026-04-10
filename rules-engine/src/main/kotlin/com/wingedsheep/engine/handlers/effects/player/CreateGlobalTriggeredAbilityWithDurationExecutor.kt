package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.event.GlobalGrantedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.CreateGlobalTriggeredAbilityWithDurationEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateGlobalTriggeredAbilityWithDurationEffect.
 * Creates a global triggered ability with a specified duration.
 * Used for temporary triggered abilities like "Until the end of your next turn, whenever..."
 */
class CreateGlobalTriggeredAbilityWithDurationExecutor :
    EffectExecutor<CreateGlobalTriggeredAbilityWithDurationEffect> {

    override val effectType: KClass<CreateGlobalTriggeredAbilityWithDurationEffect> =
        CreateGlobalTriggeredAbilityWithDurationEffect::class

    override fun execute(
        state: GameState,
        effect: CreateGlobalTriggeredAbilityWithDurationEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "No source for global triggered ability")

        val global = GlobalGrantedTriggeredAbility(
            ability = effect.ability,
            controllerId = context.controllerId,
            sourceId = sourceId,
            sourceName = state.getEntity(sourceId)
                ?.get<CardComponent>()?.name
                ?: "Unknown",
            duration = effect.duration
        )

        val newState = state.copy(
            globalGrantedTriggeredAbilities = state.globalGrantedTriggeredAbilities + global
        )

        return ExecutionResult.success(newState)
    }
}
