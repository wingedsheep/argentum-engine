package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.destroyPermanent
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.DestroyEffect
import kotlin.reflect.KClass

/**
 * Executor for DestroyEffect.
 * "Destroy target creature/permanent"
 */
class DestroyExecutor : EffectExecutor<DestroyEffect> {

    override val effectType: KClass<DestroyEffect> = DestroyEffect::class

    override fun execute(
        state: GameState,
        effect: DestroyEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for destroy")

        return destroyPermanent(state, targetId)
    }
}
