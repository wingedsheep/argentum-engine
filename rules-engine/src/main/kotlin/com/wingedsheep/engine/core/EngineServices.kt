package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.ContinuationHandler
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.EffectHandler
import com.wingedsheep.engine.handlers.MulliganHandler
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry

/**
 * Composition root for the rules engine.
 *
 * Constructs and wires all engine services from a single [CardRegistry].
 * This eliminates duplicated wiring across ActionProcessor and GameSession,
 * and ensures all consumers share the same service instances.
 */
class EngineServices(
    val cardRegistry: CardRegistry
) {
    init {
        DamageUtils.cardRegistry = cardRegistry
    }
    val combatManager = CombatManager(cardRegistry)
    val effectExecutorRegistry = EffectExecutorRegistry(cardRegistry = cardRegistry)
    val triggerDetector = TriggerDetector(cardRegistry)
    val stackResolver = StackResolver(
        effectHandler = EffectHandler(cardRegistry = cardRegistry),
        cardRegistry = cardRegistry
    )
    val triggerProcessor = TriggerProcessor(cardRegistry = cardRegistry, stackResolver = stackResolver)
    val manaSolver = ManaSolver(cardRegistry)
    val costCalculator = CostCalculator(cardRegistry)
    val alternativePaymentHandler = AlternativePaymentHandler()
    val costHandler = CostHandler()
    val mulliganHandler = MulliganHandler()
    val conditionEvaluator = ConditionEvaluator()
    val targetValidator = TargetValidator()
    val targetFinder = TargetFinder()
    val predicateEvaluator = PredicateEvaluator()
    val sbaChecker = StateBasedActionChecker(cardRegistry = cardRegistry)
    val turnManager = TurnManager(
        cardRegistry = cardRegistry,
        combatManager = combatManager,
        sbaChecker = sbaChecker,
        effectExecutor = effectExecutorRegistry::execute
    )
    val continuationHandler = ContinuationHandler(this)
}
