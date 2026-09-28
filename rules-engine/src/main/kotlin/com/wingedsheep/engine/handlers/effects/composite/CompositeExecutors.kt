package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Module providing composite effect executors.
 *
 * These executors run sub-effects through the parent registry's execute function, which the
 * registry hands in at construction.
 */
class CompositeExecutors(
    /** The registry's re-entrant entry point, for the executors that run sub-effects. */
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val targetFinder: TargetFinder,
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val amountEvaluator: DynamicAmountEvaluator,
    private val targetValidator: TargetValidator
) : ExecutorModule {
    private val compositeEffectExecutor = CompositeEffectExecutor(effectExecutor)
    private val createDelayedTriggerExecutor = CreateDelayedTriggerExecutor(dynamicAmountEvaluator = amountEvaluator)
    private val forEachExecutor = ForEachExecutor(effectExecutor, predicateEvaluator = amountEvaluator.predicates, cardRegistry = cardRegistry)
    private val forEachCapturedControllerExecutor = ForEachCapturedControllerExecutor(effectExecutor)
    private val mayRevealCardFromHandEffectExecutor = MayRevealCardFromHandEffectExecutor(effectExecutor, predicateEvaluator = amountEvaluator.predicates)
    private val beholdEffectExecutor = BeholdEffectExecutor(effectExecutor, predicateEvaluator = amountEvaluator.predicates)
    private val budgetModalEffectExecutor = BudgetModalEffectExecutor(effectExecutor)
    private val modalEffectExecutor = ModalEffectExecutor(effectExecutor, amountEvaluator = amountEvaluator, targetValidator = targetValidator)
    private val gatedEffectExecutor = GatedEffectExecutor(cardRegistry, effectExecutor, predicateEvaluator = amountEvaluator.predicates)
    private val payManaCostExecutor = PayManaCostExecutor(cardRegistry, predicateEvaluator = amountEvaluator.predicates)
    private val payDynamicManaCostExecutor = PayDynamicManaCostExecutor(cardRegistry, dynamicAmountEvaluator = amountEvaluator)
    private val payManaCostRepeatedlyExecutor = PayManaCostRepeatedlyExecutor(cardRegistry, decisionHandler, predicateEvaluator = amountEvaluator.predicates)
    private val reflexiveTriggerEffectExecutor = ReflexiveTriggerEffectExecutor(effectExecutor, targetFinder, decisionHandler, cardRegistry, amountEvaluator = amountEvaluator)
    private val flipCoinExecutor = FlipCoinExecutor(cardRegistry, effectExecutor, decisionHandler)
    private val repeatWhileExecutor = RepeatWhileExecutor(effectExecutor, conditionEvaluator = amountEvaluator.conditions)
    private val conditionalOnCollectionExecutor = ConditionalOnCollectionExecutor(effectExecutor, predicateEvaluator = amountEvaluator.predicates)
    private val flipTwoCoinsExecutor = FlipTwoCoinsExecutor(cardRegistry, effectExecutor, decisionHandler)
    private val flipCoinsExecutor = FlipCoinsExecutor(cardRegistry, decisionHandler)
    private val flipCoinsUntilLossExecutor = FlipCoinsUntilLossExecutor(cardRegistry, decisionHandler)
    private val chooseActionEffectExecutor = ChooseActionEffectExecutor(effectExecutor, predicateEvaluator = amountEvaluator.predicates)
    private val repeatDynamicTimesExecutor = RepeatDynamicTimesExecutor(effectExecutor, amountEvaluator = amountEvaluator)
    private val chooseNumberThenExecutor = ChooseNumberThenExecutor(decisionHandler)

    override fun executors(): List<EffectExecutor<*>> = listOf(
        budgetModalEffectExecutor,
        chooseActionEffectExecutor,
        compositeEffectExecutor,
        createDelayedTriggerExecutor,
        forEachExecutor,
        forEachCapturedControllerExecutor,
        mayRevealCardFromHandEffectExecutor,
        beholdEffectExecutor,
        modalEffectExecutor,
        gatedEffectExecutor,
        payManaCostExecutor,
        payDynamicManaCostExecutor,
        payManaCostRepeatedlyExecutor,
        reflexiveTriggerEffectExecutor,
        flipCoinExecutor,
        flipTwoCoinsExecutor,
        flipCoinsExecutor,
        flipCoinsUntilLossExecutor,
        repeatWhileExecutor,
        repeatDynamicTimesExecutor,
        conditionalOnCollectionExecutor,
        chooseNumberThenExecutor
    )
}
