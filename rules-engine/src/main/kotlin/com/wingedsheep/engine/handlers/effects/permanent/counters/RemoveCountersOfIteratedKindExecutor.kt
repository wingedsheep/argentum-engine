package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersOfIteratedKindEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlin.reflect.KClass

/**
 * Executor for [RemoveCountersOfIteratedKindEffect] — removes counters of the kind the enclosing
 * counter-kind loop bound as `chosenCounterType` (none → no-op), Dramatist's Puppet's "or remove
 * one from it". Removal goes through [RemoveCountersEffect], so it takes off only what is there
 * and emits the usual event.
 */
class RemoveCountersOfIteratedKindExecutor(
    amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<RemoveCountersOfIteratedKindEffect> {

    override val effectType: KClass<RemoveCountersOfIteratedKindEffect> = RemoveCountersOfIteratedKindEffect::class

    private val removeCounters = RemoveCountersExecutor(amountEvaluator)

    override fun execute(
        state: GameState,
        effect: RemoveCountersOfIteratedKindEffect,
        context: EffectContext
    ): EffectResult {
        val kind = context.chosenCounterType
        if (kind == null || effect.count <= 0) return EffectResult.success(state, emptyList())
        val targetId = if (effect.target is EffectTarget.PlayerRef) {
            context.resolvePlayerTarget(effect.target, state)
        } else {
            context.resolveTarget(effect.target, state)
        } ?: return EffectResult.success(state, emptyList())

        return removeCounters.execute(
            state,
            RemoveCountersEffect(kind, DynamicAmount.Fixed(effect.count), EffectTarget.SpecificEntity(targetId)),
            context
        )
    }
}
