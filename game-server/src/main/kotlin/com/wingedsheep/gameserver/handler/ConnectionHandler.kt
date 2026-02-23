package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.TimeUnit

@Component
class ConnectionHandler(
    private val sessionRegistry: SessionRegistry,
    private val gameRepository: GameRepository,
    private val lobbyRepository: LobbyRepository,
    private val sender: MessageSender
) {
    private val logger = LoggerFactory.getLogger(ConnectionHandler::class.java)

    fun handleConnect(session: WebSocketSession, message: ClientMessage.Connect) {
        if (sessionRegistry.getPlayerSession(session.id) != null) {
            sender.sendError(session, ErrorCode.ALREADY_CONNECTED, "Already connected")
            return
        }

        val token = message.token
        logger.info("Connect request from ${message.playerName}, token: ${token?.take(8) ?: "none"}...")
        if (token != null) {
            val existingIdentity = sessionRegistry.getIdentityByToken(token)
            logger.info("Token lookup result: ${if (existingIdentity != null) "found identity for ${existingIdentity.playerName}" else "no identity found"}")
            if (existingIdentity != null) {
                handleReconnect(session, existingIdentity)
                return
            }
        }

        val playerId = EntityId.generate()
        val identity = PlayerIdentity(
            playerId = playerId,
            playerName = message.playerName
        )

        val playerSession = PlayerSession(
            webSocketSession = session,
            playerId = playerId,
            playerName = message.playerName
        )

        sessionRegistry.register(identity, session, playerSession)

        logger.info("Player connected: ${message.playerName} (${playerId.value}), token: ${identity.token}")
        sender.send(session, ServerMessage.Connected(playerId.value, identity.token))
    }

    private fun handleReconnect(session: WebSocketSession, identity: PlayerIdentity) {
        logger.info("Player reconnecting: ${identity.playerName} (${identity.playerId.value})")

        identity.disconnectTimer?.cancel(false)
        identity.disconnectTimer = null
        val wasDisconnectedFromTournament = identity.disconnectExpiresAt != null
        identity.disconnectExpiresAt = null

        // Broadcast reconnection to tournament lobby
        if (wasDisconnectedFromTournament) {
            val lobbyId = identity.currentLobbyId
            if (lobbyId != null) {
                val lobby = lobbyRepository.findLobbyById(lobbyId)
                if (lobby != null) {
                    val msg = ServerMessage.TournamentPlayerReconnected(
                        playerId = identity.playerId.value,
                        playerName = identity.playerName
                    )
                    lobby.players.forEach { (_, playerState) ->
                        val ws = playerState.identity.webSocketSession
                        if (ws != null && ws.isOpen) sender.send(ws, msg)
                    }
                    for ((_, spectatorIdentity) in lobby.spectators) {
                        val ws = spectatorIdentity.webSocketSession
                        if (ws != null && ws.isOpen) sender.send(ws, msg)
                    }
                }
            }
        }

        // Cancel in-game disconnect timer and notify opponent
        if (identity.gameDisconnectTimer != null) {
            identity.gameDisconnectTimer?.cancel(false)
            identity.gameDisconnectTimer = null

            val gameSessionId = identity.currentGameSessionId
            if (gameSessionId != null) {
                val gameSession = gameRepository.findById(gameSessionId)
                if (gameSession != null) {
                    val opponentId = gameSession.getOpponentId(identity.playerId)
                    val opponentSession = if (opponentId != null) gameSession.getPlayerSession(opponentId) else null
                    if (opponentSession?.isConnected == true) {
                        sender.send(opponentSession.webSocketSession, ServerMessage.OpponentReconnected)
                    }
                }
            }
        }

        sessionRegistry.removeOldWsMapping(identity.token)

        identity.webSocketSession = session
        sessionRegistry.mapWsToToken(session.id, identity.token)

        val playerSession = PlayerSession(
            webSocketSession = session,
            playerId = identity.playerId,
            playerName = identity.playerName,
            currentGameSessionId = identity.currentGameSessionId
        )
        sessionRegistry.setPlayerSession(session.id, playerSession)

        val context: String?
        val contextId: String?
        val lobbyId = identity.currentLobbyId
        val gameSessionId = identity.currentGameSessionId

        when {
            lobbyId != null -> {
                val lobby = lobbyRepository.findLobbyById(lobbyId)
                when {
                    lobby == null -> { context = null; contextId = null }
                    lobby.state == LobbyState.WAITING_FOR_PLAYERS -> { context = "lobby"; contextId = lobbyId }
                    lobby.state == LobbyState.DRAFTING -> { context = "drafting"; contextId = lobbyId }
                    lobby.state == LobbyState.DECK_BUILDING -> { context = "deckBuilding"; contextId = lobbyId }
                    lobby.state == LobbyState.TOURNAMENT_ACTIVE -> { context = "tournament"; contextId = lobbyId }
                    lobby.state == LobbyState.TOURNAMENT_COMPLETE -> { context = "tournament"; contextId = lobbyId }
                    else -> { context = null; contextId = null }
                }
            }
            gameSessionId != null && gameRepository.findById(gameSessionId) != null -> {
                context = "game"; contextId = gameSessionId
            }
            else -> { context = null; contextId = null }
        }

        logger.info("Sending Reconnected message: context=$context, contextId=$contextId, gameSessionId=${identity.currentGameSessionId}")
        sender.send(session, ServerMessage.Reconnected(
            playerId = identity.playerId.value,
            token = identity.token,
            context = context,
            contextId = contextId
        ))

        when (context) {
            "lobby", "drafting", "deckBuilding", "tournament" -> {
                lobbyReconnectCallback?.invoke(session, identity, playerSession, lobbyId!!)
            }
            "game" -> {
                val gameSession = gameRepository.findById(gameSessionId!!)
                logger.info("Reconnecting to game: found=${gameSession != null}, isStarted=${gameSession?.isStarted}, player1=${gameSession?.player1?.playerId?.value}, player2=${gameSession?.player2?.playerId?.value}")
                if (gameSession != null) {
                    // Remove old player session if exists, then associate new one
                    if (gameSession.getPlayerSession(identity.playerId) != null) {
                        gameSession.removePlayer(identity.playerId)
                    }
                    gameSession.associatePlayer(playerSession)

                    if (gameSession.isStarted) {
                        when {
                            // Player needs to choose cards to put on bottom
                            gameSession.isAwaitingBottomCards(identity.playerId) -> {
                                val hand = gameSession.getHand(identity.playerId)
                                val cardsToBottom = gameSession.getCardsToBottom(identity.playerId)
                                sender.send(session, ServerMessage.ChooseBottomCards(hand, cardsToBottom))
                            }
                            // Player hasn't made mulligan decision yet
                            gameSession.isMulliganPhase && !gameSession.hasMulliganComplete(identity.playerId) -> {
                                val decision = gameSession.getMulliganDecision(identity.playerId)
                                sender.send(session, decision)
                            }
                            // Player finished but opponent still in mulligan
                            gameSession.isMulliganPhase -> {
                                sender.send(session, ServerMessage.WaitingForOpponentMulligan)
                            }
                            // Normal game in progress
                            else -> {
                                // Use broadcastStateUpdate to trigger auto-pass loop for both players
                                logger.info("Sending state update for game $gameSessionId")
                                broadcastStateUpdateCallback?.invoke(gameSession, emptyList())
                            }
                        }
                    } else {
                        logger.info("Game not started yet, not sending state update")
                    }
                }
            }
        }
    }

    fun handleDisconnect(session: WebSocketSession) {
        val (token, playerSession) = sessionRegistry.removeByWsId(session.id)

        if (token != null) {
            val identity = sessionRegistry.getIdentityByToken(token)
            if (identity != null) {
                // Use longer grace period for tournament players
                val lobbyId = identity.currentLobbyId
                val lobby = if (lobbyId != null) lobbyRepository.findLobbyById(lobbyId) else null
                val isInTournament = lobby?.state == LobbyState.TOURNAMENT_ACTIVE
                val gracePeriodMinutes = if (isInTournament) {
                    sessionRegistry.tournamentDisconnectGracePeriodMinutes
                } else {
                    sessionRegistry.disconnectGracePeriodMinutes
                }

                logger.info("Player disconnected: ${identity.playerName} (starting ${gracePeriodMinutes}m grace period, tournament=$isInTournament)")
                identity.webSocketSession = null

                val gracePeriodSeconds = gracePeriodMinutes * 60
                identity.disconnectExpiresAt = System.currentTimeMillis() + gracePeriodSeconds * 1000
                identity.disconnectTimer = sessionRegistry.disconnectScheduler.schedule({
                    handleDisconnectTimeout(token)
                }, gracePeriodMinutes, TimeUnit.MINUTES)

                // Broadcast to tournament lobby so other players can see disconnect + add time
                if (isInTournament) {
                    val msg = ServerMessage.TournamentPlayerDisconnected(
                        playerId = identity.playerId.value,
                        playerName = identity.playerName,
                        secondsRemaining = gracePeriodSeconds.toInt()
                    )
                    lobby.players.forEach { (_, playerState) ->
                        val ws = playerState.identity.webSocketSession
                        if (ws != null && ws.isOpen) sender.send(ws, msg)
                    }
                    for ((_, spectatorIdentity) in lobby.spectators) {
                        val ws = spectatorIdentity.webSocketSession
                        if (ws != null && ws.isOpen) sender.send(ws, msg)
                    }
                }

                // Start 2-minute in-game auto-concede timer and notify opponent
                val gameSessionId = identity.currentGameSessionId
                if (gameSessionId != null) {
                    val gameSession = gameRepository.findById(gameSessionId)
                    if (gameSession != null && !gameSession.isGameOver()) {
                        val opponentId = gameSession.getOpponentId(identity.playerId)
                        val opponentSession = if (opponentId != null) gameSession.getPlayerSession(opponentId) else null
                        if (opponentSession?.isConnected == true) {
                            sender.send(opponentSession.webSocketSession,
                                ServerMessage.OpponentDisconnected(secondsRemaining = GAME_DISCONNECT_SECONDS))
                        }

                        identity.gameDisconnectTimer = sessionRegistry.disconnectScheduler.schedule({
                            handleGameDisconnectTimeout(token)
                        }, GAME_DISCONNECT_SECONDS.toLong(), TimeUnit.SECONDS)
                    }
                }

                if (lobby != null) broadcastLobbyUpdate(lobby)
                return
            }
        }

        if (playerSession != null) {
            logger.info("Player disconnected (no identity): ${playerSession.playerName}")
            legacyHandleDisconnect(playerSession)
        }
    }

    private fun handleDisconnectTimeout(token: String) {
        val identity = sessionRegistry.removeIdentity(token) ?: return

        logger.info("Disconnect timeout for ${identity.playerName} — treating as abandonment")

        val lobbyId = identity.currentLobbyId
        if (lobbyId != null) {
            val lobby = lobbyRepository.findLobbyById(lobbyId)
            if (lobby != null) {
                when (lobby.state) {
                    LobbyState.WAITING_FOR_PLAYERS, LobbyState.DRAFTING, LobbyState.DECK_BUILDING -> {
                        lobby.removePlayer(identity.playerId)
                        if (lobby.playerCount == 0) {
                            lobbyRepository.removeLobby(lobbyId)
                            lobbyRepository.removeTournament(lobbyId)
                        } else {
                            broadcastLobbyUpdate(lobby)
                        }
                    }
                    LobbyState.TOURNAMENT_ACTIVE -> {
                        // Handle abandon under the per-lobby lock to avoid racing
                        // with handleReadyForNextRound and handleRoundComplete
                        handleAbandonCallback?.invoke(lobbyId, identity.playerId)
                    }
                    LobbyState.TOURNAMENT_COMPLETE -> {}
                }
            }
        }

        val gameSessionId = identity.currentGameSessionId
        if (gameSessionId != null) {
            val gameSession = gameRepository.findById(gameSessionId)
            if (gameSession != null) {
                val opponentId = gameSession.getOpponentId(identity.playerId)
                if (opponentId != null) {
                    gameSession.playerConcedes(identity.playerId)
                    handleGameOverCallback?.invoke(gameSession, GameOverReason.DISCONNECTION)
                } else {
                    gameRepository.remove(gameSessionId)
                }
            }
        }
    }

    /**
     * Handles the in-game 2-minute disconnect timer expiring.
     * Forces the disconnected player to concede.
     */
    private fun handleGameDisconnectTimeout(token: String) {
        val identity = sessionRegistry.getIdentityByToken(token) ?: return
        identity.gameDisconnectTimer = null

        // Only concede if still disconnected and still in a game
        if (identity.isConnected) return
        val gameSessionId = identity.currentGameSessionId ?: return
        val gameSession = gameRepository.findById(gameSessionId) ?: return
        if (gameSession.isGameOver()) return

        logger.info("Game disconnect timeout for ${identity.playerName} — auto-conceding")
        gameSession.playerConcedes(identity.playerId)
        handleGameOverCallback?.invoke(gameSession, GameOverReason.DISCONNECTION)
    }

    /**
     * Handle a request from a tournament player to add 5 minutes to a disconnected
     * player's timer, giving them more time to reconnect.
     */
    fun handleAddDisconnectTime(session: WebSocketSession, message: ClientMessage.AddDisconnectTime) {
        val targetPlayerId = EntityId(message.playerId)

        // Find the disconnected player's identity by scanning tokens
        var targetIdentity: PlayerIdentity? = null
        var targetToken: String? = null
        sessionRegistry.forEachIdentity { token, identity ->
            if (identity.playerId == targetPlayerId && identity.disconnectExpiresAt != null) {
                targetIdentity = identity
                targetToken = token
            }
        }

        val identity = targetIdentity ?: return
        val token = targetToken ?: return

        // Cancel old timer
        identity.disconnectTimer?.cancel(false)

        // Extend by 5 minutes
        val addedMs = ADD_DISCONNECT_TIME_SECONDS * 1000L
        val newExpiresAt = (identity.disconnectExpiresAt ?: System.currentTimeMillis()) + addedMs
        identity.disconnectExpiresAt = newExpiresAt
        val remainingMs = newExpiresAt - System.currentTimeMillis()
        val remainingSeconds = (remainingMs / 1000).coerceAtLeast(0).toInt()

        // Schedule new timer
        identity.disconnectTimer = sessionRegistry.disconnectScheduler.schedule({
            handleDisconnectTimeout(token)
        }, remainingMs, TimeUnit.MILLISECONDS)

        logger.info("Added ${ADD_DISCONNECT_TIME_SECONDS}s to disconnect timer for ${identity.playerName} (${remainingSeconds}s remaining)")

        // Broadcast updated timer to lobby
        val lobbyId = identity.currentLobbyId ?: return
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return

        val msg = ServerMessage.TournamentPlayerDisconnected(
            playerId = identity.playerId.value,
            playerName = identity.playerName,
            secondsRemaining = remainingSeconds
        )
        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) sender.send(ws, msg)
        }
        for ((_, spectatorIdentity) in lobby.spectators) {
            val ws = spectatorIdentity.webSocketSession
            if (ws != null && ws.isOpen) sender.send(ws, msg)
        }
    }

    /**
     * Handle a request to kick a disconnected tournament player.
     * Only allowed after the player has been disconnected for 2+ minutes.
     */
    fun handleKickPlayer(session: WebSocketSession, message: ClientMessage.KickPlayer) {
        val targetPlayerId = EntityId(message.playerId)

        var targetIdentity: PlayerIdentity? = null
        var targetToken: String? = null
        sessionRegistry.forEachIdentity { token, identity ->
            if (identity.playerId == targetPlayerId && identity.disconnectExpiresAt != null) {
                targetIdentity = identity
                targetToken = token
            }
        }

        val identity = targetIdentity ?: return
        val token = targetToken ?: return

        // Check minimum disconnect time (2 minutes)
        val disconnectedAt = identity.disconnectExpiresAt?.let { expiresAt ->
            // Original disconnect time = expiresAt - original grace period
            // But we can't know the original grace. Instead, track elapsed time differently.
            // Since we store disconnectExpiresAt, just check: has enough time passed?
            val lobbyId = identity.currentLobbyId
            val lobby = if (lobbyId != null) lobbyRepository.findLobbyById(lobbyId) else null
            val isInTournament = lobby?.state == LobbyState.TOURNAMENT_ACTIVE
            val originalGracePeriodMs = (if (isInTournament) sessionRegistry.tournamentDisconnectGracePeriodMinutes else sessionRegistry.disconnectGracePeriodMinutes) * 60 * 1000
            val remainingMs = expiresAt - System.currentTimeMillis()
            val elapsedMs = originalGracePeriodMs - remainingMs
            elapsedMs
        } ?: return

        if (disconnectedAt < KICK_MINIMUM_DISCONNECT_SECONDS * 1000L) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Player must be disconnected for at least 2 minutes before kicking")
            return
        }

        logger.info("Player ${identity.playerName} kicked from tournament by vote")

        // Cancel existing timers and immediately trigger timeout
        identity.disconnectTimer?.cancel(false)
        identity.disconnectTimer = null
        identity.gameDisconnectTimer?.cancel(false)
        identity.gameDisconnectTimer = null

        handleDisconnectTimeout(token)
    }

    private fun legacyHandleDisconnect(playerSession: PlayerSession) {
        val gameSessionId = playerSession.currentGameSessionId ?: return

        val gameSession = gameRepository.findById(gameSessionId)
        if (gameSession != null) {
            val opponentId = gameSession.getOpponentId(playerSession.playerId)
            if (opponentId != null) {
                val opponentSession = gameSession.getPlayerSession(opponentId)
                if (opponentSession?.isConnected == true) {
                    sender.send(
                        opponentSession.webSocketSession,
                        ServerMessage.GameOver(opponentId, GameOverReason.DISCONNECTION)
                    )
                }
            }
            gameRepository.remove(gameSessionId)
        }

        val sealedSession = lobbyRepository.findSealedSessionById(gameSessionId)
        if (sealedSession != null) {
            val opponentId = sealedSession.getOpponentId(playerSession.playerId)
            if (opponentId != null) {
                val opponentPlayerSession = sealedSession.getPlayerSession(opponentId)
                if (opponentPlayerSession?.isConnected == true) {
                    sender.send(
                        opponentPlayerSession.webSocketSession,
                        ServerMessage.Error(ErrorCode.GAME_NOT_FOUND, "Opponent disconnected")
                    )
                }
            }
            lobbyRepository.removeSealedSession(gameSessionId)
        }
    }

    private fun broadcastLobbyUpdate(lobby: com.wingedsheep.gameserver.lobby.TournamentLobby) {
        lobby.players.forEach { (playerId, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, lobby.buildLobbyUpdate(playerId))
            }
        }
    }

    // Callbacks set by GameWebSocketHandler to avoid circular dependencies
    var handleGameOverCallback: ((com.wingedsheep.gameserver.session.GameSession, GameOverReason) -> Unit)? = null
    var broadcastStateUpdateCallback: ((com.wingedsheep.gameserver.session.GameSession, List<com.wingedsheep.engine.core.GameEvent>) -> Unit)? = null
    var sendActiveMatchesToPlayerCallback: ((PlayerIdentity, WebSocketSession) -> Unit)? = null
    var handleAbandonCallback: ((String, EntityId) -> Unit)? = null
    var restoreSpectatingCallback: ((PlayerIdentity, PlayerSession, WebSocketSession, String) -> Unit)? = null
    var lobbyReconnectCallback: ((WebSocketSession, PlayerIdentity, PlayerSession, String) -> Unit)? = null

    companion object {
        /** Time in seconds before a disconnected player auto-concedes their game */
        const val GAME_DISCONNECT_SECONDS = 120

        /** Time in seconds added per "Add Time" click */
        const val ADD_DISCONNECT_TIME_SECONDS = 60

        /** Minimum seconds a player must be disconnected before they can be kicked */
        const val KICK_MINIMUM_DISCONNECT_SECONDS = 120

        fun cardToSealedCardInfo(card: com.wingedsheep.sdk.model.CardDefinition): ServerMessage.SealedCardInfo {
            return ServerMessage.SealedCardInfo(
                name = card.name,
                manaCost = if (card.manaCost.symbols.isEmpty()) null else card.manaCost.toString(),
                typeLine = card.typeLine.toString(),
                rarity = card.metadata.rarity.name,
                imageUri = card.metadata.imageUri,
                power = card.creatureStats?.basePower,
                toughness = card.creatureStats?.baseToughness,
                oracleText = if (card.oracleText.isBlank()) null else card.oracleText,
                rulings = card.metadata.rulings.map { ServerMessage.SealedRuling(it.date, it.text) }
            )
        }
    }
}
