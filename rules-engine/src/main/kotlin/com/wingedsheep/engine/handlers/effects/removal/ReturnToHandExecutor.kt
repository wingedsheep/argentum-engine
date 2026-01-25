package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.movePermanentToZone
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.scripting.ReturnToHandEffect

/**
 * Executor for ReturnToHandEffect.
 * "Return target creature to its owner's hand"
 */
class ReturnToHandExecutor : EffectExecutor<ReturnToHandEffect> {

    override fun execute(
        state: GameState,
        effect: ReturnToHandEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for return")

        return movePermanentToZone(state, targetId, ZoneType.HAND)
    }
}
