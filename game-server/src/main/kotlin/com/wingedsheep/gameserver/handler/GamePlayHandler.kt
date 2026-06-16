package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.ai.AiGameManager
import com.wingedsheep.gameserver.ai.AiWebSocketSession
import com.wingedsheep.ai.engine.SealedDeckGenerator
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.replay.GameHistoryRepository
import com.wingedsheep.gameserver.replay.GameReplayRecord
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PlayerLostEvent
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Component
class GamePlayHandler(
    private val sessionRegistry: SessionRegistry,
    private val gameRepository: GameRepository,
    private val lobbyRepository: LobbyRepository,
    private val sender: MessageSender,
    private val cardRegistry: CardRegistry,
    private val printingRegistry: com.wingedsheep.engine.registry.PrintingRegistry,
    private val deckGenerator: SealedDeckGenerator,
    private val gameProperties: GameProperties,
    private val gameHistoryRepository: GameHistoryRepository,
    private val aiGameManager: AiGameManager
) {
    private val logger = LoggerFactory.getLogger(GamePlayHandler::class.java)

    @Volatile
    var waitingGameSession: GameSession? = null

    private val mulliganBroadcastSent = ConcurrentHashMap<String, AtomicBoolean>()

    // Throttle active-match broadcasts to at most once per second per lobby
    private val lastActiveMatchBroadcast = ConcurrentHashMap<String, Long>()
    private val activeMatchBroadcastIntervalMs = 1000L

    /**
     * Remove tracking entries for game sessions that no longer exist.
     * Called by [ZombieSessionSweeper] to prevent unbounded map growth.
     */
    fun sweepStaleEntries(activeGameSessionIds: Set<String>, activeLobbyIds: Set<String>) {
        mulliganBroadcastSent.keys.removeAll { it !in activeGameSessionIds }
        lastActiveMatchBroadcast.keys.removeAll { it !in activeLobbyIds }
    }

    // Callback to broadcast active matches during ongoing games (throttled)
    var broadcastActiveMatchesCallback: ((String) -> Unit)? = null

    // Callback for full match result handling (report result + notify + check round complete, all under lock)
    var handleMatchResultCallback: ((String, String, EntityId?, Int) -> Unit)? = null

    // Callback fired when ANY game ends, used by the dev-only LLM tournament orchestrator to
    // advance its bracket. Args: (gameSessionId, winnerId, winnerLifeRemaining). The orchestrator
    // ignores game ids it doesn't own, so this is safe to fire unconditionally.
    var llmTournamentGameOverCallback: ((String, EntityId?, Int) -> Unit)? = null

    fun handle(session: WebSocketSession, message: ClientMessage) {
        when (message) {
            is ClientMessage.CreateGame -> handleCreateGame(session, message)
            is ClientMessage.JoinGame -> handleJoinGame(session, message)
            is ClientMessage.SubmitAction -> handleSubmitAction(session, message)
            is ClientMessage.Concede -> handleConcede(session)
            is ClientMessage.CancelGame -> handleCancelGame(session)
            is ClientMessage.KeepHand -> handleKeepHand(session)
            is ClientMessage.Mulligan -> handleMulligan(session)
            is ClientMessage.ChooseBottomCards -> handleChooseBottomCards(session, message)
            is ClientMessage.UpdateAttackerTargets -> handleUpdateAttackerTargets(session, message)
            is ClientMessage.UpdateBlockerAssignments -> handleUpdateBlockerAssignments(session, message)
            is ClientMessage.SetFullControl -> handleSetFullControl(session, message)
            is ClientMessage.SetPriorityMode -> handleSetPriorityMode(session, message)
            is ClientMessage.SetStopOverrides -> handleSetStopOverrides(session, message)
            is ClientMessage.SetAbilityYield -> handleSetAbilityYield(session, message)
            is ClientMessage.ClearAbilityYield -> handleClearAbilityYield(session, message)
            is ClientMessage.ClearAllYields -> handleClearAllYields(session)
            is ClientMessage.RequestUndo -> handleRequestUndo(session)

            is ClientMessage.RequestResync -> handleRequestResync(session)
            else -> {}
        }
    }

    private fun handleCreateGame(session: WebSocketSession, message: ClientMessage.CreateGame) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        // Pick a shared set for quick games so both players draft from the same pool
        val quickGameSetCode = if (message.deckList.isEmpty()) {
            message.setCode ?: deckGenerator.randomSetCode()
        } else null

        val deckList = if (quickGameSetCode != null) {
            val randomDeck = deckGenerator.generate(quickGameSetCode)
            logger.info("Generated random deck for ${playerSession.playerName} from set $quickGameSetCode: ${randomDeck.entries.take(5)}... (${randomDeck.values.sum()} cards)")
            randomDeck
        } else {
            message.deckList
        }

        val gameSession = GameSession(
            cardRegistry = cardRegistry,
            useHandSmoother = gameProperties.handSmoother.enabled,
            debugMode = gameProperties.debugMode,
            printingRegistry = printingRegistry,
        )
        gameSession.quickGameSetCode = quickGameSetCode
        gameSession.addPlayer(playerSession, deckList)

        // Store player info for persistence
        val token = sessionRegistry.getTokenByWsId(session.id)
        if (token != null) {
            gameSession.setPlayerPersistenceInfo(playerSession.playerId, playerSession.playerName, token)
            sessionRegistry.getIdentityByToken(token)?.currentGameSessionId = gameSession.sessionId
        }

        gameRepository.save(gameSession)

        if (message.vsAi) {
            if (!aiGameManager.isEnabled) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "AI opponent is not enabled on this server")
                return
            }

            logger.info("Game created vs AI: ${gameSession.sessionId} by ${playerSession.playerName}")
            sender.send(session, ServerMessage.GameCreated(gameSession.sessionId))

            // Create AI opponent with async callbacks, using same set as human player
            aiGameManager.createAiOpponent(
                gameSession = gameSession,
                setCode = quickGameSetCode,
                onActionReady = { aiPlayerId, action ->
                    handleAiAction(gameSession, aiPlayerId, action)
                },
                onMulliganKeep = { aiPlayerId ->
                    handleAiMulliganKeep(gameSession, aiPlayerId)
                },
                onMulliganTake = { aiPlayerId ->
                    handleAiMulliganTake(gameSession, aiPlayerId)
                },
                onBottomCards = { aiPlayerId, cardIds ->
                    handleAiBottomCards(gameSession, aiPlayerId, cardIds)
                }
            )

            // Start the game immediately
            startGame(gameSession)
        } else {
            waitingGameSession = gameSession
            logger.info("Game created: ${gameSession.sessionId} by ${playerSession.playerName}")
            sender.send(session, ServerMessage.GameCreated(gameSession.sessionId))
        }
    }

    private fun handleCancelGame(session: WebSocketSession) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSessionId = playerSession.currentGameSessionId
        if (gameSessionId == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a game")
            return
        }

        val gameSession = gameRepository.findById(gameSessionId)
        if (gameSession == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found")
            return
        }

        // If the game has already started, treat cancel as a concede
        if (gameSession.isStarted) {
            logger.info("Player ${playerSession.playerName} cancelled started game ${gameSession.sessionId} - treating as concede")
            concedeSeat(gameSession, playerSession.playerId)
            return
        }

        logger.info("Player ${playerSession.playerName} cancelled game ${gameSession.sessionId}")

        // Clear the waiting game session if this is it
        if (waitingGameSession?.sessionId == gameSession.sessionId) {
            waitingGameSession = null
        }

        // Clear player's current game session
        playerSession.currentGameSessionId = null
        val token = sessionRegistry.getTokenByWsId(session.id)
        if (token != null) {
            sessionRegistry.getIdentityByToken(token)?.currentGameSessionId = null
        }

        // Remove the game
        gameRepository.remove(gameSessionId)

        // Notify the player
        sender.send(session, ServerMessage.GameCancelled)
    }

    fun handleJoinGame(session: WebSocketSession, message: ClientMessage.JoinGame) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        // Check if this is a sealed session (auto-detect)
        val sealedSession = lobbyRepository.findSealedSessionById(message.sessionId)
        if (sealedSession != null) {
            // Delegate to lobby handler via callback
            joinSealedGameCallback?.invoke(session, ClientMessage.JoinSealedGame(message.sessionId))
            return
        }

        // Check if this is a lobby (auto-detect)
        val lobby = lobbyRepository.findLobbyById(message.sessionId)
        if (lobby != null) {
            joinLobbyCallback?.invoke(session, ClientMessage.JoinLobby(message.sessionId))
            return
        }

        val gameSession = gameRepository.findById(message.sessionId)
        if (gameSession == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found: ${message.sessionId}")
            return
        }

        if (gameSession.isFull) {
            sender.sendError(session, ErrorCode.GAME_FULL, "Game is full")
            return
        }

        val deckList = if (message.deckList.isEmpty()) {
            val setCode = gameSession.quickGameSetCode ?: deckGenerator.randomSetCode()
            val randomDeck = deckGenerator.generate(setCode)
            logger.info("Generated random deck for ${playerSession.playerName} from set $setCode: ${randomDeck.entries.take(5)}... (${randomDeck.values.sum()} cards)")
            randomDeck
        } else {
            message.deckList
        }

        gameSession.addPlayer(playerSession, deckList)

        // Store player info for persistence
        val token = sessionRegistry.getTokenByWsId(session.id)
        if (token != null) {
            gameSession.setPlayerPersistenceInfo(playerSession.playerId, playerSession.playerName, token)
            sessionRegistry.getIdentityByToken(token)?.currentGameSessionId = gameSession.sessionId
        }

        if (waitingGameSession?.sessionId == gameSession.sessionId) {
            waitingGameSession = null
        }

        logger.info("Player ${playerSession.playerName} joined game ${gameSession.sessionId}")

        if (gameSession.isReady) {
            startGame(gameSession)
        }
    }

    fun startGame(gameSession: GameSession) {
        logger.info("Starting game: ${gameSession.sessionId}")
        gameSession.startGame()

        // Save game state after starting (so gameState is persisted)
        gameRepository.save(gameSession)

        // Send each seat the full roster from its own perspective, then its mulligan decision.
        // 2-player is the degenerate case (one opponent in the roster).
        for (player in gameSession.getPlayers()) {
            sender.send(player.webSocketSession, ServerMessage.GameStarted(gameSession.seatInfos(player.playerId)))
            sendMulliganDecision(gameSession, player)
        }
    }

    private fun sendMulliganDecision(gameSession: GameSession, playerSession: PlayerSession) {
        val decision = gameSession.getMulliganDecision(playerSession.playerId)
        sender.send(playerSession.webSocketSession, decision)
    }

    /**
     * Create and start a fresh AI-vs-AI [GameSession] with no human seats, for the dev-only
     * LLM tournament orchestrator. Both players must already be registered AI identities (minted
     * via [AiGameManager.createAiIdentity]); each gets its own pre-built deck and model. Mirrors
     * the AI-wiring half of [TournamentMatchHandler.startSingleMatch] without any lobby plumbing.
     *
     * @return the new game session id (use it to spectate / locate the replay).
     */
    fun createAndStartAiVsAiGame(
        player1Id: EntityId,
        player1Deck: Map<String, Int>,
        player2Id: EntityId,
        player2Deck: Map<String, Int>,
        setCode: String?,
        thinkingDelayMs: Long
    ): String {
        val gameSession = GameSession(
            cardRegistry = cardRegistry,
            useHandSmoother = gameProperties.handSmoother.enabled,
            debugMode = gameProperties.debugMode,
            printingRegistry = printingRegistry,
        )
        gameSession.quickGameSetCode = setCode

        wireAiSeat(gameSession, player1Id, player1Deck, thinkingDelayMs)
        wireAiSeat(gameSession, player2Id, player2Deck, thinkingDelayMs)

        gameRepository.save(gameSession)
        startGame(gameSession)
        logger.info("Started AI-vs-AI game {} ({} vs {})",
            gameSession.sessionId, player1Id.value, player2Id.value)
        return gameSession.sessionId
    }

    private fun wireAiSeat(
        gameSession: GameSession,
        aiPlayerId: EntityId,
        deck: Map<String, Int>,
        thinkingDelayMs: Long
    ) {
        val identity = sessionRegistry.getAllIdentities().firstOrNull { it.playerId == aiPlayerId }
            ?: error("AI identity not found for ${aiPlayerId.value} — create it via AiGameManager.createAiIdentity first")

        gameSession.addPlayer(identity.toPlayerSession(), deck)
        gameSession.setPlayerPersistenceInfo(
            aiPlayerId, identity.playerName, identity.token,
            isAi = true, aiModelOverride = identity.aiModelOverride
        )

        aiGameManager.wireAiForGame(
            gameSession = gameSession,
            aiPlayerId = aiPlayerId,
            deckList = deck,
            onActionReady = { id, action -> handleAiAction(gameSession, id, action) },
            onMulliganKeep = { id -> handleAiMulliganKeep(gameSession, id) },
            onMulliganTake = { id -> handleAiMulliganTake(gameSession, id) },
            onBottomCards = { id, cardIds -> handleAiBottomCards(gameSession, id, cardIds) }
        )

        // wireAiForGame swapped in a fresh AiWebSocketSession; point the game session's player
        // slot at it so broadcasts reach the wired session, then apply the pacing speed.
        identity.webSocketSession?.let { newWs ->
            gameSession.replacePlayerSession(aiPlayerId, PlayerSession(
                webSocketSession = newWs,
                playerId = aiPlayerId,
                playerName = identity.playerName,
                currentGameSessionId = gameSession.sessionId
            ))
        }
        aiGameManager.setThinkingDelay(aiPlayerId, thinkingDelayMs)
    }

    private fun handleKeepHand(session: WebSocketSession) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        if (!gameSession.isMulliganPhase) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Not in mulligan phase")
            return
        }

        val result = gameSession.keepHand(playerSession.playerId)
        when (result) {
            is GameSession.MulliganActionResult.Success -> {
                logger.info("Player ${playerSession.playerName} kept hand")
                val finalHandSize = gameSession.getHand(playerSession.playerId).size
                sender.send(session, ServerMessage.MulliganComplete(finalHandSize))
                checkMulliganPhaseComplete(gameSession)
            }
            is GameSession.MulliganActionResult.NeedsBottomCards -> {
                logger.info("Player ${playerSession.playerName} kept hand, needs to choose ${result.count} cards for bottom")
                val msg = gameSession.getChooseBottomCardsMessage(playerSession.playerId)
                if (msg != null) sender.send(session, msg)
            }
            is GameSession.MulliganActionResult.Failure -> {
                sender.sendError(session, ErrorCode.INVALID_ACTION, result.reason)
            }
        }
    }

    private fun handleMulligan(session: WebSocketSession) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        if (!gameSession.isMulliganPhase) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Not in mulligan phase")
            return
        }

        val result = gameSession.takeMulligan(playerSession.playerId)
        when (result) {
            is GameSession.MulliganActionResult.Success -> {
                val count = gameSession.getMulliganCount(playerSession.playerId)
                logger.info("Player ${playerSession.playerName} mulliganed (count: $count)")
                sendMulliganDecision(gameSession, playerSession)
            }
            is GameSession.MulliganActionResult.NeedsBottomCards -> {
                val msg = gameSession.getChooseBottomCardsMessage(playerSession.playerId)
                if (msg != null) sender.send(session, msg)
            }
            is GameSession.MulliganActionResult.Failure -> {
                sender.sendError(session, ErrorCode.INVALID_ACTION, result.reason)
            }
        }
    }

    private fun handleChooseBottomCards(session: WebSocketSession, message: ClientMessage.ChooseBottomCards) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        if (!gameSession.isAwaitingBottomCards(playerSession.playerId)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Not awaiting bottom card selection")
            return
        }

        val result = gameSession.chooseBottomCards(playerSession.playerId, message.cardIds)
        when (result) {
            is GameSession.MulliganActionResult.Success -> {
                logger.info("Player ${playerSession.playerName} completed mulligan")
                val finalHandSize = gameSession.getHand(playerSession.playerId).size
                sender.send(session, ServerMessage.MulliganComplete(finalHandSize))
                checkMulliganPhaseComplete(gameSession)
            }
            is GameSession.MulliganActionResult.NeedsBottomCards -> {
                sender.sendError(session, ErrorCode.INTERNAL_ERROR, "Unexpected state")
            }
            is GameSession.MulliganActionResult.Failure -> {
                sender.sendError(session, ErrorCode.INVALID_ACTION, result.reason)
            }
        }
    }

    private fun checkMulliganPhaseComplete(gameSession: GameSession) {
        if (gameSession.allMulligansComplete) {
            val broadcastFlag = mulliganBroadcastSent.computeIfAbsent(gameSession.sessionId) { AtomicBoolean(false) }
            if (broadcastFlag.compareAndSet(false, true)) {
                logger.info("Mulligan phase complete for game ${gameSession.sessionId}")
                broadcastStateUpdate(gameSession, emptyList())
            }
        } else {
            // Notify players who have completed their mulligan that they're waiting
            gameSession.getPlayers().forEach { player ->
                if (gameSession.hasMulliganComplete(player.playerId)) {
                    sender.send(player.webSocketSession, ServerMessage.WaitingForOpponentMulligan)
                }
            }
        }
    }

    private fun handleSubmitAction(session: WebSocketSession, message: ClientMessage.SubmitAction) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        if (gameSession.isMulliganPhase) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Mulligan phase not complete")
            return
        }

        val result = gameSession.executeAction(playerSession.playerId, message.action, message.messageId)
        when (result) {
            is GameSession.ActionResult.Success -> {
                logger.debug("Action executed successfully")
                broadcastStateUpdate(gameSession, result.events)
                if (gameSession.isGameOver()) handleGameOver(gameSession, events = result.events)
            }
            is GameSession.ActionResult.PausedForDecision -> {
                logger.debug("Action paused for decision: ${result.decision}")
                broadcastStateUpdate(gameSession, result.events)
                // Safety net: if the engine paused for a decision but the game is already
                // over, no one can answer that decision (ActionProcessor rejects actions
                // when gameOver is true). Finalize the match instead of leaving the
                // session hung. The engine should never reach this state, but if it does,
                // this prevents a tournament-blocking deadlock.
                if (gameSession.isGameOver()) {
                    logger.warn("Action paused for decision despite gameOver=true; finalizing match")
                    handleGameOver(gameSession, events = result.events)
                }
            }
            is GameSession.ActionResult.Failure -> {
                sender.sendError(session, ErrorCode.INVALID_ACTION, result.reason)
            }
        }
    }

    private fun handleConcede(session: WebSocketSession) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        logger.info("Player ${playerSession.playerName} conceded game ${gameSession.sessionId}")
        concedeSeat(gameSession, playerSession.playerId)
    }

    /**
     * Concede [playerId]'s seat. If that ends the game (≤1 player remains, CR 104.2a) the match
     * is finalized; otherwise the game continues for the remaining seats (CR 800.4a) — the
     * conceder gets a personal [ServerMessage.PlayerEliminated] so their client can leave the
     * table, and everyone else sees the seat drop out via the state rebroadcast. In a 2-player
     * game conceding always ends it — the degenerate case.
     */
    private fun concedeSeat(gameSession: GameSession, playerId: EntityId) {
        gameSession.playerConcedes(playerId)
        if (gameSession.isGameOver()) {
            handleGameOver(gameSession, GameOverReason.CONCESSION)
            return
        }

        // In a hotseat pod the conceding identity may still control other live seats
        // (scenario builder self-play) — keep them at the table instead of showing the
        // personal elimination overlay.
        val state = gameSession.getStateSnapshot()
        val stillControlsLiveSeat = state != null && state.turnOrder.any { seat ->
            seat != playerId && seat in state.activePlayers && state.actorFor(seat) == playerId
        }
        val conceder = gameSession.getPlayerSession(playerId)
        if (!stillControlsLiveSeat && conceder != null) {
            if (conceder.webSocketSession.isOpen) {
                sender.send(conceder.webSocketSession, ServerMessage.PlayerEliminated(
                    gameId = gameSession.sessionId,
                    reason = GameOverReason.CONCESSION,
                ))
            }
            // The conceder is out of the game (the table plays on without them). Clear their
            // game-session routing — exactly as handleGameOver does for everyone — so that
            // returning to the lobby sticks: a reconnect/refresh treats them as "waiting" instead
            // of dropping them back into a game they've left. They stay seated in the engine state
            // (CR 800.4a) and still receive the rebroadcast below until they choose to leave.
            conceder.currentGameSessionId = null
            sessionRegistry.getIdentityByWsId(conceder.webSocketSession.id)?.currentGameSessionId = null
            sessionRegistry.getPlayerSession(conceder.webSocketSession.id)?.currentGameSessionId = null
        }
        broadcastStateUpdate(gameSession, emptyList())
    }

    fun handleGameOver(gameSession: GameSession, reason: GameOverReason? = null, events: List<GameEvent> = emptyList()) {
        val winnerId = gameSession.getWinnerId()
        val gameOverReason = reason ?: gameSession.getGameOverReason() ?: GameOverReason.LIFE_ZERO
        // Extract custom message from PlayerLostEvent if present
        val customMessage = events.filterIsInstance<PlayerLostEvent>().firstOrNull()?.message
        val message = ServerMessage.GameOver(winnerId, gameOverReason, customMessage, gameSession.sessionId)

        gameSession.getPlayers().forEach { sender.send(it.webSocketSession, message) }

        // Notify spectators that the game has ended and return them to tournament overview
        for (spectator in gameSession.getSpectators()) {
            if (spectator.webSocketSession.isOpen) {
                sender.send(spectator.webSocketSession, ServerMessage.SpectatingStopped)
            }
            // Clear their spectating state
            sessionRegistry.getIdentityByWsId(spectator.webSocketSession.id)?.let { identity ->
                identity.currentSpectatingGameId = null
            }
        }

        val gameSessionId = gameSession.sessionId

        val lobbyId = gameRepository.getLobbyForGame(gameSessionId)
        if (lobbyId != null) {
            // Every lobby-linked game reports back to its lobby: bracket tournaments via
            // TournamentManager, Free-for-All pods via FreeForAllHandler (which never creates
            // a TournamentManager). The callback routes by the lobby's game mode.
            // Capture winner's remaining life for tiebreaker calculations
            val winnerLifeRemaining = if (winnerId != null) {
                gameSession.getStateForTesting()?.getEntity(winnerId)
                    ?.get<LifeTotalComponent>()?.life ?: 0
            } else {
                0
            }
            gameRepository.removeLobbyLink(gameSessionId)

            // Clear currentGameSessionId for all players so they are
            // considered "waiting" and receive the active matches broadcast
            gameSession.getPlayers().forEach { player ->
                player.currentGameSessionId = null
                sessionRegistry.getIdentityByWsId(player.webSocketSession.id)
                    ?.currentGameSessionId = null
                // Also clear the registry's PlayerSession to prevent stale references
                sessionRegistry.getPlayerSession(player.webSocketSession.id)
                    ?.currentGameSessionId = null
            }

            // Report result, notify players, check round complete — all under the per-lobby lock
            handleMatchResultCallback?.invoke(lobbyId, gameSessionId, winnerId, winnerLifeRemaining)
        }

        // Save replay history if the game had meaningful activity (>= 5 frames)
        val initialSnapshot = gameSession.getReplayInitialSnapshot()
        val frameCount = gameSession.getReplayFrameCount()
        if (initialSnapshot != null && frameCount >= 5) {
            val winnerName = winnerId?.let { wId ->
                gameSession.getPlayers().find { it.playerId == wId }?.playerName
            }

            // Look up tournament context for grouping replays
            val replayLobbyId = lobbyId ?: gameRepository.getLobbyForGame(gameSessionId)
            val replayLobby = replayLobbyId?.let { lobbyRepository.findLobbyById(it) }
            val replayTournament = replayLobbyId?.let { lobbyRepository.findTournamentById(it) }
            val tournamentName = replayLobby?.let {
                it.setNames.joinToString(" / ") + " " + it.format.name.lowercase()
                    .replaceFirstChar { c -> c.uppercase() }
            }
            val tournamentRound = replayTournament?.getRoundForMatch(gameSessionId)?.roundNumber

            val record = GameReplayRecord(
                gameId = gameSessionId,
                players = gameSession.getPlayers().map {
                    com.wingedsheep.gameserver.replay.ReplayPlayerInfo(it.playerId.value, it.playerName)
                },
                startedAt = gameSession.replayStartedAt ?: Instant.now(),
                endedAt = Instant.now(),
                winnerName = winnerName,
                tournamentName = tournamentName,
                tournamentRound = tournamentRound,
                initialSnapshot = initialSnapshot,
                deltas = gameSession.getReplayDeltas(),
                fullStates = gameSession.getReplayFullStates()
            )
            gameHistoryRepository.save(record)
            logger.info("Saved replay for game $gameSessionId ($frameCount frames)")
        }

        // Notify the dev LLM-tournament orchestrator (no-op for games it doesn't own).
        // Fired before cleanup so it can launch the next bracket game off its own coroutine.
        llmTournamentGameOverCallback?.let { callback ->
            val winnerLife = if (winnerId != null) {
                gameSession.getStateForTesting()?.getEntity(winnerId)
                    ?.get<LifeTotalComponent>()?.life ?: 0
            } else 0
            callback(gameSessionId, winnerId, winnerLife)
        }

        gameRepository.remove(gameSessionId)
        mulliganBroadcastSent.remove(gameSessionId)
        aiGameManager.cleanupGame(gameSessionId)
    }

    fun broadcastStateUpdate(gameSession: GameSession, events: List<GameEvent>) {
        val allEvents = processAutoPassLoop(gameSession, events)

        val sessionPlayers = gameSession.getPlayers()

        // AI players must always receive full StateUpdate (never deltas) because
        // they don't reconstruct state from diffs — they need the complete picture.
        if (aiGameManager.hasAiPlayer(gameSession.sessionId)) {
            sessionPlayers.forEach { session ->
                if (session.webSocketSession is AiWebSocketSession) {
                    gameSession.clearLastSentState(session.playerId)
                }
            }
        }

        // Log what the AI will receive at combat steps
        if (aiGameManager.hasAiPlayer(gameSession.sessionId)) {
            val state = gameSession.getStateForTesting()
            if (state != null && (state.step == com.wingedsheep.sdk.core.Step.DECLARE_ATTACKERS || state.step == com.wingedsheep.sdk.core.Step.DECLARE_BLOCKERS)) {
                val aiPlayer = sessionPlayers.find { it.webSocketSession is AiWebSocketSession }
                if (aiPlayer != null) {
                    val aiLegalActions = gameSession.getLegalActions(aiPlayer.playerId)
                    logger.info("AI combat state: step={}, priorityPlayer={}, aiPlayer={}, legalActions={}",
                        state.step, state.priorityPlayerId?.value, aiPlayer.playerId.value,
                        aiLegalActions.map { "${it.actionType}(${it.description})" })
                }
            }
        }

        try {
            sessionPlayers.forEach { session ->
                val update = gameSession.createStateUpdate(session.playerId, allEvents)
                if (update != null) sender.send(session.webSocketSession, update)
                else logger.warn("createStateUpdate returned null for player ${session.playerId.value}")
            }

            // Update spectators
            val spectatorState = gameSession.buildSpectatorState()
            if (spectatorState != null) {
                // Record snapshot for replay
                gameSession.recordSnapshot(spectatorState)

                for (spectator in gameSession.getSpectators()) {
                    if (spectator.webSocketSession.isOpen) {
                        sender.send(spectator.webSocketSession, spectatorState)
                    }
                }
            }

            // Persist state after every update
            gameRepository.save(gameSession)

            // Update tournament overview life totals for waiting players (throttled)
            val lobbyId = gameRepository.getLobbyForGame(gameSession.sessionId)
            if (lobbyId != null) {
                val now = System.currentTimeMillis()
                val lastBroadcast = lastActiveMatchBroadcast[lobbyId] ?: 0L
                if (now - lastBroadcast >= activeMatchBroadcastIntervalMs) {
                    lastActiveMatchBroadcast[lobbyId] = now
                    broadcastActiveMatchesCallback?.invoke(lobbyId)
                }
            }
        } catch (e: Exception) {
            logger.error("Error broadcasting state update", e)
        }
    }

    private fun processAutoPassLoop(gameSession: GameSession, initialEvents: List<GameEvent>): List<GameEvent> {
        val allEvents = initialEvents.toMutableList()
        var loopCount = 0
        val maxLoops = 100

        while (loopCount < maxLoops) {
            if (gameSession.isGameOver()) break
            val autoPassPlayer = gameSession.getAutoPassPlayer() ?: break

            // Never auto-pass combat declarations for AI players — the AI controller
            // needs to decide which creatures to attack/block with. executeAutoPass would
            // submit empty maps (= no attacks, no blocks).
            if (aiGameManager.isAiPlayer(autoPassPlayer)) {
                val state = gameSession.getStateForTesting()
                if (state != null && (state.step == com.wingedsheep.sdk.core.Step.DECLARE_ATTACKERS || state.step == com.wingedsheep.sdk.core.Step.DECLARE_BLOCKERS)) {
                    logger.debug("Skipping auto-pass for AI player at {} — AI will handle combat", state.step)
                    break
                }
            }

            logger.debug("Auto-passing for player: ${autoPassPlayer.value}")

            val result = gameSession.executeAutoPass(autoPassPlayer)
            when (result) {
                is GameSession.ActionResult.Success -> allEvents.addAll(result.events)
                is GameSession.ActionResult.PausedForDecision -> {
                    allEvents.addAll(result.events)
                    break
                }
                is GameSession.ActionResult.Failure -> {
                    logger.warn("Auto-pass failed: ${result.reason}")
                    break
                }
            }
            loopCount++
        }

        if (loopCount >= maxLoops) logger.warn("Auto-pass loop hit safety limit!")
        return allEvents
    }

    private fun getGameSession(session: WebSocketSession, playerSession: PlayerSession): GameSession? {
        val gameSessionId = playerSession.currentGameSessionId
        if (gameSessionId == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a game")
            return null
        }
        val gameSession = gameRepository.findById(gameSessionId)
        if (gameSession == null) {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found")
            return null
        }
        return gameSession
    }

    private fun handleUpdateAttackerTargets(session: WebSocketSession, message: ClientMessage.UpdateAttackerTargets) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        // Forward the attacker targets to every opponent (2-player = the one opponent)
        val serverMessage = ServerMessage.OpponentAttackerTargets(message.selectedAttackers, message.attackerTargets)

        gameSession.getOpponentIds(playerSession.playerId).forEach { opponentId ->
            gameSession.getPlayerSession(opponentId)?.let { sender.send(it.webSocketSession, serverMessage) }
        }

        // Also forward to all spectators so they can see attacker arrows in real-time
        for (spectator in gameSession.getSpectators()) {
            sender.send(spectator.webSocketSession, serverMessage)
        }
    }

    private fun handleUpdateBlockerAssignments(session: WebSocketSession, message: ClientMessage.UpdateBlockerAssignments) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        // Forward the blocker assignments to every opponent (2-player = the one opponent)
        gameSession.getOpponentIds(playerSession.playerId).forEach { opponentId ->
            gameSession.getPlayerSession(opponentId)?.let {
                sender.send(it.webSocketSession, ServerMessage.OpponentBlockerAssignments(message.assignments))
            }
        }

        // Also forward to all spectators so they can see blocker arrows in real-time
        for (spectator in gameSession.getSpectators()) {
            sender.send(spectator.webSocketSession, ServerMessage.OpponentBlockerAssignments(message.assignments))
        }
    }

    private fun handleSetFullControl(session: WebSocketSession, message: ClientMessage.SetFullControl) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        gameSession.setFullControl(playerSession.playerId, message.enabled)
        logger.info("Player ${playerSession.playerName} set full control to ${message.enabled}")

        // Broadcast state update so the UI reflects the change
        broadcastStateUpdate(gameSession, emptyList())
    }

    private fun handleSetPriorityMode(session: WebSocketSession, message: ClientMessage.SetPriorityMode) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        val mode = when (message.mode) {
            "auto" -> GameSession.PriorityMode.AUTO
            "stops" -> GameSession.PriorityMode.STOPS
            "fullControl" -> GameSession.PriorityMode.FULL_CONTROL
            else -> {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Invalid priority mode: ${message.mode}")
                return
            }
        }

        gameSession.setPriorityMode(playerSession.playerId, mode)
        logger.info("Player ${playerSession.playerName} set priority mode to ${message.mode}")

        // Broadcast state update so the UI reflects the change
        broadcastStateUpdate(gameSession, emptyList())
    }

    private fun handleSetStopOverrides(session: WebSocketSession, message: ClientMessage.SetStopOverrides) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        gameSession.setStopOverrides(playerSession.playerId, message.myTurnStops, message.opponentTurnStops)
        logger.info("Player ${playerSession.playerName} set stop overrides: myTurn=${message.myTurnStops}, opponentTurn=${message.opponentTurnStops}")

        // Broadcast state update so the UI reflects the change (and next stop point updates)
        broadcastStateUpdate(gameSession, emptyList())
    }

    private fun handleSetAbilityYield(session: WebSocketSession, message: ClientMessage.SetAbilityYield) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }
        val gameSession = getGameSession(session, playerSession) ?: return

        val identity = com.wingedsheep.sdk.scripting.AbilityIdentity(
            message.cardDefinitionId,
            com.wingedsheep.sdk.scripting.AbilityId(message.abilityId)
        )
        gameSession.setAbilityYield(playerSession.playerId, identity, message.kind)
        logger.info("Player ${playerSession.playerName} set yield ${message.kind} on $identity")

        // Re-broadcast (drives the auto-pass loop in case the yield now lets the game advance).
        broadcastStateUpdate(gameSession, emptyList())
    }

    private fun handleClearAbilityYield(session: WebSocketSession, message: ClientMessage.ClearAbilityYield) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }
        val gameSession = getGameSession(session, playerSession) ?: return

        val identity = com.wingedsheep.sdk.scripting.AbilityIdentity(
            message.cardDefinitionId,
            com.wingedsheep.sdk.scripting.AbilityId(message.abilityId)
        )
        gameSession.clearAbilityYield(playerSession.playerId, identity)
        broadcastStateUpdate(gameSession, emptyList())
    }

    private fun handleClearAllYields(session: WebSocketSession) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }
        val gameSession = getGameSession(session, playerSession) ?: return

        gameSession.clearAllYields(playerSession.playerId)
        broadcastStateUpdate(gameSession, emptyList())
    }

    private fun handleRequestUndo(session: WebSocketSession) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        val result = gameSession.executeUndo(playerSession.playerId)
        when (result) {
            is GameSession.ActionResult.Success -> {
                logger.info("Player ${playerSession.playerName} undid their last action")
                broadcastStateUpdate(gameSession, emptyList())
            }
            is GameSession.ActionResult.Failure -> {
                sender.sendError(session, ErrorCode.INVALID_ACTION, result.reason)
            }
            is GameSession.ActionResult.PausedForDecision -> {
                // Should not happen for undo
                broadcastStateUpdate(gameSession, result.events)
            }
        }
    }

    private fun handleRequestResync(session: WebSocketSession) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        logger.info("Player ${playerSession.playerName} requested state resync")
        // Clear cached state so the next update sends a full StateUpdate instead of a delta
        gameSession.clearLastSentState(playerSession.playerId)
        val update = gameSession.createStateUpdate(playerSession.playerId, emptyList())
        if (update != null) {
            sender.send(session, update)
        }
    }

    // =========================================================================
    // AI recovery (rewire AI into GameSessions restored from Redis on startup)
    // =========================================================================

    /**
     * Re-attach AI players to a [GameSession] that was restored from Redis.
     *
     * The restored session has its [GameState] but no live [PlayerSession]s and
     * no live [AiWebSocketSession]. For each AI player recorded in the session's
     * persistence info, this creates a fresh wired-up [AiWebSocketSession] (via
     * [AiGameManager.wireAiForGame]) and associates a [PlayerSession] back into
     * the session so that messages addressed to the AI flow through and so that
     * the engine's broadcast logic finds the AI in `players`.
     *
     * If no human player has reconnected yet, the AI sits idle until they do —
     * because `broadcastStateUpdate` only sends state to players present in the
     * session. Once the human reconnects via [associatePlayer] / `handleReconnect`,
     * the next broadcast lands at the AI's virtual session and play resumes.
     */
    fun rewireAiForRecoveredGame(gameSession: GameSession) {
        if (!aiGameManager.isEnabled) return
        val info = gameSession.getPlayerPersistenceInfo()
        val aiPlayers = info.filter { (_, pi) -> pi.isAi }
        if (aiPlayers.isEmpty()) return

        for ((aiPlayerId, pi) in aiPlayers) {
            val deckList = gameSession.getDeckList(aiPlayerId)
                ?.groupingBy { it }?.eachCount()
            aiGameManager.wireAiForGame(
                gameSession = gameSession,
                aiPlayerId = aiPlayerId,
                deckList = deckList,
                onActionReady = { id, action -> handleAiAction(gameSession, id, action) },
                onMulliganKeep = { id -> handleAiMulliganKeep(gameSession, id) },
                onMulliganTake = { id -> handleAiMulliganTake(gameSession, id) },
                onBottomCards = { id, cardIds -> handleAiBottomCards(gameSession, id, cardIds) }
            )

            val aiIdentity = sessionRegistry.getAllIdentities().firstOrNull { it.playerId == aiPlayerId }
            val newWs = aiIdentity?.webSocketSession
            if (newWs != null) {
                gameSession.associatePlayer(PlayerSession(
                    webSocketSession = newWs,
                    playerId = aiPlayerId,
                    playerName = pi.playerName,
                    currentGameSessionId = gameSession.sessionId
                ))

                // Nudge the AI with the message it would have received before the crash,
                // so it doesn't sit idle waiting for its next prompt.
                if (gameSession.isStarted) {
                    when {
                        gameSession.isAwaitingBottomCards(aiPlayerId) -> {
                            gameSession.getChooseBottomCardsMessage(aiPlayerId)?.let {
                                sender.send(newWs, it)
                            }
                        }
                        gameSession.isMulliganPhase && !gameSession.hasMulliganComplete(aiPlayerId) -> {
                            sender.send(newWs, gameSession.getMulliganDecision(aiPlayerId))
                        }
                    }
                }
            }
            logger.info("Re-attached AI {} to recovered game {}", aiPlayerId.value, gameSession.sessionId)
        }

        // After wiring all AI players, if the game is past mulligan, broadcast current
        // state so an AI with priority can act without waiting for a human to reconnect.
        if (gameSession.isStarted && !gameSession.isMulliganPhase) {
            broadcastStateUpdate(gameSession, emptyList())
        }
    }

    /**
     * On startup, after [SessionRecoveryService] has rehydrated AI identities and
     * loaded all GameSessions from Redis, walk every restored game and re-wire any
     * AI players. Without this, an AI mid-match before the crash would never act
     * again because its virtual WebSocket and game callbacks weren't persisted.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun resumeAiGamesOnStartup() {
        val games = gameRepository.findAll()
        if (games.isEmpty()) return

        var rewired = 0
        for (game in games) {
            try {
                val before = game.getPlayerPersistenceInfo().count { (_, pi) -> pi.isAi }
                if (before == 0) continue
                rewireAiForRecoveredGame(game)
                rewired++
            } catch (e: Exception) {
                logger.error("Failed to re-wire AI for recovered game ${game.sessionId}", e)
            }
        }
        if (rewired > 0) logger.info("Re-wired AI for {} recovered game session(s)", rewired)
    }

    // =========================================================================
    // AI opponent callbacks (invoked async from AiWebSocketSession coroutine)
    // =========================================================================

    fun handleAiAction(gameSession: GameSession, aiPlayerId: EntityId, action: com.wingedsheep.engine.core.GameAction) {
        try {
            val result = gameSession.executeAction(aiPlayerId, action)
            when (result) {
                is GameSession.ActionResult.Success -> {
                    logger.debug("AI action executed successfully")
                    broadcastStateUpdate(gameSession, result.events)
                    if (gameSession.isGameOver()) handleGameOver(gameSession, events = result.events)
                }
                is GameSession.ActionResult.PausedForDecision -> {
                    logger.debug("AI action paused for decision: ${result.decision}")
                    broadcastStateUpdate(gameSession, result.events)
                }
                is GameSession.ActionResult.Failure -> {
                    // The chosen action was rejected (e.g. an illegal block the AI's combat model
                    // didn't foresee, like Ring-bearer "can't be blocked by greater power"). Try a
                    // sequence of step-appropriate, always-legal fallbacks so the game can't get
                    // stuck. During DECLARE_BLOCKERS/DECLARE_ATTACKERS, PassPriority is itself
                    // illegal ("you must declare blockers before passing priority"), so a do-nothing
                    // declaration must come first — otherwise the AI re-receives the same state and
                    // loops forever.
                    logger.warn("AI action failed: {} — trying safe fallbacks", result.reason)
                    var recovered = false
                    for (fallback in safeFallbackActions(gameSession, aiPlayerId)) {
                        when (val fb = gameSession.executeAction(aiPlayerId, fallback)) {
                            is GameSession.ActionResult.Success -> {
                                broadcastStateUpdate(gameSession, fb.events)
                                if (gameSession.isGameOver()) handleGameOver(gameSession, events = fb.events)
                                recovered = true
                            }
                            is GameSession.ActionResult.PausedForDecision -> {
                                broadcastStateUpdate(gameSession, fb.events)
                                recovered = true
                            }
                            is GameSession.ActionResult.Failure -> {
                                logger.warn("AI fallback {} also failed: {}", fallback::class.simpleName, fb.reason)
                            }
                        }
                        if (recovered) break
                    }
                    if (!recovered) {
                        // Last resort: broadcast current state so the AI gets another chance.
                        broadcastStateUpdate(gameSession, emptyList())
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling AI action", e)
        }
    }

    /**
     * Step-appropriate, always-legal recovery actions to try (in order) when an AI's chosen action
     * is rejected. During the declare steps a "do nothing" declaration (no blockers / no attackers)
     * is always legal and advances combat; `PassPriority` is NOT legal there, so it can only be the
     * last resort. Outside combat declaration, passing priority is the safe no-op.
     */
    private fun safeFallbackActions(
        gameSession: GameSession,
        aiPlayerId: EntityId
    ): List<com.wingedsheep.engine.core.GameAction> {
        val pass = com.wingedsheep.engine.core.PassPriority(aiPlayerId)
        // If PassPriority is currently a legal action, we're in a priority window (e.g. after
        // blockers/attackers were already declared), not the actual declare-step decision. In that
        // case a do-nothing DeclareBlockers/DeclareAttackers can succeed as a no-op and hand
        // priority straight back to the AI → infinite loop. Pass FIRST so the loop breaks; keep the
        // do-nothing declaration only as a fallback for the genuine declare-step decision (where
        // PassPriority is NOT yet legal).
        val passIsLegal = gameSession.getLegalActions(aiPlayerId).any {
            it.action is com.wingedsheep.engine.core.PassPriority
        }
        return when (gameSession.getStateForTesting()?.step) {
            com.wingedsheep.sdk.core.Step.DECLARE_BLOCKERS -> {
                val declare = com.wingedsheep.engine.core.DeclareBlockers(aiPlayerId, emptyMap())
                if (passIsLegal) listOf(pass, declare) else listOf(declare, pass)
            }
            com.wingedsheep.sdk.core.Step.DECLARE_ATTACKERS -> {
                val declare = com.wingedsheep.engine.core.DeclareAttackers(aiPlayerId, emptyMap())
                if (passIsLegal) listOf(pass, declare) else listOf(declare, pass)
            }
            else -> listOf(pass)
        }
    }

    fun handleAiMulliganKeep(gameSession: GameSession, aiPlayerId: EntityId) {
        try {
            val result = gameSession.keepHand(aiPlayerId)
            when (result) {
                is GameSession.MulliganActionResult.Success -> {
                    logger.info("AI kept hand")
                    checkMulliganPhaseComplete(gameSession)
                }
                is GameSession.MulliganActionResult.NeedsBottomCards -> {
                    logger.info("AI kept hand, needs to choose bottom cards")
                    // The AI will receive the ChooseBottomCards message via its virtual session
                    val msg = gameSession.getChooseBottomCardsMessage(aiPlayerId)
                    if (msg != null) {
                        val aiPlayer = gameSession.getPlayerSession(aiPlayerId)
                        if (aiPlayer != null) sender.send(aiPlayer.webSocketSession, msg)
                    }
                }
                is GameSession.MulliganActionResult.Failure -> {
                    logger.warn("AI mulligan keep failed: ${result.reason}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling AI mulligan keep", e)
        }
    }

    fun handleAiMulliganTake(gameSession: GameSession, aiPlayerId: EntityId) {
        try {
            val result = gameSession.takeMulligan(aiPlayerId)
            when (result) {
                is GameSession.MulliganActionResult.Success -> {
                    logger.info("AI took mulligan")
                    // Send new mulligan decision to AI
                    val aiPlayer = gameSession.getPlayerSession(aiPlayerId)
                    if (aiPlayer != null) {
                        sendMulliganDecision(gameSession, aiPlayer)
                    }
                }
                is GameSession.MulliganActionResult.NeedsBottomCards -> {
                    val aiPlayer = gameSession.getPlayerSession(aiPlayerId)
                    if (aiPlayer != null) {
                        val msg = gameSession.getChooseBottomCardsMessage(aiPlayerId)
                        if (msg != null) sender.send(aiPlayer.webSocketSession, msg)
                    }
                }
                is GameSession.MulliganActionResult.Failure -> {
                    logger.warn("AI mulligan take failed: ${result.reason}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling AI mulligan take", e)
        }
    }

    fun handleAiBottomCards(gameSession: GameSession, aiPlayerId: EntityId, cardIds: List<EntityId>) {
        try {
            val result = gameSession.chooseBottomCards(aiPlayerId, cardIds)
            when (result) {
                is GameSession.MulliganActionResult.Success -> {
                    logger.info("AI chose bottom cards")
                    checkMulliganPhaseComplete(gameSession)
                }
                is GameSession.MulliganActionResult.NeedsBottomCards -> {
                    logger.warn("AI bottom cards: unexpected NeedsBottomCards")
                }
                is GameSession.MulliganActionResult.Failure -> {
                    logger.warn("AI bottom cards failed: ${result.reason}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling AI bottom cards", e)
        }
    }

    // Callbacks to avoid circular dependencies with LobbyHandler
    var joinSealedGameCallback: ((WebSocketSession, ClientMessage.JoinSealedGame) -> Unit)? = null
    var joinLobbyCallback: ((WebSocketSession, ClientMessage.JoinLobby) -> Unit)? = null
}
