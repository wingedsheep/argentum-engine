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
import com.wingedsheep.engine.registry.CardRegistry
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
    private val gamePlayHandler: GamePlayHandler
) {
    private val logger = LoggerFactory.getLogger(LobbyHandler::class.java)

    @Volatile
    var waitingSealedSession: SealedSession? = null

    /** Coroutine scope for draft timers */
    private val draftScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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
            BoosterGenerator.getSetConfig(setCode) ?: run {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Unknown set code: $setCode")
                return
            }
        }

        val sealedSession = SealedSession(
            setCodes = setConfigs.map { it.setCode },
            setNames = setConfigs.map { it.setName }
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
        val setConfigs = message.setCodes.mapNotNull { BoosterGenerator.getSetConfig(it) }
        if (setConfigs.size != message.setCodes.size) {
            val invalidCodes = message.setCodes.filter { BoosterGenerator.getSetConfig(it) == null }
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
        // Sealed: default 6 boosters, max 12
        val boosterCount = if (format == TournamentFormat.DRAFT) {
            if (message.boosterCount == 6) 3 else message.boosterCount.coerceIn(1, 6)  // 6 is the client default, use 3 for draft
        } else {
            message.boosterCount.coerceIn(1, 12)
        }

        val lobby = TournamentLobby(
            setCodes = setConfigs.map { it.setCode },
            setNames = setConfigs.map { it.setName },
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

        if (lobby.isFull) {
            sender.sendError(session, ErrorCode.GAME_FULL, "Lobby is full")
            return
        }

        if (lobby.state != LobbyState.WAITING_FOR_PLAYERS) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Lobby not accepting players")
            return
        }

        // Leave current lobby if in one
        leaveCurrentLobbyIfPresent(identity)

        lobby.addPlayer(identity)
        logger.info("Player ${identity.playerName} joined lobby ${lobby.lobbyId}")
        broadcastLobbyUpdate(lobby)
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

        // Update sets if provided (can be empty to disable start)
        message.setCodes?.let { newSetCodes ->
            // Allow empty setCodes to disable start button (but won't be able to start)
            if (newSetCodes.isNotEmpty() && !lobby.updateSets(newSetCodes)) {
                val invalidCodes = newSetCodes.filter { BoosterGenerator.getSetConfig(it) == null }
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
            val maxCount = if (lobby.format == TournamentFormat.DRAFT) 6 else 12
            lobby.boosterCount = it.coerceIn(1, maxCount)
        }
        message.maxPlayers?.let { lobby.maxPlayers = it.coerceIn(2, 8) }
        message.gamesPerMatch?.let { lobby.gamesPerMatch = it.coerceIn(1, 5) }
        message.pickTimeSeconds?.let { lobby.pickTimeSeconds = it.coerceIn(15, 180) }
        message.picksPerRound?.let { lobby.picksPerRound = it.coerceIn(1, 2) }

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
            is TournamentLobby.DeckSubmissionResult.Success -> {
                val deckSize = deckList.values.sum()
                logger.info("Player ${identity.playerName} submitted deck ($deckSize cards) in lobby $lobbyId")
                sender.send(session, ServerMessage.DeckSubmitted(deckSize))
                broadcastLobbyUpdate(lobby)

                if (result.allReady) {
                    startTournament(lobby)
                }
            }
            is TournamentLobby.DeckSubmissionResult.Error -> {
                sender.sendError(session, ErrorCode.INVALID_DECK, result.message)
            }
        }
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
            if (spectator.webSocketSession.isOpen) {
                sender.send(spectator.webSocketSession, spectatorState)
            }
        }
    }
}
