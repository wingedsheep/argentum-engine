package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.deck.RandomDeckGenerator
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.protocol.ServerMessage
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
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Component
class GamePlayHandler(
    private val sessionRegistry: SessionRegistry,
    private val gameRepository: GameRepository,
    private val lobbyRepository: LobbyRepository,
    private val sender: MessageSender,
    private val cardRegistry: CardRegistry,
    private val deckGenerator: RandomDeckGenerator,
    private val gameProperties: GameProperties
) {
    private val logger = LoggerFactory.getLogger(GamePlayHandler::class.java)

    @Volatile
    var waitingGameSession: GameSession? = null

    private val mulliganBroadcastSent = ConcurrentHashMap<String, AtomicBoolean>()

    // Throttle active-match broadcasts to at most once per second per lobby
    private val lastActiveMatchBroadcast = ConcurrentHashMap<String, Long>()
    private val activeMatchBroadcastIntervalMs = 1000L

    // Callback for tournament round complete
    var handleRoundCompleteCallback: ((String) -> Unit)? = null

    // Callback to broadcast active matches when a tournament match ends
    var broadcastActiveMatchesCallback: ((String) -> Unit)? = null

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
            is ClientMessage.UpdateBlockerAssignments -> handleUpdateBlockerAssignments(session, message)
            is ClientMessage.SetFullControl -> handleSetFullControl(session, message)
            is ClientMessage.SetStopOverrides -> handleSetStopOverrides(session, message)
            else -> {}
        }
    }

    private fun handleCreateGame(session: WebSocketSession, message: ClientMessage.CreateGame) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val deckList = if (message.deckList.isEmpty()) {
            val randomDeck = deckGenerator.generate()
            logger.info("Generated random deck for ${playerSession.playerName}: ${randomDeck.entries.take(5)}... (${randomDeck.values.sum()} cards)")
            randomDeck
        } else {
            message.deckList
        }

        val gameSession = GameSession(
            cardRegistry = cardRegistry,
            useHandSmoother = gameProperties.handSmoother.enabled
        )
        gameSession.addPlayer(playerSession, deckList)

        // Store player info for persistence
        val token = sessionRegistry.getTokenByWsId(session.id)
        if (token != null) {
            gameSession.setPlayerPersistenceInfo(playerSession.playerId, playerSession.playerName, token)
            sessionRegistry.getIdentityByToken(token)?.currentGameSessionId = gameSession.sessionId
        }

        gameRepository.save(gameSession)
        waitingGameSession = gameSession

        logger.info("Game created: ${gameSession.sessionId} by ${playerSession.playerName}")
        sender.send(session, ServerMessage.GameCreated(gameSession.sessionId))
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
            gameSession.playerConcedes(playerSession.playerId)
            handleGameOver(gameSession, GameOverReason.CONCESSION)
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
            val randomDeck = deckGenerator.generate()
            logger.info("Generated random deck for ${playerSession.playerName}: ${randomDeck.entries.take(5)}... (${randomDeck.values.sum()} cards)")
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

        val player1 = gameSession.player1
        val player2 = gameSession.player2

        if (player1 != null && player2 != null) {
            sender.send(player1.webSocketSession, ServerMessage.GameStarted(player2.playerName))
            sender.send(player2.webSocketSession, ServerMessage.GameStarted(player1.playerName))

            sendMulliganDecision(gameSession, player1)
            sendMulliganDecision(gameSession, player2)
        }
    }

    private fun sendMulliganDecision(gameSession: GameSession, playerSession: PlayerSession) {
        val decision = gameSession.getMulliganDecision(playerSession.playerId)
        sender.send(playerSession.webSocketSession, decision)
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
            listOfNotNull(gameSession.player1, gameSession.player2).forEach { player ->
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
        gameSession.playerConcedes(playerSession.playerId)
        handleGameOver(gameSession, GameOverReason.CONCESSION)
    }

    fun handleGameOver(gameSession: GameSession, reason: GameOverReason? = null, events: List<GameEvent> = emptyList()) {
        val winnerId = gameSession.getWinnerId()
        val gameOverReason = reason ?: gameSession.getGameOverReason() ?: GameOverReason.LIFE_ZERO
        // Extract custom message from PlayerLostEvent if present
        val customMessage = events.filterIsInstance<PlayerLostEvent>().firstOrNull()?.message
        val message = ServerMessage.GameOver(winnerId, gameOverReason, customMessage)

        gameSession.player1?.let { sender.send(it.webSocketSession, message) }
        gameSession.player2?.let { sender.send(it.webSocketSession, message) }

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
            val tournament = lobbyRepository.findTournamentById(lobbyId)
            if (tournament != null) {
                // Capture winner's remaining life for tiebreaker calculations
                val winnerLifeRemaining = if (winnerId != null) {
                    gameSession.getStateForTesting()?.getEntity(winnerId)
                        ?.get<LifeTotalComponent>()?.life ?: 0
                } else {
                    0
                }
                tournament.reportMatchResult(gameSessionId, winnerId, winnerLifeRemaining)
                gameRepository.removeLobbyLink(gameSessionId)

                // Clear currentGameSessionId for both players so they are
                // considered "waiting" and receive the active matches broadcast
                listOfNotNull(gameSession.player1, gameSession.player2).forEach { player ->
                    player.currentGameSessionId = null
                    sessionRegistry.getIdentityByWsId(player.webSocketSession.id)
                        ?.currentGameSessionId = null
                }

                // Broadcast updated active matches to waiting players
                broadcastActiveMatchesCallback?.invoke(lobbyId)

                if (tournament.isRoundComplete()) {
                    handleRoundCompleteCallback?.invoke(lobbyId)
                }
            }
        }

        gameRepository.remove(gameSessionId)
        mulliganBroadcastSent.remove(gameSessionId)
    }

    fun broadcastStateUpdate(gameSession: GameSession, events: List<GameEvent>) {
        val allEvents = processAutoPassLoop(gameSession, events)

        val player1 = gameSession.player1
        val player2 = gameSession.player2

        try {
            player1?.let { session ->
                val update = gameSession.createStateUpdate(session.playerId, allEvents)
                if (update != null) sender.send(session.webSocketSession, update)
                else logger.warn("createStateUpdate returned null for player1")
            }
            player2?.let { session ->
                val update = gameSession.createStateUpdate(session.playerId, allEvents)
                if (update != null) sender.send(session.webSocketSession, update)
                else logger.warn("createStateUpdate returned null for player2")
            }

            // Update spectators
            val spectatorState = gameSession.buildSpectatorState()
            if (spectatorState != null) {
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

    private fun handleUpdateBlockerAssignments(session: WebSocketSession, message: ClientMessage.UpdateBlockerAssignments) {
        val playerSession = sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSession = getGameSession(session, playerSession) ?: return

        // Forward the blocker assignments to the opponent
        val opponent = if (gameSession.player1?.playerId == playerSession.playerId) {
            gameSession.player2
        } else {
            gameSession.player1
        }

        if (opponent != null) {
            sender.send(opponent.webSocketSession, ServerMessage.OpponentBlockerAssignments(message.assignments))
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

    // Callbacks to avoid circular dependencies with LobbyHandler
    var joinSealedGameCallback: ((WebSocketSession, ClientMessage.JoinSealedGame) -> Unit)? = null
    var joinLobbyCallback: ((WebSocketSession, ClientMessage.JoinLobby) -> Unit)? = null
}
