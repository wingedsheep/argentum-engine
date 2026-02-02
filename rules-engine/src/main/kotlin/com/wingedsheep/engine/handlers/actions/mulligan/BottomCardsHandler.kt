package com.wingedsheep.engine.handlers.actions.mulligan

import com.wingedsheep.engine.core.BottomCards
import com.wingedsheep.engine.core.EngineResult
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.handlers.MulliganHandler
import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import kotlin.reflect.KClass

/**
 * Handler for the BottomCards action.
 *
 * After keeping a mulligan hand, players must put a number of cards
 * equal to their mulligans on the bottom of their library (London mulligan).
 */
class BottomCardsHandler(
    private val mulliganHandler: MulliganHandler,
    private val turnManager: TurnManager
) : ActionHandler<BottomCards> {
    override val actionType: KClass<BottomCards> = BottomCards::class

    override fun validate(state: GameState, action: BottomCards): String? {
        val mullState = state.getEntity(action.playerId)?.get<MulliganStateComponent>()
            ?: return "Player mulligan state not found"

        if (!mullState.hasKept) {
            return "You have not kept your hand yet"
        }

        if (action.cardIds.size != mullState.cardsToBottom) {
            return "Must put exactly ${mullState.cardsToBottom} cards on bottom, got ${action.cardIds.size}"
        }

        // Validate cards are in hand
        val hand = state.getHand(action.playerId).toSet()
        val invalidCards = action.cardIds.filter { it !in hand }
        if (invalidCards.isNotEmpty()) {
            return "Cards not in hand: $invalidCards"
        }

        return null
    }

    override fun execute(state: GameState, action: BottomCards): ExecutionResult {
        return when (val result = mulliganHandler.handleBottomCards(state, action)) {
            is EngineResult.Success -> checkMulliganCompletion(result.newState, result.events)
            is EngineResult.Failure -> ExecutionResult.error(result.originalState, result.message)
            is EngineResult.PausedForDecision -> ExecutionResult.paused(result.partialState, result.decision, result.events)
            is EngineResult.GameOver -> ExecutionResult.success(result.finalState.copy(gameOver = true, winnerId = result.winnerId), result.events)
        }
    }

    /**
     * Check if all mulligans are complete and advance the game to the first turn if so.
     */
    private fun checkMulliganCompletion(state: GameState, events: List<GameEvent>): ExecutionResult {
        if (mulliganHandler.isInMulliganPhase(state) || mulliganHandler.needsBottomCards(state)) {
            return ExecutionResult.success(state, events)
        }

        val advanceResult = turnManager.advanceStep(state)
        return ExecutionResult.success(
            advanceResult.newState,
            events + advanceResult.events
        )
    }

    companion object {
        fun create(context: ActionContext): BottomCardsHandler {
            return BottomCardsHandler(context.mulliganHandler, context.turnManager)
        }
    }
}
