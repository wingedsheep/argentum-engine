package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all stack-related effect executors.
 */
class StackExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        CounterAbilityExecutor(),
        CounterSpellExecutor(),
        CounterTriggeringSpellExecutor(),
        CounterSpellWithFilterExecutor(),
        CounterUnlessPaysExecutor(),
        CounterUnlessDynamicPaysExecutor(amountEvaluator),
        ChangeSpellTargetExecutor(),
        ChangeTargetExecutor(),
        StormCopyEffectExecutor(),
        CopyTargetSpellExecutor()
    )
}
