package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantStaticAbilityEffect].
 * "Target creature gains '[static ability]' until end of turn"
 *
 * Adds the static ability to [GameState.grantedStaticAbilities], where the relevant
 * point-of-use checks (e.g. combat blocker validation for
 * [com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan]) consult it alongside the
 * creature's printed static abilities. Mirrors [GrantTriggeredAbilityExecutor].
 */
class GrantStaticAbilityExecutor : EffectExecutor<GrantStaticAbilityEffect> {

    override val effectType: KClass<GrantStaticAbilityEffect> =
        GrantStaticAbilityEffect::class

    override fun execute(
        state: GameState,
        effect: GrantStaticAbilityEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for static ability grant")

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target no longer exists")
        targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")
        if (!state.getBattlefield().contains(targetId)) {
            return EffectResult.error(state, "Target is not on the battlefield")
        }

        val grant = GrantedStaticAbility(
            entityId = targetId,
            ability = effect.ability,
            duration = effect.duration
        )

        val newState = state.copy(
            grantedStaticAbilities = state.grantedStaticAbilities + grant
        )

        return EffectResult.success(newState)
    }
}
