package com.wingedsheep.gameserver.session

import com.wingedsheep.rulesengine.ecs.EntityId
import org.springframework.web.socket.WebSocketSession

/**
 * Represents a connected player's session.
 */
data class PlayerSession(
    val webSocketSession: WebSocketSession,
    val playerId: EntityId,
    val playerName: String,
    var currentGameSessionId: String? = null
) {
    val sessionId: String get() = webSocketSession.id

    val isConnected: Boolean get() = webSocketSession.isOpen
}
