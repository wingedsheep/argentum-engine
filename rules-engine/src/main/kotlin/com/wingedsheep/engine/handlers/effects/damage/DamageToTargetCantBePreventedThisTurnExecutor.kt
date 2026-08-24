package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageUnpreventableThisTurnComponent
import com.wingedsheep.sdk.scripting.effects.DamageToTargetCantBePreventedThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for [DamageToTargetCantBePreventedThisTurnEffect] (Whippoorwill).
 *
 * Stamps [DamageUnpreventableThisTurnComponent] on the resolved target. Everything else is read
 * side: `DamageUtils.isDamagePreventionDisabled(state, recipientId)` consults the marker wherever
 * prevention is applied, and the redirection check skips a marked recipient — which together cover
 * "can't be prevented **or dealt instead to another permanent or player**".
 *
 * A no-op when the target has already left the battlefield: there is nothing to mark, and the
 * marker would be stripped on the way out anyway.
 */
class DamageToTargetCantBePreventedThisTurnExecutor :
    EffectExecutor<DamageToTargetCantBePreventedThisTurnEffect> {

    override val effectType: KClass<DamageToTargetCantBePreventedThisTurnEffect> =
        DamageToTargetCantBePreventedThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: DamageToTargetCantBePreventedThisTurnEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = TargetResolutionUtils.resolveTarget(effect.target, context, state)
            ?: return EffectResult.success(state)
        if (targetId !in state.getBattlefield()) return EffectResult.success(state)
        return EffectResult.success(
            state.updateEntity(targetId) { it.with(DamageUnpreventableThisTurnComponent) }
        )
    }
}
