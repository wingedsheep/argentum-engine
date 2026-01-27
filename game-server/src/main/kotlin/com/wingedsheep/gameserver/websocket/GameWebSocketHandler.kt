package com.wingedsheep.gameserver.websocket

import com.wingedsheep.gameserver.deck.RandomDeckGenerator
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.lobby.SealedLobby
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.sealed.BoosterGenerator
import com.wingedsheep.gameserver.sealed.SealedSession
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.gameserver.tournament.TournamentManager
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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebSocket handler for game communication.
 *
 * Handles:
 * - Player connections, disconnections, and reconnections
 * - Game creation and joining
 * - Action submission and routing
 * - State updates broadcast
 * - Sealed lobbies (multi-player)
 * - Round-robin tournaments
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
        register(PortalSet.basicLands)
    }

    // Random deck generator using Portal set cards with land variants for varied art
    private val deckGenerator = RandomDeckGenerator(
        cardPool = PortalSet.allCards,
        basicLandVariants = PortalSet.basicLands
    )

    // =========================================================================
    // Player Identity (persistent across reconnects)
    // =========================================================================

    /** Player identities indexed by token */
    private val playerIdentities = ConcurrentHashMap<String, PlayerIdentity>()

    /** WebSocket session ID → player token (for quick lookup on disconnect) */
    private val wsToToken = ConcurrentHashMap<String, String>()

    /** Scheduler for disconnect grace period timers */
    private val disconnectScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    /** Grace period before treating a disconnect as abandonment */
    private val disconnectGracePeriodMinutes = 5L

    // =========================================================================
    // Legacy player sessions (used by GameSession/SealedSession internally)
    // =========================================================================

    // Connected players indexed by WebSocket session ID
    private val playerSessions = ConcurrentHashMap<String, PlayerSession>()

    // Active game sessions indexed by session ID
    private val gameSessions = ConcurrentHashMap<String, GameSession>()

    // Active sealed sessions indexed by session ID
    private val sealedSessions = ConcurrentHashMap<String, SealedSession>()

    // Sealed lobbies indexed by lobby ID
    private val sealedLobbies = ConcurrentHashMap<String, SealedLobby>()

    // Tournament managers indexed by lobby ID
    private val tournaments = ConcurrentHashMap<String, TournamentManager>()

    // Map from game session ID → lobby ID (for tournament match tracking)
    private val gameToLobby = ConcurrentHashMap<String, String>()

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
                // Sealed lobby messages
                is ClientMessage.CreateSealedLobby -> handleCreateSealedLobby(session, clientMessage)
                is ClientMessage.JoinLobby -> handleJoinLobby(session, clientMessage)
                is ClientMessage.StartSealedLobby -> handleStartSealedLobby(session)
                is ClientMessage.LeaveLobby -> handleLeaveLobby(session)
                is ClientMessage.UpdateLobbySettings -> handleUpdateLobbySettings(session, clientMessage)
                // Tournament messages
                is ClientMessage.ReadyForNextRound -> handleReadyForNextRound(session)
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

    // =========================================================================
    // Connection & Reconnection
    // =========================================================================

    private fun handleConnect(session: WebSocketSession, message: ClientMessage.Connect) {
        // Check if already connected via this WebSocket
        if (playerSessions.containsKey(session.id)) {
            sendError(session, ErrorCode.ALREADY_CONNECTED, "Already connected")
            return
        }

        // Check for reconnection via token
        val token = message.token
        if (token != null) {
            val existingIdentity = playerIdentities[token]
            if (existingIdentity != null) {
                handleReconnect(session, existingIdentity)
                return
            }
            // Token not found — fall through to create new identity
        }

        // Create new player identity
        val playerId = EntityId.generate()
        val identity = PlayerIdentity(
            playerId = playerId,
            playerName = message.playerName
        )
        identity.webSocketSession = session

        playerIdentities[identity.token] = identity
        wsToToken[session.id] = identity.token

        // Create legacy player session
        val playerSession = PlayerSession(
            webSocketSession = session,
            playerId = playerId,
            playerName = message.playerName
        )
        playerSessions[session.id] = playerSession

        logger.info("Player connected: ${message.playerName} (${playerId.value}), token: ${identity.token}")

        // Send confirmation with token
        send(session, ServerMessage.Connected(playerId.value, identity.token))
    }

    private fun handleReconnect(session: WebSocketSession, identity: PlayerIdentity) {
        logger.info("Player reconnecting: ${identity.playerName} (${identity.playerId.value})")

        // Cancel disconnect timer
        identity.disconnectTimer?.cancel(false)
        identity.disconnectTimer = null

        // Swap WebSocket session
        val oldWsId = wsToToken.entries.find { it.value == identity.token }?.key
        if (oldWsId != null) {
            playerSessions.remove(oldWsId)
            wsToToken.remove(oldWsId)
            sessionLocks.remove(oldWsId)
        }

        identity.webSocketSession = session
        wsToToken[session.id] = identity.token

        // Create new legacy player session
        val playerSession = PlayerSession(
            webSocketSession = session,
            playerId = identity.playerId,
            playerName = identity.playerName,
            currentGameSessionId = identity.currentGameSessionId
        )
        playerSessions[session.id] = playerSession

        // Determine context for reconnect message
        val context: String?
        val contextId: String?

        val lobbyId = identity.currentLobbyId
        val gameSessionId = identity.currentGameSessionId

        when {
            lobbyId != null -> {
                val lobby = sealedLobbies[lobbyId]
                when {
                    lobby == null -> {
                        context = null
                        contextId = null
                    }
                    lobby.state == LobbyState.WAITING_FOR_PLAYERS -> {
                        context = "lobby"
                        contextId = lobbyId
                    }
                    lobby.state == LobbyState.DECK_BUILDING -> {
                        context = "deckBuilding"
                        contextId = lobbyId
                    }
                    lobby.state == LobbyState.TOURNAMENT_ACTIVE -> {
                        context = "tournament"
                        contextId = lobbyId
                    }
                    else -> {
                        context = null
                        contextId = null
                    }
                }
            }
            gameSessionId != null && gameSessions.containsKey(gameSessionId) -> {
                context = "game"
                contextId = gameSessionId
            }
            else -> {
                context = null
                contextId = null
            }
        }

        send(session, ServerMessage.Reconnected(
            playerId = identity.playerId.value,
            token = identity.token,
            context = context,
            contextId = contextId
        ))

        // Re-send current state based on context
        when (context) {
            "lobby" -> {
                val lobby = sealedLobbies[lobbyId!!]!!
                // Update lobby player's session reference
                lobby.players[identity.playerId]?.let { lobbyPlayer ->
                    // Identity already has the correct webSocketSession
                }
                send(session, lobby.buildLobbyUpdate(identity.playerId))
            }
            "deckBuilding" -> {
                val lobby = sealedLobbies[lobbyId!!]!!
                send(session, lobby.buildLobbyUpdate(identity.playerId))
                // Re-send sealed pool
                val playerState = lobby.players[identity.playerId]
                if (playerState != null && playerState.cardPool.isNotEmpty()) {
                    val basicLandInfos = lobby.basicLands.values.map { cardToSealedCardInfo(it) }
                    val poolInfos = playerState.cardPool.map { cardToSealedCardInfo(it) }
                    send(session, ServerMessage.SealedPoolGenerated(
                        setCode = lobby.setCode,
                        setName = lobby.setName,
                        cardPool = poolInfos,
                        basicLands = basicLandInfos
                    ))
                }
            }
            "game" -> {
                val gameSession = gameSessions[gameSessionId!!]
                if (gameSession != null) {
                    // Update the game session's player reference
                    gameSession.getPlayerSession(identity.playerId)?.let { oldPs ->
                        // Replace with new session reference
                        gameSession.removePlayer(identity.playerId)
                        gameSession.addPlayer(playerSession, emptyMap()) // deck already loaded
                    }
                    // Re-send game state
                    if (gameSession.isStarted) {
                        if (gameSession.isMulliganPhase) {
                            sendMulliganDecision(gameSession, playerSession)
                        } else {
                            val update = gameSession.createStateUpdate(identity.playerId, emptyList())
                            if (update != null) {
                                send(session, update)
                            }
                        }
                    }
                }
            }
            "tournament" -> {
                val tournament = tournaments[lobbyId!!]
                val lobby = sealedLobbies[lobbyId]
                if (tournament != null && lobby != null) {
                    val connectedIds = lobby.players.values
                        .filter { it.identity.isConnected }
                        .map { it.identity.playerId }
                        .toSet()
                    send(session, ServerMessage.TournamentStarted(
                        lobbyId = lobbyId,
                        totalRounds = tournament.totalRounds,
                        standings = tournament.getStandingsInfo(connectedIds)
                    ))
                    // Check if player has an active game in current round
                    val currentMatch = tournament.getCurrentRoundGameMatches().find {
                        it.player1Id == identity.playerId || it.player2Id == identity.playerId
                    }
                    if (currentMatch?.gameSessionId != null) {
                        val gs = gameSessions[currentMatch.gameSessionId]
                        if (gs != null && gs.isStarted && !gs.isGameOver()) {
                            identity.currentGameSessionId = currentMatch.gameSessionId
                            playerSession.currentGameSessionId = currentMatch.gameSessionId
                            val opponentId = if (currentMatch.player1Id == identity.playerId) currentMatch.player2Id else currentMatch.player1Id
                            val opponentName = lobby.players[opponentId]?.identity?.playerName ?: "Unknown"
                            send(session, ServerMessage.TournamentMatchStarting(
                                lobbyId = lobbyId,
                                round = tournament.currentRound?.roundNumber ?: 0,
                                gameSessionId = currentMatch.gameSessionId!!,
                                opponentName = opponentName
                            ))
                            // Re-send game state
                            val update = gs.createStateUpdate(identity.playerId, emptyList())
                            if (update != null) send(session, update)
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // Game Creation & Joining
    // =========================================================================

    private fun handleCreateGame(session: WebSocketSession, message: ClientMessage.CreateGame) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val deckList = if (message.deckList.isEmpty()) {
            val randomDeck = deckGenerator.generate()
            logger.info("Generated random deck for ${playerSession.playerName}: ${randomDeck.entries.take(5)}... (${randomDeck.values.sum()} cards)")
            randomDeck
        } else {
            message.deckList
        }

        val gameSession = GameSession(cardRegistry = cardRegistry)
        gameSession.addPlayer(playerSession, deckList)

        gameSessions[gameSession.sessionId] = gameSession
        waitingGameSession = gameSession

        // Update identity
        val token = wsToToken[session.id]
        if (token != null) {
            playerIdentities[token]?.currentGameSessionId = gameSession.sessionId
        }

        logger.info("Game created: ${gameSession.sessionId} by ${playerSession.playerName}")
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
            handleJoinSealedGame(session, ClientMessage.JoinSealedGame(message.sessionId))
            return
        }

        // Check if this is a lobby (auto-detect)
        val lobby = sealedLobbies[message.sessionId]
        if (lobby != null) {
            handleJoinLobby(session, ClientMessage.JoinLobby(message.sessionId))
            return
        }

        val gameSession = gameSessions[message.sessionId]
        if (gameSession == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found: ${message.sessionId}")
            return
        }

        if (gameSession.isFull) {
            sendError(session, ErrorCode.GAME_FULL, "Game is full")
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

        if (waitingGameSession?.sessionId == gameSession.sessionId) {
            waitingGameSession = null
        }

        // Update identity
        val token = wsToToken[session.id]
        if (token != null) {
            playerIdentities[token]?.currentGameSessionId = gameSession.sessionId
        }

        logger.info("Player ${playerSession.playerName} joined game ${gameSession.sessionId}")

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
            send(player1.webSocketSession, ServerMessage.GameStarted(player2.playerName))
            send(player2.webSocketSession, ServerMessage.GameStarted(player1.playerName))

            sendMulliganDecision(gameSession, player1)
            sendMulliganDecision(gameSession, player2)
        }
    }

    private fun sendMulliganDecision(gameSession: GameSession, playerSession: PlayerSession) {
        val decision = gameSession.getMulliganDecision(playerSession.playerId)
        send(playerSession.webSocketSession, decision)
    }

    // =========================================================================
    // Mulligan Handlers
    // =========================================================================

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
                sendError(session, ErrorCode.INTERNAL_ERROR, "Unexpected state")
            }
            is GameSession.MulliganActionResult.Failure -> {
                sendError(session, ErrorCode.INVALID_ACTION, result.reason)
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
        }
    }

    // =========================================================================
    // Game Action Handlers
    // =========================================================================

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

        if (gameSession.isMulliganPhase) {
            sendError(session, ErrorCode.INVALID_ACTION, "Mulligan phase not complete")
            return
        }

        val result = gameSession.executeAction(playerSession.playerId, message.action)
        when (result) {
            is GameSession.ActionResult.Success -> {
                logger.debug("Action executed successfully")
                broadcastStateUpdate(gameSession, result.events)

                if (gameSession.isGameOver()) {
                    handleGameOver(gameSession)
                }
            }
            is GameSession.ActionResult.PausedForDecision -> {
                logger.debug("Action paused for decision: ${result.decision}")
                broadcastStateUpdate(gameSession, result.events)
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
        handleGameOver(gameSession, GameOverReason.CONCESSION)
    }

    // =========================================================================
    // Sealed Draft Handlers (legacy 2-player)
    // =========================================================================

    private fun handleCreateSealedGame(session: WebSocketSession, message: ClientMessage.CreateSealedGame) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val setConfig = BoosterGenerator.getSetConfig(message.setCode)
        if (setConfig == null) {
            sendError(session, ErrorCode.INVALID_ACTION, "Unknown set code: ${message.setCode}")
            return
        }

        val sealedSession = SealedSession(
            setCode = setConfig.setCode,
            setName = setConfig.setName
        )
        sealedSession.addPlayer(playerSession)

        sealedSessions[sealedSession.sessionId] = sealedSession
        waitingSealedSession = sealedSession

        logger.info("Sealed game created: ${sealedSession.sessionId} by ${playerSession.playerName} (set: ${setConfig.setName})")

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

        val sealedSession = sealedSessions[message.sessionId]
        if (sealedSession == null) {
            val gameSession = gameSessions[message.sessionId]
            if (gameSession != null) {
                handleJoinGame(session, ClientMessage.JoinGame(message.sessionId, emptyMap()))
                return
            }
            // Check lobbies too
            val lobby = sealedLobbies[message.sessionId]
            if (lobby != null) {
                handleJoinLobby(session, ClientMessage.JoinLobby(message.sessionId))
                return
            }
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found: ${message.sessionId}")
            return
        }

        if (sealedSession.isFull) {
            sendError(session, ErrorCode.GAME_FULL, "Sealed game is full")
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
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        // Check if player is in a lobby first
        val token = wsToToken[session.id]
        val identity = token?.let { playerIdentities[it] }
        val lobbyId = identity?.currentLobbyId
        if (lobbyId != null) {
            handleLobbyDeckSubmit(session, playerSession, identity, lobbyId, message.deckList)
            return
        }

        // Legacy 2-player sealed
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

        val result = sealedSession.submitDeck(playerSession.playerId, message.deckList)
        when (result) {
            is SealedSession.DeckSubmissionResult.Success -> {
                val deckSize = message.deckList.values.sum()
                logger.info("Player ${playerSession.playerName} submitted deck ($deckSize cards)")

                send(session, ServerMessage.DeckSubmitted(deckSize))

                if (result.bothReady) {
                    startGameFromSealed(sealedSession)
                } else {
                    send(session, ServerMessage.WaitingForOpponent)

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

        sealedSession.players.forEach { (_, playerState) ->
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

        val gameSession = GameSession(cardRegistry = cardRegistry)

        sealedSession.players.forEach { (playerId, playerState) ->
            val deck = playerState.submittedDeck
                ?: throw IllegalStateException("Player $playerId has no submitted deck")
            gameSession.addPlayer(playerState.session, deck)
        }

        gameSessions[gameSession.sessionId] = gameSession
        sealedSessions.remove(sealedSession.sessionId)

        startGame(gameSession)
    }

    // =========================================================================
    // Sealed Lobby Handlers (multi-player)
    // =========================================================================

    private fun handleCreateSealedLobby(session: WebSocketSession, message: ClientMessage.CreateSealedLobby) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val setConfig = BoosterGenerator.getSetConfig(message.setCode)
        if (setConfig == null) {
            sendError(session, ErrorCode.INVALID_ACTION, "Unknown set code: ${message.setCode}")
            return
        }

        val token = wsToToken[session.id]
        val identity = token?.let { playerIdentities[it] }
        if (identity == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Identity not found")
            return
        }

        val lobby = SealedLobby(
            setCode = setConfig.setCode,
            setName = setConfig.setName,
            boosterCount = message.boosterCount.coerceIn(1, 12),
            maxPlayers = message.maxPlayers.coerceIn(2, 8)
        )
        lobby.addPlayer(identity)

        sealedLobbies[lobby.lobbyId] = lobby

        logger.info("Sealed lobby created: ${lobby.lobbyId} by ${identity.playerName} (set: ${setConfig.setName})")

        send(session, ServerMessage.LobbyCreated(lobby.lobbyId))
        broadcastLobbyUpdate(lobby)
    }

    private fun handleJoinLobby(session: WebSocketSession, message: ClientMessage.JoinLobby) {
        val playerSession = playerSessions[session.id]
        if (playerSession == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val token = wsToToken[session.id]
        val identity = token?.let { playerIdentities[it] }
        if (identity == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Identity not found")
            return
        }

        val lobby = sealedLobbies[message.lobbyId]
        if (lobby == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found: ${message.lobbyId}")
            return
        }

        if (lobby.isFull) {
            sendError(session, ErrorCode.GAME_FULL, "Lobby is full")
            return
        }

        if (lobby.state != LobbyState.WAITING_FOR_PLAYERS) {
            sendError(session, ErrorCode.INVALID_ACTION, "Lobby not accepting players")
            return
        }

        lobby.addPlayer(identity)
        logger.info("Player ${identity.playerName} joined lobby ${lobby.lobbyId}")

        broadcastLobbyUpdate(lobby)
    }

    private fun handleStartSealedLobby(session: WebSocketSession) {
        val token = wsToToken[session.id]
        val identity = token?.let { playerIdentities[it] }
        if (identity == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby")
            return
        }

        val lobby = sealedLobbies[lobbyId]
        if (lobby == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found")
            return
        }

        if (!lobby.isHost(identity.playerId)) {
            sendError(session, ErrorCode.INVALID_ACTION, "Only the host can start")
            return
        }

        if (lobby.playerCount < 2) {
            sendError(session, ErrorCode.INVALID_ACTION, "Need at least 2 players")
            return
        }

        val started = lobby.startDeckBuilding(identity.playerId)
        if (!started) {
            sendError(session, ErrorCode.INVALID_ACTION, "Failed to start lobby")
            return
        }

        logger.info("Lobby ${lobby.lobbyId} started deck building (${lobby.playerCount} players)")

        // Send pool to all players
        val basicLandInfos = lobby.basicLands.values.map { cardToSealedCardInfo(it) }
        lobby.players.forEach { (_, playerState) ->
            val poolInfos = playerState.cardPool.map { cardToSealedCardInfo(it) }
            val ws = playerState.identity.webSocketSession
            if (ws != null) {
                send(ws, ServerMessage.SealedPoolGenerated(
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
        val token = wsToToken[session.id]
        val identity = token?.let { playerIdentities[it] }
        if (identity == null) return

        val lobbyId = identity.currentLobbyId ?: return
        val lobby = sealedLobbies[lobbyId] ?: return

        lobby.removePlayer(identity.playerId)
        identity.currentLobbyId = null

        logger.info("Player ${identity.playerName} left lobby $lobbyId")

        if (lobby.playerCount == 0) {
            sealedLobbies.remove(lobbyId)
            logger.info("Lobby $lobbyId removed (empty)")
        } else {
            broadcastLobbyUpdate(lobby)
        }
    }

    private fun handleUpdateLobbySettings(session: WebSocketSession, message: ClientMessage.UpdateLobbySettings) {
        val token = wsToToken[session.id]
        val identity = token?.let { playerIdentities[it] }
        if (identity == null) {
            sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby")
            return
        }

        val lobby = sealedLobbies[lobbyId]
        if (lobby == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found")
            return
        }

        if (!lobby.isHost(identity.playerId)) {
            sendError(session, ErrorCode.INVALID_ACTION, "Only the host can change settings")
            return
        }

        if (lobby.state != LobbyState.WAITING_FOR_PLAYERS) {
            sendError(session, ErrorCode.INVALID_ACTION, "Cannot change settings after start")
            return
        }

        message.boosterCount?.let { lobby.boosterCount = it.coerceIn(1, 12) }
        message.maxPlayers?.let { lobby.maxPlayers = it.coerceIn(2, 8) }

        broadcastLobbyUpdate(lobby)
    }

    private fun handleLobbyDeckSubmit(
        session: WebSocketSession,
        playerSession: PlayerSession,
        identity: PlayerIdentity,
        lobbyId: String,
        deckList: Map<String, Int>
    ) {
        val lobby = sealedLobbies[lobbyId]
        if (lobby == null) {
            sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found")
            return
        }

        val result = lobby.submitDeck(identity.playerId, deckList)
        when (result) {
            is SealedLobby.DeckSubmissionResult.Success -> {
                val deckSize = deckList.values.sum()
                logger.info("Player ${identity.playerName} submitted deck ($deckSize cards) in lobby $lobbyId")

                send(session, ServerMessage.DeckSubmitted(deckSize))
                broadcastLobbyUpdate(lobby)

                if (result.allReady) {
                    startTournament(lobby)
                }
            }
            is SealedLobby.DeckSubmissionResult.Error -> {
                sendError(session, ErrorCode.INVALID_DECK, result.message)
            }
        }
    }

    private fun broadcastLobbyUpdate(lobby: SealedLobby) {
        lobby.players.forEach { (playerId, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                send(ws, lobby.buildLobbyUpdate(playerId))
            }
        }
    }

    // =========================================================================
    // Tournament Handlers
    // =========================================================================

    private fun startTournament(lobby: SealedLobby) {
        logger.info("Starting tournament for lobby ${lobby.lobbyId} with ${lobby.playerCount} players")

        lobby.startTournament()

        val players = lobby.players.values.map { ps ->
            ps.identity.playerId to ps.identity.playerName
        }

        val tournament = TournamentManager(lobby.lobbyId, players)
        tournaments[lobby.lobbyId] = tournament

        // Broadcast tournament started
        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                send(ws, ServerMessage.TournamentStarted(
                    lobbyId = lobby.lobbyId,
                    totalRounds = tournament.totalRounds,
                    standings = tournament.getStandingsInfo(connectedIds)
                ))
            }
        }

        // Start first round
        startNextTournamentRound(lobby.lobbyId)
    }

    private fun startNextTournamentRound(lobbyId: String) {
        val lobby = sealedLobbies[lobbyId] ?: return
        val tournament = tournaments[lobbyId] ?: return

        val round = tournament.startNextRound()
        if (round == null) {
            // Tournament complete
            completeTournament(lobbyId)
            return
        }

        logger.info("Starting round ${round.roundNumber} for tournament $lobbyId")

        // Create game sessions for each match
        val gameMatches = tournament.getCurrentRoundGameMatches()

        for (match in gameMatches) {
            val player1State = lobby.players[match.player1Id] ?: continue
            val player2State = lobby.players[match.player2Id ?: continue] ?: continue

            val deck1 = lobby.getSubmittedDeck(match.player1Id) ?: continue
            val deck2 = lobby.getSubmittedDeck(match.player2Id!!) ?: continue

            // Create game session
            val gameSession = GameSession(cardRegistry = cardRegistry)

            val ps1 = player1State.identity.toPlayerSession()
            val ps2 = player2State.identity.toPlayerSession()

            gameSession.addPlayer(ps1, deck1)
            gameSession.addPlayer(ps2, deck2)

            gameSessions[gameSession.sessionId] = gameSession
            gameToLobby[gameSession.sessionId] = lobbyId
            match.gameSessionId = gameSession.sessionId

            // Update identities
            player1State.identity.currentGameSessionId = gameSession.sessionId
            player2State.identity.currentGameSessionId = gameSession.sessionId

            // Update player sessions in the map
            val ws1 = player1State.identity.webSocketSession
            val ws2 = player2State.identity.webSocketSession
            if (ws1 != null) {
                playerSessions[ws1.id]?.currentGameSessionId = gameSession.sessionId
            }
            if (ws2 != null) {
                playerSessions[ws2.id]?.currentGameSessionId = gameSession.sessionId
            }

            // Notify players
            if (ws1 != null && ws1.isOpen) {
                send(ws1, ServerMessage.TournamentMatchStarting(
                    lobbyId = lobbyId,
                    round = round.roundNumber,
                    gameSessionId = gameSession.sessionId,
                    opponentName = player2State.identity.playerName
                ))
            }
            if (ws2 != null && ws2.isOpen) {
                send(ws2, ServerMessage.TournamentMatchStarting(
                    lobbyId = lobbyId,
                    round = round.roundNumber,
                    gameSessionId = gameSession.sessionId,
                    opponentName = player1State.identity.playerName
                ))
            }

            // Start the game
            startGame(gameSession)
        }

        // Notify BYE players
        for (match in round.matches) {
            if (match.isBye) {
                val playerState = lobby.players[match.player1Id]
                val ws = playerState?.identity?.webSocketSession
                if (ws != null && ws.isOpen) {
                    send(ws, ServerMessage.TournamentBye(
                        lobbyId = lobbyId,
                        round = round.roundNumber
                    ))
                }
            }
        }
    }

    private fun handleGameOver(gameSession: GameSession, reason: GameOverReason? = null) {
        val winnerId = gameSession.getWinnerId()
        val gameOverReason = reason ?: gameSession.getGameOverReason() ?: GameOverReason.LIFE_ZERO
        val message = ServerMessage.GameOver(winnerId, gameOverReason)

        gameSession.player1?.let { send(it.webSocketSession, message) }
        gameSession.player2?.let { send(it.webSocketSession, message) }

        val gameSessionId = gameSession.sessionId

        // Check if this is a tournament match
        val lobbyId = gameToLobby[gameSessionId]
        if (lobbyId != null) {
            val tournament = tournaments[lobbyId]
            if (tournament != null) {
                tournament.reportMatchResult(gameSessionId, winnerId)
                gameToLobby.remove(gameSessionId)

                // Check if round is complete
                if (tournament.isRoundComplete()) {
                    handleRoundComplete(lobbyId)
                }
            }
        }

        // Clean up game session
        gameSessions.remove(gameSessionId)
        mulliganBroadcastSent.remove(gameSessionId)
    }

    private fun handleRoundComplete(lobbyId: String) {
        val lobby = sealedLobbies[lobbyId] ?: return
        val tournament = tournaments[lobbyId] ?: return

        val round = tournament.currentRound ?: return
        logger.info("Round ${round.roundNumber} complete for tournament $lobbyId")

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        val roundComplete = ServerMessage.RoundComplete(
            lobbyId = lobbyId,
            round = round.roundNumber,
            results = tournament.getCurrentRoundResults(),
            standings = tournament.getStandingsInfo(connectedIds)
        )

        // Clear game session IDs from identities
        lobby.players.forEach { (_, playerState) ->
            playerState.identity.currentGameSessionId = null
        }

        // Broadcast to all players
        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                send(ws, roundComplete)
            }
        }

        // Check if tournament is complete
        if (tournament.isComplete) {
            completeTournament(lobbyId)
        } else {
            // Auto-advance to next round
            startNextTournamentRound(lobbyId)
        }
    }

    private fun completeTournament(lobbyId: String) {
        val lobby = sealedLobbies[lobbyId] ?: return
        val tournament = tournaments[lobbyId] ?: return

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
                send(ws, message)
            }
        }
    }

    private fun handleReadyForNextRound(session: WebSocketSession) {
        // Currently auto-advancing rounds, so this is a no-op.
        // Could be used for manual round advancement in the future.
    }

    // =========================================================================
    // Disconnect & Reconnect
    // =========================================================================

    private fun handleDisconnect(session: WebSocketSession) {
        sessionLocks.remove(session.id)

        val token = wsToToken.remove(session.id)
        val playerSession = playerSessions.remove(session.id)

        if (token != null) {
            val identity = playerIdentities[token]
            if (identity != null) {
                logger.info("Player disconnected: ${identity.playerName} (starting ${disconnectGracePeriodMinutes}m grace period)")

                identity.webSocketSession = null

                // Start grace period timer
                identity.disconnectTimer = disconnectScheduler.schedule({
                    handleDisconnectTimeout(token)
                }, disconnectGracePeriodMinutes, TimeUnit.MINUTES)

                // Broadcast updated connection status to lobby
                val lobbyId = identity.currentLobbyId
                if (lobbyId != null) {
                    val lobby = sealedLobbies[lobbyId]
                    if (lobby != null) {
                        broadcastLobbyUpdate(lobby)
                    }
                }

                return
            }
        }

        // Fallback: no identity found, use legacy cleanup
        if (playerSession != null) {
            logger.info("Player disconnected (no identity): ${playerSession.playerName}")
            legacyHandleDisconnect(playerSession)
        }
    }

    private fun handleDisconnectTimeout(token: String) {
        val identity = playerIdentities.remove(token) ?: return

        logger.info("Disconnect timeout for ${identity.playerName} — treating as abandonment")

        // Handle lobby abandonment
        val lobbyId = identity.currentLobbyId
        if (lobbyId != null) {
            val lobby = sealedLobbies[lobbyId]
            if (lobby != null) {
                when (lobby.state) {
                    LobbyState.WAITING_FOR_PLAYERS, LobbyState.DECK_BUILDING -> {
                        lobby.removePlayer(identity.playerId)
                        if (lobby.playerCount == 0) {
                            sealedLobbies.remove(lobbyId)
                            tournaments.remove(lobbyId)
                        } else {
                            broadcastLobbyUpdate(lobby)
                        }
                    }
                    LobbyState.TOURNAMENT_ACTIVE -> {
                        // Record auto-losses
                        val tournament = tournaments[lobbyId]
                        tournament?.recordAbandon(identity.playerId)

                        // Check if current round is now complete
                        if (tournament?.isRoundComplete() == true) {
                            handleRoundComplete(lobbyId)
                        }
                    }
                    LobbyState.TOURNAMENT_COMPLETE -> {
                        // Nothing to do
                    }
                }
            }
        }

        // Handle active game abandonment
        val gameSessionId = identity.currentGameSessionId
        if (gameSessionId != null) {
            val gameSession = gameSessions[gameSessionId]
            if (gameSession != null) {
                val opponentId = gameSession.getOpponentId(identity.playerId)
                if (opponentId != null) {
                    gameSession.playerConcedes(identity.playerId)
                    handleGameOver(gameSession, GameOverReason.DISCONNECTION)
                } else {
                    // No opponent, just clean up
                    gameSessions.remove(gameSessionId)
                    mulliganBroadcastSent.remove(gameSessionId)
                }
            }
        }
    }

    /**
     * Legacy disconnect handler for players without a PlayerIdentity.
     */
    private fun legacyHandleDisconnect(playerSession: PlayerSession) {
        val gameSessionId = playerSession.currentGameSessionId
        if (gameSessionId != null) {
            val gameSession = gameSessions[gameSessionId]
            if (gameSession != null) {
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

                gameSessions.remove(gameSessionId)
                mulliganBroadcastSent.remove(gameSessionId)
                if (waitingGameSession?.sessionId == gameSessionId) {
                    waitingGameSession = null
                }
            }

            val sealedSession = sealedSessions[gameSessionId]
            if (sealedSession != null) {
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

                sealedSessions.remove(gameSessionId)
                if (waitingSealedSession?.sessionId == gameSessionId) {
                    waitingSealedSession = null
                }
            }
        }
    }

    // =========================================================================
    // State Broadcasting
    // =========================================================================

    private fun broadcastStateUpdate(
        gameSession: GameSession,
        events: List<GameEvent>
    ) {
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

    private fun processAutoPassLoop(
        gameSession: GameSession,
        initialEvents: List<GameEvent>
    ): List<GameEvent> {
        val allEvents = initialEvents.toMutableList()
        var loopCount = 0
        val maxLoops = 100

        while (loopCount < maxLoops) {
            if (gameSession.isGameOver()) break

            val autoPassPlayer = gameSession.getAutoPassPlayer() ?: break

            logger.debug("Auto-passing for player: ${autoPassPlayer.value}")

            val result = gameSession.executeAutoPass(autoPassPlayer)
            when (result) {
                is GameSession.ActionResult.Success -> {
                    allEvents.addAll(result.events)
                }
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

        if (loopCount >= maxLoops) {
            logger.warn("Auto-pass loop hit safety limit!")
        }

        return allEvents
    }

    // =========================================================================
    // Sending
    // =========================================================================

    private fun send(session: WebSocketSession, message: ServerMessage) {
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
