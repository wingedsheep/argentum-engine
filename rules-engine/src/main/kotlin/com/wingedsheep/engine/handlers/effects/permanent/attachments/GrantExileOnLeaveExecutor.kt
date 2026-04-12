package com.wingedsheep.engine.handlers.effects.permanent.attachments

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ExileOnLeaveBattlefieldComponent
import com.wingedsheep.sdk.scripting.effects.GrantExileOnLeaveEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantExileOnLeaveEffect.
 *
 * Adds ExileOnLeaveBattlefieldComponent to a target permanent so that
 * if it would leave the battlefield, it is exiled instead.
 */
class GrantExileOnLeaveExecutor : EffectExecutor<GrantExileOnLeaveEffect> {

    override val effectType: KClass<GrantExileOnLeaveEffect> = GrantExileOnLeaveEffect::class

    override fun execute(
        state: GameState,
        effect: GrantExileOnLeaveEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for exile-on-leave grant")

        if (state.getEntity(targetId) == null) {
            return EffectResult.error(state, "Target entity not found")
        }

        val newState = state.updateEntity(targetId) { container ->
            container.with(ExileOnLeaveBattlefieldComponent)
        }

        return EffectResult.success(newState)
    }
}
