package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ResolutionFilterBaker
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.MustBlockGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for [MustBlockGroupEffect] — "[filter] block this turn if able" (Brutal Hordechief).
 *
 * Mirrors [MustAttackGroupExecutor]: one floating [SerializableModification.SetMustBlock] over a
 * dynamic group, so creatures that arrive later are bound too (CR 611.2c). That is the projection
 * the static `MustBlock` (Grand Melee) and [MarkMustBlockThisTurnExecutor] write, so
 * `BlockPhaseManager.validateProjectedMustBlockRequirements` enforces it with no further wiring.
 */
class MustBlockGroupExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<MustBlockGroupEffect> {

    override val effectType: KClass<MustBlockGroupEffect> = MustBlockGroupEffect::class

    override fun execute(
        state: GameState,
        effect: MustBlockGroupEffect,
        context: EffectContext
    ): EffectResult {
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetMustBlock,
            affectedEntities = emptySet(),
            duration = effect.duration,
            context = context,
            dynamicGroupFilter = ResolutionFilterBaker.bake(effect.filter, context, state, amountEvaluator)
        )
        return EffectResult.success(newState)
    }
}
