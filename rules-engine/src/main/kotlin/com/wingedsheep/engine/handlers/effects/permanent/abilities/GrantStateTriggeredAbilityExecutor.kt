package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedStateTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantStateTriggeredAbilityEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantStateTriggeredAbilityEffect].
 * "Target permanent gains '[state-triggered ability]'"
 *
 * Adds the ability to `GameState.grantedStateTriggeredAbilities`, where
 * [com.wingedsheep.engine.event.StateTriggerPoller] folds it in beside the abilities printed on
 * the permanent's own card.
 *
 * Deliberately mirrors [GrantTriggeredAbilityExecutor], including its non-gating on creature-ness:
 * nothing in the rules restricts "gains '<ability>'" to creatures, and deciding what may legally
 * be picked is the `TargetRequirement`'s job, not this executor's.
 */
class GrantStateTriggeredAbilityExecutor : EffectExecutor<GrantStateTriggeredAbilityEffect> {

    override val effectType: KClass<GrantStateTriggeredAbilityEffect> =
        GrantStateTriggeredAbilityEffect::class

    override fun execute(
        state: GameState,
        effect: GrantStateTriggeredAbilityEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for state-triggered ability grant")

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target no longer exists")
        targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")
        if (!ObjectGrantTarget.canReceive(state, targetId)) {
            return EffectResult.error(state, ObjectGrantTarget.NOT_A_PERMANENT_OR_PERMANENT_SPELL)
        }

        val grant = GrantedStateTriggeredAbility(
            entityId = targetId,
            ability = effect.ability,
            duration = effect.duration
        )

        return EffectResult.success(
            state.copy(grantedStateTriggeredAbilities = state.grantedStateTriggeredAbilities + grant)
        )
    }
}
