package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.registry.CardRegistry

/**
 * Module providing all stack-related effect executors.
 */
class StackExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val cardRegistry: CardRegistry? = null
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        CounterAbilityExecutor(),
        CounterSpellExecutor(cardRegistry),
        CounterTriggeringSpellExecutor(cardRegistry),
        CounterSpellWithFilterExecutor(cardRegistry),
        CounterUnlessPaysExecutor(cardRegistry),
        CounterUnlessDynamicPaysExecutor(amountEvaluator, cardRegistry),
        ChangeSpellTargetExecutor(),
        ChangeTargetExecutor(),
        StormCopyEffectExecutor(),
        CopyTargetSpellExecutor(),
        ReselectTargetRandomlyExecutor()
    )
}
