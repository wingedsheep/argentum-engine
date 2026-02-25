package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.handler.ConnectionHandler.Companion.cardToSealedCardInfo
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.lobby.PickResult
import com.wingedsheep.gameserver.lobby.TournamentFormat
import com.wingedsheep.gameserver.lobby.TournamentLobby
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
import com.wingedsheep.gameserver.tournament.TournamentMatch
import com.wingedsheep.gameserver.tournament.TournamentRound
import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.model.EntityId
import kotlinx.coroutines.*
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
    private val gamePlayHandler: GamePlayHandler,
    private val gameProperties: GameProperties,
    private val boosterGenerator: BoosterGenerator
) {
    private val logger = LoggerFactory.getLogger(LobbyHandler::class.java)

    @Volatile
    var waitingSealedSession: SealedSession? = null

    /** Coroutine scope for draft timers */
    private val draftScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Per-lobby locks for round advancement to prevent concurrent ready/round-complete races */
    private val roundLocks = java.util.concurrent.ConcurrentHashMap<String, Any>()

    fun handle(session: WebSocketSession, message: ClientMessage) {
        when (message) {
            is ClientMessage.CreateSealedGame -> handleCreateSealedGame(session, message)
            is ClientMessage.JoinSealedGame -> handleJoinSealedGame(session, message)
            is ClientMessage.SubmitSealedDeck -> handleSubmitSealedDeck(session, message)
            is ClientMessage.UnsubmitDeck -> handleUnsubmitDeck(session)
            is ClientMessage.CreateTournamentLobby -> handleCreateTournamentLobby(session, message)
            is ClientMessage.JoinLobby -> handleJoinLobby(session, message)
            is ClientMessage.StartTournamentLobby -> handleStartTournamentLobby(session)
            is ClientMessage.MakePick -> handleMakePick(session, message)
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

        if (message.setCodes.isEmpty()) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "At least one set code is required")
            return
        }

        // Validate all set codes
        val setConfigs = message.setCodes.map { setCode ->
            boosterGenerator.getSetConfig(setCode) ?: run {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Unknown set code: $setCode")
                return
            }
        }

        val sealedSession = SealedSession(
            setCodes = setConfigs.map { it.setCode },
            setNames = setConfigs.map { it.setName },
            boosterGenerator = boosterGenerator
        )
        sealedSession.addPlayer(playerSession)

        lobbyRepository.saveSealedSession(sealedSession)
        waitingSealedSession = sealedSession

        logger.info("Sealed game created: ${sealedSession.sessionId} by ${playerSession.playerName} (sets: ${setConfigs.map { it.setName }})")
        sender.send(session, ServerMessage.SealedGameCreated(
            sessionId = sealedSession.sessionId,
            setCodes = sealedSession.setCodes,
            setNames = sealedSession.setNames
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
                    setCodes = sealedSession.setCodes,
                    setNames = sealedSession.setNames,
                    cardPool = poolInfos,
                    basicLands = basicLandInfos
                )
            )
        }
    }

    private fun startGameFromSealed(sealedSession: SealedSession) {
        logger.info("Starting game from sealed session: ${sealedSession.sessionId}")
        val gameSession = GameSession(
            cardRegistry = cardRegistry,
            useHandSmoother = gameProperties.handSmoother.enabled
        )

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

    private fun handleCreateTournamentLobby(session: WebSocketSession, message: ClientMessage.CreateTournamentLobby) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        // Validate all set codes
        if (message.setCodes.isEmpty()) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "At least one set code is required")
            return
        }
        val setConfigs = message.setCodes.mapNotNull { boosterGenerator.getSetConfig(it) }
        if (setConfigs.size != message.setCodes.size) {
            val invalidCodes = message.setCodes.filter { boosterGenerator.getSetConfig(it) == null }
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Unknown set codes: ${invalidCodes.joinToString()}")
            return
        }

        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Identity not found")
            return
        }

        // Leave current lobby if in one
        leaveCurrentLobbyIfPresent(identity)

        val format = try {
            TournamentFormat.valueOf(message.format.uppercase())
        } catch (e: IllegalArgumentException) {
            TournamentFormat.SEALED
        }

        // Set appropriate default booster count based on format
        // Draft: default 3 packs, max 6
        // Sealed: default 6 boosters, max 16
        val boosterCount = if (format == TournamentFormat.DRAFT) {
            if (message.boosterCount == 6) 3 else message.boosterCount.coerceIn(1, 6)  // 6 is the client default, use 3 for draft
        } else {
            message.boosterCount.coerceIn(1, 16)
        }

        val lobby = TournamentLobby(
            setCodes = setConfigs.map { it.setCode },
            setNames = setConfigs.map { it.setName },
            boosterGenerator = boosterGenerator,
            format = format,
            boosterCount = boosterCount,
            maxPlayers = message.maxPlayers.coerceIn(2, 8),
            pickTimeSeconds = message.pickTimeSeconds.coerceIn(15, 120)
        )
        lobby.addPlayer(identity)
        lobbyRepository.saveLobby(lobby)

        val setNamesStr = setConfigs.joinToString(", ") { it.setName }
        logger.info("Tournament lobby created: ${lobby.lobbyId} by ${identity.playerName} (sets: $setNamesStr, format: ${format.name})")
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

        // Check if player was in this tournament and can rejoin
        if (lobby.wasPlayerInTournament(identity.playerId)) {
            handleTournamentRejoin(session, identity, lobby)
            return
        }

        // Normal join logic for new players
        if (lobby.state != LobbyState.WAITING_FOR_PLAYERS) {
            // Tournament already started — join as spectator
            handleSpectatorJoin(session, identity, lobby)
            return
        }

        if (lobby.isFull) {
            sender.sendError(session, ErrorCode.GAME_FULL, "Lobby is full")
            return
        }

        // Leave current lobby if in one
        leaveCurrentLobbyIfPresent(identity)

        lobby.addPlayer(identity)
        logger.info("Player ${identity.playerName} joined lobby ${lobby.lobbyId}")
        broadcastLobbyUpdate(lobby)
        lobbyRepository.saveLobby(lobby)
    }

    /**
     * Find a lobby by ID for reconnection purposes.
     */
    fun findLobbyForReconnect(lobbyId: String): TournamentLobby? {
        return lobbyRepository.findLobbyById(lobbyId)
    }

    /**
     * Handle a player rejoining a tournament they were previously in.
     */
    private fun handleTournamentRejoin(
        session: WebSocketSession,
        identity: PlayerIdentity,
        lobby: TournamentLobby
    ) {
        // Leave any other lobby first (but not the one we're rejoining)
        if (identity.currentLobbyId != lobby.lobbyId) {
            leaveCurrentLobbyIfPresent(identity)
        }

        // Rejoin the tournament
        if (!lobby.rejoinPlayer(identity)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Failed to rejoin tournament")
            return
        }

        logger.info("Player ${identity.playerName} rejoined tournament ${lobby.lobbyId}")

        val playerSession = sessionRegistry.getPlayerSession(session.id)
        sendLobbyReconnectionState(session, identity, playerSession, lobby)
    }

    /**
     * Send lobby/tournament state to a reconnecting player.
     * Called from both handleTournamentRejoin (JoinLobby flow) and ConnectionHandler (reconnect flow).
     */
    fun sendLobbyReconnectionState(
        session: WebSocketSession,
        identity: PlayerIdentity,
        playerSession: PlayerSession?,
        lobby: TournamentLobby
    ) {
        when (lobby.state) {
            LobbyState.WAITING_FOR_PLAYERS -> {
                broadcastLobbyUpdate(lobby)
            }
            LobbyState.DRAFTING -> {
                sender.send(session, lobby.buildLobbyUpdate(identity.playerId))
                val playerState = lobby.players[identity.playerId]
                if (playerState?.currentPack != null) {
                    sender.send(session, ServerMessage.DraftPackReceived(
                        packNumber = lobby.currentPackNumber,
                        pickNumber = lobby.currentPickNumber,
                        cards = playerState.currentPack!!.map { cardToSealedCardInfo(it) },
                        timeRemainingSeconds = lobby.pickTimeRemaining,
                        passDirection = lobby.getPassDirection().name,
                        picksPerRound = minOf(lobby.picksPerRound, playerState.currentPack!!.size),
                        pickedCards = playerState.cardPool.map { cardToSealedCardInfo(it) }
                    ))
                }
            }
            LobbyState.DECK_BUILDING -> {
                val playerState = lobby.players[identity.playerId]
                val basicLandInfos = lobby.basicLands.values.map { cardToSealedCardInfo(it) }
                val poolInfos = playerState?.cardPool?.map { cardToSealedCardInfo(it) } ?: emptyList()

                sender.send(session, ServerMessage.SealedPoolGenerated(
                    setCodes = lobby.setCodes,
                    setNames = lobby.setNames,
                    cardPool = poolInfos,
                    basicLands = basicLandInfos
                ))
                broadcastLobbyUpdate(lobby)

                // If this player already submitted their deck, restore tournament state
                if (playerState?.hasSubmittedDeck == true) {
                    val tournament = lobbyRepository.findTournamentById(lobby.lobbyId)
                    if (tournament != null) {
                        sendTournamentStartedToPlayer(lobby, tournament, identity)
                    }
                }
            }
            LobbyState.TOURNAMENT_ACTIVE -> {
                sendTournamentActiveState(session, identity, playerSession, lobby)
            }
            LobbyState.TOURNAMENT_COMPLETE -> {
                val tournament = lobbyRepository.findTournamentById(lobby.lobbyId)
                if (tournament != null) {
                    val connectedIds = lobby.players.values
                        .filter { it.identity.isConnected }
                        .map { it.identity.playerId }
                        .toSet()

                    sender.send(session, ServerMessage.TournamentComplete(
                        lobbyId = lobby.lobbyId,
                        finalStandings = tournament.getStandingsInfo(connectedIds)
                    ))
                }
            }
        }
    }

    /**
     * Send full tournament state to a reconnecting player during TOURNAMENT_ACTIVE.
     */
    private fun sendTournamentActiveState(
        session: WebSocketSession,
        identity: PlayerIdentity,
        playerSession: PlayerSession?,
        lobby: TournamentLobby
    ) {
        // Send card pool so client can edit deck if needed (before first match)
        val playerState = lobby.players[identity.playerId]
        val basicLandInfos = lobby.basicLands.values.map { cardToSealedCardInfo(it) }
        val poolInfos = playerState?.cardPool?.map { cardToSealedCardInfo(it) } ?: emptyList()
        sender.send(session, ServerMessage.SealedPoolGenerated(
            setCodes = lobby.setCodes,
            setNames = lobby.setNames,
            cardPool = poolInfos,
            basicLands = basicLandInfos
        ))
        sender.send(session, lobby.buildLobbyUpdate(identity.playerId))

        val tournament = lobbyRepository.findTournamentById(lobby.lobbyId) ?: return
        sendTournamentStartedToPlayer(lobby, tournament, identity)

        val currentRound = tournament.currentRound
        val playerMatch = currentRound?.matches?.find {
            it.player1Id == identity.playerId || it.player2Id == identity.playerId
        }

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        // Find this player's active game (in progress across any round)
        val activeMatch = rounds@ run {
            for (round in tournament.getRoundsForPersistence()) {
                for (match in round.matches) {
                    if ((match.player1Id == identity.playerId || match.player2Id == identity.playerId) &&
                        match.gameSessionId != null && !match.isComplete) {
                        return@run round to match
                    }
                }
            }
            null
        }

        when {
            // Before first round starts, waiting for players to ready up
            currentRound == null -> {
                // TournamentStarted + ready status already sent by sendTournamentStartedToPlayer
            }

            // Player has an active game in progress (across any round)
            activeMatch != null -> {
                val (matchRound, activePlayerMatch) = activeMatch
                val gs = gameRepository.findById(activePlayerMatch.gameSessionId!!)
                if (gs != null && gs.isStarted && !gs.isGameOver() && playerSession != null) {
                    identity.currentGameSessionId = activePlayerMatch.gameSessionId
                    playerSession.currentGameSessionId = activePlayerMatch.gameSessionId
                    val opponentId = if (activePlayerMatch.player1Id == identity.playerId) activePlayerMatch.player2Id else activePlayerMatch.player1Id
                    val opponentName = lobby.players[opponentId]?.identity?.playerName ?: "Unknown"
                    sender.send(session, ServerMessage.TournamentMatchStarting(
                        lobbyId = lobby.lobbyId,
                        round = matchRound.roundNumber,
                        gameSessionId = activePlayerMatch.gameSessionId!!,
                        opponentName = opponentName
                    ))
                    if (gs.getPlayerSession(identity.playerId) != null) {
                        gs.removePlayer(identity.playerId)
                    }
                    gs.associatePlayer(playerSession)
                    when {
                        gs.isAwaitingBottomCards(identity.playerId) -> {
                            val hand = gs.getHand(identity.playerId)
                            val cardsToBottom = gs.getCardsToBottom(identity.playerId)
                            sender.send(session, ServerMessage.ChooseBottomCards(hand, cardsToBottom))
                        }
                        gs.isMulliganPhase && !gs.hasMulliganComplete(identity.playerId) -> {
                            val decision = gs.getMulliganDecision(identity.playerId)
                            sender.send(session, decision)
                        }
                        gs.isMulliganPhase -> {
                            sender.send(session, ServerMessage.WaitingForOpponentMulligan)
                        }
                        else -> {
                            gamePlayHandler.broadcastStateUpdate(gs, emptyList())
                        }
                    }
                }
            }

            // Player has a bye this round
            playerMatch?.isBye == true -> {
                sender.send(session, ServerMessage.TournamentBye(
                    lobbyId = lobby.lobbyId,
                    round = currentRound.roundNumber
                ))
                val spectatingGameId = identity.currentSpectatingGameId
                if (spectatingGameId != null && playerSession != null) {
                    restoreSpectating(identity, playerSession, session, spectatingGameId)
                } else {
                    sendActiveMatchesToPlayer(identity, session)
                }
            }

            // Player's match hasn't started yet (waiting for opponent to ready up)
            playerMatch?.gameSessionId == null && playerMatch?.isBye == false -> {
                // Send MatchComplete-style info so they can ready up for their next match
                val nextMatch = tournament.getNextMatchForPlayer(identity.playerId)
                if (nextMatch != null) {
                    val (nextRound, nm) = nextMatch
                    val opponentId = if (nm.player1Id == identity.playerId) nm.player2Id else nm.player1Id
                    val nextOpponentName = opponentId?.let { lobby.players[it]?.identity?.playerName }
                    sender.send(session, ServerMessage.MatchComplete(
                        lobbyId = lobby.lobbyId,
                        round = nextRound.roundNumber,
                        results = emptyList(),
                        standings = tournament.getStandingsInfo(connectedIds),
                        nextOpponentName = nextOpponentName,
                        nextRoundHasBye = nm.isBye,
                        isTournamentComplete = false
                    ))
                }
                // Send ready status
                val readyPlayerIds = lobby.getReadyPlayerIds()
                if (readyPlayerIds.isNotEmpty()) {
                    sender.send(session, ServerMessage.PlayerReadyForRound(
                        lobbyId = lobby.lobbyId,
                        playerId = identity.playerId.value,
                        playerName = identity.playerName,
                        readyPlayerIds = readyPlayerIds.map { it.value },
                        totalConnectedPlayers = connectedIds.size
                    ))
                }
            }

            // Player's match is complete — with dynamic matchmaking, show next opponent
            playerMatch?.isComplete == true -> {
                // Send MatchComplete with next opponent info so they can ready up
                val nextMatch = tournament.getNextMatchForPlayer(identity.playerId)
                if (nextMatch != null) {
                    val (nextRound, nm) = nextMatch
                    val opponentId = if (nm.player1Id == identity.playerId) nm.player2Id else nm.player1Id
                    val nextOpponentName = opponentId?.let { lobby.players[it]?.identity?.playerName }
                    sender.send(session, ServerMessage.MatchComplete(
                        lobbyId = lobby.lobbyId,
                        round = currentRound.roundNumber,
                        results = tournament.getCurrentRoundResults(),
                        standings = tournament.getStandingsInfo(connectedIds),
                        nextOpponentName = nextOpponentName,
                        nextRoundHasBye = nm.isBye,
                        isTournamentComplete = false
                    ))
                } else if (tournament.isComplete) {
                    // Tournament is done
                    sender.send(session, ServerMessage.RoundComplete(
                        lobbyId = lobby.lobbyId,
                        round = currentRound.roundNumber,
                        results = tournament.getCurrentRoundResults(),
                        standings = tournament.getStandingsInfo(connectedIds),
                        isTournamentComplete = true
                    ))
                } else {
                    // Waiting for others or spectating
                    val spectatingGameId = identity.currentSpectatingGameId
                    if (spectatingGameId != null && playerSession != null) {
                        restoreSpectating(identity, playerSession, session, spectatingGameId)
                    } else {
                        sendActiveMatchesToPlayer(identity, session)
                    }
                }

                // Send ready status
                val readyPlayerIds = lobby.getReadyPlayerIds()
                if (readyPlayerIds.isNotEmpty()) {
                    sender.send(session, ServerMessage.PlayerReadyForRound(
                        lobbyId = lobby.lobbyId,
                        playerId = identity.playerId.value,
                        playerName = identity.playerName,
                        readyPlayerIds = readyPlayerIds.map { it.value },
                        totalConnectedPlayers = connectedIds.size
                    ))
                }
            }
        }
    }

    /**
     * Handle a non-participant joining an active/complete tournament as a spectator.
     */
    private fun handleSpectatorJoin(
        session: WebSocketSession,
        identity: PlayerIdentity,
        lobby: TournamentLobby
    ) {
        // Leave any other lobby first
        leaveCurrentLobbyIfPresent(identity)

        // Add as tournament-level spectator
        lobby.addSpectator(identity)

        logger.info("Player ${identity.playerName} joined tournament ${lobby.lobbyId} as spectator")

        // Send lobby update so client shows lobby/tournament UI
        sender.send(session, lobby.buildLobbyUpdate(identity.playerId))

        // Send tournament state based on lobby phase
        val tournament = lobbyRepository.findTournamentById(lobby.lobbyId)
        if (tournament != null) {
            val connectedIds = lobby.players.values
                .filter { it.identity.isConnected }
                .map { it.identity.playerId }
                .toSet()

            when (lobby.state) {
                LobbyState.TOURNAMENT_ACTIVE -> {
                    sender.send(session, ServerMessage.TournamentStarted(
                        lobbyId = lobby.lobbyId,
                        totalRounds = tournament.totalRounds,
                        standings = tournament.getStandingsInfo(connectedIds)
                    ))
                    // Send active matches so they can spectate
                    sendActiveMatchesToPlayer(identity, session)
                }
                LobbyState.TOURNAMENT_COMPLETE -> {
                    sender.send(session, ServerMessage.TournamentComplete(
                        lobbyId = lobby.lobbyId,
                        finalStandings = tournament.getStandingsInfo(connectedIds)
                    ))
                }
                else -> {
                    // DECK_BUILDING or DRAFTING — still show standings
                    sender.send(session, ServerMessage.TournamentStarted(
                        lobbyId = lobby.lobbyId,
                        totalRounds = tournament.totalRounds,
                        standings = tournament.getStandingsInfo(connectedIds)
                    ))
                }
            }
        }
    }

    private fun handleStartTournamentLobby(session: WebSocketSession) {
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

        when (lobby.format) {
            TournamentFormat.SEALED -> {
                val started = lobby.startDeckBuilding(identity.playerId)
                if (!started) {
                    sender.sendError(session, ErrorCode.INVALID_ACTION, "Failed to start lobby")
                    return
                }

                logger.info("Lobby ${lobby.lobbyId} started deck building - sealed (${lobby.playerCount} players)")

                val basicLandInfos = lobby.basicLands.values.map { cardToSealedCardInfo(it) }
                lobby.players.forEach { (_, playerState) ->
                    val poolInfos = playerState.cardPool.map { cardToSealedCardInfo(it) }
                    val ws = playerState.identity.webSocketSession
                    if (ws != null) {
                        sender.send(ws, ServerMessage.SealedPoolGenerated(
                            setCodes = lobby.setCodes,
                            setNames = lobby.setNames,
                            cardPool = poolInfos,
                            basicLands = basicLandInfos
                        ))
                    }
                }
            }

            TournamentFormat.DRAFT -> {
                val started = lobby.startDraft(identity.playerId)
                if (!started) {
                    sender.sendError(session, ErrorCode.INVALID_ACTION, "Failed to start draft")
                    return
                }

                logger.info("Lobby ${lobby.lobbyId} started drafting (${lobby.playerCount} players)")

                // Send first packs to all players
                broadcastDraftPacks(lobby)

                // Start the pick timer
                startDraftTimer(lobby)
            }
        }

        broadcastLobbyUpdate(lobby)
        lobbyRepository.saveLobby(lobby)
    }

    /**
     * Handle a player making a pick during draft.
     */
    private fun handleMakePick(session: WebSocketSession, message: ClientMessage.MakePick) {
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

        if (lobby.state != LobbyState.DRAFTING) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Not in drafting phase")
            return
        }

        val result = lobby.makePick(identity.playerId, message.cardNames)
        when (result) {
            is PickResult.Success -> {
                val pickedNames = result.pickedCards.map { it.name }
                logger.info("Player ${identity.playerName} picked ${pickedNames.joinToString(", ")} (${result.totalPicked} total)")

                // Send confirmation to the player who picked
                sender.send(session, ServerMessage.DraftPickConfirmed(
                    cardNames = pickedNames,
                    totalPicked = result.totalPicked
                ))

                // Broadcast to all players who is still waiting
                broadcastDraftPickMade(lobby, identity, result.waitingForPlayers)
                lobbyRepository.saveLobby(lobby)

                // Check if all players have picked
                if (lobby.allPlayersPicked()) {
                    processDraftRound(lobby)
                }
            }
            is PickResult.Error -> {
                sender.sendError(session, ErrorCode.INVALID_ACTION, result.message)
            }
        }
    }

    /**
     * Process end of a draft pick round - pass packs and continue or finish.
     */
    private fun processDraftRound(lobby: TournamentLobby) {
        // Cancel the current timer
        lobby.pickTimerJob?.cancel()
        lobby.pickTimerJob = null

        // Pass packs
        val continuesDraft = lobby.passPacks()

        if (continuesDraft) {
            // Continue drafting - send new packs
            broadcastDraftPacks(lobby)
            lobbyRepository.saveLobby(lobby)
            startDraftTimer(lobby)
        } else {
            // Draft complete - transition to deck building
            logger.info("Draft complete for lobby ${lobby.lobbyId}, transitioning to deck building")

            val basicLandInfos = lobby.basicLands.values.map { cardToSealedCardInfo(it) }

            lobby.players.forEach { (_, playerState) ->
                val poolInfos = playerState.cardPool.map { cardToSealedCardInfo(it) }
                val ws = playerState.identity.webSocketSession
                if (ws != null && ws.isOpen) {
                    sender.send(ws, ServerMessage.DraftComplete(
                        pickedCards = poolInfos,
                        basicLands = basicLandInfos
                    ))
                }
            }

            broadcastLobbyUpdate(lobby)
            lobbyRepository.saveLobby(lobby)
        }
    }

    /**
     * Broadcast current draft packs to all players.
     */
    private fun broadcastDraftPacks(lobby: TournamentLobby) {
        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            val pack = playerState.currentPack
            if (ws != null && ws.isOpen && pack != null) {
                sender.send(ws, ServerMessage.DraftPackReceived(
                    packNumber = lobby.currentPackNumber,
                    pickNumber = lobby.currentPickNumber,
                    cards = pack.map { cardToSealedCardInfo(it) },
                    timeRemainingSeconds = lobby.pickTimeSeconds,
                    passDirection = lobby.getPassDirection().name,
                    picksPerRound = minOf(lobby.picksPerRound, pack.size),
                    pickedCards = playerState.cardPool.map { cardToSealedCardInfo(it) }
                ))
            }
        }
    }

    /**
     * Broadcast that a player made a pick.
     */
    private fun broadcastDraftPickMade(lobby: TournamentLobby, picker: PlayerIdentity, waitingForPlayers: List<String>) {
        val message = ServerMessage.DraftPickMade(
            playerId = picker.playerId.value,
            playerName = picker.playerName,
            waitingForPlayers = waitingForPlayers
        )

        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, message)
            }
        }
    }

    /**
     * Start the draft pick timer.
     */
    private fun startDraftTimer(lobby: TournamentLobby) {
        lobby.pickTimeRemaining = lobby.pickTimeSeconds

        lobby.pickTimerJob = draftScope.launch {
            var remaining = lobby.pickTimeSeconds

            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                lobby.pickTimeRemaining = remaining

                // Broadcast timer update every second
                broadcastTimerUpdate(lobby, remaining)
            }

            // Timer expired - auto-pick for players who haven't picked
            if (isActive && lobby.state == LobbyState.DRAFTING) {
                autoPickForSlowPlayers(lobby)
            }
        }
    }

    /**
     * Broadcast timer update to all players.
     */
    private fun broadcastTimerUpdate(lobby: TournamentLobby, secondsRemaining: Int) {
        val message = ServerMessage.DraftTimerUpdate(secondsRemaining)

        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, message)
            }
        }
    }

    /**
     * Auto-pick for players who haven't made a selection when timer expires.
     */
    private fun autoPickForSlowPlayers(lobby: TournamentLobby) {
        val slowPlayers = lobby.getPlayersWaitingToPick()

        for (playerId in slowPlayers) {
            val result = lobby.autoPickFirstCards(playerId)
            if (result is PickResult.Success) {
                val playerState = lobby.players[playerId]
                val ws = playerState?.identity?.webSocketSession
                val pickedNames = result.pickedCards.map { it.name }
                if (ws != null && ws.isOpen) {
                    sender.send(ws, ServerMessage.DraftPickConfirmed(
                        cardNames = pickedNames,
                        totalPicked = result.totalPicked
                    ))
                }
                logger.info("Auto-picked ${pickedNames.joinToString(", ")} for player ${playerState?.identity?.playerName} (timeout)")
            }
        }

        // Now process the round
        if (lobby.allPlayersPicked()) {
            processDraftRound(lobby)
        }
    }

    private fun handleLeaveLobby(session: WebSocketSession) {
        val token = sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { sessionRegistry.getIdentityByToken(it) }
        if (identity == null) return

        val lobbyId = identity.currentLobbyId ?: return
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return

        // If the player is a spectator, just remove from spectators
        if (lobby.isSpectator(identity.playerId)) {
            lobby.removeSpectator(identity.playerId)
            logger.info("Spectator ${identity.playerName} left lobby $lobbyId")
            return
        }

        // Use forceRemovePlayer for explicit leave - player cannot rejoin
        lobby.forceRemovePlayer(identity.playerId)
        identity.currentLobbyId = null

        logger.info("Player ${identity.playerName} left lobby $lobbyId (cannot rejoin)")

        if (lobby.playerCount == 0) {
            lobbyRepository.removeLobby(lobbyId)
            logger.info("Lobby $lobbyId removed (empty)")
        } else {
            broadcastLobbyUpdate(lobby)
        }
    }

    /**
     * Helper to leave current lobby if the player is in one.
     * Used when creating/joining a new lobby to auto-leave the old one.
     */
    private fun leaveCurrentLobbyIfPresent(identity: PlayerIdentity) {
        val lobbyId = identity.currentLobbyId ?: return
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return

        lobby.removePlayer(identity.playerId)
        identity.currentLobbyId = null

        logger.info("Player ${identity.playerName} auto-left lobby $lobbyId")

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

        // Can only stop during WAITING_FOR_PLAYERS, DRAFTING, or DECK_BUILDING (not during active tournament)
        if (lobby.state == LobbyState.TOURNAMENT_ACTIVE || lobby.state == LobbyState.TOURNAMENT_COMPLETE) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Cannot stop lobby during tournament")
            return
        }

        logger.info("Host ${identity.playerName} stopped lobby $lobbyId")

        // Cancel the pick timer if we're in drafting
        lobby.pickTimerJob?.cancel()
        lobby.pickTimerJob = null

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

        // Check if player's match has already started (can't unsubmit after match starts)
        val tournament = lobbyRepository.findTournamentById(lobbyId)
        if (tournament != null) {
            val match = tournament.getPlayerMatchInCurrentRound(identity.playerId)
            if (match != null && match.gameSessionId != null) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Cannot edit deck - match already started")
                return
            }
        }

        val success = lobby.unsubmitDeck(identity.playerId)
        if (!success) {
            val playerState = lobby.players[identity.playerId]
            logger.warn("Failed to unsubmit deck for player ${identity.playerName} (${identity.playerId.value}): " +
                "lobbyState=${lobby.state}, playerFound=${playerState != null}, hasSubmittedDeck=${playerState?.hasSubmittedDeck}")
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Cannot unsubmit deck")
            return
        }

        logger.info("Player ${identity.playerName} unsubmitted deck in lobby $lobbyId")

        // Notify all players of the updated lobby state
        broadcastLobbyUpdate(lobby)

        // If in tournament, broadcast the updated ready status
        if (tournament != null) {
            broadcastReadyStatus(lobby, identity)
        }
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

        // Update sets if provided (can be empty to disable start)
        message.setCodes?.let { newSetCodes ->
            // Allow empty setCodes to disable start button (but won't be able to start)
            if (newSetCodes.isNotEmpty() && !lobby.updateSets(newSetCodes)) {
                val invalidCodes = newSetCodes.filter { boosterGenerator.getSetConfig(it) == null }
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Invalid set codes: ${invalidCodes.joinToString()}")
                return
            }
            if (newSetCodes.isEmpty()) {
                lobby.setCodes = emptyList()
                lobby.setNames = emptyList()
            }
        }

        // Update format if provided
        message.format?.let { formatStr ->
            val newFormat = try {
                TournamentFormat.valueOf(formatStr)
            } catch (e: IllegalArgumentException) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Invalid format: $formatStr")
                return
            }
            // When switching formats, adjust boosterCount to appropriate default
            if (newFormat != lobby.format) {
                lobby.format = newFormat
                lobby.boosterCount = if (newFormat == TournamentFormat.DRAFT) 3 else 6
            }
        }

        // Manual boosterCount override (apply after format change)
        message.boosterCount?.let {
            val maxCount = if (lobby.format == TournamentFormat.DRAFT) 6 else 16
            lobby.boosterCount = it.coerceIn(1, maxCount)
        }
        message.maxPlayers?.let { lobby.maxPlayers = it.coerceIn(2, 8) }
        message.gamesPerMatch?.let { lobby.gamesPerMatch = it.coerceIn(1, 5) }
        message.pickTimeSeconds?.let { lobby.pickTimeSeconds = it.coerceIn(15, 180) }
        message.picksPerRound?.let { lobby.picksPerRound = it.coerceIn(1, 2) }

        broadcastLobbyUpdate(lobby)
        lobbyRepository.saveLobby(lobby)
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
            is TournamentLobby.DeckSubmissionResult.Success -> {
                val deckSize = deckList.values.sum()
                logger.info("Player ${identity.playerName} submitted deck ($deckSize cards) in lobby $lobbyId")
                sender.send(session, ServerMessage.DeckSubmitted(deckSize))
                broadcastLobbyUpdate(lobby)

                // Ensure tournament is created (for matchup info)
                val tournament = ensureTournamentCreated(lobby)

                // Send TournamentStarted to this player (they can now ready up for round 1)
                sendTournamentStartedToPlayer(lobby, tournament, identity)

                // NOTE: Don't auto-start matches - require players to press Ready
                // This allows them to return to deck building while waiting

                // Transition lobby state when all decks are submitted
                if (result.allReady && lobby.state == LobbyState.DECK_BUILDING) {
                    lobby.activateTournament()
                }

                lobbyRepository.saveLobby(lobby)
            }
            is TournamentLobby.DeckSubmissionResult.Error -> {
                sender.sendError(session, ErrorCode.INVALID_DECK, result.message)
            }
        }
    }

    /**
     * Ensure the TournamentManager exists for a lobby.
     * Creates it if it doesn't exist yet.
     * Note: Does NOT transition lobby state - that still happens when all decks are submitted.
     * This allows reconnecting players to see deck building UI while matches may be starting.
     */
    private fun ensureTournamentCreated(lobby: TournamentLobby): TournamentManager {
        val lock = roundLocks.computeIfAbsent(lobby.lobbyId) { Any() }
        return synchronized(lock) {
            val existing = lobbyRepository.findTournamentById(lobby.lobbyId)
            if (existing != null) return@synchronized existing

            logger.info("Creating tournament for lobby ${lobby.lobbyId} with ${lobby.playerCount} players (early creation for matchups)")

            // Sort players by playerId for stable ordering regardless of ConcurrentHashMap iteration order
            val players = lobby.players.values
                .map { ps -> ps.identity.playerId to ps.identity.playerName }
                .sortedBy { it.first.value }

            val tournament = TournamentManager(lobby.lobbyId, players, lobby.gamesPerMatch)
            lobbyRepository.saveTournament(lobby.lobbyId, tournament)

            tournament
        }
    }

    /**
     * Send TournamentStarted message to a single player.
     */
    private fun sendTournamentStartedToPlayer(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        identity: PlayerIdentity
    ) {
        val ws = identity.webSocketSession ?: return

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        // Find this player's next match across all rounds (not just peeking at the next round)
        val nextMatch = tournament.getNextMatchForPlayer(identity.playerId)
        val nextOpponentName: String?
        val hasBye: Boolean

        if (nextMatch != null) {
            val (_, match) = nextMatch
            val opponentId = if (match.player1Id == identity.playerId) match.player2Id else match.player1Id
            nextOpponentName = opponentId?.let { lobby.players[it]?.identity?.playerName }
            hasBye = match.isBye
        } else {
            // No upcoming match — either haven't started or all done.
            // Fall back to peekNextRoundMatchups for the pre-first-round case
            val firstRoundMatchups = tournament.peekNextRoundMatchups()
            val nextOpponentId = firstRoundMatchups[identity.playerId]
            nextOpponentName = nextOpponentId?.let { lobby.players[it]?.identity?.playerName }
            hasBye = firstRoundMatchups.containsKey(identity.playerId) && nextOpponentId == null
        }

        sender.send(ws, ServerMessage.TournamentStarted(
            lobbyId = lobby.lobbyId,
            totalRounds = tournament.totalRounds,
            standings = tournament.getStandingsInfo(connectedIds),
            nextOpponentName = nextOpponentName,
            nextRoundHasBye = hasBye
        ))

        // Send current ready status so the client knows who is already ready
        // (TournamentStarted resets readyPlayerIds on the client)
        val readyPlayerIds = lobby.getReadyPlayerIds()
        if (readyPlayerIds.isNotEmpty()) {
            sender.send(ws, ServerMessage.PlayerReadyForRound(
                lobbyId = lobby.lobbyId,
                playerId = identity.playerId.value,
                playerName = identity.playerName,
                readyPlayerIds = readyPlayerIds.map { it.value },
                totalConnectedPlayers = connectedIds.size
            ))
        }
    }

    /**
     * Try to start a match after a player submits their deck.
     * Starts immediately if opponent has also submitted.
     */
    private fun tryStartMatchAfterDeckSubmit(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        identity: PlayerIdentity
    ) {
        // Prepare first round if needed
        if (tournament.currentRound == null) {
            val round = tournament.startNextRound()
            if (round == null) {
                completeTournament(lobby.lobbyId)
                return
            }
            logger.info("Prepared round ${round.roundNumber} for tournament ${lobby.lobbyId}")
        }

        val (round, match) = tournament.getNextMatchForPlayer(identity.playerId) ?: return

        // Handle BYE
        if (match.isBye) {
            match.isComplete = true
            val ws = identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, ServerMessage.TournamentBye(
                    lobbyId = lobby.lobbyId,
                    round = round.roundNumber
                ))
                sendActiveMatchesToPlayer(identity, ws)
            }
            lobbyRepository.saveTournament(lobby.lobbyId, tournament)
            return
        }

        // Already started?
        if (match.gameSessionId != null) return

        // Find opponent
        val opponentId = if (match.player1Id == identity.playerId) match.player2Id else match.player1Id
        if (opponentId == null) return

        // Opponent has submitted deck?
        val opponentState = lobby.players[opponentId] ?: return
        if (!opponentState.hasSubmittedDeck) return

        // Both have submitted - start the match!
        logger.info("Both players submitted decks, starting match: ${identity.playerName} vs ${opponentState.identity.playerName}")
        startSingleMatch(lobby, tournament, round, match)
    }

    fun broadcastLobbyUpdate(lobby: TournamentLobby) {
        lobby.players.forEach { (playerId, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, lobby.buildLobbyUpdate(playerId))
            }
        }
    }

    fun startTournament(lobby: TournamentLobby) {
        logger.info("Starting tournament for lobby ${lobby.lobbyId} with ${lobby.playerCount} players")
        lobby.startTournament()

        val players = lobby.players.values
            .map { ps -> ps.identity.playerId to ps.identity.playerName }
            .sortedBy { it.first.value }

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

    /**
     * Start the next tournament round.
     * This is now primarily used as a fallback when all players ready at once.
     * Normally, matches start eagerly via tryStartMatchForPlayer when both players are ready.
     */
    fun startNextTournamentRound(lobbyId: String) {
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = lobbyRepository.findTournamentById(lobbyId) ?: return

        // Prepare round if needed (might already be prepared by eager match starting)
        if (tournament.currentRound?.isComplete != false) {
            val round = tournament.startNextRound()
            if (round == null) {
                completeTournament(lobbyId)
                return
            }
            lobby.clearReadyState()
            lobbyRepository.saveLobby(lobby)
            lobbyRepository.saveTournament(lobbyId, tournament)
        }

        val round = tournament.currentRound ?: return
        logger.info("Starting round ${round.roundNumber} for tournament $lobbyId")

        // Start all un-started matches
        for (match in tournament.getCurrentRoundGameMatches()) {
            if (match.gameSessionId == null) {
                startSingleMatch(lobby, tournament, round, match)
            }
        }
        lobbyRepository.saveTournament(lobbyId, tournament)

        // Handle BYEs
        for (match in round.matches) {
            if (match.isBye) {
                val playerState = lobby.players[match.player1Id]
                val identity = playerState?.identity
                val ws = identity?.webSocketSession
                if (identity != null && ws != null && ws.isOpen) {
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

    /**
     * Handle a player abandoning the tournament. Records auto-losses for
     * incomplete matches and checks for round/tournament completion.
     *
     * All mutations happen under the per-lobby lock.
     */
    fun handleAbandon(lobbyId: String, playerId: EntityId) {
        val lock = roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            val tournament = lobbyRepository.findTournamentById(lobbyId) ?: return
            tournament.recordAbandon(playerId)
            lobbyRepository.saveTournament(lobbyId, tournament)

            broadcastActiveMatchesToWaitingPlayers(lobbyId)

            if (tournament.isRoundComplete()) {
                handleRoundComplete(lobbyId)
            }
        }
    }

    fun handleRoundComplete(lobbyId: String) {
        val lock = roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return
            val tournament = lobbyRepository.findTournamentById(lobbyId) ?: return
            val round = tournament.currentRound ?: return

            logger.info("Round ${round.roundNumber} complete for tournament $lobbyId")

            // Clear ready state so all players must re-ready for next round
            lobby.clearReadyState()

            val connectedIds = lobby.players.values
                .filter { it.identity.isConnected }
                .map { it.identity.playerId }
                .toSet()

            lobby.players.forEach { (playerId, playerState) ->
                playerState.identity.currentGameSessionId = null

                val ws = playerState.identity.webSocketSession
                if (ws != null && ws.isOpen) {
                    // Find this player's next opponent using dynamic matchmaking
                    val nextMatch = tournament.getNextMatchForPlayer(playerId)
                    val nextOpponentName: String?
                    val hasBye: Boolean

                    if (nextMatch != null) {
                        val (_, nm) = nextMatch
                        val opponentId = if (nm.player1Id == playerId) nm.player2Id else nm.player1Id
                        nextOpponentName = opponentId?.let { lobby.players[it]?.identity?.playerName }
                        hasBye = nm.isBye
                    } else {
                        nextOpponentName = null
                        hasBye = false
                    }

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

            // Also send round complete to tournament spectators
            for ((_, spectatorIdentity) in lobby.spectators) {
                val ws = spectatorIdentity.webSocketSession ?: continue
                if (!ws.isOpen) continue

                val roundComplete = ServerMessage.RoundComplete(
                    lobbyId = lobbyId,
                    round = round.roundNumber,
                    results = tournament.getCurrentRoundResults(),
                    standings = tournament.getStandingsInfo(connectedIds),
                    isTournamentComplete = tournament.isComplete
                )
                sender.send(ws, roundComplete)
            }

            // Persist round completion state
            lobbyRepository.saveLobby(lobby)
            lobbyRepository.saveTournament(lobbyId, tournament)

            // If tournament is complete, don't wait for ready - just finish
            if (tournament.isComplete) {
                completeTournament(lobbyId)
            }
            // Otherwise, wait for all players to signal ready before starting next round
        }
    }

    /**
     * Handle the full match result flow: report result, notify players, broadcast
     * active matches, and check for round completion — all under the per-lobby lock.
     *
     * This ensures that tournament state mutations from game completion don't race
     * with handleReadyForNextRound or handleRoundComplete.
     */
    fun handleMatchResult(
        lobbyId: String,
        gameSessionId: String,
        winnerId: EntityId?,
        winnerLifeRemaining: Int
    ) {
        val lock = roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            val tournament = lobbyRepository.findTournamentById(lobbyId) ?: return
            tournament.reportMatchResult(gameSessionId, winnerId, winnerLifeRemaining)
            lobbyRepository.saveTournament(lobbyId, tournament)

            handleMatchComplete(lobbyId, gameSessionId)
            broadcastActiveMatchesToWaitingPlayers(lobbyId)

            if (tournament.isRoundComplete()) {
                handleRoundComplete(lobbyId)
            }
        }
    }

    /**
     * Handle an individual match completion. Sends MatchComplete to both players
     * in the match immediately, allowing them to ready up for their next match
     * without waiting for the entire round to finish.
     *
     * Must be called under the per-lobby lock.
     */
    private fun handleMatchComplete(lobbyId: String, gameSessionId: String) {
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = lobbyRepository.findTournamentById(lobbyId) ?: return

        val completedRound = tournament.getRoundForMatch(gameSessionId) ?: return

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        // Find the two players who were in this match
        val match = completedRound.matches.find { it.gameSessionId == gameSessionId } ?: return
        val matchPlayerIds = listOfNotNull(match.player1Id, match.player2Id)

        for (playerId in matchPlayerIds) {
            val playerState = lobby.players[playerId] ?: continue
            val ws = playerState.identity.webSocketSession ?: continue
            if (!ws.isOpen) continue

            // Find this player's next match
            val nextMatch = tournament.getNextMatchForPlayer(playerId)
            val nextOpponentName: String?
            val hasBye: Boolean

            if (nextMatch != null) {
                val (_, nm) = nextMatch
                val opponentId = if (nm.player1Id == playerId) nm.player2Id else nm.player1Id
                nextOpponentName = opponentId?.let { lobby.players[it]?.identity?.playerName }
                hasBye = nm.isBye
            } else {
                nextOpponentName = null
                hasBye = false
            }

            sender.send(ws, ServerMessage.MatchComplete(
                lobbyId = lobbyId,
                round = completedRound.roundNumber,
                results = tournament.getRoundResults(completedRound),
                standings = tournament.getStandingsInfo(connectedIds),
                nextOpponentName = nextOpponentName,
                nextRoundHasBye = hasBye,
                isTournamentComplete = tournament.isComplete
            ))
        }

        lobbyRepository.saveTournament(lobbyId, tournament)
    }

    /**
     * Handle a player signaling they are ready for the next round.
     * Uses eager match starting: starts a match as soon as both players are ready,
     * rather than waiting for ALL players to be ready.
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

        // Spectators can't ready up
        if (lobby.isSpectator(identity.playerId)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Spectators cannot ready up")
            return
        }

        // Capture epoch before acquiring lock to detect stale ready requests.
        // If a round completes (clearing ready state) while we wait for the lock,
        // the epoch will have changed and this ready request should be discarded.
        val epochBeforeLock = lobby.readyEpoch

        // Synchronize per-lobby to prevent concurrent round advancement races.
        // Without this, two players sending ReadyForNextRound simultaneously could both
        // see needsPrepare==true, double-increment the round index, and clear each other's
        // ready state.
        val lock = roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            // If the epoch changed, a round completed while we were waiting — discard stale ready
            if (lobby.readyEpoch != epochBeforeLock) return
            // Prepare rounds as needed — advance through any rounds that haven't been
            // initialized yet, auto-completing BYEs along the way. With dynamic matchmaking,
            // we may need to advance past multiple rounds if BYE-only rounds exist.
            while (true) {
                val needsPrepare = tournament.currentRound == null ||
                        tournament.currentRound?.isComplete == true
                if (!needsPrepare) break

                val round = tournament.startNextRound()
                if (round == null) {
                    completeTournament(lobbyId)
                    return
                }

                // Notify BYE players for this newly prepared round
                for (match in round.matches) {
                    if (match.isBye && match.isComplete) {
                        val byePlayerState = lobby.players[match.player1Id]
                        val byeWs = byePlayerState?.identity?.webSocketSession
                        if (byeWs != null && byeWs.isOpen) {
                            sender.send(byeWs, ServerMessage.TournamentBye(
                                lobbyId = lobbyId,
                                round = round.roundNumber
                            ))
                            sendActiveMatchesToPlayer(byePlayerState.identity, byeWs)
                        }
                    }
                }

                lobbyRepository.saveTournament(lobbyId, tournament)
                logger.info("Prepared round ${round.roundNumber} for tournament $lobbyId")
            }

            // Mark player as ready
            val wasNewlyReady = lobby.markPlayerReady(identity.playerId)
            if (!wasNewlyReady) {
                return // Already marked ready
            }

            logger.info("Player ${identity.playerName} ready for next round in tournament $lobbyId")

            // Broadcast ready status to all players
            broadcastReadyStatus(lobby, identity)
            lobbyRepository.saveLobby(lobby)

            // Try to start this player's match eagerly
            tryStartMatchForPlayer(lobby, tournament, identity)
        }
    }

    /**
     * Broadcast ready status update to all players in the lobby.
     */
    private fun broadcastReadyStatus(lobby: TournamentLobby, identity: PlayerIdentity) {
        val connectedPlayers = lobby.players.values.filter { it.identity.isConnected }
        val readyPlayerIds = lobby.getReadyPlayerIds().map { it.value }

        val readyMessage = ServerMessage.PlayerReadyForRound(
            lobbyId = lobby.lobbyId,
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
    }

    /**
     * Try to start the match for a player who just became ready.
     * Uses getNextMatchForPlayer to find the next unplayed match across all rounds,
     * enabling dynamic matchmaking where players don't wait for the whole round.
     */
    private fun tryStartMatchForPlayer(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        identity: PlayerIdentity
    ) {
        val (round, match) = tournament.getNextMatchForPlayer(identity.playerId) ?: return

        // Handle BYE - player gets notified immediately, auto-complete and look for next match
        if (match.isBye) {
            match.isComplete = true
            val ws = identity.webSocketSession
            if (ws != null && ws.isOpen) {
                sender.send(ws, ServerMessage.TournamentBye(
                    lobbyId = lobby.lobbyId,
                    round = round.roundNumber
                ))
                sendActiveMatchesToPlayer(identity, ws)
            }
            lobbyRepository.saveTournament(lobby.lobbyId, tournament)
            // Recursively check if there's another match after the BYE
            tryStartMatchForPlayer(lobby, tournament, identity)
            return
        }

        // Find opponent
        val opponentId = if (match.player1Id == identity.playerId) match.player2Id else match.player1Id
        if (opponentId == null) return

        // Opponent ready?
        if (opponentId !in lobby.getReadyPlayerIds()) return

        // Prevent cross-round matchmaking from stranding players. If this match is in a
        // later round, only start it if the opponent has also completed their current-round
        // match. Otherwise we'd "steal" the opponent from a player waiting in the current round.
        val currentRound = tournament.currentRound
        if (currentRound != null && round.roundNumber > currentRound.roundNumber) {
            val opponentCurrentMatch = currentRound.matches.find {
                (it.player1Id == opponentId || it.player2Id == opponentId)
            }
            if (opponentCurrentMatch != null && !opponentCurrentMatch.isComplete) return
        }

        // Both ready - start the match!
        logger.info("Both players ready, starting match: ${identity.playerName} vs ${lobby.players[opponentId]?.identity?.playerName}")
        startSingleMatch(lobby, tournament, round, match)

        // Clear ready state for both players after their match starts
        lobby.clearPlayerReady(identity.playerId)
        lobby.clearPlayerReady(opponentId)
    }

    /**
     * Start a single tournament match between two players.
     */
    private fun startSingleMatch(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        round: TournamentRound,
        match: TournamentMatch
    ) {
        val player1State = lobby.players[match.player1Id] ?: return
        val player2State = lobby.players[match.player2Id ?: return] ?: return

        val deck1 = lobby.getSubmittedDeck(match.player1Id) ?: return
        val deck2 = lobby.getSubmittedDeck(match.player2Id!!) ?: return

        val gameSession = GameSession(
            cardRegistry = cardRegistry,
            useHandSmoother = gameProperties.handSmoother.enabled
        )
        val ps1 = player1State.identity.toPlayerSession()
        val ps2 = player2State.identity.toPlayerSession()

        gameSession.addPlayer(ps1, deck1)
        gameSession.addPlayer(ps2, deck2)

        // Store player info for persistence
        gameSession.setPlayerPersistenceInfo(ps1.playerId, ps1.playerName, player1State.identity.token)
        gameSession.setPlayerPersistenceInfo(ps2.playerId, ps2.playerName, player2State.identity.token)

        gameRepository.save(gameSession)
        gameRepository.linkToLobby(gameSession.sessionId, lobby.lobbyId)
        match.gameSessionId = gameSession.sessionId
        lobbyRepository.saveTournament(lobby.lobbyId, tournament)

        // Clean up spectating state before starting the match
        cleanUpSpectatingState(player1State.identity)
        cleanUpSpectatingState(player2State.identity)

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
                lobbyId = lobby.lobbyId,
                round = round.roundNumber,
                gameSessionId = gameSession.sessionId,
                opponentName = player2State.identity.playerName
            ))
        }
        if (ws2 != null && ws2.isOpen) {
            sender.send(ws2, ServerMessage.TournamentMatchStarting(
                lobbyId = lobby.lobbyId,
                round = round.roundNumber,
                gameSessionId = gameSession.sessionId,
                opponentName = player1State.identity.playerName
            ))
        }

        gamePlayHandler.startGame(gameSession)

        // Broadcast updated active matches to waiting players (bye players, etc.)
        broadcastActiveMatchesToWaitingPlayers(lobby.lobbyId)
    }

    /**
     * Check if the current round is complete and handle it if so.
     */
    private fun checkRoundComplete(lobbyId: String, tournament: TournamentManager) {
        if (tournament.isRoundComplete()) {
            handleRoundComplete(lobbyId)
        }
    }

    private fun completeTournament(lobbyId: String) {
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = lobbyRepository.findTournamentById(lobbyId) ?: return

        logger.info("Tournament complete for lobby $lobbyId")
        lobby.completeTournament()
        lobbyRepository.saveLobby(lobby)
        lobbyRepository.saveTournament(lobbyId, tournament)

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

        // Also notify tournament spectators
        lobby.spectators.forEach { (_, spectatorIdentity) ->
            val ws = spectatorIdentity.webSocketSession
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
     * Clean up spectating state for a player (e.g., when they start an active game).
     * Removes them from the spectated game's spectator list and clears the tracking field.
     */
    private fun cleanUpSpectatingState(identity: PlayerIdentity) {
        val spectatingGameId = identity.currentSpectatingGameId ?: return
        val gameSession = gameRepository.findById(spectatingGameId)
        val playerSession = identity.webSocketSession?.let { sessionRegistry.getPlayerSession(it.id) }
        if (playerSession != null) {
            gameSession?.removeSpectator(playerSession)
        }
        identity.currentSpectatingGameId = null
        logger.info("Cleared spectating state for ${identity.playerName} (was spectating $spectatingGameId)")
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
     * Broadcast active matches to all players who are waiting (not in an active game).
     * This includes players with byes and players whose matches haven't started yet.
     */
    fun broadcastActiveMatchesToWaitingPlayers(lobbyId: String) {
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = lobbyRepository.findTournamentById(lobbyId) ?: return

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        val activeMatches = buildActiveMatchesList(tournament)
        val message = ServerMessage.ActiveMatches(
            lobbyId = lobbyId,
            round = tournament.currentRound?.roundNumber ?: 0,
            matches = activeMatches,
            standings = tournament.getStandingsInfo(connectedIds)
        )

        // Send to all players who are not currently in an active game
        for ((playerId, playerState) in lobby.players) {
            val identity = playerState.identity
            val ws = identity.webSocketSession ?: continue
            if (!ws.isOpen) continue

            // Skip players who are currently in a game
            if (identity.currentGameSessionId != null) continue

            // Skip players who are spectating (they'll get updates through spectating)
            if (identity.currentSpectatingGameId != null) continue

            sender.send(ws, message)
        }

        // Also send to tournament-level spectators
        for ((_, spectatorIdentity) in lobby.spectators) {
            val ws = spectatorIdentity.webSocketSession ?: continue
            if (!ws.isOpen) continue
            if (spectatorIdentity.currentSpectatingGameId != null) continue
            sender.send(ws, message)
        }
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
     * Restore spectating state for a player after server restart.
     * Re-adds them as a spectator and sends the spectating messages.
     */
    fun restoreSpectating(
        identity: PlayerIdentity,
        playerSession: PlayerSession,
        session: WebSocketSession,
        gameSessionId: String
    ) {
        val gameSession = gameRepository.findById(gameSessionId)
        if (gameSession == null || gameSession.isGameOver()) {
            // Game no longer exists or is over, clear spectating state and send active matches
            identity.currentSpectatingGameId = null
            sendActiveMatchesToPlayer(identity, session)
            return
        }

        // Re-add as spectator
        gameSession.addSpectator(playerSession)

        // Send SpectatingStarted
        val playerNames = gameSession.getPlayerNames()
        if (playerNames != null) {
            sender.send(session, ServerMessage.SpectatingStarted(
                gameSessionId = gameSessionId,
                player1Name = playerNames.first,
                player2Name = playerNames.second
            ))
        }

        // Send current game state
        val spectatorState = gameSession.buildSpectatorState()
        if (spectatorState != null) {
            sender.send(session, spectatorState)
        }

        logger.info("Restored spectating for ${identity.playerName} to game $gameSessionId")
    }

    /**
     * Broadcast spectator state update to all spectators of a game.
     */
    fun broadcastSpectatorUpdate(gameSession: GameSession) {
        val spectatorState = gameSession.buildSpectatorState() ?: return

        for (spectator in gameSession.getSpectators()) {
            if (spectator.webSocketSession.isOpen) {
                sender.send(spectator.webSocketSession, spectatorState)
            }
        }
    }
}
