package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PlayerLostEvent
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Format

/**
 * CR 704.5c — A player who's been dealt 21 or more combat damage by the same commander over the
 * course of the game loses the game.
 *
 * Threshold is read from `state.format` so non-Commander variants (Brawl, Oathbreaker, Pauper
 * Commander) drop in by config alone. The check is a no-op outside Commander shapes.
 */
class CommanderDamageLossCheck : StateBasedActionCheck {
    override val name = "704.5c Commander Damage Loss"
    override val order = SbaOrder.COMMANDER_DAMAGE_LOSS

    override fun check(state: GameState): ExecutionResult {
        if (state.gameOver) return ExecutionResult.success(state)
        val threshold = state.format.commanderDamageThreshold ?: return ExecutionResult.success(state)
        if (state.commanderDamage.isEmpty()) return ExecutionResult.success(state)

        var newState = state
        val events = mutableListOf<GameEvent>()

        // A player loses to commander damage if any single commander has dealt them >= threshold
        // cumulative combat damage. Aggregate per-defender to keep the loop O(entries).
        val byDefender: Map<com.wingedsheep.sdk.model.EntityId, List<Int>> =
            state.commanderDamage.groupBy({ it.defendingPlayerId }, { it.amount })

        for (playerId in state.turnOrder) {
            val container = state.getEntity(playerId) ?: continue
            if (container.has<PlayerLostComponent>()) continue
            if (playerCantLoseGame(state, playerId)) continue

            val perCommanderTallies = byDefender[playerId] ?: continue
            if (perCommanderTallies.any { it >= threshold }) {
                newState = newState.updateEntity(playerId) { c ->
                    c.with(PlayerLostComponent(LossReason.COMMANDER_DAMAGE))
                }
                events.add(PlayerLostEvent(playerId, GameEndReason.COMMANDER_DAMAGE))
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
