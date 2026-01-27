package com.wingedsheep.gameserver.session

import com.wingedsheep.sdk.model.EntityId
import org.springframework.web.socket.WebSocketSession
import java.util.UUID
import java.util.concurrent.ScheduledFuture

/**
 * Stable player identity that survives WebSocket disconnects and reconnects.
 *
 * The token is a UUID that the client stores in sessionStorage. On reconnect,
 * the client sends the token to re-associate with their existing identity.
 */
class PlayerIdentity(
    val token: String = UUID.randomUUID().toString(),
    val playerId: EntityId,
    val playerName: String
) {
    /** Current WebSocket session — swapped on reconnect */
    @Volatile
    var webSocketSession: WebSocketSession? = null

    /** Current game session ID the player is in */
    @Volatile
    var currentGameSessionId: String? = null

    /** Current lobby ID the player is in */
    @Volatile
    var currentLobbyId: String? = null

    /** Scheduled disconnect cleanup task — cancelled on reconnect */
    @Volatile
    var disconnectTimer: ScheduledFuture<*>? = null

    val isConnected: Boolean get() = webSocketSession?.isOpen == true

    /**
     * Create a legacy PlayerSession for compatibility with GameSession/SealedSession.
     */
    fun toPlayerSession(): PlayerSession {
        val ws = webSocketSession ?: throw IllegalStateException("No WebSocket session for player $playerName")
        return PlayerSession(
            webSocketSession = ws,
            playerId = playerId,
            playerName = playerName,
            currentGameSessionId = currentGameSessionId
        )
    }
}
