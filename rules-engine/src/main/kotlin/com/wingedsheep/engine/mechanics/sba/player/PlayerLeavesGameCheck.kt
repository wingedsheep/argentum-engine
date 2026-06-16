package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLeftGameComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent

/**
 * CR 800.4a–c: when a player has lost the game, apply the "leaving the game" processing
 * (remove their owned objects, clear their stack objects, end control effects involving
 * them). Strictly speaking this isn't a state-based action — CR 800.4a says it happens the
 * instant the player leaves — but recording the loss already runs through the SBA loop, so
 * doing the cleanup here (ordered just before [com.wingedsheep.engine.mechanics.sba.game.GameEndCheck])
 * applies it at that same settle point, before the game-end check decides whether one
 * player remains.
 *
 * Each invocation processes the first lost-but-unprocessed player; the SBA loop re-runs
 * until every one carries [PlayerLeftGameComponent].
 */
class PlayerLeavesGameCheck : StateBasedActionCheck {
    override val name = "800.4 Leave the Game"
    override val order = SbaOrder.LEAVE_GAME

    override fun check(state: GameState): ExecutionResult {
        if (state.gameOver) return ExecutionResult.success(state)

        // When a loss leaves one or zero *teams* standing, the game is ending — the game-end SBA
        // settles it and there is nothing to keep playing for, so we don't tear down the losers'
        // boards. Leave-the-game processing only matters when the pod continues. In a non-team
        // game [activeTeams] mirrors the surviving players, keeping a two-player game's end-state
        // byte-identical to before; in 2HG it stops a wiped team's board being torn down mid-end.
        if (state.activeTeams.size <= 1) return ExecutionResult.success(state)

        val leaver = state.turnOrder.firstOrNull { id ->
            val container = state.getEntity(id) ?: return@firstOrNull false
            container.has<PlayerLostComponent>() && !container.has<PlayerLeftGameComponent>()
        } ?: return ExecutionResult.success(state)

        val reason = state.getEntity(leaver)
            ?.get<PlayerLostComponent>()?.reason
            .toGameEndReason()

        return PlayerLeavesGameProcessor.process(state, leaver, reason)
    }
}

internal fun LossReason?.toGameEndReason(): GameEndReason = when (this) {
    LossReason.LIFE_ZERO -> GameEndReason.LIFE_ZERO
    LossReason.POISON_COUNTERS -> GameEndReason.POISON_COUNTERS
    LossReason.EMPTY_LIBRARY -> GameEndReason.DECK_EMPTY
    LossReason.CONCESSION -> GameEndReason.CONCESSION
    LossReason.CARD_EFFECT -> GameEndReason.CARD_EFFECT
    LossReason.COMMANDER_DAMAGE -> GameEndReason.COMMANDER_DAMAGE
    LossReason.TEAM_DEFEATED -> GameEndReason.TEAM_DEFEATED
    null -> GameEndReason.UNKNOWN
}
