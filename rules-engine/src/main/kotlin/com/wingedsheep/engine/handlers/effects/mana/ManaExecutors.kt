package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.registry.CardRegistry

/**
 * Module providing all mana-related effect executors.
 */
class ManaExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val cardRegistry: CardRegistry,
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        AddManaExecutor(amountEvaluator),
        AddColorlessManaExecutor(amountEvaluator),
        AddAnyColorManaExecutor(amountEvaluator),
        AddAnyColorManaSpendOnChosenTypeExecutor(amountEvaluator),
        AddAnyColorManaSpendOnChosenTypeUncounterableExecutor(amountEvaluator),
        AddDynamicManaExecutor(amountEvaluator),
        AddManaOfChosenColorExecutor(amountEvaluator),
        AddManaOfColorAmongExecutor(),
        AddManaOfColorLandsCouldProduceExecutor(cardRegistry),
        AddOneManaOfEachColorAmongExecutor()
    )
}
