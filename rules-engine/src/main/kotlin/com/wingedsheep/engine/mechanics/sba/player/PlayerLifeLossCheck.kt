package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.PlayerLostEvent
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * 704.5a - A player with 0 or less life loses the game.
 */
class PlayerLifeLossCheck : StateBasedActionCheck {
    override val name = "704.5a Player Life Loss"
    override val order = SbaOrder.PLAYER_LIFE_LOSS

    override fun check(state: GameState): ExecutionResult {
        if (state.gameOver) return ExecutionResult.success(state)

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for (playerId in state.turnOrder) {
            val container = state.getEntity(playerId) ?: continue
            if (container.has<PlayerLostComponent>()) continue
            if (playerCantLoseGame(state, playerId)) continue

            // Presence guard stays per-player; the value is the team's shared total (CR 810.9c).
            // Reading through the resolver means every member of a 0-life team is marked in this
            // same pass — the principled single team-loss check is Phase 3.
            if (container.get<LifeTotalComponent>() == null) continue
            if (state.lifeTotal(playerId) <= 0) {
                newState = newState.updateEntity(playerId) { c ->
                    c.with(PlayerLostComponent(LossReason.LIFE_ZERO))
                }
                events.add(PlayerLostEvent(playerId, GameEndReason.LIFE_ZERO))
            }
        }

        return ExecutionResult.success(newState, events)
    }
}

internal fun playerCantLoseGame(state: GameState, playerId: EntityId): Boolean {
    // CR 810.8a — "if an effect says a player can't lose the game, that player's team can't lose":
    // a can't-lose grant controlled by any teammate protects the whole team. This team-wide reach
    // only applies when players win/lose as a team (2HG); in Team vs. Team (CR 808) and non-team
    // games the grant protects only its own controller.
    val team = (if (state.format.playersWinLoseAsTeam) state.teamOf(playerId) else listOf(playerId))
        .toHashSet()
    return state.getBattlefield().any { entityId ->
        val container = state.getEntity(entityId) ?: return@any false
        container.has<GrantsCantLoseGameComponent>() &&
            container.get<ControllerComponent>()?.playerId in team
    }
}
