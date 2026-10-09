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
    playerName: String,
    val isAi: Boolean = false,
    /** LLM model override for AI players, null otherwise. Persisted alongside the lobby. */
    val aiModelOverride: String? = null
) {
    /**
     * Display name shown to opponents and spectators. For a signed-in player this is the account's
     * profile display name — the server overwrites it whenever the account is (re)linked (see
     * `ConnectionHandler.linkAccount`), so the name set on the profile is authoritative for games.
     * Guests keep the name they connected with.
     */
    @Volatile
    var playerName: String = playerName

    /** Account id this identity is signed in as (from magic-link auth), or null for guest play. */
    @Volatile
    var userId: UUID? = null

    /** The account's preset avatar id (refreshed with [playerName] on link), or null. */
    @Volatile
    var avatar: String? = null

    /**
     * Connecting client's IP, captured at handshake. Admin-only (used for a geolocation estimate);
     * never sent to clients. Null for AI and when unavailable.
     */
    @Volatile
    var clientIp: String? = null

    /** Current WebSocket session — swapped on reconnect */
    @Volatile
    var webSocketSession: WebSocketSession? = null

    /** Current game session ID the player is in */
    @Volatile
    var currentGameSessionId: String? = null

    /** Current lobby ID the player is in */
    @Volatile
    var currentLobbyId: String? = null

    /** Current quick-game lobby ID the player is in (separate from tournament lobbies). */
    @Volatile
    var currentQuickGameLobbyId: String? = null

    /** Current game session ID being spectated (if any) */
    @Volatile
    var currentSpectatingGameId: String? = null

    /** Scheduled disconnect cleanup task — cancelled on reconnect */
    @Volatile
    var disconnectTimer: ScheduledFuture<*>? = null

    /** Scheduled in-game disconnect timer — auto-concedes after 2 minutes */
    @Volatile
    var gameDisconnectTimer: ScheduledFuture<*>? = null

    /** Timestamp (epoch millis) when the disconnect timer expires */
    @Volatile
    var disconnectExpiresAt: Long? = null

    val isConnected: Boolean get() = isAi || webSocketSession?.isOpen == true

    /**
     * Create a legacy PlayerSession for compatibility with GameSession/SealedSession.
     */
    fun toPlayerSession(): PlayerSession {
        val ws = webSocketSession ?: throw IllegalStateException("No WebSocket session for player $playerName")
        return PlayerSession(
            webSocketSession = ws,
            playerId = playerId,
            playerName = playerName,
            currentGameSessionId = currentGameSessionId,
            avatar = avatar,
        )
    }
}
