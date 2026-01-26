package com.wingedsheep.gameserver.websocket

import com.wingedsheep.gameserver.deck.RandomDeckGenerator
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.sealed.BoosterGenerator
import com.wingedsheep.gameserver.sealed.SealedSession
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebSocket handler for game communication.
 *
 * Handles:
 * - Player connections and disconnections
 * - Game creation and joining
 * - Action submission and routing
 * - State updates broadcast
 */
@Component
class GameWebSocketHandler : TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(GameWebSocketHandler::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        serializersModule = engineSerializersModule
    }

    // Card registry for loading card definitions
    private val cardRegistry = CardRegistry().apply {
        register(PortalSet.allCards)
        // Also register basic land variants
        register(PortalSet.basicLands)
    }

    // Random deck generator using Portal set cards with land variants for varied art
    private val deckGenerator = RandomDeckGenerator(
        cardPool = PortalSet.allCards,
        basicLandVariants = PortalSet.basicLands
    )

    // Connected players indexed by WebSocket session ID
    private val playerSessions = ConcurrentHashMap<String, PlayerSession>()

    // Active game sessions indexed by session ID
    private val gameSessions = ConcurrentHashMap<String, GameSession>()

    // Active sealed sessions indexed by session ID
    private val sealedSessions = ConcurrentHashMap<String, SealedSession>()

    // Waiting game (single game session waiting for second player)
    @Volatile
    private var waitingGameSession: GameSession? = null

    // Waiting sealed game (single sealed session waiting for second player)
    @Volatile
    private var waitingSealedSession: SealedSession? = null

    // Per-session locks for thread-safe WebSocket writes
    private val sessionLocks = ConcurrentHashMap<String, Any>()

    // Track which games have already sent the mulligan-complete broadcast
    private val mulliganBroadcastSent = ConcurrentHashMap<String, AtomicBoolean>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.info("WebSocket connection established: ${session.id}")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val clientMessage = json.decodeFromString<ClientMessage>(message.payload)
            logger.debug("Received message from ${session.id}: $clientMessage")

            when (clientMessage) {
                is ClientMessage.Connect -> handleConnect(session, clientMessage)
                is ClientMessage.CreateGame -> handleCreateGame(session, clientMessage)
                is ClientMessage.JoinGame -> handleJoinGame(session, clientMessage)
                is ClientMessage.SubmitAction -> handleSubmitAction(session, clientMessage)
                is ClientMessage.Concede -> handleConcede(session)
                is ClientMessage.KeepHand -> handleKeepHand(session)
                is ClientMessage.Mulligan -> handleMulligan(session)
                is ClientMessage.ChooseBottomCards -> handleChooseBottomCards(session, clientMessage)
                // Sealed draft messages
                is ClientMessage.CreateSealedGame -> handleCreateSealedGame(session, clientMessage)
                is ClientMessage.JoinSealedGame -> handleJoinSealedGame(session, clientMessage)
                is ClientMessage.SubmitSealedDeck -> handleSubmitSealedDeck(session, clientMessage)
            }
        } catch (e: Exception) {
            logger.error("Error handling message from ${session.id}", e)
            sendError(session, ErrorCode.INTERNAL_ERROR, "Failed to process message: ${e.message}")
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info("WebSocket connection closed: ${session.id}, status: $status")
        handleDisconnect(session)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        logger.error("Transport error for ${session.id}", exception)
        handleDisconnect(session)
    }

    private fun handleConnect(session: WebSocketSession, message: ClientMessage.Connect) {
        // Check if already connected
        if (playerSessions.containsKey(session.id)) {
            sendError(session, ErrorCode.ALREADY_CONNECTED, "Already connected")
            return
        }

        // Create new player session
        val playerId = EntityId.generate()
        val playerSession = PlayerSession(
            webSocketSession = session,
            playerId = playerId,
            playerName = message.playerName
        )
        playerSessions[session.id] = playerSession

        logger.info("Player connected: ${message.playerName} (${playerId.value})")

        // Send confirmation
        send(session, ServerMessage.Connected(playerId.value))
    }

    private fun handleCreateGame(session: WebSocketSession, message: ClientMessage.CreateGame) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        // If client sends empty deck, generate a random one from Portal set
        val deckList = if (message.deckList.isEmpty()) {
            val randomDeck = deckGenerator.generate()
            logger.info("Generated random deck for ${playerSession.playerName}: ${randomDeck.entries.take(5)}... (${randomDeck.values.sum()} cards)")
            randomDeck
        } else {
            message.deckList
        }

        // Create new game session with CardRegistry
        val gameSession = GameSession(cardRegistry = cardRegistry)
        gameSession.addPlayer(playerSession, deckList)

        gameSessions[gameSession.sessionId] = gameSession
        waitingGameSession = gameSession

        logger.info("Game created: ${gameSession.sessionId} by ${playerSession.playerName}")

        // Send confirmation with session ID
        send(session, ServerMessage.GameCreated(gameSession.sessionId))
    }

    private fun handleJoinGame(session: WebSocketSession, message: ClientMessage.JoinGame) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        // Check if this is a sealed session (auto-detect)
        val sealedSession = sealedSessions[message.sessionId]
        if (sealedSession != null) {
            // Redirect to sealed join handler
            handleJoinSealedGame(session, ClientMessage.JoinSealedGame(message.sessionId))
            return
        }

        // Find game session
        val gameSession = gameSessions[message.sessionId]
        if (gameSession == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found: ${message.sessionId}")
            return
        }

        if (gameSession.isFull) {
            sendError(session, ErrorCode.GAME_FULL, "Game is full")
            return
        }

        // If client sends empty deck, generate a random one from Portal set
        val deckList = if (message.deckList.isEmpty()) {
            val randomDeck = deckGenerator.generate()
            logger.info("Generated random deck for ${playerSession.playerName}: ${randomDeck.entries.take(5)}... (${randomDeck.values.sum()} cards)")
            randomDeck
        } else {
            message.deckList
        }

        // Add player to game
        gameSession.addPlayer(playerSession, deckList)

        // Clear waiting game if this was it
        if (waitingGameSession?.sessionId == gameSession.sessionId) {
            waitingGameSession = null
        }

        logger.info("Player ${playerSession.playerName} joined game ${gameSession.sessionId}")

        // Start the game if ready
        if (gameSession.isReady) {
            startGame(gameSession)
        }
    }

    private fun startGame(gameSession: GameSession) {
        logger.info("Starting game: ${gameSession.sessionId}")

        gameSession.startGame()

        val player1 = gameSession.player1
        val player2 = gameSession.player2

        if (player1 != null && player2 != null) {
            // Notify both players
            send(player1.webSocketSession, ServerMessage.GameStarted(player2.playerName))
            send(player2.webSocketSession, ServerMessage.GameStarted(player1.playerName))

            // Send mulligan decisions to both players
            sendMulliganDecision(gameSession, player1)
            sendMulliganDecision(gameSession, player2)
        }
    }

    private fun sendMulliganDecision(gameSession: GameSession, playerSession: PlayerSession) {
        val decision = gameSession.getMulliganDecision(playerSession.playerId)
        send(playerSession.webSocketSession, decision)
    }

    private fun handleKeepHand(session: WebSocketSession) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSessionId = playerSession.currentGameSessionId
        if (gameSessionId == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a game")
            return
        }

        val gameSession = gameSessions[gameSessionId]
        if (gameSession == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found")
            return
        }

        if (!gameSession.isMulliganPhase) {
            sendError(session, ErrorCode.INVALID_ACTION, "Not in mulligan phase")
            return
        }

        val result = gameSession.keepHand(playerSession.playerId)
        when (result) {
            is GameSession.MulliganActionResult.Success -> {
                logger.info("Player ${playerSession.playerName} kept hand")
                val finalHandSize = gameSession.getHand(playerSession.playerId).size
                send(session, ServerMessage.MulliganComplete(finalHandSize))
                checkMulliganPhaseComplete(gameSession)
            }
            is GameSession.MulliganActionResult.NeedsBottomCards -> {
                logger.info("Player ${playerSession.playerName} kept hand, needs to choose ${result.count} cards for bottom")
                val msg = gameSession.getChooseBottomCardsMessage(playerSession.playerId)
                if (msg != null) {
                    send(session, msg)
                }
            }
            is GameSession.MulliganActionResult.Failure -> {
                sendError(session, ErrorCode.INVALID_ACTION, result.reason)
            }
        }
    }

    private fun handleMulligan(session: WebSocketSession) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSessionId = playerSession.currentGameSessionId
        if (gameSessionId == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a game")
            return
        }

        val gameSession = gameSessions[gameSessionId]
        if (gameSession == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found")
            return
        }

        if (!gameSession.isMulliganPhase) {
            sendError(session, ErrorCode.INVALID_ACTION, "Not in mulligan phase")
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
                // This shouldn't happen from takeMulligan, but handle it
                val msg = gameSession.getChooseBottomCardsMessage(playerSession.playerId)
                if (msg != null) {
                    send(session, msg)
                }
            }
            is GameSession.MulliganActionResult.Failure -> {
                sendError(session, ErrorCode.INVALID_ACTION, result.reason)
            }
        }
    }

    private fun handleChooseBottomCards(session: WebSocketSession, message: ClientMessage.ChooseBottomCards) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSessionId = playerSession.currentGameSessionId
        if (gameSessionId == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a game")
            return
        }

        val gameSession = gameSessions[gameSessionId]
        if (gameSession == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found")
            return
        }

        if (!gameSession.isAwaitingBottomCards(playerSession.playerId)) {
            sendError(session, ErrorCode.INVALID_ACTION, "Not awaiting bottom card selection")
            return
        }

        val result = gameSession.chooseBottomCards(playerSession.playerId, message.cardIds)
        when (result) {
            is GameSession.MulliganActionResult.Success -> {
                logger.info("Player ${playerSession.playerName} completed mulligan")
                val finalHandSize = gameSession.getHand(playerSession.playerId).size
                send(session, ServerMessage.MulliganComplete(finalHandSize))
                checkMulliganPhaseComplete(gameSession)
            }
            is GameSession.MulliganActionResult.NeedsBottomCards -> {
                // Shouldn't happen from chooseBottomCards
                sendError(session, ErrorCode.INTERNAL_ERROR, "Unexpected state")
            }
            is GameSession.MulliganActionResult.Failure -> {
                sendError(session, ErrorCode.INVALID_ACTION, result.reason)
            }
        }
    }

    private fun checkMulliganPhaseComplete(gameSession: GameSession) {
        if (gameSession.allMulligansComplete) {
            // Use atomic flag to ensure only one thread broadcasts the state update
            val broadcastFlag = mulliganBroadcastSent.computeIfAbsent(gameSession.sessionId) { AtomicBoolean(false) }
            if (broadcastFlag.compareAndSet(false, true)) {
                logger.info("Mulligan phase complete for game ${gameSession.sessionId}")
                // The engine handles advancing out of mulligan phase automatically
                // Send initial state updates to both players
                broadcastStateUpdate(gameSession, emptyList())
            }
        }
    }

    private fun handleSubmitAction(session: WebSocketSession, message: ClientMessage.SubmitAction) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSessionId = playerSession.currentGameSessionId
        if (gameSessionId == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a game")
            return
        }

        val gameSession = gameSessions[gameSessionId]
        if (gameSession == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found")
            return
        }

        // Check if we're still in mulligan phase
        if (gameSession.isMulliganPhase) {
            sendError(session, ErrorCode.INVALID_ACTION, "Mulligan phase not complete")
            return
        }

        // Execute the action
        val result = gameSession.executeAction(playerSession.playerId, message.action)
        when (result) {
            is GameSession.ActionResult.Success -> {
                logger.debug("Action executed successfully")
                broadcastStateUpdate(gameSession, result.events)

                // Check if game ended
                if (gameSession.isGameOver()) {
                    broadcastGameOver(gameSession)
                }
            }
            is GameSession.ActionResult.PausedForDecision -> {
                logger.debug("Action paused for decision: ${result.decision}")
                broadcastStateUpdate(gameSession, result.events)
                // The client will receive the state with pending decision info
            }
            is GameSession.ActionResult.Failure -> {
                sendError(session, ErrorCode.INVALID_ACTION, result.reason)
            }
        }
    }

    private fun handleConcede(session: WebSocketSession) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val gameSessionId = playerSession.currentGameSessionId
        if (gameSessionId == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a game")
            return
        }

        val gameSession = gameSessions[gameSessionId]
        if (gameSession == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found")
            return
        }

        logger.info("Player ${playerSession.playerName} conceded game ${gameSession.sessionId}")

        gameSession.playerConcedes(playerSession.playerId)
        broadcastGameOver(gameSession, GameOverReason.CONCESSION)
    }

    // =========================================================================
    // Sealed Draft Handlers
    // =========================================================================

    private fun handleCreateSealedGame(session: WebSocketSession, message: ClientMessage.CreateSealedGame) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        // Validate set code
        val setConfig = BoosterGenerator.getSetConfig(message.setCode)
        if (setConfig == null) {
            sendError(session, ErrorCode.INVALID_ACTION, "Unknown set code: ${message.setCode}")
            return
        }

        // Create new sealed session
        val sealedSession = SealedSession(
            setCode = setConfig.setCode,
            setName = setConfig.setName
        )
        sealedSession.addPlayer(playerSession)

        sealedSessions[sealedSession.sessionId] = sealedSession
        waitingSealedSession = sealedSession

        logger.info("Sealed game created: ${sealedSession.sessionId} by ${playerSession.playerName} (set: ${setConfig.setName})")

        // Send confirmation
        send(session, ServerMessage.SealedGameCreated(
            sessionId = sealedSession.sessionId,
            setCode = setConfig.setCode,
            setName = setConfig.setName
        ))
    }

    private fun handleJoinSealedGame(session: WebSocketSession, message: ClientMessage.JoinSealedGame) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        // Find sealed session
        val sealedSession = sealedSessions[message.sessionId]
        if (sealedSession == null) {
            // Check if this is a regular game (auto-detect)
            val gameSession = gameSessions[message.sessionId]
            if (gameSession != null) {
                // Redirect to regular join handler
                handleJoinGame(session, ClientMessage.JoinGame(message.sessionId, emptyMap()))
                return
            }
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found: ${message.sessionId}")
            return
        }

        if (sealedSession.isFull) {
            sendError(session, ErrorCode.GAME_FULL, "Sealed game is full")
            return
        }

        // Add player to sealed session
        sealedSession.addPlayer(playerSession)

        // Clear waiting session if this was it
        if (waitingSealedSession?.sessionId == sealedSession.sessionId) {
            waitingSealedSession = null
        }

        logger.info("Player ${playerSession.playerName} joined sealed game ${sealedSession.sessionId}")

        // Generate pools for both players
        sealedSession.generatePools()

        // Send pool information to both players
        sendSealedPoolToAllPlayers(sealedSession)
    }

    private fun handleSubmitSealedDeck(session: WebSocketSession, message: ClientMessage.SubmitSealedDeck) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val sealedSessionId = playerSession.currentGameSessionId
        if (sealedSessionId == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a sealed game")
            return
        }

        val sealedSession = sealedSessions[sealedSessionId]
        if (sealedSession == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Sealed game not found")
            return
        }

        // Submit the deck
        val result = sealedSession.submitDeck(playerSession.playerId, message.deckList)
        when (result) {
            is SealedSession.DeckSubmissionResult.Success -> {
                val deckSize = message.deckList.values.sum()
                logger.info("Player ${playerSession.playerName} submitted deck ($deckSize cards)")

                // Notify this player their deck was accepted
                send(session, ServerMessage.DeckSubmitted(deckSize))

                if (result.bothReady) {
                    // Both players ready - start the actual game
                    startGameFromSealed(sealedSession)
                } else {
                    // Notify this player they're waiting
                    send(session, ServerMessage.WaitingForOpponent)

                    // Notify opponent that this player submitted
                    val opponentId = sealedSession.getOpponentId(playerSession.playerId)
                    if (opponentId != null) {
                        val opponentSession = sealedSession.getPlayerSession(opponentId)
                        if (opponentSession != null) {
                            send(opponentSession.webSocketSession, ServerMessage.OpponentDeckSubmitted)
                        }
                    }
                }
            }
            is SealedSession.DeckSubmissionResult.Error -> {
                sendError(session, ErrorCode.INVALID_DECK, result.message)
            }
        }
    }

    private fun sendSealedPoolToAllPlayers(sealedSession: SealedSession) {
        val basicLandInfos = sealedSession.basicLands.values.map { cardToSealedCardInfo(it) }

        sealedSession.players.forEach { (playerId, playerState) ->
            val poolInfos = playerState.cardPool.map { cardToSealedCardInfo(it) }

            send(
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

    private fun cardToSealedCardInfo(card: CardDefinition): ServerMessage.SealedCardInfo {
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

    private fun startGameFromSealed(sealedSession: SealedSession) {
        logger.info("Starting game from sealed session: ${sealedSession.sessionId}")

        // Create a new game session
        val gameSession = GameSession(cardRegistry = cardRegistry)

        // Add both players with their submitted decks
        sealedSession.players.forEach { (playerId, playerState) ->
            val deck = playerState.submittedDeck
                ?: throw IllegalStateException("Player $playerId has no submitted deck")
            gameSession.addPlayer(playerState.session, deck)
        }

        // Register the game session
        gameSessions[gameSession.sessionId] = gameSession

        // Clean up sealed session
        sealedSessions.remove(sealedSession.sessionId)

        // Start the game
        startGame(gameSession)
    }

    private fun handleDisconnect(session: WebSocketSession) {
        // Clean up session lock
        sessionLocks.remove(session.id)

        val playerSession = playerSessions.remove(session.id) ?: return

        logger.info("Player disconnected: ${playerSession.playerName}")

        // Handle game cleanup
        val gameSessionId = playerSession.currentGameSessionId
        if (gameSessionId != null) {
            // Check if it's a regular game session
            val gameSession = gameSessions[gameSessionId]
            if (gameSession != null) {
                // Notify opponent and end game
                val opponentId = gameSession.getOpponentId(playerSession.playerId)
                if (opponentId != null) {
                    val opponentSession = gameSession.getPlayerSession(opponentId)
                    if (opponentSession?.isConnected == true) {
                        send(
                            opponentSession.webSocketSession,
                            ServerMessage.GameOver(opponentId, GameOverReason.DISCONNECTION)
                        )
                    }
                }

                // Clean up game session
                gameSessions.remove(gameSessionId)
                mulliganBroadcastSent.remove(gameSessionId)
                if (waitingGameSession?.sessionId == gameSessionId) {
                    waitingGameSession = null
                }
            }

            // Check if it's a sealed session
            val sealedSession = sealedSessions[gameSessionId]
            if (sealedSession != null) {
                // Notify opponent if present
                val opponentId = sealedSession.getOpponentId(playerSession.playerId)
                if (opponentId != null) {
                    val opponentPlayerSession = sealedSession.getPlayerSession(opponentId)
                    if (opponentPlayerSession?.isConnected == true) {
                        send(
                            opponentPlayerSession.webSocketSession,
                            ServerMessage.Error(ErrorCode.GAME_NOT_FOUND, "Opponent disconnected")
                        )
                    }
                }

                // Clean up sealed session
                sealedSessions.remove(gameSessionId)
                if (waitingSealedSession?.sessionId == gameSessionId) {
                    waitingSealedSession = null
                }
            }
        }
    }

    private fun broadcastStateUpdate(
        gameSession: GameSession,
        events: List<GameEvent>
    ) {
        // First, process any auto-passes and accumulate events
        val allEvents = processAutoPassLoop(gameSession, events)

        val player1 = gameSession.player1
        val player2 = gameSession.player2

        try {
            player1?.let { session ->
                val update = gameSession.createStateUpdate(session.playerId, allEvents)
                if (update != null) {
                    send(session.webSocketSession, update)
                } else {
                    logger.warn("createStateUpdate returned null for player1")
                }
            }

            player2?.let { session ->
                val update = gameSession.createStateUpdate(session.playerId, allEvents)
                if (update != null) {
                    send(session.webSocketSession, update)
                } else {
                    logger.warn("createStateUpdate returned null for player2")
                }
            }
        } catch (e: Exception) {
            logger.error("Error broadcasting state update", e)
        }
    }

    /**
     * Process auto-pass loop until a player needs to make a decision.
     *
     * This implements Arena-style smart priority passing:
     * - If the player with priority should auto-pass, execute PassPriority automatically
     * - Continue until a player needs to make a real decision
     * - Accumulate all events from auto-passes to send to clients
     *
     * @param gameSession The game session
     * @param initialEvents Events from the original action
     * @return All accumulated events (initial + auto-pass events)
     */
    private fun processAutoPassLoop(
        gameSession: GameSession,
        initialEvents: List<GameEvent>
    ): List<GameEvent> {
        val allEvents = initialEvents.toMutableList()
        var loopCount = 0
        val maxLoops = 100 // Safety limit to prevent infinite loops

        while (loopCount < maxLoops) {
            // Check if game is over
            if (gameSession.isGameOver()) {
                break
            }

            // Check if there's a player that should auto-pass
            val autoPassPlayer = gameSession.getAutoPassPlayer()
            if (autoPassPlayer == null) {
                // No auto-pass needed, stop the loop
                break
            }

            logger.debug("Auto-passing for player: ${autoPassPlayer.value}")

            // Execute the auto-pass
            val result = gameSession.executeAutoPass(autoPassPlayer)
            when (result) {
                is GameSession.ActionResult.Success -> {
                    allEvents.addAll(result.events)
                }
                is GameSession.ActionResult.PausedForDecision -> {
                    // A decision is needed, stop auto-passing
                    allEvents.addAll(result.events)
                    break
                }
                is GameSession.ActionResult.Failure -> {
                    // Something went wrong, stop auto-passing
                    logger.warn("Auto-pass failed: ${result.reason}")
                    break
                }
            }

            loopCount++
        }

        if (loopCount >= maxLoops) {
            logger.warn("Auto-pass loop hit safety limit!")
        }

        return allEvents
    }

    private fun broadcastGameOver(
        gameSession: GameSession,
        reason: GameOverReason? = null
    ) {
        val winnerId = gameSession.getWinnerId()
        val gameOverReason = reason ?: gameSession.getGameOverReason() ?: GameOverReason.LIFE_ZERO
        val message = ServerMessage.GameOver(winnerId, gameOverReason)

        gameSession.player1?.let { send(it.webSocketSession, message) }
        gameSession.player2?.let { send(it.webSocketSession, message) }

        // Clean up
        gameSessions.remove(gameSession.sessionId)
        mulliganBroadcastSent.remove(gameSession.sessionId)
    }

    private fun send(session: WebSocketSession, message: ServerMessage) {
        // Get or create a lock object for this session to prevent concurrent writes
        val lock = sessionLocks.computeIfAbsent(session.id) { Any() }
        synchronized(lock) {
            try {
                if (session.isOpen) {
                    val jsonText = json.encodeToString(message)
                    session.sendMessage(TextMessage(jsonText))
                } else {
                    logger.warn("Cannot send message - session ${session.id} is closed")
                }
            } catch (e: Exception) {
                logger.error("Failed to send message to ${session.id}", e)
            }
        }
    }

    private fun sendError(session: WebSocketSession, code: ErrorCode, message: String) {
        send(session, ServerMessage.Error(code, message))
    }
}
