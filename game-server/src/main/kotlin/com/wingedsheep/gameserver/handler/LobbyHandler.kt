package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.ai.AiGameManager
import com.wingedsheep.gameserver.ai.flattenOracle
import com.wingedsheep.gameserver.ai.AiWebSocketSession
import com.wingedsheep.gameserver.handler.ConnectionHandler.Companion.cardToSealedCardInfo
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.lobby.TournamentFormat
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.gameserver.sealed.SealedSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.deck.EasterEggDeckInjector
import com.wingedsheep.sdk.model.EntityId
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val boosterGenerator: BoosterGenerator,
    private val aiGameManager: AiGameManager,
    private val ctx: LobbySharedContext,
    private val boosterDraftHandler: BoosterDraftHandler,
    private val winstonDraftHandler: WinstonDraftHandler,
    private val gridDraftHandler: GridDraftHandler,
    private val spectatingHandler: SpectatingHandler,
    private val tournamentMatchHandler: TournamentMatchHandler
) {
    private val logger = LoggerFactory.getLogger(LobbyHandler::class.java)

    @PostConstruct
    fun wireCallbacks() {
        boosterDraftHandler.onDraftComplete = { lobby -> launchAiDeckBuilding(lobby) }
        winstonDraftHandler.onDraftComplete = { lobby -> launchAiDeckBuilding(lobby) }
        gridDraftHandler.onDraftComplete = { lobby -> launchAiDeckBuilding(lobby) }
    }

    @Volatile
    var waitingSealedSession: SealedSession? = null

    fun handle(session: WebSocketSession, message: ClientMessage) {
        when (message) {
            is ClientMessage.CreateSealedGame -> handleCreateSealedGame(session, message)
            is ClientMessage.JoinSealedGame -> handleJoinSealedGame(session, message)
            is ClientMessage.SubmitSealedDeck -> handleSubmitSealedDeck(session, message)
            is ClientMessage.UnsubmitDeck -> handleUnsubmitDeck(session)
            is ClientMessage.CreateTournamentLobby -> handleCreateTournamentLobby(session, message)
            is ClientMessage.JoinLobby -> handleJoinLobby(session, message)
            is ClientMessage.StartTournamentLobby -> handleStartTournamentLobby(session)
            is ClientMessage.MakePick -> boosterDraftHandler.handleMakePick(session, message)
            is ClientMessage.WinstonTakePile -> winstonDraftHandler.handleWinstonTakePile(session)
            is ClientMessage.WinstonSkipPile -> winstonDraftHandler.handleWinstonSkipPile(session)
            is ClientMessage.GridDraftPick -> gridDraftHandler.handleGridDraftPick(session, message)
            is ClientMessage.LeaveLobby -> handleLeaveLobby(session)
            is ClientMessage.StopLobby -> handleStopLobby(session)
            is ClientMessage.UpdateLobbySettings -> handleUpdateLobbySettings(session, message)
            is ClientMessage.AddAiToLobby -> handleAddAiToLobby(session)
            is ClientMessage.RemoveAiFromLobby -> handleRemoveAiFromLobby(session, message)
            is ClientMessage.SpectateGame -> spectatingHandler.handleSpectateGame(session, message)
            is ClientMessage.StopSpectating -> spectatingHandler.handleStopSpectating(session)
            else -> {}
        }
    }

    // =========================================================================
    // Public API (delegates to sub-handlers, preserves facade for callers)
    // =========================================================================

    fun handleReadyForNextRound(session: WebSocketSession) =
        tournamentMatchHandler.handleReadyForNextRound(session)

    fun handleMatchResult(lobbyId: String, gameSessionId: String, winnerId: EntityId?, winnerLifeRemaining: Int) =
        tournamentMatchHandler.handleMatchResult(lobbyId, gameSessionId, winnerId, winnerLifeRemaining)

    fun handleAbandon(lobbyId: String, playerId: EntityId) =
        tournamentMatchHandler.handleAbandon(lobbyId, playerId)

    fun handleAddExtraRound(session: WebSocketSession) =
        tournamentMatchHandler.handleAddExtraRound(session)

    fun broadcastLobbyUpdate(lobby: TournamentLobby) =
        ctx.broadcastLobbyUpdate(lobby)

    fun startTournament(lobby: TournamentLobby) =
        tournamentMatchHandler.startTournament(lobby)

    fun startNextTournamentRound(lobbyId: String) =
        tournamentMatchHandler.startNextTournamentRound(lobbyId)

    fun handleRoundComplete(lobbyId: String) =
        tournamentMatchHandler.handleRoundComplete(lobbyId)

    fun sendActiveMatchesToPlayer(identity: PlayerIdentity, session: WebSocketSession) =
        spectatingHandler.sendActiveMatchesToPlayer(identity, session)

    fun broadcastActiveMatchesToWaitingPlayers(lobbyId: String) =
        spectatingHandler.broadcastActiveMatchesToWaitingPlayers(lobbyId)

    fun restoreSpectating(identity: PlayerIdentity, playerSession: PlayerSession, session: WebSocketSession, gameSessionId: String) =
        spectatingHandler.restoreSpectating(identity, playerSession, session, gameSessionId)

    fun broadcastSpectatorUpdate(gameSession: GameSession) =
        spectatingHandler.broadcastSpectatorUpdate(gameSession)

    fun findLobbyForReconnect(lobbyId: String): TournamentLobby? {
        return lobbyRepository.findLobbyById(lobbyId)
    }

    /**
     * Programmatically create a sealed tournament with AI-only players.
     * Returns the lobby ID for spectating. Used by the dev AI tournament endpoint.
     */
    fun createAiTournament(setCodes: List<String>, playerCount: Int = 2): String {
        require(aiGameManager.isEnabled) { "AI opponent is not enabled on this server" }
        require(playerCount in 2..8) { "Player count must be between 2 and 8" }

        val setConfigs = setCodes.map { code ->
            boosterGenerator.getSetConfig(code)
                ?: error("Unknown set code: $code")
        }

        val codes = setConfigs.map { it.setCode }
        val boosterCount = 6
        val lobby = TournamentLobby(
            setCodes = codes,
            setNames = setConfigs.map { it.setName },
            boosterGenerator = boosterGenerator,
            format = TournamentFormat.SEALED,
            boosterCount = boosterCount,
            boosterDistribution = TournamentLobby.calculateDefaultDistribution(codes, boosterCount),
            maxPlayers = playerCount
        )

        // Add AI players
        repeat(playerCount) {
            val aiIdentity = aiGameManager.createAiIdentity()
            lobby.addPlayer(aiIdentity)
        }

        lobbyRepository.saveLobby(lobby)

        // Start sealed deck building (first AI is host)
        val hostId = lobby.hostPlayerId!!
        val started = lobby.startDeckBuilding(hostId)
        require(started) { "Failed to start deck building" }

        logger.info("AI tournament created: ${lobby.lobbyId} (${playerCount} AI players, sets: ${setConfigs.joinToString(", ") { it.setName }})")

        // Send SealedPoolGenerated to AI sessions (matches normal flow)
        val basicLandInfos = lobby.basicLands.values.map { ConnectionHandler.cardToSealedCardInfo(it) }
        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null) {
                sender.send(ws, ServerMessage.SealedPoolGenerated(
                    setCodes = lobby.setCodes,
                    setNames = lobby.setNames,
                    cardPool = playerState.cardPool.map { ConnectionHandler.cardToSealedCardInfo(it) },
                    basicLands = basicLandInfos
                ))
            }
        }

        // Launch AI deck building in background (handles tournament activation + match start)
        launchAiDeckBuilding(lobby)

        lobbyRepository.saveLobby(lobby)
        return lobby.lobbyId
    }

    // =========================================================================
    // Sealed Game
    // =========================================================================

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
            useHandSmoother = gameProperties.handSmoother.enabled,
            debugMode = gameProperties.debugMode
        )

        sealedSession.players.forEach { (playerId, playerState) ->
            val baseDeck = playerState.submittedDeck
                ?: throw IllegalStateException("Player $playerId has no submitted deck")
            val deckWithVariants = BoosterGenerator.distributeBasicLandVariants(baseDeck, sealedSession.allBasicLandVariants)
            val deck = EasterEggDeckInjector.maybeInjectEasterEggs(playerState.session.playerName, deckWithVariants)
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

    // =========================================================================
    // Lobby CRUD
    // =========================================================================

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

        val maxPlayers = when (format) {
            TournamentFormat.WINSTON_DRAFT -> 2
            TournamentFormat.GRID_DRAFT -> message.maxPlayers.coerceIn(2, 4)
            else -> message.maxPlayers.coerceIn(2, 8)
        }

        // Set appropriate default booster count based on format
        // Draft: default 3 packs, max 6
        // Sealed: default 6 boosters, max 16
        // Winston: default 6 boosters, max 16
        // Grid Draft: player-count-aware default (2p=9, 3p=13), max 18
        val boosterCount = when (format) {
            TournamentFormat.DRAFT -> {
                if (message.boosterCount == 6) 3 else message.boosterCount.coerceIn(1, 6)  // 6 is the client default, use 3 for draft
            }
            TournamentFormat.SEALED, TournamentFormat.WINSTON_DRAFT -> {
                message.boosterCount.coerceIn(1, 16)
            }
            TournamentFormat.GRID_DRAFT -> {
                gridDraftHandler.gridDraftDefaultBoosters(maxPlayers)
            }
        }

        val codes = setConfigs.map { it.setCode }
        val lobby = TournamentLobby(
            setCodes = codes,
            setNames = setConfigs.map { it.setName },
            boosterGenerator = boosterGenerator,
            format = format,
            boosterCount = boosterCount,
            boosterDistribution = TournamentLobby.calculateDefaultDistribution(codes, boosterCount),
            maxPlayers = maxPlayers,
            pickTimeSeconds = message.pickTimeSeconds.coerceIn(15, 120)
        )
        lobby.addPlayer(identity)
        lobbyRepository.saveLobby(lobby)

        val setNamesStr = setConfigs.joinToString(", ") { it.setName }
        logger.info("Tournament lobby created: ${lobby.lobbyId} by ${identity.playerName} (sets: $setNamesStr, format: ${format.name})")
        sender.send(session, ServerMessage.LobbyCreated(lobby.lobbyId))
        ctx.broadcastLobbyUpdate(lobby)
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
        // Auto-adjust grid draft booster count when player count changes
        if (lobby.format == TournamentFormat.GRID_DRAFT && lobby.state == LobbyState.WAITING_FOR_PLAYERS) {
            lobby.boosterCount = gridDraftHandler.gridDraftDefaultBoosters(lobby.players.size)
        }
        logger.info("Player ${identity.playerName} joined lobby ${lobby.lobbyId}")
        ctx.broadcastLobbyUpdate(lobby)
        lobbyRepository.saveLobby(lobby)
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
                ctx.broadcastLobbyUpdate(lobby)
            }
            LobbyState.DRAFTING -> {
                sender.send(session, lobby.buildLobbyUpdate(identity.playerId, aiGameManager::isAiPlayer))
                // Winston Draft reconnection
                if (lobby.format == TournamentFormat.WINSTON_DRAFT) {
                    winstonDraftHandler.broadcastWinstonDraftState(lobby, null)
                    return
                }

                // Grid Draft reconnection
                if (lobby.format == TournamentFormat.GRID_DRAFT) {
                    gridDraftHandler.broadcastGridDraftState(lobby, null)
                    return
                }

                boosterDraftHandler.sendDraftReconnectionState(session, lobby, identity)
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
                ctx.broadcastLobbyUpdate(lobby)

                // If this player already submitted their deck, restore tournament state
                if (playerState?.hasSubmittedDeck == true) {
                    val tournament = lobbyRepository.findTournamentById(lobby.lobbyId)
                    if (tournament != null) {
                        tournamentMatchHandler.sendTournamentStartedToPlayer(lobby, tournament, identity, wsOverride = session)
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
        sender.send(session, lobby.buildLobbyUpdate(identity.playerId, aiGameManager::isAiPlayer))

        val tournament = lobbyRepository.findTournamentById(lobby.lobbyId) ?: return
        tournamentMatchHandler.sendTournamentStartedToPlayer(lobby, tournament, identity, wsOverride = session)

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
                            // Clear delta cache so reconnecting player gets full state
                            gs.clearLastSentState(identity.playerId)
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
                    spectatingHandler.restoreSpectating(identity, playerSession, session, spectatingGameId)
                } else {
                    spectatingHandler.sendActiveMatchesToPlayer(identity, session)
                }
            }

            // Player's match hasn't started yet (waiting for opponent to ready up)
            playerMatch?.gameSessionId == null && playerMatch?.isBye == false -> {
                // Send MatchComplete-style info so they can ready up for their next match.
                // Only show the opponent if the match is in the current round.
                val nextMatch = tournament.getNextMatchForPlayer(identity.playerId)
                if (nextMatch != null) {
                    val (nextRound, nm) = nextMatch
                    val isCurrentRound = nextRound.roundNumber == currentRound.roundNumber
                    val opponentId = if (nm.player1Id == identity.playerId) nm.player2Id else nm.player1Id
                    val nextOpponentName = if (isCurrentRound && !nm.isBye) opponentId?.let { lobby.players[it]?.identity?.playerName } else null
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
                // Send active matches so player can watch live games while waiting
                spectatingHandler.sendActiveMatchesToPlayer(identity, session)
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
            // only if it's in the current round (future-round opponents are not guaranteed)
            playerMatch?.isComplete == true -> {
                // Send MatchComplete with next opponent info so they can ready up
                val nextMatch = tournament.getNextMatchForPlayer(identity.playerId)
                if (nextMatch != null) {
                    val (nextRound, nm) = nextMatch
                    val isCurrentRound = nextRound.roundNumber == currentRound.roundNumber
                    val opponentId = if (nm.player1Id == identity.playerId) nm.player2Id else nm.player1Id
                    val nextOpponentName = if (isCurrentRound && !nm.isBye) opponentId?.let { lobby.players[it]?.identity?.playerName } else null
                    sender.send(session, ServerMessage.MatchComplete(
                        lobbyId = lobby.lobbyId,
                        round = currentRound.roundNumber,
                        results = tournament.getCurrentRoundResults(),
                        standings = tournament.getStandingsInfo(connectedIds),
                        nextOpponentName = nextOpponentName,
                        nextRoundHasBye = nm.isBye,
                        isTournamentComplete = false
                    ))
                    // Send active matches so player can watch live games while waiting
                    val spectatingGameId = identity.currentSpectatingGameId
                    if (spectatingGameId != null && playerSession != null) {
                        spectatingHandler.restoreSpectating(identity, playerSession, session, spectatingGameId)
                    } else {
                        spectatingHandler.sendActiveMatchesToPlayer(identity, session)
                    }
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
                        spectatingHandler.restoreSpectating(identity, playerSession, session, spectatingGameId)
                    } else {
                        spectatingHandler.sendActiveMatchesToPlayer(identity, session)
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
        sender.send(session, lobby.buildLobbyUpdate(identity.playerId, aiGameManager::isAiPlayer))

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
                    spectatingHandler.sendActiveMatchesToPlayer(identity, session)
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

    // =========================================================================
    // Tournament Start (dispatches to format-specific draft handlers)
    // =========================================================================

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

                // Auto-submit decks for AI players in background so LLM deckbuilding
                // doesn't block the host's WebSocket handler thread
                launchAiDeckBuilding(lobby)
            }

            TournamentFormat.DRAFT -> {
                val started = lobby.startDraft(identity.playerId)
                if (!started) {
                    sender.sendError(session, ErrorCode.INVALID_ACTION, "Failed to start draft")
                    return
                }

                logger.info("Lobby ${lobby.lobbyId} started drafting (${lobby.playerCount} players)")

                // Wire AI draft callbacks before broadcasting packs
                wireAiDraftCallbacks(lobby)

                // Send first packs to all players (AI sessions will auto-pick via callbacks)
                boosterDraftHandler.broadcastDraftPacks(lobby)

                // Start per-player pick timers
                boosterDraftHandler.startAllPlayerTimers(lobby)
            }

            TournamentFormat.WINSTON_DRAFT -> {
                if (lobby.playerCount != 2) {
                    sender.sendError(session, ErrorCode.INVALID_ACTION, "Winston Draft requires exactly 2 players")
                    return
                }

                val started = lobby.startWinstonDraft(identity.playerId)
                if (!started) {
                    sender.sendError(session, ErrorCode.INVALID_ACTION, "Failed to start Winston Draft")
                    return
                }

                logger.info("Lobby ${lobby.lobbyId} started Winston Draft (2 players)")

                // Wire AI draft callbacks before broadcasting state
                wireAiDraftCallbacks(lobby)

                // Send initial state to both players
                winstonDraftHandler.broadcastWinstonDraftState(lobby, null)

                // Start the turn timer
                winstonDraftHandler.startWinstonTimer(lobby)
            }
            TournamentFormat.GRID_DRAFT -> {
                val started = lobby.startGridDraft(identity.playerId)
                if (!started) {
                    sender.sendError(session, ErrorCode.INVALID_ACTION, "Failed to start grid draft")
                    return
                }

                logger.info("Lobby ${lobby.lobbyId} started grid draft (${lobby.playerCount} players)")

                // Wire AI draft callbacks before broadcasting state
                wireAiDraftCallbacks(lobby)

                // Broadcast initial grid state
                gridDraftHandler.broadcastGridDraftState(lobby, null)

                // Start the pick timer
                gridDraftHandler.startGridDraftTimer(lobby)
            }
        }

        ctx.broadcastLobbyUpdate(lobby)
        lobbyRepository.saveLobby(lobby)
    }

    // =========================================================================
    // AI Lobby Integration
    // =========================================================================

    private fun handleAddAiToLobby(session: WebSocketSession) {
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

        if (!lobby.isHost(identity.playerId)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can add AI players")
            return
        }

        if (lobby.state != LobbyState.WAITING_FOR_PLAYERS) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Can only add AI while waiting for players")
            return
        }

        if (lobby.isFull) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Lobby is full")
            return
        }

        if (!aiGameManager.isEnabled) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "AI opponent is not enabled on this server")
            return
        }

        val aiIdentity = aiGameManager.createAiIdentity()
        lobby.addPlayer(aiIdentity)
        lobbyRepository.saveLobby(lobby)

        logger.info("AI player ${aiIdentity.playerName} (${aiIdentity.playerId.value}) added to lobby $lobbyId")
        ctx.broadcastLobbyUpdate(lobby)
    }

    private fun handleRemoveAiFromLobby(session: WebSocketSession, message: ClientMessage.RemoveAiFromLobby) {
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

        if (!lobby.isHost(identity.playerId)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can remove AI players")
            return
        }

        if (lobby.state != LobbyState.WAITING_FOR_PLAYERS) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Can only remove AI while waiting for players")
            return
        }

        val aiPlayerId = EntityId(message.playerId)
        if (!aiGameManager.isAiPlayer(aiPlayerId)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Player is not an AI")
            return
        }

        val aiPlayerState = lobby.players[aiPlayerId]
        if (aiPlayerState == null) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "AI player not found in lobby")
            return
        }

        lobby.forceRemovePlayer(aiPlayerId)
        sessionRegistry.removeIdentity(aiPlayerState.identity.token)
        lobbyRepository.saveLobby(lobby)

        logger.info("AI player ${aiPlayerState.identity.playerName} (${aiPlayerId.value}) removed from lobby $lobbyId")
        ctx.broadcastLobbyUpdate(lobby)
    }

    /**
     * Launch AI deck building in a background coroutine so it doesn't block the
     * host's WebSocket handler thread. LLM deckbuilding can take 30+ seconds,
     * and blocking would prevent the human player from submitting their own deck.
     */
    private fun launchAiDeckBuilding(lobby: TournamentLobby) {
        val aiPlayers = lobby.players.filter { (playerId, ps) ->
            aiGameManager.isAiPlayer(playerId) && !ps.hasSubmittedDeck && ps.cardPool.isNotEmpty()
        }
        if (aiPlayers.isEmpty()) return

        ctx.draftScope.launch(Dispatchers.IO) {
            for ((playerId, playerState) in aiPlayers) {
                try {
                    val deck = buildAiSealedDeck(playerState.cardPool)
                    val result = lobby.submitDeck(playerId, deck)
                    when (result) {
                        is TournamentLobby.DeckSubmissionResult.Success -> {
                            logger.info("AI ${playerState.identity.playerName} auto-submitted sealed deck (${deck.values.sum()} cards)")
                        }
                        is TournamentLobby.DeckSubmissionResult.Error -> {
                            logger.warn("AI deck submission failed: ${result.message}")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("AI deck building failed for ${playerState.identity.playerName}: ${e.message}", e)
                }
            }

            // After AI decks are built, broadcast updated lobby state and handle tournament readiness
            ctx.broadcastLobbyUpdate(lobby)

            if (lobby.allDecksSubmitted() && lobby.state == LobbyState.DECK_BUILDING) {
                lobby.activateTournament()
            }

            // Create tournament if needed and auto-ready AI players
            val tournament = tournamentMatchHandler.ensureTournamentCreated(lobby)
            tournamentMatchHandler.autoReadyAiPlayers(lobby, tournament)
            lobbyRepository.saveLobby(lobby)
        }
    }

    // =========================================================================
    // AI Draft Integration
    // =========================================================================

    /**
     * Wire draft callbacks on all AI players' sessions so their picks route
     * back into the lobby. Called once when a draft starts.
     */
    private fun wireAiDraftCallbacks(lobby: TournamentLobby) {
        val aiPlayers = lobby.players.filter { (playerId, _) -> aiGameManager.isAiPlayer(playerId) }
        if (aiPlayers.isEmpty()) return

        for ((_, playerState) in aiPlayers) {
            val ws = playerState.identity.webSocketSession as? AiWebSocketSession ?: continue

            ws.onDraftPick = { playerId, cardNames ->
                handleAiBoosterDraftPick(lobby, playerId, cardNames)
            }

            ws.onWinstonTakePile = { playerId ->
                handleAiWinstonTakePile(lobby, playerId)
            }

            ws.onWinstonSkipPile = { playerId ->
                handleAiWinstonSkipPile(lobby, playerId)
            }

            ws.onGridDraftPick = { playerId, selection ->
                handleAiGridDraftPick(lobby, playerId, selection)
            }
        }

        logger.info("Wired draft callbacks for {} AI players in lobby {}", aiPlayers.size, lobby.lobbyId)
    }

    private fun handleAiBoosterDraftPick(lobby: TournamentLobby, playerId: EntityId, cardNames: List<String>) {
        if (lobby.state != LobbyState.DRAFTING) return

        synchronized(lobby.draftLock) {
            val playerState = lobby.players[playerId]
            val identity = playerState?.identity ?: return

            val result = lobby.makePick(playerId, cardNames)
            when (result) {
                is com.wingedsheep.gameserver.lobby.PickResult.Success -> {
                    boosterDraftHandler.processPickResult(lobby, playerId, identity, result)
                }
                is com.wingedsheep.gameserver.lobby.PickResult.Error -> {
                    logger.warn("AI draft pick failed for {}: {}", playerId.value, result.message)
                    // Fallback: auto-pick first cards
                    val fallback = lobby.autoPickFirstCards(playerId)
                    if (fallback is com.wingedsheep.gameserver.lobby.PickResult.Success) {
                        boosterDraftHandler.processPickResult(lobby, playerId, identity, fallback)
                    }
                }
            }
        }
    }

    private fun handleAiWinstonTakePile(lobby: TournamentLobby, playerId: EntityId) {
        // Delegate to the WinstonDraftHandler's internal logic by simulating the action
        // We need to get the AI's WebSocket session to route through the handler
        val playerState = lobby.players[playerId]
        val ws = playerState?.identity?.webSocketSession
        if (ws != null) {
            winstonDraftHandler.handleWinstonTakePile(ws)
        }
    }

    private fun handleAiWinstonSkipPile(lobby: TournamentLobby, playerId: EntityId) {
        val playerState = lobby.players[playerId]
        val ws = playerState?.identity?.webSocketSession
        if (ws != null) {
            winstonDraftHandler.handleWinstonSkipPile(ws)
        }
    }

    private fun handleAiGridDraftPick(lobby: TournamentLobby, playerId: EntityId, selection: String) {
        val playerState = lobby.players[playerId]
        val ws = playerState?.identity?.webSocketSession
        if (ws != null) {
            gridDraftHandler.handleGridDraftPick(ws, ClientMessage.GridDraftPick(selection))
        }
    }

    /**
     * Build a 40-card sealed deck from a card pool using the LLM.
     * Falls back to a color-based heuristic if the LLM fails.
     */
    private fun buildAiSealedDeck(pool: List<com.wingedsheep.sdk.model.CardDefinition>): Map<String, Int> {
        logger.info("AI building sealed deck from pool of {} cards", pool.size)

        val aiProperties = gameProperties.ai
        if (aiProperties.enabled && aiProperties.effectiveApiKey.isNotBlank()) {
            val llmDeck = tryLlmSealedDeck(pool, aiProperties)
            if (llmDeck != null) return llmDeck
            logger.info("AI LLM deckbuild failed, falling back to heuristic")
        }

        return buildHeuristicSealedDeck(pool)
    }

    /**
     * Ask the LLM to analyze the sealed pool and build a deck.
     */
    private fun tryLlmSealedDeck(
        pool: List<com.wingedsheep.sdk.model.CardDefinition>,
        aiProperties: com.wingedsheep.gameserver.config.AiProperties
    ): Map<String, Int>? {
        val nonLands = pool.filter { !it.typeLine.isLand }
        val poolLands = pool.filter { it.typeLine.isLand && !it.typeLine.isBasicLand }

        val prompt = buildString {
            appendLine("You are building a 40-card sealed deck from this card pool.")
            appendLine()
            appendLine("RULES:")
            appendLine("- Exactly 40 cards total")
            appendLine("- ~23 non-land cards (creatures + spells) and ~17 lands")
            appendLine("- Pick 2 colors (sometimes splash a 3rd). Do NOT play all 5 colors.")
            appendLine("- Only include cards you can actually cast with your lands")
            appendLine("- You may add any number of basic lands: Plains, Island, Swamp, Mountain, Forest")
            appendLine("- Prioritize creatures, removal, and a good mana curve")
            appendLine("- Include non-basic lands from your pool if they fit your colors")
            appendLine()
            appendLine("YOUR CARD POOL:")

            val byType = nonLands.groupBy { card ->
                when {
                    card.typeLine.isCreature -> "Creatures"
                    card.typeLine.isInstant || card.typeLine.isSorcery -> "Spells"
                    card.typeLine.isEnchantment -> "Enchantments"
                    card.typeLine.isArtifact -> "Artifacts"
                    else -> "Other"
                }
            }

            for ((type, cards) in byType.entries.sortedBy { it.key }) {
                appendLine()
                appendLine("$type:")
                // Group duplicates
                val grouped = cards.groupBy { it.name }
                for ((name, copies) in grouped.entries.sortedBy { it.value.first().cmc }) {
                    val card = copies.first()
                    val stats = if (card.creatureStats != null) " ${card.creatureStats}" else ""
                    val oracle = if (card.oracleText.isNotBlank()) " — ${card.oracleText.flattenOracle()}" else ""
                    val count = if (copies.size > 1) "${copies.size}x " else ""
                    appendLine("  $count${card.name} ${card.manaCost} — ${card.typeLine}$stats$oracle")
                }
            }

            if (poolLands.isNotEmpty()) {
                appendLine()
                appendLine("Non-basic lands in pool:")
                val grouped = poolLands.groupBy { it.name }
                for ((name, copies) in grouped) {
                    val card = copies.first()
                    val count = if (copies.size > 1) "${copies.size}x " else ""
                    val oracle = if (card.oracleText.isNotBlank()) " — ${card.oracleText.flattenOracle()}" else ""
                    appendLine("  $count${card.name}$oracle")
                }
            }

            appendLine()
            appendLine("Reply ONLY with the deck list, one entry per line:")
            appendLine("1x Card Name")
            appendLine("9x Forest")
        }

        val client = com.wingedsheep.gameserver.ai.LlmClient(aiProperties)
        val messages = listOf(
            com.wingedsheep.gameserver.ai.ChatMessage("system",
                "You are an expert Magic: The Gathering limited deckbuilder. " +
                "Analyze the sealed pool, pick the best 2 colors (with optional light splash), " +
                "and build a strong 40-card deck. Reply ONLY with the deck list."),
            com.wingedsheep.gameserver.ai.ChatMessage("user", prompt)
        )

        logger.info("AI sealed deckbuild prompt ({} chars)", prompt.length)
        val response = client.chatCompletion(messages) ?: return null
        logger.info("AI sealed deckbuild response:\n{}", response)

        return parseSealedDeckList(response, pool)
    }

    /**
     * Parse an LLM deck list response, validating against the actual pool.
     */
    private fun parseSealedDeckList(
        response: String,
        pool: List<com.wingedsheep.sdk.model.CardDefinition>
    ): Map<String, Int>? {
        val basics = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
        // Count how many copies of each card are in the pool
        val poolCounts = pool.groupBy { it.name }.mapValues { it.value.size }
        val validNames = poolCounts.keys + basics

        val deckMap = mutableMapOf<String, Int>()
        val linePattern = Regex("""(\d+)\s*x?\s+(.+)""", RegexOption.IGNORE_CASE)

        for (line in response.lines()) {
            val match = linePattern.find(line.trim()) ?: continue
            val count = match.groupValues[1].toIntOrNull() ?: continue
            val name = match.groupValues[2].trim()

            val exactMatch = validNames.find { it.equals(name, ignoreCase = true) } ?: continue
            if (count < 1) continue

            // Enforce pool limits for non-basics
            val maxAllowed = if (exactMatch in basics) count else poolCounts[exactMatch] ?: 0
            val actual = count.coerceAtMost(maxAllowed)
            if (actual > 0) {
                deckMap[exactMatch] = (deckMap[exactMatch] ?: 0) + actual
            }
        }

        val totalCards = deckMap.values.sum()
        if (totalCards < 30) {
            logger.warn("AI sealed deckbuild: deck too small ({} cards), rejecting", totalCards)
            return null
        }

        // Pad to 40 if under
        if (totalCards < 40) {
            val landsNeeded = 40 - totalCards
            // Determine primary color from non-land cards in deck
            val primaryLand = guessPrimaryBasicLand(deckMap, pool)
            deckMap[primaryLand] = (deckMap[primaryLand] ?: 0) + landsNeeded
            logger.info("AI sealed deckbuild: padded {} {} to reach 40", landsNeeded, primaryLand)
        }

        // Trim to 40 if over (remove excess lands first)
        while (deckMap.values.sum() > 40) {
            val landToTrim = basics.filter { (deckMap[it] ?: 0) > 0 }
                .maxByOrNull { deckMap[it] ?: 0 } ?: break
            deckMap[landToTrim] = (deckMap[landToTrim] ?: 0) - 1
            if (deckMap[landToTrim] == 0) deckMap.remove(landToTrim)
        }

        logger.info("AI sealed deckbuild: final deck ({} cards): {}", deckMap.values.sum(),
            deckMap.entries.sortedByDescending { it.value }.joinToString(", ") { "${it.value}x ${it.key}" })

        return deckMap
    }

    /**
     * Heuristic fallback: pick the best 2 colors, include on-color cards, add correct basics.
     */
    private fun buildHeuristicSealedDeck(pool: List<com.wingedsheep.sdk.model.CardDefinition>): Map<String, Int> {
        val deck = com.wingedsheep.engine.ai.buildHeuristicSealedDeck(pool)
        logger.info("AI heuristic deck ({} cards): {}", deck.values.sum(),
            deck.entries.sortedByDescending { it.value }.joinToString(", ") { "${it.value}x ${it.key}" })
        return deck
    }

    /**
     * Guess the primary basic land name from the colors of non-land cards already in the deck.
     */
    private fun guessPrimaryBasicLand(
        deckMap: Map<String, Int>,
        pool: List<com.wingedsheep.sdk.model.CardDefinition>
    ): String {
        val basics = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
        val poolByName = pool.associateBy { it.name }
        val colorCounts = mutableMapOf<com.wingedsheep.sdk.core.Color, Int>()

        for ((name, count) in deckMap) {
            if (name in basics) continue
            val card = poolByName[name] ?: continue
            for (color in card.colors) {
                colorCounts[color] = (colorCounts[color] ?: 0) + count
            }
        }

        val topColor = colorCounts.maxByOrNull { it.value }?.key
        return when (topColor) {
            com.wingedsheep.sdk.core.Color.WHITE -> "Plains"
            com.wingedsheep.sdk.core.Color.BLUE -> "Island"
            com.wingedsheep.sdk.core.Color.BLACK -> "Swamp"
            com.wingedsheep.sdk.core.Color.RED -> "Mountain"
            com.wingedsheep.sdk.core.Color.GREEN -> "Forest"
            else -> "Forest"
        }
    }

    // =========================================================================
    // Lobby Leave / Stop / Settings
    // =========================================================================

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
            // Auto-adjust grid draft booster count when player count changes
            if (lobby.format == TournamentFormat.GRID_DRAFT && lobby.state == LobbyState.WAITING_FOR_PLAYERS) {
                lobby.boosterCount = gridDraftHandler.gridDraftDefaultBoosters(lobby.players.size)
            }
            ctx.broadcastLobbyUpdate(lobby)
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
            ctx.broadcastLobbyUpdate(lobby)
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

        // Cancel all timers if we're in drafting
        lobby.pickTimerJob?.cancel()
        lobby.pickTimerJob = null
        lobby.cancelAllPlayerTimers()

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

        // Check if player has any active match (across all rounds, not just current)
        val tournament = lobbyRepository.findTournamentById(lobbyId)
        if (tournament != null && tournament.hasActiveMatch(identity.playerId)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Cannot edit deck - match already started")
            return
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
        ctx.broadcastLobbyUpdate(lobby)

        // If in tournament, broadcast the updated ready status
        if (tournament != null) {
            tournamentMatchHandler.broadcastReadyStatus(lobby, identity)
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
                lobby.boosterDistribution = emptyMap()
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
            // When switching formats, adjust boosterCount and maxPlayers to appropriate defaults
            if (newFormat != lobby.format) {
                lobby.format = newFormat
                if (newFormat == TournamentFormat.WINSTON_DRAFT) {
                    lobby.maxPlayers = 2
                } else if (newFormat == TournamentFormat.GRID_DRAFT) {
                    lobby.maxPlayers = minOf(lobby.maxPlayers, 4)
                }
                lobby.boosterCount = when (newFormat) {
                    TournamentFormat.DRAFT -> 3
                    TournamentFormat.SEALED -> 6
                    TournamentFormat.WINSTON_DRAFT -> 6
                    TournamentFormat.GRID_DRAFT -> gridDraftHandler.gridDraftDefaultBoosters(lobby.players.size)
                }
                lobby.recalculateDistribution()
            }
        }

        // Manual boosterCount override (apply after format change)
        // Grid draft uses fixed booster counts based on player count — no manual override
        if (lobby.format != TournamentFormat.GRID_DRAFT) {
            message.boosterCount?.let {
                val maxCount = when (lobby.format) {
                    TournamentFormat.DRAFT -> 6
                    TournamentFormat.SEALED -> 16
                    TournamentFormat.WINSTON_DRAFT -> 16
                    TournamentFormat.GRID_DRAFT -> 24 // unreachable
                }
                lobby.boosterCount = it.coerceIn(1, maxCount)
                lobby.recalculateDistribution()
            }
        }

        // Manual booster distribution override (apply after boosterCount)
        message.boosterDistribution?.let { dist ->
            // Validate: all keys must be in setCodes, values must be positive, total must equal boosterCount
            val validKeys = dist.keys.all { it in lobby.setCodes }
            val allPositive = dist.values.all { it >= 0 }
            val totalMatches = dist.values.sum() == lobby.boosterCount
            if (validKeys && allPositive && totalMatches) {
                lobby.boosterDistribution = dist
            }
        }
        message.maxPlayers?.let {
            val oldMaxPlayers = lobby.maxPlayers
            when (lobby.format) {
                TournamentFormat.WINSTON_DRAFT -> lobby.maxPlayers = 2
                TournamentFormat.GRID_DRAFT -> lobby.maxPlayers = it.coerceIn(2, 4)
                else -> lobby.maxPlayers = it.coerceIn(2, 8)
            }
            // Auto-adjust grid draft booster count when player count changes (always, since it's fixed)
            if (lobby.format == TournamentFormat.GRID_DRAFT && lobby.maxPlayers != oldMaxPlayers) {
                lobby.boosterCount = gridDraftHandler.gridDraftDefaultBoosters(lobby.players.size)
                lobby.recalculateDistribution()
            }
        }
        message.gamesPerMatch?.let { lobby.gamesPerMatch = it.coerceIn(1, 5) }
        message.pickTimeSeconds?.let { lobby.pickTimeSeconds = it.coerceIn(15, 180) }
        message.picksPerRound?.let { lobby.picksPerRound = it.coerceIn(1, 2) }

        ctx.broadcastLobbyUpdate(lobby)
        lobbyRepository.saveLobby(lobby)
    }

    // =========================================================================
    // Deck Building / Submit
    // =========================================================================

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
                ctx.broadcastLobbyUpdate(lobby)

                // Ensure tournament is created (for matchup info)
                val tournament = tournamentMatchHandler.ensureTournamentCreated(lobby)

                // Send TournamentStarted to this player (they can now ready up for round 1)
                tournamentMatchHandler.sendTournamentStartedToPlayer(lobby, tournament, identity)

                // NOTE: Don't auto-start matches - require players to press Ready
                // This allows them to return to deck building while waiting

                // Transition lobby state when all decks are submitted
                if (result.allReady && lobby.state == LobbyState.DECK_BUILDING) {
                    lobby.activateTournament()
                }

                // Auto-ready AI players so they participate in matchmaking
                tournamentMatchHandler.autoReadyAiPlayers(lobby, tournament)

                lobbyRepository.saveLobby(lobby)
            }
            is TournamentLobby.DeckSubmissionResult.Error -> {
                sender.sendError(session, ErrorCode.INVALID_DECK, result.message)
            }
        }
    }
}
