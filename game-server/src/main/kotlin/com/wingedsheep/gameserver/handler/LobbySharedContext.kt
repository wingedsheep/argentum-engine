package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.ai.AiGameManager
import com.wingedsheep.gameserver.handler.ConnectionHandler.Companion.cardToSealedCardInfo
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.SessionRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

@Component
class LobbySharedContext(
    val sessionRegistry: SessionRegistry,
    val gameRepository: GameRepository,
    val lobbyRepository: LobbyRepository,
    val sender: MessageSender,
    val aiGameManager: AiGameManager
) {
    /** Coroutine scope for draft timers */
    val draftScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Per-lobby locks for round advancement to prevent concurrent ready/round-complete races */
    val roundLocks = java.util.concurrent.ConcurrentHashMap<String, Any>()

    /**
     * Remove lock entries for lobbies that no longer exist.
     * Called by [ZombieSessionSweeper] to prevent unbounded map growth.
     */
    fun sweepStaleLocks(activeLobbyIds: Set<String>) {
        roundLocks.keys.removeAll { it !in activeLobbyIds }
    }

    fun getIdentityAndLobby(session: WebSocketSession): Pair<PlayerIdentity, TournamentLobby>? {
        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return null
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby")
            return null
        }

        val lobby = lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found")
            return null
        }

        return identity to lobby
    }

    fun broadcastLobbyUpdate(lobby: TournamentLobby) {
        lobby.players.forEach { (playerId, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, lobby.buildLobbyUpdate(playerId, aiGameManager::isAiPlayer))
            }
        }
    }

    fun broadcastTimerUpdate(lobby: TournamentLobby, secondsRemaining: Int) {
        val message = ServerMessage.DraftTimerUpdate(secondsRemaining)

        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, message)
            }
        }
    }

    fun cleanUpSpectatingState(identity: PlayerIdentity) {
        val spectatingGameId = identity.currentSpectatingGameId ?: return
        val gameSession = gameRepository.findById(spectatingGameId)
        if (gameSession != null) {
            val playerSession = identity.webSocketSession?.let { sessionRegistry.getPlayerSession(it.id) }
            if (playerSession != null) {
                gameSession.removeSpectator(playerSession)
            }
        }
        identity.currentSpectatingGameId = null
    }
}
