package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.PlayerLostEvent
import com.wingedsheep.engine.mechanics.ControllerGrants
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameFromLifeComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsOpponentsCantWinGameComponent
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
            // Narrow "don't lose for 0 or less life" (Marina Vendrell's Grimoire) — 704.5a only.
            if (playerCantLoseGameFromLife(state, playerId)) continue

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
    return ControllerGrants.anyGranting<GrantsCantLoseGameComponent>(state) { it in team }
}

/**
 * Narrow sibling of [playerCantLoseGame]: true when [playerId] — or, where players win and lose
 * as a team, a teammate — controls a permanent granting "you don't lose the game for having 0 or
 * less life" (Transcendence, Marina Vendrell's Grimoire). Consulted only by the 704.5a life-loss
 * check, so poison / empty-library / effect losses are unaffected.
 *
 * The team reach is CR 810.8a's own example: a Two-Headed Giant team at 0 or less life with
 * Transcendence on one head doesn't lose. The life total is the team's (810.4), so a grant that
 * excuses one head from the 0-life loss has to excuse the other — otherwise the unprotected head
 * is marked, TeamLossPropagationCheck drags the protected one down, and the grant did nothing.
 */
internal fun playerCantLoseGameFromLife(state: GameState, playerId: EntityId): Boolean {
    val team = (if (state.format.playersWinLoseAsTeam) state.teamOf(playerId) else listOf(playerId))
        .toHashSet()
    return ControllerGrants.anyGranting<GrantsCantLoseGameFromLifeComponent>(state) { it in team }
}

/**
 * True when [playerId] can't win the game because one of its opponents controls a permanent
 * granting "your opponents can't win the game" (Herald of Eternal Dawn). Consulted by the win
 * path so an effect that would make [playerId] win does nothing. [GameState.getOpponents] is
 * team-aware, so a teammate of [playerId] never counts as the source of this restriction.
 */
internal fun playerCantWinGame(state: GameState, playerId: EntityId): Boolean {
    val opponents = state.getOpponents(playerId).toHashSet()
    return ControllerGrants.anyGranting<GrantsOpponentsCantWinGameComponent>(state) { it in opponents }
}
