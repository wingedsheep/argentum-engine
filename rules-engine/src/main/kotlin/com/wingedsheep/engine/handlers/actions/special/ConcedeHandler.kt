package com.wingedsheep.engine.handlers.actions.special

import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.GameEndedEvent
import com.wingedsheep.engine.core.PlayerLostEvent
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.state.GameState
import kotlin.reflect.KClass

/**
 * Handler for the Concede action.
 *
 * Concession is always valid and immediately ends the game with
 * the opponent as the winner.
 */
class ConcedeHandler : ActionHandler<Concede> {
    override val actionType: KClass<Concede> = Concede::class

    override fun validate(state: GameState, action: Concede): String? {
        // Concession is always valid
        return null
    }

    override fun execute(state: GameState, action: Concede): ExecutionResult {
        val opponent = state.getOpponent(action.playerId)
        return ExecutionResult.success(
            state.copy(gameOver = true, winnerId = opponent),
            listOf(
                PlayerLostEvent(action.playerId, GameEndReason.CONCESSION),
                GameEndedEvent(opponent, GameEndReason.CONCESSION)
            )
        )
    }
}
