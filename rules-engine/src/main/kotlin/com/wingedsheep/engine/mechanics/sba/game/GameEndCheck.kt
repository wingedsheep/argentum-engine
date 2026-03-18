package com.wingedsheep.engine.mechanics.sba.game

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.GameEndedEvent
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent

/**
 * Check if the game should end (one or zero players remaining).
 */
class GameEndCheck : StateBasedActionCheck {
    override val name = "Game End"
    override val order = SbaOrder.GAME_END

    override fun check(state: GameState): ExecutionResult {
        if (state.gameOver) return ExecutionResult.success(state)

        val activePlayers = state.turnOrder.filter { playerId ->
            val container = state.getEntity(playerId) ?: return@filter false
            !container.has<PlayerLostComponent>()
        }

        if (activePlayers.size == 1) {
            val winner = activePlayers.first()
            val losingPlayer = state.turnOrder.find { it != winner }
            val lossComponent = losingPlayer?.let { state.getEntity(it)?.get<PlayerLostComponent>() }
            val reason = when (lossComponent?.reason) {
                LossReason.LIFE_ZERO -> GameEndReason.LIFE_ZERO
                LossReason.POISON_COUNTERS -> GameEndReason.POISON_COUNTERS
                LossReason.EMPTY_LIBRARY -> GameEndReason.DECK_EMPTY
                LossReason.CONCESSION -> GameEndReason.CONCESSION
                LossReason.CARD_EFFECT -> GameEndReason.CARD_EFFECT
                null -> GameEndReason.UNKNOWN
            }
            return ExecutionResult.success(
                state.copy(gameOver = true, winnerId = winner),
                listOf(GameEndedEvent(winner, reason))
            )
        } else if (activePlayers.isEmpty()) {
            return ExecutionResult.success(
                state.copy(gameOver = true, winnerId = null),
                listOf(GameEndedEvent(null, GameEndReason.DRAW))
            )
        }

        return ExecutionResult.success(state)
    }
}
