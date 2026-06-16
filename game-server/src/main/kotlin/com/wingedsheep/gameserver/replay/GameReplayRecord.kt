package com.wingedsheep.gameserver.replay

import com.wingedsheep.gameserver.protocol.ServerMessage
import java.time.Instant

/**
 * A recorded game replay stored as an initial full snapshot plus a list of deltas.
 *
 * This is significantly more memory-efficient than storing full snapshots for every step,
 * since most of the game state (cards, zones) doesn't change between consecutive updates.
 */
/** A single seat in a recorded replay, in turn order. */
data class ReplayPlayerInfo(
    val playerId: String,
    val name: String,
)

data class GameReplayRecord(
    val gameId: String,
    /** Every seat in turn order. 2-player is the degenerate case (two entries). */
    val players: List<ReplayPlayerInfo>,
    val startedAt: Instant,
    val endedAt: Instant,
    val winnerName: String?,
    val tournamentName: String? = null,
    val tournamentRound: Int? = null,
    /** The first full spectator state snapshot */
    val initialSnapshot: ServerMessage.SpectatorStateUpdate,
    /** Deltas from each snapshot to the next */
    val deltas: List<SpectatorReplayDelta>,
    /**
     * Full (unmasked) game state per frame, in lockstep with [initialSnapshot] (frame 0) +
     * [deltas]. Used only by the explicit "share frame as scenario" endpoint so a shared
     * snapshot reproduces the EXACT position; never surfaced during normal masked replay viewing.
     */
    val fullStates: List<com.wingedsheep.engine.state.GameState> = emptyList(),
) {
    /** Total number of frames (initial + deltas) */
    val frameCount: Int get() = 1 + deltas.size
}
