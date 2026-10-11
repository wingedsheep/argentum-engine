package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.AdaptAsThoughNoCountersEffect
import kotlin.reflect.KClass

/**
 * Executor for [AdaptAsThoughNoCountersEffect] — places a one-shot
 * [SerializableModification.AdaptAsThoughNoCountersShield] on the target that lasts until end of
 * turn. [AdaptExecutor] consumes it on the creature's next adapt. Adds no counters itself.
 */
class AdaptAsThoughNoCountersExecutor : EffectExecutor<AdaptAsThoughNoCountersEffect> {

    override val effectType: KClass<AdaptAsThoughNoCountersEffect> = AdaptAsThoughNoCountersEffect::class

    override fun execute(
        state: GameState,
        effect: AdaptAsThoughNoCountersEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state, emptyList())
        if (targetId !in state.getBattlefield()) return EffectResult.success(state, emptyList())

        return EffectResult.success(
            state.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.AdaptAsThoughNoCountersShield,
                affectedEntities = setOf(targetId),
                duration = Duration.EndOfTurn,
                context = context
            )
        )
    }
}
