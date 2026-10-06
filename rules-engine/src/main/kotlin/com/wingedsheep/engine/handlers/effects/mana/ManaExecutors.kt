package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.registry.CardRegistry

/**
 * Module providing all mana-related effect executors.
 */
class ManaExecutors(
    private val amountEvaluator: DynamicAmountEvaluator,
    private val cardRegistry: CardRegistry,
    /** Providers: both are built from the whole engine graph, this module's registry included. */
    private val legalActionEnumerator: () -> com.wingedsheep.engine.legalactions.LegalActionEnumerator,
    private val activateAbilityHandler: () -> com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler,
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        AddManaExecutor(amountEvaluator),
        AddColorlessManaExecutor(amountEvaluator),
        AddManaOfChoiceExecutor(cardRegistry, amountEvaluator),
        AddAnyColorManaSpendOnChosenTypeExecutor(amountEvaluator),
        AddDynamicManaExecutor(amountEvaluator = amountEvaluator),
        AddOneManaOfEachColorAmongExecutor(predicateEvaluator = amountEvaluator.predicates),
        RetainUnspentManaExecutor(),
        LoseUnspentManaExecutor(cardRegistry),
        ActivateManaAbilityExecutor(legalActionEnumerator, activateAbilityHandler),
    )
}
