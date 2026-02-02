package com.wingedsheep.engine.handlers.actions.mulligan

import com.wingedsheep.engine.core.EngineResult
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.handlers.MulliganHandler
import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import kotlin.reflect.KClass

/**
 * Handler for the KeepHand action.
 *
 * Players can keep their current hand during the mulligan phase.
 * After all players have kept, those who took mulligans must put
 * cards on the bottom of their library.
 */
class KeepHandHandler(
    private val mulliganHandler: MulliganHandler,
    private val turnManager: TurnManager
) : ActionHandler<KeepHand> {
    override val actionType: KClass<KeepHand> = KeepHand::class

    override fun validate(state: GameState, action: KeepHand): String? {
        val mullState = state.getEntity(action.playerId)?.get<MulliganStateComponent>()
            ?: return "Player mulligan state not found"

        if (mullState.hasKept) {
            return "You have already kept your hand"
        }

        return null
    }

    override fun execute(state: GameState, action: KeepHand): ExecutionResult {
        return when (val result = mulliganHandler.handleKeepHand(state, action)) {
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
        fun create(context: ActionContext): KeepHandHandler {
            return KeepHandHandler(context.mulliganHandler, context.turnManager)
        }
    }
}
