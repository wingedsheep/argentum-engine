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
    private val cardRegistry: CardRegistry
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        CounterEffectExecutor(amountEvaluator, cardRegistry),
        CounterAllOnStackExecutor(cardRegistry),
        WardCounterEffectExecutor(cardRegistry),
        ChangeSpellTargetExecutor(),
        ChangeTargetExecutor(),
        StormCopyEffectExecutor(cardRegistry),
        CopyTargetSpellExecutor(cardRegistry),
        CopyTargetTriggeredAbilityExecutor(cardRegistry),
        CopyNextSpellCastExecutor(),
        CopyEachSpellCastExecutor(),
        ReselectTargetRandomlyExecutor(),
        GrantKeywordToSpellExecutor()
    )
}
