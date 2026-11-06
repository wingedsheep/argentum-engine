package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.AttemptedDrawFromEmptyLibraryComponent
import com.wingedsheep.engine.state.components.player.LossReason

/**
 * 704.5b - If a player attempted to draw a card from a library with no cards in it since the last
 * time state-based actions were checked, that player loses the game.
 *
 * The draw only records the attempt ([AttemptedDrawFromEmptyLibraryComponent], CR 121.4); the loss
 * lands here, so a win granted later in the same resolution (CR 104.2b) ends the game first. The
 * marker is consumed on every check — an attempt excused by a "can't lose the game" grant
 * (Platinum Angel) is spent, not carried to the next check after the grant goes away.
 */
class EmptyLibraryDrawLossCheck(
    private val predicateEvaluator: PredicateEvaluator
) : StateBasedActionCheck {
    override val name = "704.5b Empty Library Draw Loss"
    override val order = SbaOrder.EMPTY_LIBRARY_DRAW_LOSS

    override fun check(state: GameState): ExecutionResult {
        if (state.gameOver) return ExecutionResult.success(state)
        // A win effect earlier in the same resolution already left one team standing (CR 104.2b —
        // the game ends the moment a player wins; GameEndCheck records it at this settle point).
        // The game was over before this state-based action could apply.
        if (state.activeTeams.size <= 1) return ExecutionResult.success(state)

        var newState = state
        val events = mutableListOf<GameEvent>()

        for (playerId in state.turnOrder) {
            val container = state.getEntity(playerId) ?: continue
            if (!container.has<AttemptedDrawFromEmptyLibraryComponent>()) continue

            newState = newState.updateEntity(playerId) { it.without<AttemptedDrawFromEmptyLibraryComponent>() }
            val (lost, lossEvents) = GameLoss.lose(
                newState, playerId, LossReason.EMPTY_LIBRARY, GameEndReason.DECK_EMPTY, predicateEvaluator)
            newState = lost
            events.addAll(lossEvents)
        }

        return ExecutionResult.success(newState, events)
    }
}
