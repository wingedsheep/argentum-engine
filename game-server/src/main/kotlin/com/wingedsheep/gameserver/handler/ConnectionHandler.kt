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
            "lobby" -> {
                val lobby = lobbyRepository.findLobbyById(lobbyId!!)!!
                sender.send(session, lobby.buildLobbyUpdate(identity.playerId))
            }
            "drafting" -> {
                val lobby = lobbyRepository.findLobbyById(lobbyId!!)!!
                sender.send(session, lobby.buildLobbyUpdate(identity.playerId))
                // Send current draft pack and already-picked cards
                val playerState = lobby.players[identity.playerId]
                val pack = playerState?.currentPack
                if (playerState != null && pack != null) {
                    sender.send(session, ServerMessage.DraftPackReceived(
                        packNumber = lobby.currentPackNumber,
                        pickNumber = lobby.currentPickNumber,
                        cards = pack.map { cardToSealedCardInfo(it) },
                        timeRemainingSeconds = lobby.pickTimeRemaining,  // Use actual remaining time, not full duration
                        passDirection = lobby.getPassDirection().name,
                        picksPerRound = minOf(lobby.picksPerRound, pack.size),
                        pickedCards = playerState.cardPool.map { cardToSealedCardInfo(it) }
                    ))
                }
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
            "tournament" -> {
                val tournament = lobbyRepository.findTournamentById(lobbyId!!)
                val lobby = lobbyRepository.findLobbyById(lobbyId)
                if (tournament != null && lobby != null) {
                    val connectedIds = lobby.players.values
                        .filter { it.identity.isConnected }
                        .map { it.identity.playerId }
                        .toSet()

                    // Get next opponent info for TournamentStarted message
                    val nextRoundMatchups = tournament.peekNextRoundMatchups()
                    val nextOpponentId = nextRoundMatchups[identity.playerId]
                    val nextOpponentName = if (nextOpponentId != null) {
                        lobby.players[nextOpponentId]?.identity?.playerName
                    } else {
                        null
                    }
                    val hasBye = nextRoundMatchups.containsKey(identity.playerId) && nextOpponentId == null

                    sender.send(session, ServerMessage.TournamentStarted(
                        lobbyId = lobbyId,
                        totalRounds = tournament.totalRounds,
                        standings = tournament.getStandingsInfo(connectedIds),
                        nextOpponentName = nextOpponentName,
                        nextRoundHasBye = hasBye
                    ))

                    // Find the player's match in the current round (including byes and completed matches)
                    val currentRound = tournament.currentRound
                    val playerMatch = currentRound?.matches?.find {
                        it.player1Id == identity.playerId || it.player2Id == identity.playerId
                    }

                    when {
                        // Case 0: Before first round starts, waiting for players to ready up
                        currentRound == null -> {
                            // TournamentStarted already sent with next opponent info
                            // Just send the current ready player list if any
                            val readyPlayerIds = lobby.getReadyPlayerIds()
                            if (readyPlayerIds.isNotEmpty()) {
                                sender.send(session, ServerMessage.PlayerReadyForRound(
                                    lobbyId = lobbyId,
                                    playerId = readyPlayerIds.last().value,
                                    playerName = "",
                                    readyPlayerIds = readyPlayerIds.map { it.value },
                                    totalConnectedPlayers = connectedIds.size
                                ))
                            }
                        }

                        // Case 1: Player has a bye this round (and round is still in progress)
                        playerMatch?.isBye == true && !tournament.isRoundComplete() -> {
                            sender.send(session, ServerMessage.TournamentBye(
                                lobbyId = lobbyId,
                                round = currentRound.roundNumber
                            ))
                            // Send active matches so they can spectate
                            sendActiveMatchesToPlayerCallback?.invoke(identity, session)
                        }

                        // Case 2: Player has an active game in progress (round not complete)
                        playerMatch?.gameSessionId != null && !playerMatch.isComplete && !tournament.isRoundComplete() -> {
                            val gs = gameRepository.findById(playerMatch.gameSessionId!!)
                            if (gs != null && gs.isStarted && !gs.isGameOver()) {
                                identity.currentGameSessionId = playerMatch.gameSessionId
                                playerSession.currentGameSessionId = playerMatch.gameSessionId
                                val opponentId = if (playerMatch.player1Id == identity.playerId) playerMatch.player2Id else playerMatch.player1Id
                                val opponentName = lobby.players[opponentId]?.identity?.playerName ?: "Unknown"
                                sender.send(session, ServerMessage.TournamentMatchStarting(
                                    lobbyId = lobbyId,
                                    round = currentRound.roundNumber,
                                    gameSessionId = playerMatch.gameSessionId!!,
                                    opponentName = opponentName
                                ))
                                // Associate player with game session first
                                if (gs.getPlayerSession(identity.playerId) != null) {
                                    gs.removePlayer(identity.playerId)
                                }
                                gs.associatePlayer(playerSession)
                                // Handle mulligan states
                                when {
                                    // Player needs to choose cards to put on bottom
                                    gs.isAwaitingBottomCards(identity.playerId) -> {
                                        val hand = gs.getHand(identity.playerId)
                                        val cardsToBottom = gs.getCardsToBottom(identity.playerId)
                                        sender.send(session, ServerMessage.ChooseBottomCards(hand, cardsToBottom))
                                    }
                                    // Player hasn't made mulligan decision yet
                                    gs.isMulliganPhase && !gs.hasMulliganComplete(identity.playerId) -> {
                                        val decision = gs.getMulliganDecision(identity.playerId)
                                        sender.send(session, decision)
                                    }
                                    // Player finished but opponent still in mulligan
                                    gs.isMulliganPhase -> {
                                        sender.send(session, ServerMessage.WaitingForOpponentMulligan)
                                    }
                                    // Normal game in progress
                                    else -> {
                                        // Use broadcastStateUpdate to trigger auto-pass loop
                                        broadcastStateUpdateCallback?.invoke(gs, emptyList())
                                    }
                                }
                            }
                        }

                        // Case 3: Player's match is complete but round isn't (waiting for others)
                        playerMatch?.isComplete == true && !tournament.isRoundComplete() -> {
                            // Send active matches so they can see other games/spectate
                            sendActiveMatchesToPlayerCallback?.invoke(identity, session)
                        }

                        // Case 4: Round is complete, waiting for players to ready up
                        tournament.isRoundComplete() -> {
                            val round = currentRound!!
                            // Get next round matchups to show who each player plays next
                            val nextRoundMatchups = if (!tournament.isComplete) {
                                tournament.peekNextRoundMatchups()
                            } else {
                                emptyMap()
                            }

                            // Find this player's next opponent
                            val nextOpponentId = nextRoundMatchups[identity.playerId]
                            val nextOpponentName = if (nextOpponentId != null) {
                                lobby.players[nextOpponentId]?.identity?.playerName
                            } else {
                                null
                            }
                            val hasBye = nextRoundMatchups.containsKey(identity.playerId) && nextOpponentId == null

                            sender.send(session, ServerMessage.RoundComplete(
                                lobbyId = lobbyId,
                                round = round.roundNumber,
                                results = tournament.getCurrentRoundResults(),
                                standings = tournament.getStandingsInfo(connectedIds),
                                nextOpponentName = nextOpponentName,
                                nextRoundHasBye = hasBye,
                                isTournamentComplete = tournament.isComplete
                            ))

                            // Also send the current ready player list
                            val readyPlayerIds = lobby.getReadyPlayerIds()
                            if (readyPlayerIds.isNotEmpty()) {
                                sender.send(session, ServerMessage.PlayerReadyForRound(
                                    lobbyId = lobbyId,
                                    playerId = readyPlayerIds.last().value,
                                    playerName = "",
                                    readyPlayerIds = readyPlayerIds.map { it.value },
                                    totalConnectedPlayers = connectedIds.size
                                ))
                            }
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
    var handleRoundCompleteCallback: ((String) -> Unit)? = null
    var broadcastStateUpdateCallback: ((com.wingedsheep.gameserver.session.GameSession, List<com.wingedsheep.engine.core.GameEvent>) -> Unit)? = null
    var sendActiveMatchesToPlayerCallback: ((PlayerIdentity, WebSocketSession) -> Unit)? = null

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
