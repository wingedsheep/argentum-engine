package com.wingedsheep.gameserver.persistence.dto

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.gameserver.dto.ClientEvent
import kotlinx.serialization.Serializable

/**
 * Persistent representation of a GameSession for Redis storage.
 * Excludes transient WebSocket references and reconstructable objects.
 */
@Serializable
data class PersistentGameSession(
    val sessionId: String,
    val gameState: GameState?,
    val deckLists: Map<String, List<String>>,  // playerId.value -> card names
    val lastProcessedMessageId: Map<String, String>,  // playerId.value -> messageId
    val gameLogs: Map<String, List<ClientEvent>>,  // playerId.value -> events
    val playerInfos: List<PersistentPlayerInfo>,
    val lobbyId: String?
)

/**
 * Persistent player info - contains only the data needed to restore a player's session.
 */
@Serializable
data class PersistentPlayerInfo(
    val playerId: String,
    val playerName: String,
    val token: String
)
