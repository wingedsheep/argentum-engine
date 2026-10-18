package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ResolutionPlayerBaker
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.AttacksWithoutTappingGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for [AttacksWithoutTappingGroupEffect] — "attacking doesn't cause [filter] creatures to tap".
 *
 * Mirrors [CantAttackGroupExecutor]: one floating effect over a dynamic group, so creatures that
 * arrive after it resolved are covered too (CR 611.2c), with the effect's condition riding along as
 * the floating effect's source condition and re-asked on every projection.
 */
class AttacksWithoutTappingGroupExecutor : EffectExecutor<AttacksWithoutTappingGroupEffect> {

    override val effectType: KClass<AttacksWithoutTappingGroupEffect> = AttacksWithoutTappingGroupEffect::class

    override fun execute(
        state: GameState,
        effect: AttacksWithoutTappingGroupEffect,
        context: EffectContext
    ): EffectResult = EffectResult.success(
        state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetAttacksWithoutTapping,
            affectedEntities = emptySet(),
            duration = effect.duration,
            context = context,
            dynamicGroupFilter = ResolutionPlayerBaker.bake(effect.filter, context, state),
            sourceCondition = effect.condition
        )
    )
}
