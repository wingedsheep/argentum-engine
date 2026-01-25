package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.movePermanentToZone
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.scripting.ExileEffect
import kotlin.reflect.KClass

/**
 * Executor for ExileEffect.
 * "Exile target creature/permanent"
 */
class ExileExecutor : EffectExecutor<ExileEffect> {

    override val effectType: KClass<ExileEffect> = ExileEffect::class

    override fun execute(
        state: GameState,
        effect: ExileEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for exile")

        return movePermanentToZone(state, targetId, ZoneType.EXILE)
    }
}
