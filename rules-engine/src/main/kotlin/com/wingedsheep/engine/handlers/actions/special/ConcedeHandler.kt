package com.wingedsheep.engine.handlers.actions.special

import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.PlayerLostEvent
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import kotlin.reflect.KClass

/**
 * Handler for the Concede action.
 *
 * Conceding is always legal (CR 104.3a — a player can concede at any time). It marks the
 * player as having lost; the state-based action loop then does the rest:
 * [com.wingedsheep.engine.mechanics.sba.player.PlayerLeavesGameCheck] removes the
 * conceding player's objects (CR 800.4a) and [com.wingedsheep.engine.mechanics.sba.game.GameEndCheck]
 * ends the game only when one player remains. So in a two-player game conceding ends the
 * game immediately, while in a multiplayer pod it continues for the others.
 */
class ConcedeHandler(
    private val sbaChecker: StateBasedActionChecker
) : ActionHandler<Concede> {
    override val actionType: KClass<Concede> = Concede::class

    override fun validate(state: GameState, action: Concede): String? = null

    override fun execute(state: GameState, action: Concede): ExecutionResult {
        // Idempotent: a player already out of the game can't concede again.
        if (state.getEntity(action.playerId)?.has<PlayerLostComponent>() == true) {
            return ExecutionResult.success(state)
        }

        val marked = state.updateEntity(action.playerId) { container ->
            container.with(PlayerLostComponent(LossReason.CONCESSION))
        }
        val lostEvent = PlayerLostEvent(action.playerId, GameEndReason.CONCESSION)

        val sbaResult = sbaChecker.checkAndApply(marked)
        if (sbaResult.isPaused) {
            return ExecutionResult.paused(
                sbaResult.state,
                sbaResult.pendingDecision!!,
                listOf(lostEvent) + sbaResult.events
            )
        }
        return ExecutionResult.success(
            sbaResult.newState,
            listOf(lostEvent) + sbaResult.events
        )
    }
}
