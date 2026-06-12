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
 * Concession is always valid. The winner is the sole remaining opponent — in a
 * multiplayer pod the game must instead continue without the conceding player
 * (CR 800.4a leave-the-game processing, backlog/multiplayer.md Phase 1.2).
 */
class ConcedeHandler : ActionHandler<Concede> {
    override val actionType: KClass<Concede> = Concede::class

    override fun validate(state: GameState, action: Concede): String? {
        // Concession is always valid
        return null
    }

    override fun execute(state: GameState, action: Concede): ExecutionResult {
        val winner = state.getOpponents(action.playerId).singleOrNull()
        return ExecutionResult.success(
            state.copy(gameOver = true, winnerId = winner),
            listOf(
                PlayerLostEvent(action.playerId, GameEndReason.CONCESSION),
                GameEndedEvent(winner, GameEndReason.CONCESSION)
            )
        )
    }
}
