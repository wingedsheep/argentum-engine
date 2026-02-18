package com.wingedsheep.gameserver.replay

import com.wingedsheep.gameserver.protocol.ServerMessage
import java.time.Instant

/**
 * A recorded game replay containing metadata and snapshots of spectator state updates.
 */
data class GameReplayRecord(
    val gameId: String,
    val player1Id: String,
    val player2Id: String,
    val player1Name: String,
    val player2Name: String,
    val startedAt: Instant,
    val endedAt: Instant,
    val winnerName: String?,
    val snapshots: List<ServerMessage.SpectatorStateUpdate>
)
