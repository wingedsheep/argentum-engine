package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.PlayerLostEvent
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent

/**
 * CR 810.8a (Two-Headed Giant) — players win and lose only as a team: if either player on a team
 * loses the game, the team loses. This propagation marks every still-in teammate of any lost player
 * as having lost too ([LossReason.TEAM_DEFEATED]).
 *
 * It runs after the individual loss checks (life, commander damage, poison) and after concession,
 * so the *cause* (e.g. one player decking out, taking lethal commander damage, or conceding —
 * CR 810.8b) is recorded on the originating player, and this check spreads the loss to the rest of
 * the team. The SBA loop re-runs until stable, so a loss recorded by any earlier check in the same
 * settle is propagated before [com.wingedsheep.engine.mechanics.sba.game.GameEndCheck] decides the
 * game.
 *
 * This propagation only applies when the format makes players win and lose as a team
 * ([com.wingedsheep.sdk.core.Format.playersWinLoseAsTeam] — 2HG). Under the normal multiplayer rules
 * a player is eliminated individually (CR 104.3b) and a team persists until all its members have left
 * (CR 104.2c), so in **Team vs. Team** (CR 808) and Free-for-All this is a no-op and a single player
 * can be knocked out while their teammates fight on. In a non-team game every player is a team of one,
 * so it never marks anyone regardless.
 */
class TeamLossPropagationCheck : StateBasedActionCheck {
    override val name = "810.8a Team Loss Propagation"
    override val order = SbaOrder.TEAM_LOSS_PROPAGATION

    override fun check(state: GameState): ExecutionResult {
        if (!state.format.playersWinLoseAsTeam) return ExecutionResult.success(state)

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for (playerId in state.turnOrder) {
            val lostTeammate = state.getEntity(playerId)?.has<PlayerLostComponent>() == true
            if (!lostTeammate) continue

            // This player is out — every teammate still in the game goes down with the team.
            for (teammate in state.teammatesOf(playerId)) {
                if (newState.getEntity(teammate)?.has<PlayerLostComponent>() == true) continue
                newState = newState.updateEntity(teammate) { c ->
                    c.with(PlayerLostComponent(LossReason.TEAM_DEFEATED))
                }
                events.add(PlayerLostEvent(teammate, GameEndReason.TEAM_DEFEATED))
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
