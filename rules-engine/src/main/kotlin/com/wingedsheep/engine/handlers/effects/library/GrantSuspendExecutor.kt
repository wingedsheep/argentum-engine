package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.GrantSuspendEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantSuspendEffect] — marks the target card as suspended (CR 702.62) and
 * pre-arms its haste, through the shared [markSuspended] tail.
 *
 * The marker step does not move the card or add time counters; the
 * [com.wingedsheep.sdk.dsl.Effects.Suspend] chain composes those from a move/counter step.
 */
class GrantSuspendExecutor : EffectExecutor<GrantSuspendEffect> {

    override val effectType: KClass<GrantSuspendEffect> = GrantSuspendEffect::class

    override fun execute(
        state: GameState,
        effect: GrantSuspendEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        // context.controllerId — the player who suspended/will play the card — gates the haste.
        return EffectResult.success(state.markSuspended(targetId, context))
    }
}
