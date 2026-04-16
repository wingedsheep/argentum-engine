package com.wingedsheep.gameserver.replay

import com.wingedsheep.engine.view.StateDiffCalculator
import com.wingedsheep.gameserver.protocol.ServerMessage

/**
 * Computes a [SpectatorReplayDelta] representing the difference between two
 * consecutive [ServerMessage.SpectatorStateUpdate] snapshots.
 *
 * The heavy [ClientGameState] field is diffed using [StateDiffCalculator].
 * Other fields use simple equality checks with full replacement on change.
 */
object SpectatorReplayDiffCalculator {

    fun computeDelta(
        previous: ServerMessage.SpectatorStateUpdate,
        current: ServerMessage.SpectatorStateUpdate
    ): SpectatorReplayDelta {
        // Diff the ClientGameState using the existing calculator
        val gameStateDelta = when {
            previous.gameState == null && current.gameState == null -> null
            previous.gameState != null && current.gameState != null ->
                StateDiffCalculator.computeDelta(previous.gameState, current.gameState)
            // gameState appeared or disappeared — can't diff, handled specially by reconstruction
            else -> current.gameState?.let {
                StateDiffCalculator.computeDelta(
                    previous.gameState ?: current.gameState,
                    current.gameState
                )
            }
        }

        val player1 = if (previous.player1 != current.player1) current.player1 else null
        val player2 = if (previous.player2 != current.player2) current.player2 else null

        val currentPhase = if (previous.currentPhase != current.currentPhase) current.currentPhase else null
        val activePlayerId = if (previous.activePlayerId != current.activePlayerId) (current.activePlayerId ?: "") else null
        val priorityPlayerId = if (previous.priorityPlayerId != current.priorityPlayerId) (current.priorityPlayerId ?: "") else null

        val combatChanged = previous.combat != current.combat
        val combatCleared = combatChanged && current.combat == null
        val combat = if (combatChanged && current.combat != null) current.combat else null

        val decisionChanged = previous.decisionStatus != current.decisionStatus
        val decisionCleared = decisionChanged && current.decisionStatus == null
        val decisionStatus = if (decisionChanged && current.decisionStatus != null) current.decisionStatus else null

        return SpectatorReplayDelta(
            gameStateDelta = gameStateDelta,
            player1 = player1,
            player2 = player2,
            currentPhase = currentPhase,
            activePlayerId = activePlayerId,
            priorityPlayerId = priorityPlayerId,
            combat = combat,
            combatCleared = if (combatCleared) true else null,
            decisionStatus = decisionStatus,
            decisionCleared = if (decisionCleared) true else null,
        )
    }
}
