package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.event.GlobalGrantedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.CreateGlobalTriggeredAbilityUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.Duration
import kotlin.reflect.KClass

/**
 * Executor for CreateGlobalTriggeredAbilityUntilEndOfTurnEffect.
 * Creates a global triggered ability that lasts until end of turn.
 * Used for cards like False Cure.
 */
class CreateGlobalTriggeredAbilityUntilEndOfTurnExecutor :
    EffectExecutor<CreateGlobalTriggeredAbilityUntilEndOfTurnEffect> {

    override val effectType: KClass<CreateGlobalTriggeredAbilityUntilEndOfTurnEffect> =
        CreateGlobalTriggeredAbilityUntilEndOfTurnEffect::class

    override fun execute(
        state: GameState,
        effect: CreateGlobalTriggeredAbilityUntilEndOfTurnEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "No source for global triggered ability")

        val global = GlobalGrantedTriggeredAbility(
            ability = effect.ability,
            controllerId = context.controllerId,
            sourceId = sourceId,
            sourceName = state.getEntity(sourceId)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name
                ?: "Unknown",
            duration = Duration.EndOfTurn
        )

        val newState = state.copy(
            globalGrantedTriggeredAbilities = state.globalGrantedTriggeredAbilities + global
        )

        return ExecutionResult.success(newState)
    }
}
