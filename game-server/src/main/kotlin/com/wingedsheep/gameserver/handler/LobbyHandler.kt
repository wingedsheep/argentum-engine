package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.ai.AiGameManager
import com.wingedsheep.gameserver.ai.AiWebSocketSession
import com.wingedsheep.gameserver.handler.ConnectionHandler.Companion.cardToSealedCardInfo
import com.wingedsheep.gameserver.lobby.AiDeckSpec
import com.wingedsheep.gameserver.lobby.LobbyGameMode
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.lobby.commanderRulesTableConflict
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
import com.wingedsheep.gameserver.deck.DeckValidator
import com.wingedsheep.gameserver.deck.EasterEggDeckInjector
import com.wingedsheep.gameserver.cube.CubeCardEntry
import com.wingedsheep.gameserver.cube.CubeList
import com.wingedsheep.gameserver.cube.CubeResolution
import com.wingedsheep.gameserver.cube.CubeResolver
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

@Component
class LobbyHandler(
    private val sessionRegistry: SessionRegistry,
    private val gameRepository: GameRepository,
    private val lobbyRepository: LobbyRepository,
    private val sender: MessageSender,
    private val cardRegistry: CardRegistry,
    private val printingRegistry: com.wingedsheep.engine.registry.PrintingRegistry,
    private val tokenArtRegistry: com.wingedsheep.engine.registry.TokenArtRegistry,
    private val gamePlayHandler: GamePlayHandler,
    private val gameProperties: GameProperties,
    private val boosterGenerator: BoosterGenerator,
    private val aiGameManager: AiGameManager,
    private val ctx: LobbySharedContext,
    private val boosterDraftHandler: BoosterDraftHandler,
    private val winstonDraftHandler: WinstonDraftHandler,
    private val gridDraftHandler: GridDraftHandler,
    private val spectatingHandler: SpectatingHandler,
    private val tournamentMatchHandler: TournamentMatchHandler,
    private val freeForAllHandler: FreeForAllHandler,
    private val deckValidator: DeckValidator,
    private val randomDeckResolver: com.wingedsheep.gameserver.ai.RandomDeckResolver,
    private val commanderDeckGenerator: com.wingedsheep.ai.engine.deck.CommanderDeckGenerator,
    private val tournamentResultSink: com.wingedsheep.gameserver.stats.TournamentResultSink
) {
    private val logger = LoggerFactory.getLogger(LobbyHandler::class.java)

    @PostConstruct
    fun wireCallbacks() {
        boosterDraftHandler.onDraftComplete = { lobby -> launchAiDeckBuilding(lobby) }
        winstonDraftHandler.onDraftComplete = { lobby -> launchAiDeckBuilding(lobby) }
        gridDraftHandler.onDraftComplete = { lobby -> launchAiDeckBuilding(lobby) }
    }

    /**
     * Resume in-flight AI tournament work for lobbies recovered from Redis.
     *
     * On server restart, [SessionRecoveryService] rebuilds player identities and
     * [AiGameManager.rehydrateAiIdentity] re-creates each AI's virtual WebSocket session.
     * This hook then reactivates the tournament-level flow that would otherwise be stuck:
     *   - DECK_BUILDING with AI players that haven't submitted → resume deckbuilding
     *   - TOURNAMENT_ACTIVE between matches → re-run auto-ready so the next match starts
     *
     * Runs on [ApplicationReadyEvent] so all beans (and websocket plumbing) are wired.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun resumeAiTournamentsOnStartup() {
        val lobbies = lobbyRepository.findAllLobbies()
        if (lobbies.isEmpty()) return

        var resumedDeckBuilding = 0
        var resumedAutoReady = 0
        for (lobby in lobbies) {
            val hasAi = lobby.players.any { (playerId, _) -> aiGameManager.isAiPlayer(playerId) }
            if (!hasAi) continue

            try {
                when (lobby.state) {
                    LobbyState.DECK_BUILDING -> {
                        val anyAiNeedsDeck = lobby.players.any { (playerId, ps) ->
                            aiGameManager.isAiPlayer(playerId) && !ps.hasSubmittedDeck && ps.cardPool.isNotEmpty()
                        }
                        if (anyAiNeedsDeck) {
                            launchAiDeckBuilding(lobby)
                            resumedDeckBuilding++
                        } else {
                            // All AI decks already submitted — make sure the lobby progresses.
                            resumeAiReadiness(lobby)
                            resumedAutoReady++
                        }
                    }
                    LobbyState.TOURNAMENT_ACTIVE -> {
                        resumeAiReadiness(lobby)
                        resumedAutoReady++
                    }
                    else -> { /* nothing to resume in WAITING_FOR_PLAYERS / DRAFTING / TOURNAMENT_COMPLETE */ }
                }
            } catch (e: Exception) {
                logger.error("Failed to resume AI tournament work for lobby ${lobby.lobbyId}", e)
            }
        }

        if (resumedDeckBuilding > 0 || resumedAutoReady > 0) {
            logger.info("Resumed AI tournament work: {} deckbuilding, {} auto-ready",
                resumedDeckBuilding, resumedAutoReady)
        }
    }

    /**
     * Re-arm whatever "the AI is waiting on nobody" means for this lobby's shape, after a restart
     * rebuilt its AI identities. A bracket consults its pairings; a pod has none, so its AI seats
     * are simply ready and the next game starts when the humans are.
     */
    private fun resumeAiReadiness(lobby: TournamentLobby) {
        if (lobby.isFreeForAll) {
            freeForAllHandler.autoReadyAiSeats(lobby)
            return
        }
        val tournament = tournamentMatchHandler.ensureTournamentCreated(lobby)
        tournamentMatchHandler.autoReadyAiPlayers(lobby, tournament)
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
            is ClientMessage.SetLobbyAiDeck -> handleSetLobbyAiDeck(session, message)
            is ClientMessage.SpectateGame -> spectatingHandler.handleSpectateGame(session, message)
            is ClientMessage.StopSpectating -> spectatingHandler.handleStopSpectating(session)
            else -> {}
        }
    }

    // =========================================================================
    // Public API (delegates to sub-handlers, preserves facade for callers)
    // =========================================================================

    fun handleReadyForNextRound(session: WebSocketSession) {
        val (identity, lobby) = ctx.getIdentityAndLobby(session) ?: return
        if (lobby.isFreeForAll) {
            freeForAllHandler.handleReadyForNextGame(session, identity, lobby)
        } else {
            tournamentMatchHandler.handleReadyForNextRound(session)
        }
    }

    fun handleMatchResult(lobbyId: String, gameSessionId: String, winnerId: EntityId?, winnerLifeRemaining: Int) {
        val lobby = lobbyRepository.findLobbyById(lobbyId)
        if (lobby?.isFreeForAll == true) {
            freeForAllHandler.handleGameComplete(lobbyId, gameSessionId, winnerId)
        } else {
            tournamentMatchHandler.handleMatchResult(lobbyId, gameSessionId, winnerId, winnerLifeRemaining)
        }
    }

    fun handleAbandon(lobbyId: String, playerId: EntityId) {
        val lobby = lobbyRepository.findLobbyById(lobbyId)
        if (lobby?.isFreeForAll == true) {
            // No bracket to update — just concede their seat in the running game (if any);
            // the game continues for the remaining players (CR 800.4a).
            freeForAllHandler.handlePlayerLeft(lobby, playerId)
        } else {
            tournamentMatchHandler.handleAbandon(lobbyId, playerId)
        }
    }

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
    fun createAiTournament(
        setCodes: List<String>,
        playerCount: Int = 2,
        models: List<String>? = null,
        heuristicDeckbuilding: Boolean? = null,
        gamesPerMatch: Int? = null,
    ): String {
        require(aiGameManager.isEnabled) { "AI opponent is not enabled on this server" }
        require(playerCount in 2..8) { "Player count must be between 2 and 8" }
        require(gamesPerMatch == null || gamesPerMatch in 1..9) { "Games per match must be between 1 and 9" }

        val setConfigs = setCodes.map { code ->
            boosterGenerator.getSetConfig(code)
                ?: error("Unknown set code: $code")
        }
        extensionOnlyError(setConfigs)?.let { error(it) }

        val codes = setConfigs.map { it.setCode }
        val boosterCount = 6
        val lobby = TournamentLobby(
            setCodes = codes,
            setNames = setConfigs.map { it.setName },
            boosterGenerator = boosterGenerator,
            format = TournamentFormat.SEALED,
            boosterCount = boosterCount,
            boosterDistribution = TournamentLobby.calculateDefaultDistribution(codes, boosterCount),
            maxPlayers = playerCount,
            gamesPerMatch = gamesPerMatch ?: 3,
        )

        // Add AI players
        repeat(playerCount) { index ->
            val modelOverride = models?.getOrNull(index)
            val aiIdentity = aiGameManager.createAiIdentity(modelOverride = modelOverride)
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
                    basicLands = basicLandInfos,
                    poolPlay = lobby.isCubePoolPlay,
                ))
            }
        }

        // Launch AI deck building in background (handles tournament activation + match start)
        launchAiDeckBuilding(lobby, heuristicDeckbuilding = heuristicDeckbuilding ?: false)

        lobbyRepository.saveLobby(lobby)
        return lobby.lobbyId
    }

    /**
     * Programmatically create an AI-only tournament where each player is handed a fixed,
     * pre-built deck. Skips sealed pool generation and deck-building entirely.
     *
     * Used by `just watch-ai-match` when DECK1/DECK2 paths are passed, and by anyone calling
     * `/api/dev/ai-tournament` with a `decks` field.
     */
    fun createAiTournamentWithFixedDecks(
        decks: List<Map<String, Int>>,
        models: List<String>? = null,
    ): String {
        require(aiGameManager.isEnabled) { "AI opponent is not enabled on this server" }
        require(decks.size in 2..8) { "Player count must be between 2 and 8 (got ${decks.size} decks)" }

        // Validate all decks against the registry up front so we surface bad card names before
        // creating the lobby.
        decks.forEachIndexed { index, deck ->
            val result = deckValidator.validate(deck, format = null)
            require(result.valid) {
                val msg = result.errors.firstOrNull()?.message ?: "Invalid deck"
                "Deck ${index + 1} is invalid: $msg"
            }
        }

        val lobby = TournamentLobby(
            setCodes = emptyList(),
            setNames = emptyList(),
            boosterGenerator = boosterGenerator,
            format = TournamentFormat.PREMADE_DECKS,
            boosterCount = 0,
            boosterDistribution = emptyMap(),
            maxPlayers = decks.size
        )

        val playerIds = mutableListOf<EntityId>()
        decks.forEachIndexed { index, _ ->
            val modelOverride = models?.getOrNull(index)
            val aiIdentity = aiGameManager.createAiIdentity(modelOverride = modelOverride)
            lobby.addPlayer(aiIdentity)
            playerIds += aiIdentity.playerId
        }

        // Submit each AI's pre-built deck while still in WAITING_FOR_PLAYERS — that's the
        // PREMADE_DECKS-permitted state. Pool-free validation runs inside submitDeck.
        playerIds.forEachIndexed { index, playerId ->
            val result = lobby.submitDeck(playerId, decks[index])
            require(result is TournamentLobby.DeckSubmissionResult.Success) {
                "Failed to submit deck for AI player ${index + 1}: ${(result as TournamentLobby.DeckSubmissionResult.Error).message}"
            }
        }

        // Skip DECK_BUILDING; jump straight into the tournament.
        lobby.activatePremadeTournament()
        lobbyRepository.saveLobby(lobby)

        val tournament = tournamentMatchHandler.ensureTournamentCreated(lobby)
        lobby.players.values.forEach { ps ->
            tournamentMatchHandler.sendTournamentStartedToPlayer(lobby, tournament, ps.identity)
        }
        tournamentMatchHandler.autoReadyAiPlayers(lobby, tournament)
        lobbyRepository.saveLobby(lobby)

        logger.info("AI fixed-deck tournament created: ${lobby.lobbyId} (${decks.size} AI players, deck sizes: ${decks.map { it.values.sum() }})")
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
        extensionOnlyError(setConfigs)?.let { error ->
            sender.sendError(session, ErrorCode.INVALID_ACTION, error)
            return
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
            handleLobbyDeckSubmit(
                session,
                playerSession,
                identity,
                lobbyId,
                message.deckList,
                message.commander,
                message.cardEntries,
                message.commanderPrinting,
                message.sideboard,
            )
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
            debugMode = gameProperties.debugMode,
            printingRegistry = printingRegistry,
            tokenArtRegistry = tokenArtRegistry,
        )

        sealedSession.players.forEach { (playerId, playerState) ->
            val baseDeck = playerState.submittedDeck
                ?: throw IllegalStateException("Player $playerId has no submitted deck")
            val deckWithLandArt = BoosterGenerator.withBasicLandArt(baseDeck, sealedSession.basicLands)
            val deck = EasterEggDeckInjector.maybeInjectEasterEggs(
                playerState.session.playerName, deckWithLandArt, gameProperties.easterEggs.enabled
            )
            gameSession.addPlayer(playerState.session, deck, sideboard = playerState.submittedSideboard)

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

    /**
     * Rejection message when a non-empty selection consists solely of extension sets (bonus sheets
     * like The Big Score) — those supplement a regular set's boosters and can't carry a sealed/draft
     * pool on their own. Returns null when the selection is fine.
     */
    private fun extensionOnlyError(setConfigs: List<BoosterGenerator.SetConfig>): String? {
        if (setConfigs.isEmpty() || setConfigs.any { !it.extensionSet }) return null
        val names = setConfigs.joinToString { it.setName }
        val verb = if (setConfigs.size == 1) "is an extension set" else "are extension sets"
        return "$names $verb — add at least one regular set to play with"
    }

    /**
     * How many seats a lobby of this shape holds — the cap people join up to.
     *
     * Since the client stopped offering a Seats control (nobody is asked for a number; players join
     * until the lobby is full), the cap has to follow the shape by itself. It used to be *narrowed*
     * on each switch, which was fine while a host could raise it again: a lobby that dropped to six
     * for Free-for-All and then went back to a bracket would now be stuck at six forever, with no
     * control left to fix it. So every shape change re-derives it rather than clamping it.
     *
     * Format wins over game mode where both have an opinion, which is the order the create path
     * below already used: a Winston lobby is two seats whatever table it claims to be.
     */
    private fun seatCapFor(format: TournamentFormat, gameMode: LobbyGameMode): Int = when {
        format == TournamentFormat.WINSTON_DRAFT -> 2
        format == TournamentFormat.GRID_DRAFT -> 4
        // Two teams of two.
        gameMode == LobbyGameMode.TWO_HEADED_GIANT -> 4
        // Two even teams: 4 (2v2), 6 (3v3) or 8 (4v4) — the even-pod rule is re-checked at start.
        gameMode == LobbyGameMode.TEAM_VS_TEAM -> 8
        // FFA pods cap at 6 — the multiplayer UI lays opponent boards out around the table.
        gameMode == LobbyGameMode.FREE_FOR_ALL -> 6
        else -> 8
    }

    private fun handleCreateTournamentLobby(session: WebSocketSession, message: ClientMessage.CreateTournamentLobby) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        // Validate all set codes. Random-set placeholders (RANDOM_SET_CODE) are deferred — they're
        // kept in the selection as-is and rolled to a concrete set at start (see resolveRandomSets).
        if (message.setCodes.isEmpty()) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "At least one set code is required")
            return
        }
        val hasRandomSet = message.setCodes.any { TournamentLobby.isRandomSetCode(it) }
        val concreteCodes = message.setCodes.filterNot { TournamentLobby.isRandomSetCode(it) }
        val setConfigs = concreteCodes.mapNotNull { boosterGenerator.getSetConfig(it) }
        if (setConfigs.size != concreteCodes.size) {
            val invalidCodes = concreteCodes.filter { boosterGenerator.getSetConfig(it) == null }
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Unknown set codes: ${invalidCodes.joinToString()}")
            return
        }
        // A random slot always resolves to a regular set, so it satisfies the "needs a base set" rule.
        if (!hasRandomSet) {
            extensionOnlyError(setConfigs)?.let { error ->
                sender.sendError(session, ErrorCode.INVALID_ACTION, error)
                return
            }
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

        val gameMode = try {
            LobbyGameMode.valueOf(message.gameMode.uppercase())
        } catch (e: IllegalArgumentException) {
            LobbyGameMode.TOURNAMENT
        }

        // The client's own wizard sends the shape's cap (it no longer asks anyone for a number), so
        // this is really only clamping a hand-rolled or older message.
        val seatCap = seatCapFor(format, gameMode)
        val maxPlayers = when {
            // Exactly four seats, and exactly two — no client number overrides the shape.
            gameMode == LobbyGameMode.TWO_HEADED_GIANT -> seatCap
            format == TournamentFormat.WINSTON_DRAFT -> seatCap
            // Team vs. Team is two even teams: 4 (2v2), 6 (3v3), or 8 (4v4). The even-pod rule is
            // re-checked at game start; an odd cap simply never starts a game.
            gameMode == LobbyGameMode.TEAM_VS_TEAM -> message.maxPlayers.coerceIn(4, seatCap)
            else -> message.maxPlayers.coerceIn(2, seatCap)
        }

        // Set appropriate default booster count based on format
        // Draft: default 3 packs, max 6
        // Commander Draft: default 3 packs, max 6
        // Sealed: default 6 boosters, max 16
        // Winston: default 6 boosters, max 16
        // Commander Sealed: default 8 packs × 20 cards = 160-card pool, max 8
        // Grid Draft: player-count-aware default (2p=9, 3p=13), max 18
        // Premade Decks: unused — players bring their own deck
        //
        // The client transport always carries a boosterCount; the canonical "client default"
        // sentinel is 6. When that sentinel arrives, the server picks the format-appropriate
        // default; otherwise we honor the explicit client value within the format's range.
        val boosterCount = when (format) {
            TournamentFormat.DRAFT -> {
                if (message.boosterCount == 6) 3 else message.boosterCount.coerceIn(1, 6)
            }
            TournamentFormat.COMMANDER_DRAFT -> {
                if (message.boosterCount == 6) 3 else message.boosterCount.coerceIn(1, 6)
            }
            TournamentFormat.COMMANDER_SEALED -> {
                // 8 packs × 20 cards = 160-card sealed pool. 60-card decks need real depth
                // across five colours plus the legendary slot to feel like a build.
                if (message.boosterCount == 6) 8 else message.boosterCount.coerceIn(1, 8)
            }
            TournamentFormat.SEALED, TournamentFormat.WINSTON_DRAFT -> {
                message.boosterCount.coerceIn(1, 16)
            }
            TournamentFormat.GRID_DRAFT -> {
                gridDraftHandler.gridDraftDefaultBoosters(maxPlayers)
            }
            TournamentFormat.PREMADE_DECKS -> 0
        }

        // Preserve the selection order and any random placeholders; names line up index-for-index
        // (placeholders show as "Random Set" until resolveRandomSets reveals them at start).
        val codes = message.setCodes
        val names = message.setCodes.map { code ->
            if (TournamentLobby.isRandomSetCode(code)) TournamentLobby.RANDOM_SET_NAME
            else boosterGenerator.getSetConfig(code)?.setName ?: code
        }
        // Pick-2 is the default for Draft and Commander Draft — speeds the draft and matches the
        // paper Commander Legends template. Other formats stay at the lobby's pick-1 default.
        val initialPicksPerRound = when (format) {
            TournamentFormat.DRAFT, TournamentFormat.COMMANDER_DRAFT -> 2
            else -> 1
        }
        val lobby = TournamentLobby(
            setCodes = codes,
            setNames = names,
            boosterGenerator = boosterGenerator,
            format = format,
            boosterCount = boosterCount,
            boosterDistribution = TournamentLobby.calculateDefaultDistribution(codes, boosterCount),
            maxPlayers = maxPlayers,
            pickTimeSeconds = message.pickTimeSeconds.coerceIn(15, 120),
            picksPerRound = initialPicksPerRound,
            isPublic = message.isPublic,
            // Commander formats enable Chaos boosters by default — 20-card commander packs
            // need to mix sets to give a workable pool when multiple sets are selected. A pack-shape
            // fact, so it stays keyed on the format rather than on the Rules axis.
            chaosBoosters = format.isCommanderFormat,
            // Rules axis. An explicit value wins; otherwise a Commander pack shape defaults it, which
            // is what an older client (which never sends `rules`) meant by picking one.
            rules = message.rules
                ?.let { runCatching { com.wingedsheep.sdk.core.GameRules.valueOf(it.uppercase()) }.getOrNull() }
                ?: com.wingedsheep.sdk.core.GameRules.inferred(
                    commanderPackShape = format.isCommanderFormat,
                    deckFormat = null,
                ),
            aiAssistEnabled = message.aiAssistEnabled,
            gameMode = gameMode,
            attackMode = runCatching { com.wingedsheep.sdk.core.AttackMode.valueOf(message.attackMode.uppercase()) }
                .getOrDefault(com.wingedsheep.sdk.core.AttackMode.MULTIPLE),
            // Ranked only applies to a TOURNAMENT-mode bracket (its matches are 1v1).
            ranked = message.ranked && gameMode == LobbyGameMode.TOURNAMENT,
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
                    basicLands = basicLandInfos,
                    poolPlay = lobby.isCubePoolPlay,
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
                if (lobby.isFreeForAll) {
                    sendFfaActiveState(session, identity, playerSession, lobby)
                } else {
                    sendTournamentActiveState(session, identity, playerSession, lobby)
                }
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
     * Send FFA-mode lobby state to a reconnecting player during TOURNAMENT_ACTIVE: the card pool
     * (so they can still edit between games), the lobby roster, and then either the running game
     * (re-associated) or the latest standings + ready status.
     */
    private fun sendFfaActiveState(
        session: WebSocketSession,
        identity: PlayerIdentity,
        playerSession: PlayerSession?,
        lobby: TournamentLobby
    ) {
        val playerState = lobby.players[identity.playerId]
        val basicLandInfos = lobby.basicLands.values.map { cardToSealedCardInfo(it) }
        val poolInfos = playerState?.cardPool?.map { cardToSealedCardInfo(it) } ?: emptyList()
        sender.send(session, ServerMessage.SealedPoolGenerated(
            setCodes = lobby.setCodes,
            setNames = lobby.setNames,
            cardPool = poolInfos,
            basicLands = basicLandInfos,
            poolPlay = lobby.isCubePoolPlay,
        ))
        sender.send(session, lobby.buildLobbyUpdate(identity.playerId, aiGameManager::isAiPlayer))

        freeForAllHandler.sendReconnectionState(session, identity, playerSession, lobby)
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
            basicLands = basicLandInfos,
            poolPlay = lobby.isCubePoolPlay,
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
                    // Reseat in place — un-seating first would drop the submitted deck and sideboard,
                    // blanking this seat's deck in match history and breaking sideboarding.
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

        // FFA mode: no tournament exists — send the latest standings and point the spectator at
        // the running game (if any) so they can watch it.
        if (lobby.isFreeForAll) {
            lobby.ffaLastStandings?.let { standings ->
                sender.send(session, ServerMessage.FreeForAllGameComplete(
                    lobbyId = lobby.lobbyId,
                    standings = standings,
                    gamesPlayed = lobby.ffaGamesPlayed,
                ))
            }
            val gameSessionId = lobby.ffaGameSessionId
            val gameSession = gameSessionId?.let { gameRepository.findById(it) }
            if (gameSession != null && gameSession.isStarted && !gameSession.isGameOver()) {
                sender.send(session, ServerMessage.FreeForAllGameStarting(
                    lobbyId = lobby.lobbyId,
                    gameSessionId = gameSession.sessionId,
                    gameNumber = lobby.ffaGamesPlayed + 1,
                    players = gameSession.seatInfos(),
                ))
            }
            return
        }

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

        // The one Rules × Table conflict, stated once in `commanderRulesTableConflict`.
        lobby.rulesTableConflict?.let { conflict ->
            sender.sendError(session, ErrorCode.INVALID_ACTION, conflict)
            return
        }

        // Reveal any deferred "Random Set" placeholders now that the game is starting — the concrete
        // sets stay hidden in the lobby until this moment (mirrors the Quick Game deferred roll). Done
        // before the extension gate and pool generation so both see concrete, validated set codes.
        if (!lobby.isCube) lobby.resolveRandomSets()

        lobby.cubeCapacityError()?.let { error ->
            sender.sendError(session, ErrorCode.INVALID_ACTION, error)
            return
        }

        // Booster-based formats can't run on extension sets alone (Premade brings its own decks
        // and ignores the set selection). The lobby may hold an extension-only selection while
        // the host is still assembling it, so this start gate is where the rule is enforced.
        if (lobby.format != TournamentFormat.PREMADE_DECKS && !lobby.isCube) {
            extensionOnlyError(lobby.setCodes.mapNotNull { boosterGenerator.getSetConfig(it) })?.let { error ->
                sender.sendError(session, ErrorCode.INVALID_ACTION, error)
                return
            }
        }

        // Ranked tournaments adjust ELO, so they only count when every seat is a signed-in account
        // (no AI, no guests). Rather than block the start, we downgrade to a casual game and play on —
        // the host doesn't have to chase everyone to sign in just to get a game going.
        if (lobby.ranked) {
            val notSignedIn = lobby.players.values.filter { it.identity.isAi || it.identity.userId == null }
            if (!lobby.rankedEligible || notSignedIn.isNotEmpty()) {
                if (notSignedIn.isNotEmpty()) {
                    logger.info(
                        "Lobby ${lobby.lobbyId}: starting unranked — not all players signed in ({})",
                        notSignedIn.joinToString(", ") { it.identity.playerName },
                    )
                }
                lobby.ranked = false
            }
        }

        when (lobby.format) {
            TournamentFormat.SEALED, TournamentFormat.COMMANDER_SEALED -> {
                val started = lobby.startDeckBuilding(identity.playerId)
                if (!started) {
                    sender.sendError(session, ErrorCode.INVALID_ACTION, "Failed to start lobby")
                    return
                }

                logger.info("Lobby ${lobby.lobbyId} started deck building - ${lobby.format.name} (${lobby.playerCount} players)")

                val basicLandInfos = lobby.basicLands.values.map { cardToSealedCardInfo(it) }
                lobby.players.forEach { (_, playerState) ->
                    val poolInfos = playerState.cardPool.map { cardToSealedCardInfo(it) }
                    val ws = playerState.identity.webSocketSession
                    if (ws != null) {
                        sender.send(ws, ServerMessage.SealedPoolGenerated(
                            setCodes = lobby.setCodes,
                            setNames = lobby.setNames,
                            cardPool = poolInfos,
                            basicLands = basicLandInfos,
                            poolPlay = lobby.isCubePoolPlay,
                        ))
                    }
                }

                // Auto-submit decks for AI players in background so LLM deckbuilding
                // doesn't block the host's WebSocket handler thread
                launchAiDeckBuilding(lobby)
            }

            TournamentFormat.DRAFT, TournamentFormat.COMMANDER_DRAFT -> {
                val started = lobby.startDraft(identity.playerId)
                if (!started) {
                    sender.sendError(session, ErrorCode.INVALID_ACTION, "Failed to start draft")
                    return
                }

                logger.info("Lobby ${lobby.lobbyId} started drafting - ${lobby.format.name} (${lobby.playerCount} players)")

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
            TournamentFormat.PREMADE_DECKS -> {
                // Players brought their own decks during WAITING_FOR_PLAYERS; an AI seat is dealt a
                // generated one when it sits down. Re-asking here is the backstop for a seat whose
                // generation failed, so a transient miss costs a retry rather than the lobby.
                lobby.players.keys.filter { aiGameManager.isAiPlayer(it) }
                    .forEach { ensureAiPremadeDeck(lobby, it) }

                // Require every player to have submitted before the host can start.
                val missing = lobby.players.values.filter { !it.hasSubmittedDeck }
                if (missing.isNotEmpty()) {
                    val names = missing.joinToString(", ") { it.identity.playerName }
                    sender.sendError(
                        session,
                        ErrorCode.INVALID_ACTION,
                        "Players have not submitted a deck yet: $names"
                    )
                    return
                }

                // Skip DECK_BUILDING entirely; jump straight into the tournament.
                lobby.activatePremadeTournament()
                logger.info("Lobby ${lobby.lobbyId} started premade-decks ${lobby.gameMode.name.lowercase()} (${lobby.playerCount} players)")

                if (lobby.isFreeForAll) {
                    // FFA mode: everyone's deck is in — start the one multiplayer game now.
                    val lock = ctx.roundLocks.computeIfAbsent(lobby.lobbyId) { Any() }
                    synchronized(lock) {
                        freeForAllHandler.maybeStartGame(lobby)
                    }
                } else {
                    val tournament = tournamentMatchHandler.ensureTournamentCreated(lobby)
                    lobby.players.values.forEach { ps ->
                        tournamentMatchHandler.sendTournamentStartedToPlayer(lobby, tournament, ps.identity)
                    }
                    tournamentMatchHandler.autoReadyAiPlayers(lobby, tournament)
                }
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
        ensureAiPremadeDeck(lobby, aiIdentity.playerId)
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
     * Host picks what one AI seat plays.
     *
     * The per-seat twin of [QuickGameLobbyHandler.handleSetAiDeck], and deliberately the same three
     * answers: let the server decide, pin the pool to some sets, or hand it an exact list. Storing
     * the spec and re-rolling immediately (rather than resolving at game start, as the quick lobby
     * does) is what the premade start gate requires — it wants every seat to have submitted, so the
     * seat's deck has to exist while the host is still looking at the lobby.
     */
    private fun handleSetLobbyAiDeck(session: WebSocketSession, message: ClientMessage.SetLobbyAiDeck) {
        val (identity, lobby) = ctx.getIdentityAndLobby(session) ?: return

        if (!lobby.isHost(identity.playerId)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can choose an AI's deck")
            return
        }
        if (lobby.state != LobbyState.WAITING_FOR_PLAYERS) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Can only change an AI's deck while waiting for players")
            return
        }
        if (lobby.format != TournamentFormat.PREMADE_DECKS) {
            // Not a silent no-op: in a limited lobby the AI plays the cards it was dealt, and a host
            // who picked a deck for it should be told that is not how this format works.
            sender.sendError(
                session,
                ErrorCode.INVALID_ACTION,
                "The AI builds its deck from the pool it is dealt in this format — pick Bring a deck to choose one for it"
            )
            return
        }

        val aiPlayerId = EntityId(message.playerId)
        val playerState = lobby.players[aiPlayerId]
        if (playerState == null || !aiGameManager.isAiPlayer(aiPlayerId)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "No such AI player in this lobby")
            return
        }

        // A fixed list is refused at the point the host chooses it rather than seated and found
        // illegal later; sets are only checked for existence, since whether the pool can carry a
        // deck is the resolver's problem and it falls back rather than fails.
        val spec = message.spec
        if (spec is AiDeckSpec.Fixed) {
            if (spec.deckList.isEmpty()) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "The AI's deck is empty")
                return
            }
            val validation = validateAiDeck(spec, lobby.deckFormat, lobby.usesCommanderRules)
            if (!validation.valid) {
                val reason = validation.errors.firstOrNull()?.message ?: "Deck is not legal"
                sender.sendError(session, ErrorCode.INVALID_DECK, "AI deck rejected: $reason")
                return
            }
        }

        playerState.aiDeckSpec = spec
        lobby.discardSubmittedDeck(aiPlayerId)
        ensureAiPremadeDeck(lobby, aiPlayerId)
        lobbyRepository.saveLobby(lobby)

        logger.info("Host set AI {} deck to {} in lobby {}", aiPlayerId.value, spec::class.simpleName, lobby.lobbyId)
        ctx.broadcastLobbyUpdate(lobby)
    }

    /**
     * Roll a deck for an AI seat in a **premade-decks** lobby, the way a quick game already does.
     *
     * Premade decks is the one format with no pool for [buildAiSealedDeck] to work from, which is
     * why AI seats used to be refused here outright. But a seat that has to bring a deck and has
     * none is exactly the quick lobby's `vsAi` seat, and that has always been answered by generating
     * one — [com.wingedsheep.gameserver.ai.RandomDeckResolver] builds to the lobby's deck-legality
     * axis when it has one and opens a sealed pool otherwise. Reusing it here means a premade lobby
     * seats an AI under the same rule as a quick game, rather than under a second one.
     *
     * What it plays is the host's answer for *that seat* ([LobbyPlayerState.aiDeckSpec]), defaulting
     * to Auto; the lobby's own set selection stands in for the quick lobby's "the same set as the
     * human". Only ever generates for a seat that has no deck, so it is safe to call repeatedly.
     */
    private fun ensureAiPremadeDeck(lobby: TournamentLobby, aiPlayerId: EntityId) {
        if (lobby.format != TournamentFormat.PREMADE_DECKS) return
        val playerState = lobby.players[aiPlayerId] ?: return
        if (playerState.hasSubmittedDeck) return

        val generated = runCatching {
            randomDeckResolver.resolve(
                playerState.aiDeckSpec,
                lobby.deckFormat,
                lobby.setCodes,
                lobby.usesCommanderRules,
            )
        }
            .onFailure { logger.error("Could not generate a deck for AI seat ${aiPlayerId.value}", it) }
            .getOrNull() ?: return

        val commander = generated.commander?.takeIf { lobby.usesCommanderRules }
        // Under Commander rules a deck with no commander can't be seated at all, so leave the seat
        // un-submitted rather than submit one: the lobby's own start gate then blocks on "not every
        // seat has a deck", which is a state the host can fix by picking a deck for the AI.
        if (lobby.usesCommanderRules && commander == null) {
            logger.warn(
                "No commander for AI seat {} in lobby {}; leaving the seat without a deck",
                aiPlayerId.value,
                lobby.lobbyId,
            )
            return
        }
        // Submitted in wire form — commander counted in, as `LobbyPlayerState.commander` documents
        // and as the match handlers' strip-at-start expects.
        val submitted = if (commander != null) generated.submissionList else generated.deckList
        when (val result = lobby.submitDeck(aiPlayerId, submitted, commander = commander)) {
            is TournamentLobby.DeckSubmissionResult.Success ->
                logger.info(
                    "AI {} brought a generated {} deck ({} cards{}) to premade lobby {}",
                    playerState.identity.playerName,
                    lobby.deckFormat?.displayName ?: "sealed",
                    generated.totalCards,
                    commander?.let { ", led by $it" } ?: "",
                    lobby.lobbyId,
                )
            is TournamentLobby.DeckSubmissionResult.Error ->
                logger.warn("Generated AI deck rejected in lobby ${lobby.lobbyId}: ${result.message}")
        }
    }

    private fun validateAiDeck(
        spec: AiDeckSpec.Fixed,
        format: com.wingedsheep.sdk.core.DeckFormat?,
        usesCommanderRules: Boolean,
    ) = if (usesCommanderRules) {
        deckValidator.validate(
            com.wingedsheep.sdk.model.Deck(
                cards = spec.deckList.flatMap { (name, count) -> List(count) { name } },
                commander = spec.commander?.takeIf { it.isNotBlank() },
            ),
            format,
        )
    } else {
        deckValidator.validate(spec.deckList, format)
    }

    /**
     * Keep every AI seat's deck consistent with the shape the lobby now has.
     *
     * The host can change the format or the deck-legality axis after seating an AI, and either
     * makes the deck it is holding wrong: a generated premade deck built to no restriction is not
     * legal under a restriction the host adds afterwards, and it has no business surviving a switch
     * to a limited format, where the AI must build from the pool it is dealt like everyone else.
     * Dropping it and re-rolling covers both, and touches only AI seats — a human's submitted deck
     * is theirs to resubmit.
     */
    private fun resyncAiDecks(lobby: TournamentLobby) {
        for (aiPlayerId in lobby.players.keys.filter { aiGameManager.isAiPlayer(it) }) {
            lobby.discardSubmittedDeck(aiPlayerId)
            // A no-op outside premade decks, which is the point: in a limited lobby the AI now has
            // no deck and builds one from the pool it gets dealt, like every other seat.
            ensureAiPremadeDeck(lobby, aiPlayerId)
        }
    }

    /**
     * Launch AI deck building in a background coroutine so it doesn't block the
     * host's WebSocket handler thread. LLM deckbuilding can take 30+ seconds,
     * and blocking would prevent the human player from submitting their own deck.
     */
    private fun launchAiDeckBuilding(lobby: TournamentLobby, heuristicDeckbuilding: Boolean = false) {
        val aiPlayers = lobby.players.filter { (playerId, ps) ->
            aiGameManager.isAiPlayer(playerId) && !ps.hasSubmittedDeck && ps.cardPool.isNotEmpty()
        }
        if (aiPlayers.isEmpty()) return

        ctx.draftScope.launch(Dispatchers.IO) {
            for ((playerId, playerState) in aiPlayers) {
                try {
                    val built = buildAiPoolDeck(lobby, playerState.cardPool, heuristicDeckbuilding)
                    val submitted = built.submissionList
                    val result = lobby.submitDeck(playerId, submitted, commander = built.commander)
                    when (result) {
                        is TournamentLobby.DeckSubmissionResult.Success -> {
                            logger.info(
                                "AI {} auto-submitted a {}-card deck{}",
                                playerState.identity.playerName,
                                submitted.values.sum(),
                                built.commander?.let { " led by $it" } ?: "",
                            )
                        }
                        is TournamentLobby.DeckSubmissionResult.Error -> {
                            logger.warn("AI deck submission failed: ${result.message}")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("AI deck building failed for ${playerState.identity.playerName}: ${e.message}", e)
                }
            }

            // After AI decks are built, broadcast updated lobby state and handle readiness
            ctx.broadcastLobbyUpdate(lobby)

            if (lobby.allDecksSubmitted() && lobby.state == LobbyState.DECK_BUILDING) {
                lobby.activateTournament()
            }

            if (lobby.isFreeForAll) {
                // A pod has no bracket to build — the last deck in *is* the start signal, exactly as
                // it is when the last human submits (handleSubmitSealedDeck). Routing an FFA lobby
                // through ensureTournamentCreated would stand up a TournamentManager it never uses
                // and leave the game unstarted whenever the AI happened to submit last.
                // maybeStartGame carries its own preconditions, so it is safe to ask every time.
                val lock = ctx.roundLocks.computeIfAbsent(lobby.lobbyId) { Any() }
                synchronized(lock) {
                    freeForAllHandler.maybeStartGame(lobby)
                }
            } else {
                // Create tournament if needed and auto-ready AI players
                val tournament = tournamentMatchHandler.ensureTournamentCreated(lobby)
                tournamentMatchHandler.autoReadyAiPlayers(lobby, tournament)
            }
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
     * The deck an AI seat builds from the pool it was dealt.
     *
     * Two shapes, chosen by the lobby's Rules axis rather than by its pool format: a commander game
     * needs a designated commander, a singleton-ish deck inside its colour identity and the lobby's
     * own deck size, none of which the 40-card sealed autobuilder models. A commander build that
     * finds no legal commander in the pool falls back to the sealed one — the resulting deck can't
     * be seated under Commander rules, but reporting *that* through the normal submission path is
     * more useful than throwing out of a background coroutine.
     */
    private fun buildAiPoolDeck(
        lobby: TournamentLobby,
        pool: List<com.wingedsheep.sdk.model.CardDefinition>,
        heuristic: Boolean,
    ): com.wingedsheep.ai.engine.deck.GeneratedDeck {
        if (lobby.usesCommanderRules) {
            val built = runCatching { commanderDeckGenerator.generateFromPool(pool, lobby.deckSizeMin, lobby.allowDuplicates) }
                .onFailure { logger.error("Commander deck build from pool failed in lobby ${lobby.lobbyId}", it) }
                .getOrNull()
            if (built != null) return built
            logger.warn(
                "No legal commander in the {}-card pool for lobby {}; falling back to a sealed build",
                pool.size,
                lobby.lobbyId,
            )
        }
        return com.wingedsheep.ai.engine.deck.GeneratedDeck(buildAiSealedDeck(pool, heuristic))
    }

    /**
     * Build a 40-card sealed deck from a card pool using the LLM.
     * Falls back to a color-based heuristic if the LLM fails.
     */
    private fun buildAiSealedDeck(pool: List<com.wingedsheep.sdk.model.CardDefinition>, heuristic: Boolean = false): Map<String, Int> {
        logger.info("AI building sealed deck from pool of {} cards (heuristic={})", pool.size, heuristic)

        val aiProperties = gameProperties.ai
        val forceHeuristic = heuristic || aiProperties.heuristicDeckbuilding
        if (!forceHeuristic && aiProperties.enabled && aiProperties.effectiveApiKey.isNotBlank()) {
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
                    val oracle = if (card.oracleText.isNotBlank()) " — ${card.oracleText.replace("\n", " / ")}" else ""
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
                    val oracle = if (card.oracleText.isNotBlank()) " — ${card.oracleText.replace("\n", " / ")}" else ""
                    appendLine("  $count${card.name}$oracle")
                }
            }

            appendLine()
            appendLine("Reply ONLY with the deck list, one entry per line:")
            appendLine("1x Card Name")
            appendLine("9x Forest")
        }

        val aiConfig = com.wingedsheep.ai.llm.AiConfig(
            enabled = aiProperties.enabled, mode = aiProperties.mode,
            baseUrl = aiProperties.baseUrl, apiKey = aiProperties.apiKey,
            openRouterApiKey = aiProperties.openRouterApiKey, model = aiProperties.model,
            deckbuildingModel = aiProperties.deckbuildingModel,
            reasoningEffort = aiProperties.reasoningEffort, maxRetries = aiProperties.maxRetries,
            timeoutMs = aiProperties.timeoutMs, thinkingDelayMs = aiProperties.thinkingDelayMs
        )
        val client = com.wingedsheep.ai.llm.LlmClient(aiConfig)
        val messages = listOf(
            com.wingedsheep.ai.llm.ChatMessage("system",
                "You are an expert Magic: The Gathering limited deckbuilder. " +
                "Analyze the sealed pool, pick the best 2 colors (with optional light splash), " +
                "and build a strong 40-card deck. Reply ONLY with the deck list."),
            com.wingedsheep.ai.llm.ChatMessage("user", prompt)
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
        val deck = com.wingedsheep.ai.engine.buildHeuristicSealedDeck(pool)
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

        // A departing FFA player's seat is conceded; the game continues for the rest (CR 800.4a).
        if (lobby.isFreeForAll) {
            freeForAllHandler.handlePlayerLeft(lobby, identity.playerId)
        } else {
            // Explicitly leaving a bracket is permanent, unlike a disconnect. Concede any match
            // that is already running so its game session and AI controller cannot outlive the
            // player who quit the tournament.
            val activeMatch = lobbyRepository.findTournamentById(lobbyId)
                ?.getAllInProgressMatches()
                ?.firstOrNull { it.player1Id == identity.playerId || it.player2Id == identity.playerId }
            activeMatch?.gameSessionId
                ?.let(gameRepository::findById)
                ?.takeUnless(GameSession::isGameOver)
                ?.let { gamePlayHandler.concedeSeat(it, identity.playerId) }
        }

        // Use forceRemovePlayer for explicit leave - player cannot rejoin
        lobby.forceRemovePlayer(identity.playerId)
        identity.currentLobbyId = null

        logger.info("Player ${identity.playerName} left lobby $lobbyId (cannot rejoin)")

        if (lobby.playerCount == 0) {
            tournamentResultSink.recordAbandoned(lobbyId)
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

        if (lobby.isFreeForAll) {
            freeForAllHandler.handlePlayerLeft(lobby, identity.playerId)
        }

        lobby.removePlayer(identity.playerId)
        identity.currentLobbyId = null

        logger.info("Player ${identity.playerName} auto-left lobby $lobbyId")

        if (lobby.playerCount == 0) {
            tournamentResultSink.recordAbandoned(lobbyId)
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

        // Can only stop during WAITING_FOR_PLAYERS, DRAFTING, or DECK_BUILDING (not during active
        // tournament). An FFA pod has no natural end, so its host may disband it between games.
        val ffaBetweenGames = lobby.isFreeForAll && lobby.ffaGameSessionId == null
        if ((lobby.state == LobbyState.TOURNAMENT_ACTIVE && !ffaBetweenGames) || lobby.state == LobbyState.TOURNAMENT_COMPLETE) {
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
        tournamentResultSink.recordAbandoned(lobbyId)
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
        if (lobby.isFreeForAll && lobby.ffaGameSessionId != null) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Cannot edit deck - game in progress")
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

        // What an AI seat's generated deck was built for. Compared at the end: these axes decide
        // both what may be in the deck and whether it needs a designated commander.
        val formatBefore = lobby.format
        val deckFormatBefore = lobby.deckFormat
        val rulesBefore = lobby.rules

        // Cube settings are a full replacement, like the ban list. Resolve immediately so an
        // unplayable list never becomes lobby state. Empty returns to catalogued-set mode.
        message.cubeCards?.let { rawNames ->
            val names = rawNames.map { it.trim() }.filter { it.isNotEmpty() }
            if (names.isEmpty()) {
                lobby.configureCube(null)
                lobby.setCodes = emptyList()
                lobby.setNames = emptyList()
                lobby.boosterDistribution = emptyMap()
            } else {
                val name = message.cubeName?.trim().orEmpty()
                if (name.isEmpty()) {
                    sender.sendError(session, ErrorCode.INVALID_ACTION, "Cube name is required")
                    return
                }
                val basicLandSetCode = message.cubeBasicLandSetCode?.trim().orEmpty()
                if (boosterGenerator.getSetConfig(basicLandSetCode) == null) {
                    sender.sendError(session, ErrorCode.INVALID_ACTION, "Invalid cube basic-land set code: $basicLandSetCode")
                    return
                }
                val cubeList = runCatching {
                    CubeList(
                        name = name,
                        cards = names.map { CubeCardEntry(it) },
                        basicLandSetCode = basicLandSetCode,
                        packSize = message.packSize ?: CubeList.DEFAULT_PACK_SIZE,
                    )
                }.getOrElse {
                    sender.sendError(session, ErrorCode.INVALID_ACTION, it.message ?: "Invalid cube settings")
                    return
                }
                when (val resolution = CubeResolver(cardRegistry, printingRegistry).resolve(cubeList)) {
                    is CubeResolution.Success -> lobby.configureCube(resolution.cube)
                    is CubeResolution.Failure -> {
                        val misses = resolution.unresolved.joinToString(", ") { it.name }
                        sender.sendError(
                            session,
                            ErrorCode.INVALID_ACTION,
                            "${resolution.unresolved.size} cube cards aren't implemented: $misses",
                        )
                        return
                    }
                }
            }
        }

        // Pool Play is cube-Sealed-only: on a set-based lobby there is no whole pool to build from,
        // and every drafting format contradicts "no draft". Reject rather than accept-and-ignore.
        message.cubePoolPlay?.let { poolPlay ->
            val requestedFormat = message.format?.let { runCatching { TournamentFormat.valueOf(it) }.getOrNull() }
                ?: lobby.format
            if (poolPlay && !lobby.isCube) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Pool Play requires a cube")
                return
            }
            if (poolPlay && requestedFormat != TournamentFormat.SEALED) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Pool Play is only available for Sealed")
                return
            }
            lobby.cubePoolPlay = poolPlay
        }

        // Update sets if provided (can be empty to disable start)
        if (!lobby.isCube) message.setCodes?.let { newSetCodes ->
            // Allow empty setCodes to disable start button (but won't be able to start)
            // An extension-only selection is allowed here as an intermediate state (the host may
            // add the extension set first, then the base set) — the start handler is the gate.
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
                val wasCommander = lobby.format.isCommanderFormat
                lobby.format = newFormat
                // Pool Play only exists for cube Sealed; don't leave it set (and shown) on a format
                // that ignores it.
                if (newFormat != TournamentFormat.SEALED) lobby.cubePoolPlay = false
                lobby.maxPlayers = seatCapFor(newFormat, lobby.gameMode)
                // Commander draft / sealed plays best with a broad mix of sets, so when the
                // host first switches into a commander format we replace the single-set
                // default with a curated mix (DOM / BLB / ECL / KTK) for variety. Honor any
                // explicit setCodes the same message also carries (handled above).
                if (newFormat.isCommanderFormat && !wasCommander && message.setCodes == null) {
                    val commanderDefaults = listOf("DOM", "BLB", "ECL", "KTK")
                        .filter { boosterGenerator.getSetConfig(it) != null }
                    if (commanderDefaults.isNotEmpty()) {
                        lobby.updateSets(commanderDefaults)
                    }
                }
                lobby.boosterCount = when (newFormat) {
                    TournamentFormat.DRAFT -> 3
                    TournamentFormat.COMMANDER_DRAFT -> 3
                    TournamentFormat.COMMANDER_SEALED -> 8
                    TournamentFormat.SEALED -> 6
                    TournamentFormat.WINSTON_DRAFT -> 6
                    TournamentFormat.GRID_DRAFT -> gridDraftHandler.gridDraftDefaultBoosters(lobby.players.size)
                    TournamentFormat.PREMADE_DECKS -> 0
                }
                // Reset picksPerRound to a sensible default for the new format. Draft / Commander
                // Draft default to Pick 2 (speeds the draft); all others go back to Pick 1.
                lobby.picksPerRound = when (newFormat) {
                    TournamentFormat.DRAFT, TournamentFormat.COMMANDER_DRAFT -> 2
                    else -> 1
                }
                // Commander formats enable Chaos boosters by default — 20-card commander packs
                // need to mix sets to give a workable card pool when multiple sets are selected.
                lobby.chaosBoosters = newFormat.isCommanderFormat
                lobby.recalculateDistribution()
            }
        }

        // Deck legality (Cards → Bring a deck) and the Rules axis are *resolved* here and applied
        // below, because the conflict check between them has to see the values this message is
        // setting rather than the previous ones — and has to run before anything is written, so a
        // refused message leaves the lobby exactly as it was.
        //
        // Empty string or the sentinel "NONE" clears the restriction; unknown values are silently
        // ignored so a future client/server skew can't break older lobbies.
        val nextDeckFormat: com.wingedsheep.sdk.core.DeckFormat? =
            if (message.deckFormat == null) lobby.deckFormat
            else message.deckFormat.let { value ->
                if (value.isBlank() || value.equals("NONE", ignoreCase = true)) {
                    null
                } else {
                    runCatching { com.wingedsheep.sdk.core.DeckFormat.valueOf(value.uppercase()) }
                        .getOrNull() ?: lobby.deckFormat
                }
            }

        // Rules axis. An explicit value always wins — the host owns this row. Otherwise a message that
        // switched the pack shape into Commander, or set a commander-shaped deck legality, *defaults*
        // it: those used to be the only ways to ask for Commander, so a client that doesn't know about
        // the axis still gets what it meant. Nothing here resets it — switching the pack shape back
        // leaves Commander rules on, because the axes are independent.
        val requestedRules = message.rules
            ?.let { runCatching { com.wingedsheep.sdk.core.GameRules.valueOf(it.uppercase()) }.getOrNull() }
        // Only what *this* message set can default the axis — re-inferring from the lobby's standing
        // fields would overrule the host every time they touched an unrelated setting.
        val defaultedByThisMessage = com.wingedsheep.sdk.core.GameRules.inferred(
            commanderPackShape = message.format != null && lobby.format.isCommanderFormat,
            deckFormat = if (message.deckFormat != null) nextDeckFormat else null,
        )
        val nextRules = when {
            requestedRules != null -> requestedRules
            // An unparseable value leaves the axis alone, matching how commanderPreset is parsed.
            message.rules != null -> lobby.rules
            defaultedByThisMessage.usesCommanders -> defaultedByThisMessage
            else -> lobby.rules
        }

        // Rules × Table, against the table this message leaves the lobby at — the single statement in
        // `commanderRulesTableConflict`, checked once for every way in. Commander deck legality counts
        // as asking for Commander rules because it defaults them, and it means it: CR 903.4 anchors
        // colour identity to the commander, so "Commander legality, Standard rules" is a deck the
        // validator can only half-check. Refusing here rather than at Start is what keeps a lobby from
        // reaching a state it can never leave.
        val nextIsTwoHeadedGiant = message.gameMode
            ?.let { runCatching { LobbyGameMode.valueOf(it.uppercase()) }.getOrNull() }
            ?.let { it == LobbyGameMode.TWO_HEADED_GIANT }
            ?: lobby.isTwoHeadedGiant
        commanderRulesTableConflict(nextRules, nextIsTwoHeadedGiant)?.let { conflict ->
            sender.sendError(session, ErrorCode.INVALID_ACTION, conflict)
            return
        }

        // Generated limited pools do not designate commanders. A premade lobby is different: the
        // host can choose a commander deck for every AI seat after switching the rules.
        if (nextRules.usesCommanders && !lobby.usesCommanderRules &&
            lobby.format != TournamentFormat.PREMADE_DECKS &&
            lobby.players.keys.any { aiGameManager.isAiPlayer(it) }
        ) {
            sender.sendError(
                session,
                ErrorCode.INVALID_ACTION,
                "The AI can't build a Commander deck from a limited pool yet — use Bring a deck instead"
            )
            return
        }

        lobby.deckFormat = nextDeckFormat
        lobby.rules = nextRules

        // Game-mode switch (mode axis is orthogonal to format). Each multiplayer mode caps the pod
        // at its own seat count; AI seats are ordinary seats here and count toward those caps, so
        // switching modes with AI in the lobby needs no separate check.
        message.gameMode?.let { modeStr ->
            val newMode = runCatching { LobbyGameMode.valueOf(modeStr.uppercase()) }.getOrNull()
            if (newMode == null) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Invalid game mode: $modeStr")
                return
            }
            if (newMode != lobby.gameMode) {
                // Rules × Table is already settled above, against the mode this message is switching
                // to — checked there rather than here so the deck-legality route through the same
                // conflict is covered by the same line.
                when (newMode) {
                    // Two teams of two — always exactly four seats.
                    LobbyGameMode.TWO_HEADED_GIANT -> if (lobby.playerCount > 4) {
                        sender.sendError(session, ErrorCode.INVALID_ACTION, "Two-Headed Giant is exactly four players")
                        return
                    }
                    // Two even teams: at most eight players (4v4).
                    LobbyGameMode.TEAM_VS_TEAM -> if (lobby.playerCount > 8) {
                        sender.sendError(session, ErrorCode.INVALID_ACTION, "Team vs. Team supports at most 8 players")
                        return
                    }
                    LobbyGameMode.FREE_FOR_ALL -> if (lobby.playerCount > 6) {
                        sender.sendError(session, ErrorCode.INVALID_ACTION, "Free-for-All supports at most 6 players")
                        return
                    }
                    LobbyGameMode.TOURNAMENT -> Unit
                }
                lobby.gameMode = newMode
                lobby.maxPlayers = seatCapFor(lobby.format, newMode)
            }
        }

        // Free-for-All attack rule (CR 802/803). Stored regardless of mode; only consumed when a
        // Free-for-All game starts. Invalid values are ignored (settings left unchanged).
        message.attackMode?.let { modeStr ->
            runCatching { com.wingedsheep.sdk.core.AttackMode.valueOf(modeStr.uppercase()) }
                .onSuccess { lobby.attackMode = it }
        }

        // Two-Headed Giant team setup (CR 810). Stored regardless of mode; only consumed when a 2HG
        // game starts. randomTeams=true re-rolls teams each game; when false the host's
        // teamAssignments (playerId -> team) are used, balanced/falling back if incomplete.
        message.randomTeams?.let { lobby.randomTeams = it }
        message.teamAssignments?.let { assignments ->
            lobby.setTeamAssignments(assignments.mapKeys { com.wingedsheep.sdk.model.EntityId(it.key) })
        }

        // Manual boosterCount override (apply after format change)
        // Grid draft uses fixed booster counts based on player count — no manual override.
        // Premade decks doesn't use boosters at all — ignore.
        if (lobby.format != TournamentFormat.GRID_DRAFT && lobby.format != TournamentFormat.PREMADE_DECKS) {
            message.boosterCount?.let {
                val maxCount = when (lobby.format) {
                    TournamentFormat.DRAFT -> 6
                    TournamentFormat.COMMANDER_DRAFT -> 6
                    TournamentFormat.COMMANDER_SEALED -> 8
                    TournamentFormat.SEALED -> 16
                    TournamentFormat.WINSTON_DRAFT -> 16
                    TournamentFormat.GRID_DRAFT -> 24 // unreachable
                    TournamentFormat.PREMADE_DECKS -> 0 // unreachable
                }
                lobby.boosterCount = it.coerceIn(1, maxCount)
                lobby.recalculateDistribution()
            }
        }

        // Manual booster distribution override (apply after boosterCount)
        if (!lobby.isCube) message.boosterDistribution?.let { dist ->
            // Validate: all keys must be in setCodes, values must be positive, total must equal boosterCount
            val validKeys = dist.keys.all { it in lobby.setCodes }
            val allPositive = dist.values.all { it >= 0 }
            val totalMatches = dist.values.sum() == lobby.boosterCount
            if (validKeys && allPositive && totalMatches) {
                lobby.boosterDistribution = dist
            }
        }
        // No client asks for a seat count any more — the lobby holds what its shape allows and people
        // join until it is full — but an explicit request is still honoured within that cap, so an
        // older or hand-rolled client isn't silently ignored.
        message.maxPlayers?.let {
            val oldMaxPlayers = lobby.maxPlayers
            val cap = seatCapFor(lobby.format, lobby.gameMode)
            lobby.maxPlayers = when {
                // Locked by the shape — the request can't move these.
                lobby.isTwoHeadedGiant -> cap
                lobby.format == TournamentFormat.WINSTON_DRAFT -> cap
                // Team vs. Team is two even teams, so never fewer than four.
                lobby.isTeamVsTeam -> it.coerceIn(4, cap)
                else -> it.coerceIn(2, cap)
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
        message.isPublic?.let { lobby.isPublic = it }

        // Commander deckbuild knobs. Silently ignored when the lobby isn't running Commander rules,
        // so a client sending stale settings can't break the lobby.
        if (lobby.usesCommanderRules) {
            message.deckSizeMin?.let { lobby.deckSizeMin = it.coerceIn(40, 100) }
            message.allowDuplicates?.let { lobby.allowDuplicates = it }
            message.commanderPreset?.let { value ->
                runCatching { com.wingedsheep.sdk.core.CommanderPreset.valueOf(value.uppercase()) }
                    .getOrNull()?.let { lobby.commanderPreset = it }
            }
        }

        if (!lobby.isCube) message.chaosBoosters?.let { lobby.chaosBoosters = it }
        if (!lobby.isCube) message.includedSetProducts?.let { selections ->
            lobby.includedSetProducts = selections
                .filterKeys { it in lobby.setCodes }
                .mapValues { (code, ids) ->
                    val available = boosterGenerator.getSetConfig(code)?.extraCardsByProduct?.keys.orEmpty()
                    ids.filterTo(linkedSetOf()) { it in available }
                }
                .filterValues { it.isNotEmpty() }
        }

        // Host ban list — the full list is sent each time (not a delta). Trim, drop blanks and
        // duplicates; unknown names are kept as-is (they simply never match a card in the pool),
        // so the editor round-trips exactly what the host typed/picked.
        message.bannedCardNames?.let { names ->
            lobby.bannedCardNames = names.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }

        message.aiAssistEnabled?.let { lobby.aiAssistEnabled = it }

        message.ranked?.let { lobby.ranked = it }
        // Ranked only applies to a TOURNAMENT-mode bracket (1v1 matches); a mode switch in this same
        // update — or one that landed earlier — forces it back off.
        if (!lobby.rankedEligible) lobby.ranked = false

        // Re-roll only when an axis that determines the AI deck changed. Under Commander rules,
        // ensureAiPremadeDeck deliberately leaves Auto unsubmitted until the host picks a deck.
        if (lobby.format != formatBefore ||
            lobby.deckFormat != deckFormatBefore ||
            lobby.rules != rulesBefore
        ) {
            resyncAiDecks(lobby)
        }

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
        deckList: Map<String, Int>,
        commander: String? = null,
        cardEntries: List<com.wingedsheep.gameserver.protocol.DeckEntryDTO>? = null,
        commanderPrinting: com.wingedsheep.sdk.model.PrintingRef? = null,
        sideboard: Map<String, Int> = emptyMap(),
    ) {
        val lobby = lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found")
            return
        }

        // A commander is meaningful exactly when the lobby runs Commander rules — one question, one
        // field, whether the cards came from a brought deck, a sealed pool or any draft shape.
        // Outside those, drop a stale commander so a saved deck doesn't leak its commander into a
        // Standard game.
        val commanderShape = lobby.usesCommanderRules
        val effectiveCommander = if (commanderShape) commander?.takeIf { it.isNotBlank() } else null
        val effectiveCommanderPrinting = if (commanderShape) commanderPrinting else null

        // A pool-built lobby validates the commander deck against the player's *pool* (its legality
        // universe) rather than against Scryfall legality — so the branch is "Commander rules over a
        // generated pool", i.e. anything that isn't PREMADE_DECKS. Singleton + min-size knobs come
        // from the lobby's host configuration.
        if (commanderShape && lobby.format != TournamentFormat.PREMADE_DECKS) {
            val pool = lobby.players[identity.playerId]?.cardPool ?: emptyList()
            val withoutCommander = effectiveCommander
                ?.let { stripCommanderFromCards(deckList, it) }
                ?: deckList
            val deckCards = withoutCommander.flatMap { (name, count) -> List(count) { name } }
            val richEntries = cardEntries
                ?.filterNot { it.name == effectiveCommander && it.printing == effectiveCommanderPrinting }
                ?.takeIf { it.isNotEmpty() }
            val deck = if (richEntries != null) {
                com.wingedsheep.sdk.model.Deck.fromEntries(
                    entries = richEntries.map { com.wingedsheep.sdk.model.CardEntry(it.name, it.printing) },
                    commander = effectiveCommander,
                    commanderPrinting = effectiveCommanderPrinting,
                )
            } else {
                com.wingedsheep.sdk.model.Deck(
                    cards = deckCards,
                    commander = effectiveCommander,
                    commanderPrinting = effectiveCommanderPrinting,
                )
            }
            val validation = deckValidator.validateCommanderLimited(
                deck = deck,
                pool = pool,
                minDeckSize = lobby.deckSizeMin,
                allowDuplicates = lobby.allowDuplicates,
            )
            if (!validation.valid) {
                val msg = validation.errors.firstOrNull()?.message ?: "Invalid deck"
                sender.sendError(session, ErrorCode.INVALID_DECK, msg)
                return
            }
        }

        // For PREMADE_DECKS, the deck didn't come from a generated card pool — validate
        // against the registry (≥40 cards, 4-of, all cards must resolve) before storing it.
        // If the host has set a deck-construction format, also enforce per-card legality.
        // Commander-shape formats validate via the structured Deck overload so the singleton +
        // color-identity rules kick in (matches QuickGameLobbyHandler.handleSubmitDeck).
        if (lobby.format == TournamentFormat.PREMADE_DECKS) {
            val validation = if (commanderShape) {
                // Always go through the commander-aware path for commander-shape formats so a
                // missing commander surfaces as MISSING_COMMANDER instead of silently passing.
                val withoutCommander = effectiveCommander
                    ?.let { stripCommanderFromCards(deckList, it) }
                    ?: deckList
                val deckCards = withoutCommander.flatMap { (name, count) -> List(count) { name } }
                val richEntries = cardEntries
                    ?.filterNot { it.name == effectiveCommander && it.printing == effectiveCommanderPrinting }
                    ?.takeIf { it.isNotEmpty() }
                val deck = if (richEntries != null) {
                    com.wingedsheep.sdk.model.Deck.fromEntries(
                        entries = richEntries.map {
                            com.wingedsheep.sdk.model.CardEntry(it.name, it.printing)
                        },
                        commander = effectiveCommander,
                        commanderPrinting = effectiveCommanderPrinting,
                    )
                } else {
                    com.wingedsheep.sdk.model.Deck(
                        cards = deckCards,
                        commander = effectiveCommander,
                        commanderPrinting = effectiveCommanderPrinting,
                    )
                }
                deckValidator.validate(deck, lobby.deckFormat)
            } else {
                deckValidator.validate(
                    deckList = deckList,
                    format = lobby.deckFormat,
                    cardEntries = cardEntries,
                )
            }
            if (!validation.valid) {
                val msg = validation.errors.firstOrNull()?.message ?: "Invalid deck"
                sender.sendError(session, ErrorCode.INVALID_DECK, msg)
                return
            }
        }

        // For a PREMADE_DECKS (constructed) lobby this explicit sideboard is honored; for Limited
        // lobbies TournamentLobby.submitDeck ignores it and derives pool − maindeck (CR 100.4b).
        val result = lobby.submitDeck(identity.playerId, deckList, effectiveCommander, sideboard)
        when (result) {
            is TournamentLobby.DeckSubmissionResult.Success -> {
                val deckSize = deckList.values.sum()
                logger.info("Player ${identity.playerName} submitted deck ($deckSize cards) in lobby $lobbyId")
                sender.send(session, ServerMessage.DeckSubmitted(deckSize))
                ctx.broadcastLobbyUpdate(lobby)

                // Premade decks: stay in WAITING_FOR_PLAYERS until host clicks Start. Don't
                // pre-create the tournament here — that happens in handleStartTournamentLobby.
                if (lobby.format == TournamentFormat.PREMADE_DECKS && lobby.state == LobbyState.WAITING_FOR_PLAYERS) {
                    lobbyRepository.saveLobby(lobby)
                    return
                }

                // Free-for-All mode: no bracket, no per-match readiness — submitting the last
                // deck is the ready signal, and the one multiplayer game starts immediately.
                if (lobby.isFreeForAll) {
                    if (result.allReady && lobby.state == LobbyState.DECK_BUILDING) {
                        lobby.activateTournament()
                        val lock = ctx.roundLocks.computeIfAbsent(lobby.lobbyId) { Any() }
                        synchronized(lock) {
                            freeForAllHandler.maybeStartGame(lobby)
                        }
                    }
                    lobbyRepository.saveLobby(lobby)
                    return
                }

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

    /**
     * Subtract one copy of [commander] from [deckList]. Mirrors the QuickGame helper — the
     * wire format ships the merged deck (commander counted in `deckList`), but `Deck.cards`
     * excludes it. Idempotent when the commander isn't present.
     */
    private fun stripCommanderFromCards(
        deckList: Map<String, Int>,
        commander: String,
    ): Map<String, Int> {
        val current = deckList[commander] ?: return deckList
        val next = deckList.toMutableMap()
        if (current <= 1) next.remove(commander) else next[commander] = current - 1
        return next
    }
}
