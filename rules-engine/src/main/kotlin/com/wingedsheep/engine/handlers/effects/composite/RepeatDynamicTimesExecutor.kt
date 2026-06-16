package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.RepeatDynamicTimesEffect
import kotlin.reflect.KClass

/**
 * Executor for [RepeatDynamicTimesEffect].
 *
 * Evaluates the dynamic amount to get count N, then creates a [CompositeEffect]
 * with N copies of the body and delegates execution. This leverages the existing
 * composite effect infrastructure (including [com.wingedsheep.engine.core.EffectContinuation]
 * for pause/resume) without needing a dedicated continuation type.
 */
class RepeatDynamicTimesExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<RepeatDynamicTimesEffect> {

    override val effectType: KClass<RepeatDynamicTimesEffect> = RepeatDynamicTimesEffect::class

    override fun execute(
        state: GameState,
        effect: RepeatDynamicTimesEffect,
        context: EffectContext
    ): EffectResult {
        val evaluated = amountEvaluator.evaluate(state, effect.amount, context)
        if (evaluated <= 0) return EffectResult.success(state)

        // Cap the body count: this executor materializes one body per iteration *before* running
        // them, so an unbounded dynamic count (e.g. doubled into the billions) would OOM building
        // the list. Real "repeat N times" counts are tiny; only the pathological case is clamped.
        val count = evaluated.coerceAtMost(com.wingedsheep.engine.core.GameLimits.MAX_REPEAT_ITERATIONS)
        if (count < evaluated) {
            System.err.println(
                "RepeatDynamicTimesExecutor: requested $evaluated iterations exceeds " +
                    "${com.wingedsheep.engine.core.GameLimits.MAX_REPEAT_ITERATIONS} — clamping " +
                    "(likely a runaway dynamic amount)."
            )
        }

        val repeatedEffects = (1..count).map { effect.body }
        val compositeEffect = CompositeEffect(repeatedEffects)
        return effectExecutor(state, compositeEffect, context)
    }
}
