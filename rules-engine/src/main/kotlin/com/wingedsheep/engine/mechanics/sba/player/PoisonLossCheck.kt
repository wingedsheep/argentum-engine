package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.PlayerLostEvent
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.CounterType

/**
 * 704.5b - A player with 10 or more poison counters loses the game.
 */
class PoisonLossCheck : StateBasedActionCheck {
    override val name = "704.5b Poison Loss"
    override val order = SbaOrder.POISON_LOSS

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for (playerId in state.turnOrder) {
            val container = state.getEntity(playerId) ?: continue
            if (container.has<PlayerLostComponent>()) continue
            if (playerCantLoseGame(state, playerId)) continue

            val counters = container.get<CountersComponent>() ?: continue
            val poisonCount = counters.getCount(CounterType.POISON)
            if (poisonCount >= 10) {
                newState = newState.updateEntity(playerId) { c ->
                    c.with(PlayerLostComponent(LossReason.POISON_COUNTERS))
                }
                events.add(PlayerLostEvent(playerId, GameEndReason.POISON_COUNTERS))
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
