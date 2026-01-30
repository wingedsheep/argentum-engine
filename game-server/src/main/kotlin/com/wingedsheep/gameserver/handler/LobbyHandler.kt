package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.handler.ConnectionHandler.Companion.cardToSealedCardInfo
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.lobby.SealedLobby
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.gameserver.sealed.BoosterGenerator
import com.wingedsheep.gameserver.sealed.SealedSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.tournament.TournamentManager
import com.wingedsheep.engine.registry.CardRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

@Component
class LobbyHandler(
    private val sessionRegistry: SessionRegistry,
    private val gameRepository: GameRepository,
    private val lobbyRepository: LobbyRepository,
    private val sender: MessageSender,
    private val cardRegistry: CardRegistry,
    private val gamePlayHandler: GamePlayHandler
) {
    private val logger = LoggerFactory.getLogger(LobbyHandler::class.java)

    @Volatile
    var waitingSealedSession: SealedSession? = null

    fun handle(session: WebSocketSession, message: ClientMessage) {
        when (message) {
            is ClientMessage.CreateSealedGame -> handleCreateSealedGame(session, message)
            is ClientMessage.JoinSealedGame -> handleJoinSealedGame(session, message)
            is ClientMessage.SubmitSealedDeck -> handleSubmitSealedDeck(session, message)
            is ClientMessage.UnsubmitDeck -> handleUnsubmitDeck(session)
            is ClientMessage.CreateSealedLobby -> handleCreateSealedLobby(session, message)
            is ClientMessage.JoinLobby -> handleJoinLobby(session, message)
            is ClientMessage.StartSealedLobby -> handleStartSealedLobby(session)
            is ClientMessage.LeaveLobby -> handleLeaveLobby(session)
            is ClientMessage.StopLobby -> handleStopLobby(session)
            is ClientMessage.UpdateLobbySettings -> handleUpdateLobbySettings(session, message)
            is ClientMessage.SpectateGame -> handleSpectateGame(session, message)
            is ClientMessage.StopSpectating -> handleStopSpectating(session)
            else -> {}
        }
    }

    private fun handleCreateSealedGame(session: WebSocketSession, message: ClientMessage.CreateSealedGame) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val setConfig = BoosterGenerator.getSetConfig(message.setCode)
        if (setConfig == null) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Unknown set code: ${message.setCode}")
            return
        }

        val sealedSession = SealedSession(setCode = setConfig.setCode, setName = setConfig.setName)
        sealedSession.addPlayer(playerSession)

        lobbyRepository.saveSealedSession(sealedSession)
        waitingSealedSession = sealedSession

        logger.info("Sealed game created: ${sealedSession.sessionId} by ${playerSession.playerName} (set: ${setConfig.setName})")
        sender.send(session, ServerMessage.SealedGameCreated(
            sessionId = sealedSession.sessionId,
            setCode = setConfig.setCode,
            setName = setConfig.setName
        ))
    }

    fun handleJoinSealedGame(session: WebSocketSession, message: ClientMessage.JoinSealedGame) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val sealedSession = lobbyRepository.findSealedSessionById(message.sessionId)
        if (sealedSession == null) {
            val gameSession = gameRepository.findById(message.sessionId)
            if (gameSession != null) {
                gamePlayHandler.handleJoinGame(session, ClientMessage.JoinGame(message.sessionId, emptyMap()))
                return
            }
            val lobby = lobbyRepository.findLobbyById(message.sessionId)
            if (lobby != null) {
                handleJoinLobby(session, ClientMessage.JoinLobby(message.sessionId))
                return
            }
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found: ${message.sessionId}")
            return
        }

        if (sealedSession.isFull) {
            sender.sendError(session, ErrorCode.GAME_FULL, "Sealed game is full")
            return
        }

        sealedSession.addPlayer(playerSession)

        if (waitingSealedSession?.sessionId == sealedSession.sessionId) {
            waitingSealedSession = null
        }

        logger.info("Player ${playerSession.playerName} joined sealed game ${sealedSession.sessionId}")
        sealedSession.generatePools()
        sendSealedPoolToAllPlayers(sealedSession)
    }

    private fun handleSubmitSealedDeck(session: WebSocketSession, message: ClientMessage.SubmitSealedDeck) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        val lobbyId = identity?.currentLobbyId
        if (lobbyId != null) {
            handleLobbyDeckSubmit(session, playerSession, identity, lobbyId, message.deckList)
            return
        }

        // Legacy 2-player sealed
        val sealedSessionId = playerSession.currentGameSessionId
        if (sealedSessionId == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a sealed game")
            return
        }

        val sealedSession = lobbyRepository.findSealedSessionById(sealedSessionId)
        if (sealedSession == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Sealed game not found")
            return
        }

        val result = sealedSession.submitDeck(playerSession.playerId, message.deckList)
        when (result) {
            is SealedSession.DeckSubmissionResult.Success -> {
                val deckSize = message.deckList.values.sum()
                logger.info("Player ${playerSession.playerName} submitted deck ($deckSize cards)")
                sender.send(session, ServerMessage.DeckSubmitted(deckSize))

                if (result.bothReady) {
                    startGameFromSealed(sealedSession)
                } else {
                    sender.send(session, ServerMessage.WaitingForOpponent)
                    val opponentId = sealedSession.getOpponentId(playerSession.playerId)
                    if (opponentId != null) {
                        val opponentSession = sealedSession.getPlayerSession(opponentId)
                        if (opponentSession != null) {
                            sender.send(opponentSession.webSocketSession, ServerMessage.OpponentDeckSubmitted)
                        }
                    }
                }
            }
            is SealedSession.DeckSubmissionResult.Error -> {
                sender.sendError(session, ErrorCode.INVALID_DECK, result.message)
            }
        }
    }

    private fun sendSealedPoolToAllPlayers(sealedSession: SealedSession) {
        val basicLandInfos = sealedSession.basicLands.values.map { cardToSealedCardInfo(it) }
        sealedSession.players.forEach { (_, playerState) ->
            val poolInfos = playerState.cardPool.map { cardToSealedCardInfo(it) }
            sender.send(
                playerState.session.webSocketSession,
                ServerMessage.SealedPoolGenerated(
                    setCode = sealedSession.setCode,
                    setName = sealedSession.setName,
                    cardPool = poolInfos,
                    basicLands = basicLandInfos
                )
            )
        }
    }

    private fun startGameFromSealed(sealedSession: SealedSession) {
        logger.info("Starting game from sealed session: ${sealedSession.sessionId}")
        val gameSession = GameSession(cardRegistry = cardRegistry)

        sealedSession.players.forEach { (playerId, playerState) ->
            val deck = playerState.submittedDeck
                ?: throw IllegalStateException("Player $playerId has no submitted deck")
            gameSession.addPlayer(playerState.session, deck)

            // Store player info for persistence
            val token = sessionRegistry.getTokenByWsId(playerState.session.webSocketSession.id)
            if (token != null) {
                gameSession.setPlayerPersistenceInfo(playerId, playerState.session.playerName, token)
                sessionRegistry.getIdentityByToken(token)?.currentGameSessionId = gameSession.sessionId
            }
        }

        gameRepository.save(gameSession)
        lobbyRepository.removeSealedSession(sealedSession.sessionId)

        gamePlayHandler.startGame(gameSession)
    }

    private fun handleCreateSealedLobby(session: WebSocketSession, message: ClientMessage.CreateSealedLobby) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val setConfig = BoosterGenerator.getSetConfig(message.setCode)
        if (setConfig == null) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Unknown set code: ${message.setCode}")
            return
        }

        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Identity not found")
            return
        }

        val lobby = SealedLobby(
            setCode = setConfig.setCode,
            setName = setConfig.setName,
            boosterCount = message.boosterCount.coerceIn(1, 12),
            maxPlayers = message.maxPlayers.coerceIn(2, 8)
        )
        lobby.addPlayer(identity)
        lobbyRepository.saveLobby(lobby)

        logger.info("Sealed lobby created: ${lobby.lobbyId} by ${identity.playerName} (set: ${setConfig.setName})")
        sender.send(session, ServerMessage.LobbyCreated(lobby.lobbyId))
        broadcastLobbyUpdate(lobby)
    }

    fun handleJoinLobby(session: WebSocketSession, message: ClientMessage.JoinLobby) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Identity not found")
            return
        }

        val lobby = lobbyRepository.findLobbyById(message.lobbyId)
        if (lobby == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found: ${message.lobbyId}")
            return
        }

        if (lobby.isFull) {
            sender.sendError(session, ErrorCode.GAME_FULL, "Lobby is full")
            return
        }

        if (lobby.state != LobbyState.WAITING_FOR_PLAYERS) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Lobby not accepting players")
            return
        }

        lobby.addPlayer(identity)
        logger.info("Player ${identity.playerName} joined lobby ${lobby.lobbyId}")
        broadcastLobbyUpdate(lobby)
    }

    private fun handleStartSealedLobby(session: WebSocketSession) {
        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby")
            return
        }

        val lobby = lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found")
            return
        }

        if (!lobby.isHost(identity.playerId)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can start")
            return
        }

        if (lobby.playerCount < 2) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Need at least 2 players")
            return
        }

        val started = lobby.startDeckBuilding(identity.playerId)
        if (!started) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Failed to start lobby")
            return
        }

        logger.info("Lobby ${lobby.lobbyId} started deck building (${lobby.playerCount} players)")

        val basicLandInfos = lobby.basicLands.values.map { cardToSealedCardInfo(it) }
        lobby.players.forEach { (_, playerState) ->
            val poolInfos = playerState.cardPool.map { cardToSealedCardInfo(it) }
            val ws = playerState.identity.webSocketSession
            if (ws != null) {
                sender.send(ws, ServerMessage.SealedPoolGenerated(
                    setCode = lobby.setCode,
                    setName = lobby.setName,
                    cardPool = poolInfos,
                    basicLands = basicLandInfos
                ))
            }
        }

        broadcastLobbyUpdate(lobby)
    }

    private fun handleLeaveLobby(session: WebSocketSession) {
        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) return

        val lobbyId = identity.currentLobbyId ?: return
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return

        lobby.removePlayer(identity.playerId)
        identity.currentLobbyId = null

        logger.info("Player ${identity.playerName} left lobby $lobbyId")

        if (lobby.playerCount == 0) {
            lobbyRepository.removeLobby(lobbyId)
            logger.info("Lobby $lobbyId removed (empty)")
        } else {
            broadcastLobbyUpdate(lobby)
        }
    }

    private fun handleStopLobby(session: WebSocketSession) {
        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby")
            return
        }

        val lobby = lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found")
            return
        }

        if (!lobby.isHost(identity.playerId)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can stop the lobby")
            return
        }

        // Can only stop during WAITING_FOR_PLAYERS or DECK_BUILDING
        if (lobby.state != LobbyState.WAITING_FOR_PLAYERS && lobby.state != LobbyState.DECK_BUILDING) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Cannot stop lobby during tournament")
            return
        }

        logger.info("Host ${identity.playerName} stopped lobby $lobbyId")

        // Notify all players that the lobby was stopped
        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, ServerMessage.LobbyStopped)
            }
            playerState.identity.currentLobbyId = null
        }

        // Remove the lobby
        lobbyRepository.removeLobby(lobbyId)
    }

    private fun handleUnsubmitDeck(session: WebSocketSession) {
        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby")
            return
        }

        val lobby = lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found")
            return
        }

        val success = lobby.unsubmitDeck(identity.playerId)
        if (!success) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Cannot unsubmit deck")
            return
        }

        logger.info("Player ${identity.playerName} unsubmitted deck in lobby $lobbyId")

        // Notify all players of the updated lobby state
        broadcastLobbyUpdate(lobby)
    }

    private fun handleUpdateLobbySettings(session: WebSocketSession, message: ClientMessage.UpdateLobbySettings) {
        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby")
            return
        }

        val lobby = lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found")
            return
        }

        if (!lobby.isHost(identity.playerId)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can change settings")
            return
        }

        if (lobby.state != LobbyState.WAITING_FOR_PLAYERS) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Cannot change settings after start")
            return
        }

        message.boosterCount?.let { lobby.boosterCount = it.coerceIn(1, 12) }
        message.maxPlayers?.let { lobby.maxPlayers = it.coerceIn(2, 8) }
        message.gamesPerMatch?.let { lobby.gamesPerMatch = it.coerceIn(1, 5) }

        broadcastLobbyUpdate(lobby)
    }

    private fun handleLobbyDeckSubmit(
        session: WebSocketSession,
        playerSession: PlayerSession,
        identity: PlayerIdentity,
        lobbyId: String,
        deckList: Map<String, Int>
    ) {
        val lobby = lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found")
            return
        }

        val result = lobby.submitDeck(identity.playerId, deckList)
        when (result) {
            is SealedLobby.DeckSubmissionResult.Success -> {
                val deckSize = deckList.values.sum()
                logger.info("Player ${identity.playerName} submitted deck ($deckSize cards) in lobby $lobbyId")
                sender.send(session, ServerMessage.DeckSubmitted(deckSize))
                broadcastLobbyUpdate(lobby)

                if (result.allReady) {
                    startTournament(lobby)
                }
            }
            is SealedLobby.DeckSubmissionResult.Error -> {
                sender.sendError(session, ErrorCode.INVALID_DECK, result.message)
            }
        }
    }

    fun broadcastLobbyUpdate(lobby: SealedLobby) {
        lobby.players.forEach { (playerId, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, lobby.buildLobbyUpdate(playerId))
            }
        }
    }

    fun startTournament(lobby: SealedLobby) {
        logger.info("Starting tournament for lobby ${lobby.lobbyId} with ${lobby.playerCount} players")
        lobby.startTournament()

        val players = lobby.players.values.map { ps ->
            ps.identity.playerId to ps.identity.playerName
        }

        val tournament = TournamentManager(lobby.lobbyId, players, lobby.gamesPerMatch)
        lobbyRepository.saveTournament(lobby.lobbyId, tournament)

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        // Get first round matchups to show who each player plays
        val firstRoundMatchups = tournament.peekNextRoundMatchups()

        lobby.players.forEach { (playerId, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                // Find this player's first opponent
                val nextOpponentId = firstRoundMatchups[playerId]
                val nextOpponentName = if (nextOpponentId != null) {
                    lobby.players[nextOpponentId]?.identity?.playerName
                } else {
                    null
                }
                val hasBye = firstRoundMatchups.containsKey(playerId) && nextOpponentId == null

                sender.send(ws, ServerMessage.TournamentStarted(
                    lobbyId = lobby.lobbyId,
                    totalRounds = tournament.totalRounds,
                    standings = tournament.getStandingsInfo(connectedIds),
                    nextOpponentName = nextOpponentName,
                    nextRoundHasBye = hasBye
                ))
            }
        }

        // Don't auto-start first round - wait for players to ready up
    }

    fun startNextTournamentRound(lobbyId: String) {
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = lobbyRepository.findTournamentById(lobbyId) ?: return

        // Clear ready state when starting a new round
        lobby.clearReadyState()

        val round = tournament.startNextRound()
        if (round == null) {
            completeTournament(lobbyId)
            return
        }

        logger.info("Starting round ${round.roundNumber} for tournament $lobbyId")

        val gameMatches = tournament.getCurrentRoundGameMatches()

        for (match in gameMatches) {
            val player1State = lobby.players[match.player1Id] ?: continue
            val player2State = lobby.players[match.player2Id ?: continue] ?: continue

            val deck1 = lobby.getSubmittedDeck(match.player1Id) ?: continue
            val deck2 = lobby.getSubmittedDeck(match.player2Id!!) ?: continue

            val gameSession = GameSession(cardRegistry = cardRegistry)
            val ps1 = player1State.identity.toPlayerSession()
            val ps2 = player2State.identity.toPlayerSession()

            gameSession.addPlayer(ps1, deck1)
            gameSession.addPlayer(ps2, deck2)

            // Store player info for persistence
            gameSession.setPlayerPersistenceInfo(ps1.playerId, ps1.playerName, player1State.identity.token)
            gameSession.setPlayerPersistenceInfo(ps2.playerId, ps2.playerName, player2State.identity.token)

            gameRepository.save(gameSession)
            gameRepository.linkToLobby(gameSession.sessionId, lobbyId)
            match.gameSessionId = gameSession.sessionId

            player1State.identity.currentGameSessionId = gameSession.sessionId
            player2State.identity.currentGameSessionId = gameSession.sessionId

            val ws1 = player1State.identity.webSocketSession
            val ws2 = player2State.identity.webSocketSession
            if (ws1 != null) {
                sessionRegistry.getPlayerSession(ws1.id)?.currentGameSessionId = gameSession.sessionId
            }
            if (ws2 != null) {
                sessionRegistry.getPlayerSession(ws2.id)?.currentGameSessionId = gameSession.sessionId
            }

            if (ws1 != null && ws1.isOpen) {
                sender.send(ws1, ServerMessage.TournamentMatchStarting(
                    lobbyId = lobbyId,
                    round = round.roundNumber,
                    gameSessionId = gameSession.sessionId,
                    opponentName = player2State.identity.playerName
                ))
            }
            if (ws2 != null && ws2.isOpen) {
                sender.send(ws2, ServerMessage.TournamentMatchStarting(
                    lobbyId = lobbyId,
                    round = round.roundNumber,
                    gameSessionId = gameSession.sessionId,
                    opponentName = player1State.identity.playerName
                ))
            }

            gamePlayHandler.startGame(gameSession)
        }

        for (match in round.matches) {
            if (match.isBye) {
                val playerState = lobby.players[match.player1Id]
                val identity = playerState?.identity
                val ws = identity?.webSocketSession
                if (ws != null && ws.isOpen && identity != null) {
                    sender.send(ws, ServerMessage.TournamentBye(
                        lobbyId = lobbyId,
                        round = round.roundNumber
                    ))
                    // Also send active matches so they can spectate
                    sendActiveMatchesToPlayer(identity, ws)
                }
            }
        }
    }

    fun handleRoundComplete(lobbyId: String) {
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = lobbyRepository.findTournamentById(lobbyId) ?: return
        val round = tournament.currentRound ?: return

        logger.info("Round ${round.roundNumber} complete for tournament $lobbyId")

        // Clear ready state for the next round
        lobby.clearReadyState()

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        // Get next round matchups to show who each player plays next
        val nextRoundMatchups = if (!tournament.isComplete) {
            tournament.peekNextRoundMatchups()
        } else {
            emptyMap()
        }

        lobby.players.forEach { (playerId, playerState) ->
            playerState.identity.currentGameSessionId = null

            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                // Find this player's next opponent
                val nextOpponentId = nextRoundMatchups[playerId]
                val nextOpponentName = if (nextOpponentId != null) {
                    lobby.players[nextOpponentId]?.identity?.playerName
                } else if (nextRoundMatchups.containsKey(playerId)) {
                    null // Player has a BYE
                } else {
                    null
                }
                val hasBye = nextRoundMatchups.containsKey(playerId) && nextOpponentId == null

                val roundComplete = ServerMessage.RoundComplete(
                    lobbyId = lobbyId,
                    round = round.roundNumber,
                    results = tournament.getCurrentRoundResults(),
                    standings = tournament.getStandingsInfo(connectedIds),
                    nextOpponentName = nextOpponentName,
                    nextRoundHasBye = hasBye,
                    isTournamentComplete = tournament.isComplete
                )
                sender.send(ws, roundComplete)
            }
        }

        // If tournament is complete, don't wait for ready - just finish
        if (tournament.isComplete) {
            completeTournament(lobbyId)
        }
        // Otherwise, wait for all players to signal ready before starting next round
    }

    /**
     * Handle a player signaling they are ready for the next round.
     */
    fun handleReadyForNextRound(session: WebSocketSession) {
        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Not in a lobby")
            return
        }

        val lobby = lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Lobby not found")
            return
        }

        val tournament = lobbyRepository.findTournamentById(lobbyId)
        if (tournament == null || tournament.isComplete) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Tournament not active")
            return
        }

        // Mark player as ready
        val wasNewlyReady = lobby.markPlayerReady(identity.playerId)
        if (!wasNewlyReady) {
            return // Already marked ready
        }

        logger.info("Player ${identity.playerName} ready for next round in tournament $lobbyId")

        // Broadcast ready status to all players
        val connectedPlayers = lobby.players.values.filter { it.identity.isConnected }
        val readyPlayerIds = lobby.getReadyPlayerIds().map { it.value }

        val readyMessage = ServerMessage.PlayerReadyForRound(
            lobbyId = lobbyId,
            playerId = identity.playerId.value,
            playerName = identity.playerName,
            readyPlayerIds = readyPlayerIds,
            totalConnectedPlayers = connectedPlayers.size
        )

        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, readyMessage)
            }
        }

        // If all players are ready, start the next round
        if (lobby.areAllPlayersReady()) {
            logger.info("All players ready, starting next round for tournament $lobbyId")
            startNextTournamentRound(lobbyId)
        }
    }

    private fun completeTournament(lobbyId: String) {
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = lobbyRepository.findTournamentById(lobbyId) ?: return

        logger.info("Tournament complete for lobby $lobbyId")
        lobby.completeTournament()

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        val message = ServerMessage.TournamentComplete(
            lobbyId = lobbyId,
            finalStandings = tournament.getStandingsInfo(connectedIds)
        )

        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, message)
            }
        }
    }

    // =========================================================================
    // Spectating
    // =========================================================================

    private fun handleSpectateGame(session: WebSocketSession, message: ClientMessage.SpectateGame) {
        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Player session not found")
            return
        }

        // Find the game session
        val gameSession = gameRepository.findById(message.gameSessionId)
        if (gameSession == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found")
            return
        }

        // Add as spectator
        gameSession.addSpectator(playerSession)

        // Track that this player is spectating
        identity.currentSpectatingGameId = message.gameSessionId

        val playerNames = gameSession.getPlayerNames()
        if (playerNames != null) {
            sender.send(session, ServerMessage.SpectatingStarted(
                gameSessionId = message.gameSessionId,
                player1Name = playerNames.first,
                player2Name = playerNames.second
            ))
        }

        // Send current game state
        val spectatorState = gameSession.buildSpectatorState()
        if (spectatorState != null) {
            sender.send(session, spectatorState)
        }

        logger.info("Player ${identity.playerName} started spectating game ${message.gameSessionId}")
    }

    private fun handleStopSpectating(session: WebSocketSession) {
        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            return
        }

        val gameSessionId = identity.currentSpectatingGameId
        if (gameSessionId != null) {
            val gameSession = gameRepository.findById(gameSessionId)
            gameSession?.removeSpectator(playerSession)
            identity.currentSpectatingGameId = null

            logger.info("Player ${identity.playerName} stopped spectating game $gameSessionId")
        }

        sender.send(session, ServerMessage.SpectatingStopped)

        // Send active matches again so they can see the overview
        sendActiveMatchesToPlayer(identity, session)
    }

    /**
     * Send active matches info to a player (for bye screen).
     */
    fun sendActiveMatchesToPlayer(identity: PlayerIdentity, session: WebSocketSession) {
        val lobbyId = identity.currentLobbyId ?: return
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = lobbyRepository.findTournamentById(lobbyId) ?: return

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        val activeMatches = buildActiveMatchesList(tournament)

        sender.send(session, ServerMessage.ActiveMatches(
            lobbyId = lobbyId,
            round = tournament.currentRound?.roundNumber ?: 0,
            matches = activeMatches,
            standings = tournament.getStandingsInfo(connectedIds)
        ))
    }

    /**
     * Build a list of active matches for spectating.
     */
    private fun buildActiveMatchesList(tournament: TournamentManager): List<ServerMessage.ActiveMatchInfo> {
        val matches = tournament.getCurrentRoundGameMatches()
        return matches.mapNotNull { match ->
            val gameSessionId = match.gameSessionId ?: return@mapNotNull null
            val gameSession = gameRepository.findById(gameSessionId) ?: return@mapNotNull null

            val playerNames = gameSession.getPlayerNames() ?: return@mapNotNull null
            val lifeTotals = gameSession.getLifeTotals() ?: return@mapNotNull null

            ServerMessage.ActiveMatchInfo(
                gameSessionId = gameSessionId,
                player1Name = playerNames.first,
                player2Name = playerNames.second,
                player1Life = lifeTotals.first,
                player2Life = lifeTotals.second
            )
        }
    }

    /**
     * Broadcast spectator state update to all spectators of a game.
     */
    fun broadcastSpectatorUpdate(gameSession: GameSession) {
        val spectatorState = gameSession.buildSpectatorState() ?: return

        for (spectator in gameSession.getSpectators()) {
            val ws = spectator.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, spectatorState)
            }
        }
    }
}
