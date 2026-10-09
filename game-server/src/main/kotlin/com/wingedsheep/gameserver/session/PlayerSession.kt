package com.wingedsheep.gameserver.session

import com.wingedsheep.sdk.model.EntityId
import org.springframework.web.socket.WebSocketSession

/**
 * Represents a connected player's session.
 */
data class PlayerSession(
    val webSocketSession: WebSocketSession,
    val playerId: EntityId,
    val playerName: String,
    var currentGameSessionId: String? = null,
    /** The seat's avatar id — the signed-in account's, or an AI persona's preset; null for guests. Shown on the seat roster. */
    @Volatile var avatar: String? = null,
) {
    val sessionId: String get() = webSocketSession.id

    val isConnected: Boolean get() = webSocketSession.isOpen
}
