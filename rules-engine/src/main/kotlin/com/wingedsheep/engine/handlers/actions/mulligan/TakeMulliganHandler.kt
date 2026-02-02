package com.wingedsheep.engine.handlers.actions.mulligan

import com.wingedsheep.engine.core.EngineResult
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.TakeMulligan
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.handlers.MulliganHandler
import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import kotlin.reflect.KClass

/**
 * Handler for the TakeMulligan action.
 *
 * Players can mulligan during the mulligan phase, shuffling their hand
 * back into their library and drawing one fewer card.
 */
class TakeMulliganHandler(
    private val mulliganHandler: MulliganHandler,
    private val turnManager: TurnManager
) : ActionHandler<TakeMulligan> {
    override val actionType: KClass<TakeMulligan> = TakeMulligan::class

    override fun validate(state: GameState, action: TakeMulligan): String? {
        val mullState = state.getEntity(action.playerId)?.get<MulliganStateComponent>()
            ?: return "Player mulligan state not found"

        if (mullState.hasKept) {
            return "You have already kept your hand"
        }

        if (!mullState.canMulligan) {
            return "Cannot mulligan - hand size would be 0"
        }

        return null
    }

    override fun execute(state: GameState, action: TakeMulligan): ExecutionResult {
        return when (val result = mulliganHandler.handleTakeMulligan(state, action)) {
            is EngineResult.Success -> checkMulliganCompletion(result.newState, result.events)
            is EngineResult.Failure -> ExecutionResult.error(result.originalState, result.message)
            is EngineResult.PausedForDecision -> ExecutionResult.paused(result.partialState, result.decision, result.events)
            is EngineResult.GameOver -> ExecutionResult.success(result.finalState.copy(gameOver = true, winnerId = result.winnerId), result.events)
        }
    }

    /**
     * Check if all mulligans are complete and advance the game to the first turn if so.
     */
    private fun checkMulliganCompletion(state: GameState, events: List<com.wingedsheep.engine.core.GameEvent>): ExecutionResult {
        // If still in mulligan phase or someone needs to bottom cards, just return success
        if (mulliganHandler.isInMulliganPhase(state) || mulliganHandler.needsBottomCards(state)) {
            return ExecutionResult.success(state, events)
        }

        // All mulligans complete - start the first turn
        val advanceResult = turnManager.advanceStep(state)
        return ExecutionResult.success(
            advanceResult.newState,
            events + advanceResult.events
        )
    }

    companion object {
        fun create(context: ActionContext): TakeMulliganHandler {
            return TakeMulliganHandler(context.mulliganHandler, context.turnManager)
        }
    }
}
