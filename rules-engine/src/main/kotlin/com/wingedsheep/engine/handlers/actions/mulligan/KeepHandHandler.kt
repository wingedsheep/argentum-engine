package com.wingedsheep.engine.handlers.actions.mulligan

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.handlers.MulliganHandler
import com.wingedsheep.engine.core.EngineServices
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
        val result = mulliganHandler.handleKeepHand(state, action)
        if (!result.isSuccess) return result
        return checkMulliganCompletion(result.newState, result.events)
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
        fun create(services: EngineServices): KeepHandHandler {
            return KeepHandHandler(services.mulliganHandler, services.turnManager)
        }
    }
}
