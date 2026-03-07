package com.wingedsheep.gameserver.replay

import com.wingedsheep.gameserver.protocol.ServerMessage
import java.time.Instant

/**
 * A recorded game replay stored as an initial full snapshot plus a list of deltas.
 *
 * This is significantly more memory-efficient than storing full snapshots for every step,
 * since most of the game state (cards, zones) doesn't change between consecutive updates.
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
    val tournamentName: String? = null,
    val tournamentRound: Int? = null,
    /** The first full spectator state snapshot */
    val initialSnapshot: ServerMessage.SpectatorStateUpdate,
    /** Deltas from each snapshot to the next */
    val deltas: List<SpectatorReplayDelta>,
) {
    /** Total number of frames (initial + deltas) */
    val frameCount: Int get() = 1 + deltas.size
}
