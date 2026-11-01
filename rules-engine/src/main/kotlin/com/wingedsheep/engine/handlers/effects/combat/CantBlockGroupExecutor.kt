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
import com.wingedsheep.sdk.scripting.effects.CantBlockGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for CantBlockGroupEffect.
 * "Creatures can't block this turn." / "[filter] creatures can't block this turn."
 *
 * Creates a floating effect with SetCantBlock that dynamically applies to all creatures
 * matching the filter. Per Rule 611.2c, rule-modifying effects like "can't block" apply
 * to all matching objects including those entering the battlefield after the effect resolves.
 */
class CantBlockGroupExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<CantBlockGroupEffect> {

    override val effectType: KClass<CantBlockGroupEffect> = CantBlockGroupEffect::class

    override fun execute(
        state: GameState,
        effect: CantBlockGroupEffect,
        context: EffectContext
    ): EffectResult {
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetCantBlock,
            affectedEntities = emptySet(),
            duration = effect.duration,
            context = context,
            dynamicGroupFilter = ResolutionFilterBaker.bake(effect.filter, context, state, amountEvaluator)
        )

        return EffectResult.success(newState)
    }
}
