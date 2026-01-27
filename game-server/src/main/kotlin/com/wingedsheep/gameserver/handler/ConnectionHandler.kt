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
        if (token != null) {
            val existingIdentity = sessionRegistry.getIdentityByToken(token)
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
                    lobby.state == LobbyState.DECK_BUILDING -> { context = "deckBuilding"; contextId = lobbyId }
                    lobby.state == LobbyState.TOURNAMENT_ACTIVE -> { context = "tournament"; contextId = lobbyId }
                    else -> { context = null; contextId = null }
                }
            }
            gameSessionId != null && gameRepository.findById(gameSessionId) != null -> {
                context = "game"; contextId = gameSessionId
            }
            else -> { context = null; contextId = null }
        }

        sender.send(session, ServerMessage.Reconnected(
            playerId = identity.playerId.value,
            token = identity.token,
            context = context,
            contextId = contextId
        ))

        when (context) {
            "lobby" -> {
                val lobby = lobbyRepository.findLobbyById(lobbyId!!)!!
                sender.send(session, lobby.buildLobbyUpdate(identity.playerId))
            }
            "deckBuilding" -> {
                val lobby = lobbyRepository.findLobbyById(lobbyId!!)!!
                sender.send(session, lobby.buildLobbyUpdate(identity.playerId))
                val playerState = lobby.players[identity.playerId]
                if (playerState != null && playerState.cardPool.isNotEmpty()) {
                    val basicLandInfos = lobby.basicLands.values.map { cardToSealedCardInfo(it) }
                    val poolInfos = playerState.cardPool.map { cardToSealedCardInfo(it) }
                    sender.send(session, ServerMessage.SealedPoolGenerated(
                        setCode = lobby.setCode,
                        setName = lobby.setName,
                        cardPool = poolInfos,
                        basicLands = basicLandInfos
                    ))
                }
            }
            "game" -> {
                val gameSession = gameRepository.findById(gameSessionId!!)
                if (gameSession != null) {
                    gameSession.getPlayerSession(identity.playerId)?.let {
                        gameSession.removePlayer(identity.playerId)
                        gameSession.addPlayer(playerSession, emptyMap())
                    }
                    if (gameSession.isStarted) {
                        if (gameSession.isMulliganPhase) {
                            val decision = gameSession.getMulliganDecision(identity.playerId)
                            sender.send(session, decision)
                        } else {
                            // Use broadcastStateUpdate to trigger auto-pass loop for both players
                            broadcastStateUpdateCallback?.invoke(gameSession, emptyList())
                        }
                    }
                }
            }
            "tournament" -> {
                val tournament = lobbyRepository.findTournamentById(lobbyId!!)
                val lobby = lobbyRepository.findLobbyById(lobbyId)
                if (tournament != null && lobby != null) {
                    val connectedIds = lobby.players.values
                        .filter { it.identity.isConnected }
                        .map { it.identity.playerId }
                        .toSet()
                    sender.send(session, ServerMessage.TournamentStarted(
                        lobbyId = lobbyId,
                        totalRounds = tournament.totalRounds,
                        standings = tournament.getStandingsInfo(connectedIds)
                    ))
                    val currentMatch = tournament.getCurrentRoundGameMatches().find {
                        it.player1Id == identity.playerId || it.player2Id == identity.playerId
                    }
                    if (currentMatch?.gameSessionId != null) {
                        val gs = gameRepository.findById(currentMatch.gameSessionId!!)
                        if (gs != null && gs.isStarted && !gs.isGameOver()) {
                            identity.currentGameSessionId = currentMatch.gameSessionId
                            playerSession.currentGameSessionId = currentMatch.gameSessionId
                            val opponentId = if (currentMatch.player1Id == identity.playerId) currentMatch.player2Id else currentMatch.player1Id
                            val opponentName = lobby.players[opponentId]?.identity?.playerName ?: "Unknown"
                            sender.send(session, ServerMessage.TournamentMatchStarting(
                                lobbyId = lobbyId,
                                round = tournament.currentRound?.roundNumber ?: 0,
                                gameSessionId = currentMatch.gameSessionId!!,
                                opponentName = opponentName
                            ))
                            val update = gs.createStateUpdate(identity.playerId, emptyList())
                            if (update != null) sender.send(session, update)
                        }
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
                logger.info("Player disconnected: ${identity.playerName} (starting ${sessionRegistry.disconnectGracePeriodMinutes}m grace period)")
                identity.webSocketSession = null

                identity.disconnectTimer = sessionRegistry.disconnectScheduler.schedule({
                    handleDisconnectTimeout(token)
                }, sessionRegistry.disconnectGracePeriodMinutes, TimeUnit.MINUTES)

                val lobbyId = identity.currentLobbyId
                if (lobbyId != null) {
                    val lobby = lobbyRepository.findLobbyById(lobbyId)
                    if (lobby != null) broadcastLobbyUpdate(lobby)
                }
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
                    LobbyState.WAITING_FOR_PLAYERS, LobbyState.DECK_BUILDING -> {
                        lobby.removePlayer(identity.playerId)
                        if (lobby.playerCount == 0) {
                            lobbyRepository.removeLobby(lobbyId)
                            lobbyRepository.removeTournament(lobbyId)
                        } else {
                            broadcastLobbyUpdate(lobby)
                        }
                    }
                    LobbyState.TOURNAMENT_ACTIVE -> {
                        val tournament = lobbyRepository.findTournamentById(lobbyId)
                        tournament?.recordAbandon(identity.playerId)
                        if (tournament?.isRoundComplete() == true) {
                            handleRoundCompleteCallback?.invoke(lobbyId)
                        }
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

    private fun broadcastLobbyUpdate(lobby: com.wingedsheep.gameserver.lobby.SealedLobby) {
        lobby.players.forEach { (playerId, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, lobby.buildLobbyUpdate(playerId))
            }
        }
    }

    // Callbacks set by GameWebSocketHandler to avoid circular dependencies
    var handleGameOverCallback: ((com.wingedsheep.gameserver.session.GameSession, GameOverReason) -> Unit)? = null
    var handleRoundCompleteCallback: ((String) -> Unit)? = null
    var broadcastStateUpdateCallback: ((com.wingedsheep.gameserver.session.GameSession, List<com.wingedsheep.engine.core.GameEvent>) -> Unit)? = null

    companion object {
        fun cardToSealedCardInfo(card: com.wingedsheep.sdk.model.CardDefinition): ServerMessage.SealedCardInfo {
            return ServerMessage.SealedCardInfo(
                name = card.name,
                manaCost = if (card.manaCost.symbols.isEmpty()) null else card.manaCost.toString(),
                typeLine = card.typeLine.toString(),
                rarity = card.metadata.rarity.name,
                imageUri = card.metadata.imageUri,
                power = card.creatureStats?.basePower,
                toughness = card.creatureStats?.baseToughness,
                oracleText = if (card.oracleText.isBlank()) null else card.oracleText
            )
        }
    }
}
