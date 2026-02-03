package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.ContinuationHandler
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.MulliganHandler
import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandlerRegistry
import com.wingedsheep.engine.handlers.actions.ability.AbilityModule
import com.wingedsheep.engine.handlers.actions.combat.CombatModule
import com.wingedsheep.engine.handlers.actions.decision.DecisionModule
import com.wingedsheep.engine.handlers.actions.land.LandModule
import com.wingedsheep.engine.handlers.actions.morph.MorphModule
import com.wingedsheep.engine.handlers.actions.mulligan.MulliganModule
import com.wingedsheep.engine.handlers.actions.priority.PriorityModule
import com.wingedsheep.engine.handlers.actions.special.SpecialActionsModule
import com.wingedsheep.engine.handlers.actions.spell.SpellModule
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState

/**
 * The central action processor for the game engine.
 *
 * This is the main entry point for all game actions. It validates actions,
 * executes them against the game state, and returns the result.
 *
 * The processor is stateless - it's a pure function:
 * (GameState, GameAction) -> ExecutionResult(GameState, Events)
 *
 * Action handling is delegated to specialized handlers registered in the
 * ActionHandlerRegistry. This class serves as a thin facade that:
 * 1. Performs basic validation (game not over, player exists)
 * 2. Delegates to the appropriate handler via the registry
 */
class ActionProcessor(
    cardRegistry: CardRegistry? = null,
    combatManager: CombatManager = CombatManager(cardRegistry),
    turnManager: TurnManager = TurnManager(combatManager),
    stackResolver: StackResolver = StackResolver(cardRegistry = cardRegistry),
    manaSolver: ManaSolver = ManaSolver(cardRegistry),
    costCalculator: CostCalculator = CostCalculator(cardRegistry),
    alternativePaymentHandler: AlternativePaymentHandler = AlternativePaymentHandler(),
    costHandler: CostHandler = CostHandler(),
    mulliganHandler: MulliganHandler = MulliganHandler(),
    effectExecutorRegistry: EffectExecutorRegistry = EffectExecutorRegistry(),
    continuationHandler: ContinuationHandler = ContinuationHandler(effectExecutorRegistry),
    sbaChecker: StateBasedActionChecker = StateBasedActionChecker(),
    triggerDetector: TriggerDetector = TriggerDetector(cardRegistry),
    triggerProcessor: TriggerProcessor = TriggerProcessor(),
    conditionEvaluator: ConditionEvaluator = ConditionEvaluator(),
    targetValidator: TargetValidator = TargetValidator()
) {
    /**
     * Context containing all dependencies needed by action handlers.
     */
    private val context = ActionContext(
        cardRegistry = cardRegistry,
        combatManager = combatManager,
        turnManager = turnManager,
        stackResolver = stackResolver,
        manaSolver = manaSolver,
        costCalculator = costCalculator,
        alternativePaymentHandler = alternativePaymentHandler,
        costHandler = costHandler,
        mulliganHandler = mulliganHandler,
        effectExecutorRegistry = effectExecutorRegistry,
        continuationHandler = continuationHandler,
        sbaChecker = sbaChecker,
        triggerDetector = triggerDetector,
        triggerProcessor = triggerProcessor,
        conditionEvaluator = conditionEvaluator,
        targetValidator = targetValidator
    )

    /**
     * Registry that maps action types to their handlers.
     */
    private val registry = ActionHandlerRegistry().apply {
        registerModule(SpecialActionsModule())
        registerModule(PriorityModule(context))
        registerModule(LandModule(context))
        registerModule(MulliganModule(context))
        registerModule(CombatModule(context))
        registerModule(AbilityModule(context))
        registerModule(MorphModule(context))
        registerModule(SpellModule(context))
        registerModule(DecisionModule(context))
    }

    /**
     * Process a game action and return the result.
     *
     * @param state The current game state
     * @param action The action to process
     * @return ExecutionResult with new state, events, and any error or pending decision
     */
    fun process(state: GameState, action: GameAction): ExecutionResult {
        // Basic validation that applies to all actions
        val basicError = validateBasics(state, action)
        if (basicError != null) {
            return ExecutionResult.error(state, basicError)
        }

        // Delegate to the handler registry for action-specific validation
        val validationError = registry.validate(state, action)
        if (validationError != null) {
            return ExecutionResult.error(state, validationError)
        }

        // Execute the action
        return registry.execute(state, action)
    }

    /**
     * Basic validation that applies to all actions.
     */
    private fun validateBasics(state: GameState, action: GameAction): String? {
        // Check game is not over
        if (state.gameOver) {
            return "Game is already over"
        }

        // Check player exists
        if (!state.turnOrder.contains(action.playerId)) {
            return "Unknown player: ${action.playerId}"
        }

        return null
    }
}
