package com.wingedsheep.gameserver.persistence.dto

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.view.ClientEvent
import com.wingedsheep.sdk.core.Zone
import kotlinx.serialization.Serializable

/**
 * Persistent representation of a GameSession for Redis storage.
 * Excludes transient WebSocket references and reconstructable objects.
 *
 * Deliberately *not* including the compact-replay recording, which it used to carry. Redis holds
 * live sessions with a TTL; replays are a durable artefact with a different lifetime and a different
 * consumer, and keeping a second copy here meant two stores that could disagree about what a game
 * was. The recording now lives only in [com.wingedsheep.gameserver.replay.ReplayStore], flushed
 * periodically while the game is in progress and resumed from there on restart — see
 * [com.wingedsheep.gameserver.replay.ReplayCheckpointFlusher].
 */
@Serializable
data class PersistentGameSession(
    val sessionId: String,
    val gameState: GameState?,
    val deckLists: Map<String, List<String>>,  // playerId.value -> card names
    val lastProcessedMessageId: Map<String, String>,  // playerId.value -> messageId
    val gameLogs: Map<String, List<ClientEvent>>,  // playerId.value -> events
    val playerInfos: List<PersistentPlayerInfo>,
    val lobbyId: String?,
    val sideboards: Map<String, List<String>> = emptyMap(),  // playerId.value -> sideboard card names
    val seatNames: Map<String, PersistentSeatNames> = emptyMap(),  // playerId.value -> that seat's card names
    val isScenario: Boolean = false,  // GameSession.isScenario — no replay, no stats
)

/**
 * One browser seat's card names ([com.wingedsheep.gameserver.session.SeatIdentities]). Kept across a
 * restart: a seat that came back with none would see a renamed card under its engine id again, which
 * its game log may already tie to the card.
 */
@Serializable
data class PersistentSeatNames(
    val names: Map<String, String>,  // engine id -> the seat's name for it
    val retired: Set<String>,
    val lastSeen: Map<String, PersistentSighting>,  // engine id -> where the seat last saw it
    val namesIssued: Int,
)

@Serializable
data class PersistentSighting(val zone: Zone, val zoneOpen: Boolean, val seen: Boolean = true)

/**
 * Persistent player info - contains only the data needed to restore a player's session.
 */
@Serializable
data class PersistentPlayerInfo(
    val playerId: String,
    val playerName: String,
    val token: String,
    val isAi: Boolean = false,
    val aiModelOverride: String? = null
)
