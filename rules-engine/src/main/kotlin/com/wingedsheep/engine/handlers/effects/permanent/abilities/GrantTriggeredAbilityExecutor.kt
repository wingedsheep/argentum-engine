package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantTriggeredAbilityEffect.
 * "Target creature gains '[triggered ability]' until end of turn"
 *
 * Adds the triggered ability to GameState.grantedTriggeredAbilities,
 * where TriggerDetector will find it when checking for triggers on
 * that entity.
 */
class GrantTriggeredAbilityExecutor : EffectExecutor<GrantTriggeredAbilityEffect> {

    override val effectType: KClass<GrantTriggeredAbilityEffect> =
        GrantTriggeredAbilityEffect::class

    override fun execute(
        state: GameState,
        effect: GrantTriggeredAbilityEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for triggered ability grant")

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target no longer exists")
        targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")
        if (!state.getBattlefield().contains(targetId)) {
            return EffectResult.error(state, "Target is not on the battlefield")
        }
        // Deliberately *not* gated on the target being a creature. Nothing in the rules restricts
        // "gains '<triggered ability>'" to creatures, and the printed wording routinely names a
        // noncreature permanent — Down in the Valley's chapter II is "**This Saga** gains 'Landfall
        // — Whenever a land you control enters, create a 1/1 green Elf creature token.'" A
        // creature-only guard turned that into a silent no-op (the grant errored, the chapter
        // resolved, and no trigger ever fired). Whether a given effect may legally pick a
        // noncreature is the TargetRequirement's job, not this executor's.

        val grant = GrantedTriggeredAbility(
            entityId = targetId,
            ability = effect.ability,
            duration = effect.duration
        )

        val newState = state.copy(
            grantedTriggeredAbilities = state.grantedTriggeredAbilities + grant
        )

        return EffectResult.success(newState)
    }
}
