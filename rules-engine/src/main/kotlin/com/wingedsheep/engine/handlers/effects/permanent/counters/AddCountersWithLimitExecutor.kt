package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersWithLimitEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

class AddCountersWithLimitExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<AddCountersWithLimitEffect> {
    override val effectType: KClass<AddCountersWithLimitEffect> = AddCountersWithLimitEffect::class
    private val placement = AddCountersExecutor(amountEvaluator.predicates)

    override fun execute(state: GameState, effect: AddCountersWithLimitEffect, context: EffectContext): EffectResult {
        val targetId = if (effect.target is EffectTarget.PlayerRef) {
            context.resolvePlayerTarget(effect.target, state)
        } else context.resolveTarget(effect.target, state)
        if (targetId == null || state.getEntity(targetId) == null) return EffectResult.success(state, emptyList())
        val count = amountEvaluator.evaluate(state, effect.amount, context)
        if (count <= 0) return EffectResult.success(state, emptyList())
        val limit = amountEvaluator.evaluate(state, effect.totalLimit, context)
        return placement.place(state, AddCountersEffect(effect.counterType, count, effect.target), context, limit)
    }
}
