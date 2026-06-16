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
 * 704.5c - A player with 10 or more poison counters loses the game.
 *
 * In Two-Headed Giant the threshold is 15 and poison is pooled by the team (CR 810.8d / 810.10): a
 * team loses when its members' combined poison reaches 15. Both values come from the format, so
 * non-2HG games keep the plain per-player 10.
 */
class PoisonLossCheck : StateBasedActionCheck {
    override val name = "704.5c Poison Loss"
    override val order = SbaOrder.POISON_LOSS

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        val threshold = (state.format as? com.wingedsheep.sdk.core.Format.TwoHeadedGiant)?.poisonThreshold ?: 10

        for (playerId in state.turnOrder) {
            val container = state.getEntity(playerId) ?: continue
            if (container.has<PlayerLostComponent>()) continue
            if (playerCantLoseGame(state, playerId)) continue

            // CR 810.10a — an individual player's poison count uses the team's pooled total.
            if (state.teamPoison(playerId) >= threshold) {
                newState = newState.updateEntity(playerId) { c ->
                    c.with(PlayerLostComponent(LossReason.POISON_COUNTERS))
                }
                events.add(PlayerLostEvent(playerId, GameEndReason.POISON_COUNTERS))
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
