package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.sdk.scripting.effects.AllowLoyaltyActivationsThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for [AllowLoyaltyActivationsThisTurnEffect] — raises the target permanent's own
 * loyalty allowance for the turn (CR 606.3's "only if no player has previously activated a loyalty
 * ability of that permanent that turn", relaxed to N).
 *
 * The allowance is stored on the permanent's turn-scoped [AbilityActivatedThisTurnComponent], so it
 * inherits that tracker's lifetime: cleanup strips it at end of turn, and a zone change strips it
 * because the permanent becomes a new object. It only ever raises the limit, which is what makes a
 * second grant non-additive.
 *
 * A permission, not a state change anything reacts to, so no event is emitted (like
 * `PlayAdditionalLandsExecutor`).
 */
class AllowLoyaltyActivationsThisTurnExecutor : EffectExecutor<AllowLoyaltyActivationsThisTurnEffect> {

    override val effectType: KClass<AllowLoyaltyActivationsThisTurnEffect> =
        AllowLoyaltyActivationsThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: AllowLoyaltyActivationsThisTurnEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)
        // A planeswalker that left the battlefield before this resolved is a new object;
        // nothing to grant.
        if (targetId !in state.getBattlefield()) return EffectResult.success(state)

        val newState = state.updateEntity(targetId) { container ->
            val tracker = container.get<AbilityActivatedThisTurnComponent>() ?: AbilityActivatedThisTurnComponent()
            container.with(tracker.withLoyaltyActivationLimitAtLeast(effect.times))
        }
        return EffectResult.success(newState)
    }
}
