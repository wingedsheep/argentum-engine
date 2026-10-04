package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.actions.ActionHandlerRegistry
import com.wingedsheep.engine.handlers.actions.ability.AbilityModule
import com.wingedsheep.engine.handlers.actions.combat.CombatModule
import com.wingedsheep.engine.handlers.actions.decision.DecisionModule
import com.wingedsheep.engine.handlers.actions.land.LandModule
import com.wingedsheep.engine.handlers.actions.morph.MorphModule
import com.wingedsheep.engine.handlers.actions.mulligan.MulliganModule
import com.wingedsheep.engine.handlers.actions.priority.PriorityModule
import com.wingedsheep.engine.handlers.actions.room.RoomModule
import com.wingedsheep.engine.handlers.actions.special.SpecialActionsModule
import com.wingedsheep.engine.handlers.actions.spell.SpellModule
import com.wingedsheep.engine.mechanics.SplitSecond
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.core.UndoPolicyComputer

/**
 * Wraps the result of [ActionProcessor.process] with an undo checkpoint policy.
 *
 * The engine computes the policy; the server follows it mechanically.
 */
data class ProcessedAction(
    val result: ExecutionResult,
    val undoPolicy: UndoCheckpointAction = UndoCheckpointAction.CLEAR
)

/**
 * The central action processor for the game engine.
 *
 * This is the main entry point for all game actions. It validates actions,
 * executes them against the game state, and returns the result.
 *
 * The processor is stateless - it's a pure function:
 * (GameState, GameAction) -> ProcessedAction(ExecutionResult, UndoCheckpointAction)
 *
 * Action handling is delegated to specialized handlers registered in the
 * ActionHandlerRegistry. This class serves as a thin facade that:
 * 1. Performs basic validation (game not over, player exists)
 * 2. Delegates to the appropriate handler via the registry
 */
class ActionProcessor(
    private val services: EngineServices,
    private val computeUndo: Boolean = true
) {
    /**
     * Backward-compatible constructor: wraps a CardRegistry in EngineServices.
     */
    constructor(cardRegistry: CardRegistry) : this(EngineServices(cardRegistry))

    /**
     * Registry that maps action types to their handlers.
     */
    private val registry = ActionHandlerRegistry().apply {
        registerModule(SpecialActionsModule(services))
        registerModule(PriorityModule(services))
        registerModule(LandModule(services))
        registerModule(MulliganModule(services))
        registerModule(CombatModule(services))
        registerModule(AbilityModule(services))
        registerModule(MorphModule(services))
        registerModule(RoomModule(services))
        registerModule(SpellModule(services))
        registerModule(DecisionModule(services))
    }

    /**
     * Process a game action and return the result.
     *
     * @param state The current game state
     * @param action The action to process
     * @return ExecutionResult with new state, events, and its [Outcome]
     */
    fun process(state: GameState, action: GameAction): ProcessedAction {
        val validationError = validate(state, action)
        if (validationError != null) {
            return ProcessedAction(ExecutionResult.rejected(state, Rejection.IllegalAction(validationError)))
        }

        // Handlers never detect triggers or check state-based actions themselves. The one settle
        // boundary does that for every action, paused or not (CR 117.5, 603.3).
        val executed = services.settler.settle(registry.execute(ControlHistory.initialize(state), action))

        // Action handlers may compose several immutable intermediate states before a nested
        // handler or resumed continuation rejects a later step. The public action contract is
        // atomic on error: retain only the rejection and hand back the entry state itself. A
        // rejected attempt therefore skips event-driven post-action bookkeeping entirely — its
        // events describe work that is being thrown away and must not reach the tracker. The
        // typed reason survives: validation refusals above are IllegalAction, and anything
        // execution rejects keeps its own reason.
        val outcome = executed.outcome
        val result = if (outcome is Outcome.Rejected) {
            ExecutionResult.rejected(state, outcome.reason)
        } else {
            // Cards revealed into hand or bounced back to hand stay visible until a same-named
            // card is played — see [RevealedInHandTracker]. Paused actions are accepted in-flight
            // actions, so they retain this existing bookkeeping just like completed successes.
            com.wingedsheep.engine.mechanics.RevealedInHandTracker.applyAfterAction(executed)
        }
        val undoPolicy = if (computeUndo) {
            UndoPolicyComputer.compute(action, state, result, services.cardRegistry)
        } else {
            UndoCheckpointAction.CLEAR
        }
        return ProcessedAction(result, undoPolicy)
    }

    /**
     * The verdict [process] gives [action] before executing it: `null` when it is legal, otherwise
     * why not. The legal-action enumerators must never offer a fully-specified action this refuses —
     * `LegalActionsPassValidateTest` holds them to it.
     */
    fun validate(state: GameState, action: GameAction): String? =
        // Basic validation that applies to all actions, then the handler's own.
        validateBasics(state, action) ?: registry.validate(state, action)

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

        // Nobody has priority before the first turn (CR 103.8): until every player has kept, only
        // mulligan decisions are made. A restarted game (CR 727) is back at that point.
        if (action !is TakeMulligan && action !is KeepHand && action !is BottomCards && action !is Concede &&
            state.turnOrder.any { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.player.MulliganStateComponent>()?.hasKept == false }
        ) {
            return "Opening hands are still being decided"
        }

        if (state.continuationStack.any { it is FinishForcedPlayContinuation } && state.pendingDecision != null &&
            action !is SubmitDecision && action !is Concede &&
            !(action is ActivateAbility && com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow.openFor(state, state.actorFor(action.playerId)) != null)) {
            return "Answer the current casting decision"
        }

        // Split second (CR 702.61): no spells, no non-mana activated abilities. An ActivateAbility
        // is decided by ActivationValidator, the only place that knows whether it's a mana ability.
        if (action !is ActivateAbility && SplitSecond.forbids(action) &&
            SplitSecond.isLocked(state, services.cardRegistry)
        ) {
            return SplitSecond.REJECTION
        }

        return null
    }
}
