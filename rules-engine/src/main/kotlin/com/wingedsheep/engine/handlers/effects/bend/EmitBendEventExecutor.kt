package com.wingedsheep.engine.handlers.effects.bend

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.EmitBendEventEffect
import kotlin.reflect.KClass

/**
 * Resolution tail of [com.wingedsheep.sdk.dsl.Effects.Earthbend] / [com.wingedsheep.sdk.dsl.Effects.Airbend]
 * and the firebending attack trigger: records the bend for the effect's controller (CR 701.65b /
 * 701.66b / 702.189b) and emits a [com.wingedsheep.engine.core.BendPerformedEvent] so
 * [com.wingedsheep.sdk.dsl.Triggers.YouBend] triggers fire exactly once per bend.
 *
 * Waterbend (CR 701.67c) does not route through here — it is emitted where the waterbend cost is
 * paid in the cast/activate handlers, via the same [BendEvents.record] entry point.
 */
class EmitBendEventExecutor : EffectExecutor<EmitBendEventEffect> {

    override val effectType: KClass<EmitBendEventEffect> = EmitBendEventEffect::class

    override fun execute(
        state: GameState,
        effect: EmitBendEventEffect,
        context: EffectContext
    ): EffectResult {
        val (newState, event) = BendEvents.record(state, context.controllerId, effect.bendType)
        return EffectResult.success(newState, listOf(event))
    }
}
