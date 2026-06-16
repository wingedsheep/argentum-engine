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

        // CR 104.2a / 810.8a — the game ends when one *team* remains. In a non-team game every
        // player is a team of one, so [activeTeams] mirrors the surviving players and this is the
        // ordinary "one player left" check.
        val activeTeams = state.activeTeams

        if (activeTeams.size == 1) {
            // The whole surviving team wins (CR 810.8a). winnerId records a representative — the
            // first surviving member; both members are winners (neither carries PlayerLostComponent).
            val winningTeam = activeTeams.first()
            val winner = winningTeam.first { state.getEntity(it)?.has<PlayerLostComponent>() != true }
            val reason = losingReason(state)
            return ExecutionResult.success(
                state.copy(gameOver = true, winnerId = winner),
                listOf(GameEndedEvent(winner, reason))
            )
        } else if (activeTeams.isEmpty()) {
            return ExecutionResult.success(
                state.copy(gameOver = true, winnerId = null),
                listOf(GameEndedEvent(null, GameEndReason.DRAW))
            )
        }

        return ExecutionResult.success(state)
    }

    /**
     * The game-end reason, drawn from a defeated player. Prefers a *meaningful* cause over the
     * propagated [LossReason.TEAM_DEFEATED] (CR 810.8a), so a team that lost because a member decked
     * out / hit lethal life reports that cause rather than "team defeated".
     */
    private fun losingReason(state: GameState): GameEndReason {
        val lostReasons = state.turnOrder
            .mapNotNull { state.getEntity(it)?.get<PlayerLostComponent>()?.reason }
        val reason = lostReasons.firstOrNull { it != LossReason.TEAM_DEFEATED }
            ?: lostReasons.firstOrNull()
        return when (reason) {
            LossReason.LIFE_ZERO -> GameEndReason.LIFE_ZERO
            LossReason.POISON_COUNTERS -> GameEndReason.POISON_COUNTERS
            LossReason.EMPTY_LIBRARY -> GameEndReason.DECK_EMPTY
            LossReason.CONCESSION -> GameEndReason.CONCESSION
            LossReason.CARD_EFFECT -> GameEndReason.CARD_EFFECT
            LossReason.COMMANDER_DAMAGE -> GameEndReason.COMMANDER_DAMAGE
            LossReason.TEAM_DEFEATED -> GameEndReason.TEAM_DEFEATED
            null -> GameEndReason.UNKNOWN
        }
    }
}
