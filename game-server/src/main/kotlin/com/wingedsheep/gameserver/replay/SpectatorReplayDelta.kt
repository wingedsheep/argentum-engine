package com.wingedsheep.gameserver.replay

import com.wingedsheep.engine.view.StateDelta
import com.wingedsheep.gameserver.protocol.ServerMessage
import kotlinx.serialization.Serializable

/**
 * Delta representation of a SpectatorStateUpdate change.
 *
 * Only changed fields are populated. Null fields mean "unchanged from previous snapshot".
 * The [gameStateDelta] uses the existing [StateDelta] format for the heavy ClientGameState part.
 */
@Serializable
data class SpectatorReplayDelta(
    /** Delta for the ClientGameState (null if gameState unchanged or absent) */
    val gameStateDelta: StateDelta? = null,

    /** Player 1 spectator state (null if unchanged) */
    val player1: ServerMessage.SpectatorPlayerState? = null,

    /** Player 2 spectator state (null if unchanged) */
    val player2: ServerMessage.SpectatorPlayerState? = null,

    /** Current phase (null if unchanged) */
    val currentPhase: String? = null,

    /** Active player ID (null if unchanged). Use empty string to represent "set to null". */
    val activePlayerId: String? = null,

    /** Priority player ID (null if unchanged). Use empty string to represent "set to null". */
    val priorityPlayerId: String? = null,

    /** Combat state (null if unchanged). Use [combatCleared] to represent removal. */
    val combat: ServerMessage.SpectatorCombatState? = null,

    /** True if combat was cleared (set to null) */
    val combatCleared: Boolean? = null,

    /** Decision status (null if unchanged). Use [decisionCleared] to represent removal. */
    val decisionStatus: ServerMessage.SpectatorDecisionStatus? = null,

    /** True if decision status was cleared (set to null) */
    val decisionCleared: Boolean? = null,
)
