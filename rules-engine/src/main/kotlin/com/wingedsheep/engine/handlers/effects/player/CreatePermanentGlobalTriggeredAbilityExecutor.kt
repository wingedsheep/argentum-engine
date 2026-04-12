package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GlobalGrantedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CreatePermanentGlobalTriggeredAbilityEffect
import kotlin.reflect.KClass

/**
 * Executor for CreatePermanentGlobalTriggeredAbilityEffect.
 * Creates a global triggered ability that lasts permanently.
 * Used for effects that create recurring triggers from non-permanent sources
 * (e.g., Dimensional Breach creating an upkeep trigger from a sorcery).
 */
class CreatePermanentGlobalTriggeredAbilityExecutor :
    EffectExecutor<CreatePermanentGlobalTriggeredAbilityEffect> {

    override val effectType: KClass<CreatePermanentGlobalTriggeredAbilityEffect> =
        CreatePermanentGlobalTriggeredAbilityEffect::class

    override fun execute(
        state: GameState,
        effect: CreatePermanentGlobalTriggeredAbilityEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "No source for global triggered ability")

        val global = GlobalGrantedTriggeredAbility(
            ability = effect.ability,
            controllerId = context.controllerId,
            sourceId = sourceId,
            sourceName = state.getEntity(sourceId)
                ?.get<CardComponent>()?.name
                ?: "Unknown",
            duration = Duration.Permanent,
            descriptionOverride = effect.descriptionOverride
        )

        val newState = state.copy(
            globalGrantedTriggeredAbilities = state.globalGrantedTriggeredAbilities + global
        )

        return EffectResult.success(newState)
    }
}
