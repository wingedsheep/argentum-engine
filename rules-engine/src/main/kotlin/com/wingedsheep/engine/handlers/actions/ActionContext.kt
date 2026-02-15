package com.wingedsheep.engine.handlers.actions

import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.ContinuationHandler
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.MulliganHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry

/**
 * Bundles all dependencies needed by action handlers.
 *
 * This context is passed to handler modules and individual handlers,
 * providing access to the game mechanics components they need.
 */
data class ActionContext(
    val cardRegistry: CardRegistry?,
    val combatManager: CombatManager,
    val turnManager: TurnManager,
    val stackResolver: StackResolver,
    val manaSolver: ManaSolver,
    val costCalculator: CostCalculator,
    val alternativePaymentHandler: AlternativePaymentHandler,
    val costHandler: CostHandler,
    val mulliganHandler: MulliganHandler,
    val effectExecutorRegistry: EffectExecutorRegistry,
    val continuationHandler: ContinuationHandler,
    val sbaChecker: StateBasedActionChecker,
    val triggerDetector: TriggerDetector,
    val triggerProcessor: TriggerProcessor,
    val conditionEvaluator: ConditionEvaluator,
    val targetValidator: TargetValidator,
    val stateProjector: StateProjector = StateProjector()
) {
    companion object {
        /**
         * Create an ActionContext with default values.
         * All dependencies are wired with the provided cardRegistry.
         */
        fun create(cardRegistry: CardRegistry? = null): ActionContext {
            val combatManager = CombatManager(cardRegistry)
            val effectExecutorRegistry = EffectExecutorRegistry(cardRegistry = cardRegistry)
            val triggerProcessor = TriggerProcessor()
            val triggerDetector = TriggerDetector(cardRegistry)
            return ActionContext(
                cardRegistry = cardRegistry,
                combatManager = combatManager,
                turnManager = TurnManager(combatManager, cardRegistry = cardRegistry),
                stackResolver = StackResolver(effectHandler = com.wingedsheep.engine.handlers.EffectHandler(cardRegistry = cardRegistry), cardRegistry = cardRegistry),
                manaSolver = ManaSolver(cardRegistry),
                costCalculator = CostCalculator(cardRegistry),
                alternativePaymentHandler = AlternativePaymentHandler(),
                costHandler = CostHandler(),
                mulliganHandler = MulliganHandler(),
                effectExecutorRegistry = effectExecutorRegistry,
                continuationHandler = ContinuationHandler(effectExecutorRegistry, triggerProcessor = triggerProcessor, triggerDetector = triggerDetector, combatManager = combatManager),
                sbaChecker = StateBasedActionChecker(),
                triggerDetector = triggerDetector,
                triggerProcessor = triggerProcessor,
                conditionEvaluator = ConditionEvaluator(),
                targetValidator = TargetValidator(),
                stateProjector = StateProjector()
            )
        }
    }
}
