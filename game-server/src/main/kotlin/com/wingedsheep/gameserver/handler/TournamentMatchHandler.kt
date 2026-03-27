package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.ai.AiGameManager
import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.gameserver.tournament.TournamentManager
import com.wingedsheep.gameserver.tournament.TournamentMatch
import com.wingedsheep.gameserver.tournament.TournamentRound
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.deck.EasterEggDeckInjector
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

@Component
class TournamentMatchHandler(
    private val ctx: LobbySharedContext,
    private val spectatingHandler: SpectatingHandler,
    private val cardRegistry: CardRegistry,
    private val gamePlayHandler: GamePlayHandler,
    private val gameProperties: GameProperties,
    private val gameRepository: GameRepository,
    private val aiGameManager: AiGameManager
) {
    private val logger = LoggerFactory.getLogger(TournamentMatchHandler::class.java)

    fun handleReadyForNextRound(session: WebSocketSession) {
        val token = ctx.sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { ctx.sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            ctx.sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Not in a lobby")
            return
        }

        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Lobby not found")
            return
        }

        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId)
        if (tournament == null || tournament.isComplete) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Tournament not active")
            return
        }

        // Spectators can't ready up
        if (lobby.isSpectator(identity.playerId)) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Spectators cannot ready up")
            return
        }

        val epochBeforeLock = lobby.readyEpoch

        val lock = ctx.roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            if (lobby.readyEpoch != epochBeforeLock) return
            while (true) {
                val needsPrepare = tournament.currentRound == null ||
                        tournament.currentRound?.isComplete == true
                if (!needsPrepare) break

                val round = tournament.startNextRound()
                if (round == null) {
                    completeTournament(lobbyId)
                    return
                }

                for (match in round.matches) {
                    if (match.isBye && match.isComplete) {
                        val byePlayerState = lobby.players[match.player1Id]
                        val byeWs = byePlayerState?.identity?.webSocketSession
                        if (byeWs != null && byeWs.isOpen) {
                            ctx.sender.send(byeWs, ServerMessage.TournamentBye(
                                lobbyId = lobbyId,
                                round = round.roundNumber
                            ))
                            spectatingHandler.sendActiveMatchesToPlayer(byePlayerState.identity, byeWs)
                        }
                    }
                }

                ctx.lobbyRepository.saveTournament(lobbyId, tournament)
                logger.info("Prepared round ${round.roundNumber} for tournament $lobbyId")
            }

            val wasNewlyReady = lobby.markPlayerReady(identity.playerId)
            if (!wasNewlyReady) {
                return
            }

            logger.info("Player ${identity.playerName} ready for next round in tournament $lobbyId")

            broadcastReadyStatus(lobby, identity)
            ctx.lobbyRepository.saveLobby(lobby)

            tryStartMatchForPlayer(lobby, tournament, identity)
        }
    }

    fun handleMatchResult(
        lobbyId: String,
        gameSessionId: String,
        winnerId: EntityId?,
        winnerLifeRemaining: Int
    ) {
        val lock = ctx.roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return
            tournament.reportMatchResult(gameSessionId, winnerId, winnerLifeRemaining)
            ctx.lobbyRepository.saveTournament(lobbyId, tournament)

            handleMatchComplete(lobbyId, gameSessionId)
            spectatingHandler.broadcastActiveMatchesToWaitingPlayers(lobbyId)

            val lobby = ctx.lobbyRepository.findLobbyById(lobbyId)
            if (lobby != null) {
                autoReadyAiPlayers(lobby, tournament)
                ctx.lobbyRepository.saveLobby(lobby)
            }

            if (tournament.isRoundComplete()) {
                doHandleRoundComplete(lobbyId)
            }
        }
    }

    fun handleAbandon(lobbyId: String, playerId: EntityId) {
        val lock = ctx.roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return
            tournament.recordAbandon(playerId)
            ctx.lobbyRepository.saveTournament(lobbyId, tournament)

            spectatingHandler.broadcastActiveMatchesToWaitingPlayers(lobbyId)

            if (tournament.isRoundComplete()) {
                doHandleRoundComplete(lobbyId)
            }
        }
    }

    fun handleAddExtraRound(session: WebSocketSession) {
        val token = ctx.sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { ctx.sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            ctx.sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Not in a lobby")
            return
        }

        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Lobby not found")
            return
        }

        if (!lobby.isHost(identity.playerId)) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can add extra rounds")
            return
        }

        val lock = ctx.roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            if (lobby.state != com.wingedsheep.gameserver.lobby.LobbyState.TOURNAMENT_COMPLETE) {
                ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Tournament is not complete")
                return
            }

            val tournament = ctx.lobbyRepository.findTournamentById(lobbyId)
            if (tournament == null) {
                ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Tournament not found")
                return
            }

            tournament.addExtraRound()
            lobby.resumeTournament()

            logger.info("Host ${identity.playerName} added extra rounds to tournament $lobbyId (now ${tournament.totalRounds} total)")

            val connectedIds = lobby.players.values
                .filter { it.identity.isConnected }
                .map { it.identity.playerId }
                .toSet()

            val standings = tournament.getStandingsInfo(connectedIds)
            val nextMatchups = tournament.peekNextRoundMatchups()

            lobby.players.forEach { (playerId, playerState) ->
                val ws = playerState.identity.webSocketSession
                if (ws != null && ws.isOpen) {
                    val opponentId = nextMatchups[playerId]
                    val opponentName = opponentId?.let { lobby.players[it]?.identity?.playerName }
                    val hasBye = nextMatchups.containsKey(playerId) && opponentId == null

                    ctx.sender.send(ws, ServerMessage.TournamentResumed(
                        lobbyId = lobbyId,
                        totalRounds = tournament.totalRounds,
                        standings = standings,
                        nextOpponentName = opponentName,
                        nextRoundHasBye = hasBye
                    ))
                }
            }

            lobby.spectators.forEach { (_, spectatorIdentity) ->
                val ws = spectatorIdentity.webSocketSession
                if (ws != null && ws.isOpen) {
                    ctx.sender.send(ws, ServerMessage.TournamentResumed(
                        lobbyId = lobbyId,
                        totalRounds = tournament.totalRounds,
                        standings = standings
                    ))
                }
            }

            autoReadyAiPlayers(lobby, tournament)

            ctx.lobbyRepository.saveLobby(lobby)
            ctx.lobbyRepository.saveTournament(lobbyId, tournament)
        }
    }

    fun handleRoundComplete(lobbyId: String) {
        val lock = ctx.roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            doHandleRoundComplete(lobbyId)
        }
    }

    private fun doHandleRoundComplete(lobbyId: String) {
        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return
        val round = tournament.currentRound ?: return

        logger.info("Round ${round.roundNumber} complete for tournament $lobbyId")

        lobby.clearReadyState()

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        lobby.players.forEach { (playerId, playerState) ->
            playerState.identity.currentGameSessionId = null

            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                val nextMatch = tournament.getNextMatchForPlayer(playerId)
                val nextOpponentName: String?
                val hasBye: Boolean

                if (nextMatch != null) {
                    val (nextRound, nm) = nextMatch
                    val isNextRound = nextRound.roundNumber == round.roundNumber + 1
                    val opponentId = if (nm.player1Id == playerId) nm.player2Id else nm.player1Id
                    nextOpponentName = if (isNextRound && !nm.isBye) opponentId?.let { lobby.players[it]?.identity?.playerName } else null
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
                ctx.sender.send(ws, roundComplete)
            }
        }

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
            ctx.sender.send(ws, roundComplete)
        }

        ctx.lobbyRepository.saveLobby(lobby)
        ctx.lobbyRepository.saveTournament(lobbyId, tournament)

        if (tournament.isComplete) {
            completeTournament(lobbyId)
        }
    }

    private fun handleMatchComplete(lobbyId: String, gameSessionId: String) {
        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return

        val completedRound = tournament.getRoundForMatch(gameSessionId) ?: return

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        val match = completedRound.matches.find { it.gameSessionId == gameSessionId } ?: return
        val matchPlayerIds = listOfNotNull(match.player1Id, match.player2Id)

        for (playerId in matchPlayerIds) {
            val playerState = lobby.players[playerId] ?: continue
            val ws = playerState.identity.webSocketSession ?: continue
            if (!ws.isOpen) continue

            val nextMatch = tournament.getNextMatchForPlayer(playerId)
            val nextOpponentName: String?
            val hasBye: Boolean

            if (nextMatch != null) {
                val (nextRound, nm) = nextMatch
                val isCurrentRound = nextRound.roundNumber == (tournament.currentRound?.roundNumber ?: -1)
                val opponentId = if (nm.player1Id == playerId) nm.player2Id else nm.player1Id
                nextOpponentName = if (isCurrentRound && !nm.isBye) opponentId?.let { lobby.players[it]?.identity?.playerName } else null
                hasBye = nm.isBye
            } else {
                nextOpponentName = null
                hasBye = false
            }

            ctx.sender.send(ws, ServerMessage.MatchComplete(
                lobbyId = lobbyId,
                round = completedRound.roundNumber,
                results = tournament.getRoundResults(completedRound),
                standings = tournament.getStandingsInfo(connectedIds),
                nextOpponentName = nextOpponentName,
                nextRoundHasBye = hasBye,
                isTournamentComplete = tournament.isComplete
            ))
        }

        ctx.lobbyRepository.saveTournament(lobbyId, tournament)
    }

    fun tryStartMatchForPlayer(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        identity: PlayerIdentity
    ) {
        val (round, match) = tournament.getNextMatchForPlayer(identity.playerId) ?: return

        if (match.isBye) {
            match.isComplete = true
            val ws = identity.webSocketSession
            if (ws != null && ws.isOpen) {
                ctx.sender.send(ws, ServerMessage.TournamentBye(
                    lobbyId = lobby.lobbyId,
                    round = round.roundNumber
                ))
                spectatingHandler.sendActiveMatchesToPlayer(identity, ws)
            }
            ctx.lobbyRepository.saveTournament(lobby.lobbyId, tournament)
            tryStartMatchForPlayer(lobby, tournament, identity)
            return
        }

        val opponentId = if (match.player1Id == identity.playerId) match.player2Id else match.player1Id
        if (opponentId == null) return

        if (opponentId !in lobby.getReadyPlayerIds()) return

        if (tournament.hasIncompleteMatchBefore(opponentId, round.roundNumber)) return

        logger.info("Both players ready, starting match: ${identity.playerName} vs ${lobby.players[opponentId]?.identity?.playerName}")
        startSingleMatch(lobby, tournament, round, match)

        lobby.clearPlayerReady(identity.playerId)
        lobby.clearPlayerReady(opponentId)
    }

    fun startSingleMatch(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        round: TournamentRound,
        match: TournamentMatch
    ) {
        val player1State = lobby.players[match.player1Id] ?: return
        val player2State = lobby.players[match.player2Id ?: return] ?: return

        val deck1 = EasterEggDeckInjector.maybeInjectEasterEggs(
            player1State.identity.playerName,
            lobby.getSubmittedDeck(match.player1Id) ?: return
        )
        val deck2 = EasterEggDeckInjector.maybeInjectEasterEggs(
            player2State.identity.playerName,
            lobby.getSubmittedDeck(match.player2Id) ?: return
        )

        val gameSession = GameSession(
            cardRegistry = cardRegistry,
            useHandSmoother = gameProperties.handSmoother.enabled
        )
        val ps1 = player1State.identity.toPlayerSession()
        val ps2 = player2State.identity.toPlayerSession()

        gameSession.addPlayer(ps1, deck1)
        gameSession.addPlayer(ps2, deck2)

        gameSession.setPlayerPersistenceInfo(ps1.playerId, ps1.playerName, player1State.identity.token)
        gameSession.setPlayerPersistenceInfo(ps2.playerId, ps2.playerName, player2State.identity.token)

        gameRepository.save(gameSession)
        gameRepository.linkToLobby(gameSession.sessionId, lobby.lobbyId)
        match.gameSessionId = gameSession.sessionId
        ctx.lobbyRepository.saveTournament(lobby.lobbyId, tournament)

        ctx.cleanUpSpectatingState(player1State.identity)
        ctx.cleanUpSpectatingState(player2State.identity)

        player1State.identity.currentGameSessionId = gameSession.sessionId
        player2State.identity.currentGameSessionId = gameSession.sessionId

        val ws1 = player1State.identity.webSocketSession
        val ws2 = player2State.identity.webSocketSession
        if (ws1 != null) {
            ctx.sessionRegistry.getPlayerSession(ws1.id)?.currentGameSessionId = gameSession.sessionId
        }
        if (ws2 != null) {
            ctx.sessionRegistry.getPlayerSession(ws2.id)?.currentGameSessionId = gameSession.sessionId
        }

        if (ws1 != null && ws1.isOpen) {
            ctx.sender.send(ws1, ServerMessage.TournamentMatchStarting(
                lobbyId = lobby.lobbyId,
                round = round.roundNumber,
                gameSessionId = gameSession.sessionId,
                opponentName = player2State.identity.playerName
            ))
        }
        if (ws2 != null && ws2.isOpen) {
            ctx.sender.send(ws2, ServerMessage.TournamentMatchStarting(
                lobbyId = lobby.lobbyId,
                round = round.roundNumber,
                gameSessionId = gameSession.sessionId,
                opponentName = player1State.identity.playerName
            ))
        }

        for (ps in listOf(ps1, ps2)) {
            if (aiGameManager.isAiPlayer(ps.playerId)) {
                aiGameManager.wireAiForGame(
                    gameSession = gameSession,
                    aiPlayerId = ps.playerId,
                    deckList = lobby.getSubmittedDeck(ps.playerId),
                    onActionReady = { aiPlayerId, action ->
                        gamePlayHandler.handleAiAction(gameSession, aiPlayerId, action)
                    },
                    onMulliganKeep = { aiPlayerId ->
                        gamePlayHandler.handleAiMulliganKeep(gameSession, aiPlayerId)
                    },
                    onMulliganTake = { aiPlayerId ->
                        gamePlayHandler.handleAiMulliganTake(gameSession, aiPlayerId)
                    },
                    onBottomCards = { aiPlayerId, cardIds ->
                        gamePlayHandler.handleAiBottomCards(gameSession, aiPlayerId, cardIds)
                    }
                )
                val aiIdentity = lobby.players[ps.playerId]?.identity
                if (aiIdentity != null) {
                    val newWs = aiIdentity.webSocketSession
                    if (newWs != null) {
                        val newPs = PlayerSession(
                            webSocketSession = newWs,
                            playerId = ps.playerId,
                            playerName = ps.playerName,
                            currentGameSessionId = gameSession.sessionId
                        )
                        gameSession.replacePlayerSession(ps.playerId, newPs)
                    }
                }
            }
        }

        gamePlayHandler.startGame(gameSession)

        spectatingHandler.broadcastActiveMatchesToWaitingPlayers(lobby.lobbyId)
    }

    fun ensureTournamentCreated(lobby: TournamentLobby): TournamentManager {
        val lock = ctx.roundLocks.computeIfAbsent(lobby.lobbyId) { Any() }
        return synchronized(lock) {
            val existing = ctx.lobbyRepository.findTournamentById(lobby.lobbyId)
            if (existing != null) return@synchronized existing

            logger.info("Creating tournament for lobby ${lobby.lobbyId} with ${lobby.playerCount} players (early creation for matchups)")

            val players = lobby.players.values
                .map { ps -> ps.identity.playerId to ps.identity.playerName }
                .sortedBy { it.first.value }

            val tournament = TournamentManager(lobby.lobbyId, players, lobby.gamesPerMatch)
            ctx.lobbyRepository.saveTournament(lobby.lobbyId, tournament)

            tournament
        }
    }

    fun sendTournamentStartedToPlayer(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        identity: PlayerIdentity,
        wsOverride: WebSocketSession? = null
    ) {
        val ws = wsOverride ?: identity.webSocketSession ?: return

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        val nextMatch = tournament.getNextMatchForPlayer(identity.playerId)
        val nextOpponentName: String?
        val hasBye: Boolean

        if (nextMatch != null) {
            val (nextRound, match) = nextMatch
            val isCurrentRound = tournament.currentRound?.let { nextRound.roundNumber == it.roundNumber } ?: true
            val opponentId = if (match.player1Id == identity.playerId) match.player2Id else match.player1Id
            nextOpponentName = if (isCurrentRound && !match.isBye) opponentId?.let { lobby.players[it]?.identity?.playerName } else null
            hasBye = match.isBye
        } else {
            val firstRoundMatchups = tournament.peekNextRoundMatchups()
            val nextOpponentId = firstRoundMatchups[identity.playerId]
            nextOpponentName = nextOpponentId?.let { lobby.players[it]?.identity?.playerName }
            hasBye = firstRoundMatchups.containsKey(identity.playerId) && nextOpponentId == null
        }

        ctx.sender.send(ws, ServerMessage.TournamentStarted(
            lobbyId = lobby.lobbyId,
            totalRounds = tournament.totalRounds,
            standings = tournament.getStandingsInfo(connectedIds),
            nextOpponentName = nextOpponentName,
            nextRoundHasBye = hasBye
        ))

        val readyPlayerIds = lobby.getReadyPlayerIds()
        if (readyPlayerIds.isNotEmpty()) {
            ctx.sender.send(ws, ServerMessage.PlayerReadyForRound(
                lobbyId = lobby.lobbyId,
                playerId = identity.playerId.value,
                playerName = identity.playerName,
                readyPlayerIds = readyPlayerIds.map { it.value },
                totalConnectedPlayers = connectedIds.size
            ))
        }
    }

    fun tryStartMatchAfterDeckSubmit(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        identity: PlayerIdentity
    ) {
        if (tournament.currentRound == null) {
            val round = tournament.startNextRound()
            if (round == null) {
                completeTournament(lobby.lobbyId)
                return
            }
            logger.info("Prepared round ${round.roundNumber} for tournament ${lobby.lobbyId}")
        }

        val (round, match) = tournament.getNextMatchForPlayer(identity.playerId) ?: return

        if (match.isBye) {
            match.isComplete = true
            val ws = identity.webSocketSession
            if (ws != null && ws.isOpen) {
                ctx.sender.send(ws, ServerMessage.TournamentBye(
                    lobbyId = lobby.lobbyId,
                    round = round.roundNumber
                ))
                spectatingHandler.sendActiveMatchesToPlayer(identity, ws)
            }
            ctx.lobbyRepository.saveTournament(lobby.lobbyId, tournament)
            return
        }

        if (match.gameSessionId != null) return

        val opponentId = if (match.player1Id == identity.playerId) match.player2Id else match.player1Id
        if (opponentId == null) return

        val opponentState = lobby.players[opponentId] ?: return
        if (!opponentState.hasSubmittedDeck) return

        logger.info("Both players submitted decks, starting match: ${identity.playerName} vs ${opponentState.identity.playerName}")
        startSingleMatch(lobby, tournament, round, match)
    }

    fun startTournament(lobby: TournamentLobby) {
        logger.info("Starting tournament for lobby ${lobby.lobbyId} with ${lobby.playerCount} players")
        lobby.startTournament()

        val players = lobby.players.values
            .map { ps -> ps.identity.playerId to ps.identity.playerName }
            .sortedBy { it.first.value }

        val tournament = TournamentManager(lobby.lobbyId, players, lobby.gamesPerMatch)
        ctx.lobbyRepository.saveTournament(lobby.lobbyId, tournament)

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        val firstRoundMatchups = tournament.peekNextRoundMatchups()

        lobby.players.forEach { (playerId, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                val nextOpponentId = firstRoundMatchups[playerId]
                val nextOpponentName = if (nextOpponentId != null) {
                    lobby.players[nextOpponentId]?.identity?.playerName
                } else {
                    null
                }
                val hasBye = firstRoundMatchups.containsKey(playerId) && nextOpponentId == null

                ctx.sender.send(ws, ServerMessage.TournamentStarted(
                    lobbyId = lobby.lobbyId,
                    totalRounds = tournament.totalRounds,
                    standings = tournament.getStandingsInfo(connectedIds),
                    nextOpponentName = nextOpponentName,
                    nextRoundHasBye = hasBye
                ))
            }
        }
    }

    fun startNextTournamentRound(lobbyId: String) {
        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return

        if (tournament.currentRound?.isComplete != false) {
            val round = tournament.startNextRound()
            if (round == null) {
                completeTournament(lobbyId)
                return
            }
            lobby.clearReadyState()
            ctx.lobbyRepository.saveLobby(lobby)
            ctx.lobbyRepository.saveTournament(lobbyId, tournament)
        }

        val round = tournament.currentRound ?: return
        logger.info("Starting round ${round.roundNumber} for tournament $lobbyId")

        for (match in tournament.getCurrentRoundGameMatches()) {
            if (match.gameSessionId == null) {
                startSingleMatch(lobby, tournament, round, match)
            }
        }
        ctx.lobbyRepository.saveTournament(lobbyId, tournament)

        for (match in round.matches) {
            if (match.isBye) {
                val playerState = lobby.players[match.player1Id]
                val identity = playerState?.identity
                val ws = identity?.webSocketSession
                if (identity != null && ws != null && ws.isOpen) {
                    ctx.sender.send(ws, ServerMessage.TournamentBye(
                        lobbyId = lobbyId,
                        round = round.roundNumber
                    ))
                    spectatingHandler.sendActiveMatchesToPlayer(identity, ws)
                }
            }
        }
    }

    fun autoReadyAiPlayers(lobby: TournamentLobby, tournament: TournamentManager) {
        for ((playerId, playerState) in lobby.players) {
            if (!aiGameManager.isAiPlayer(playerId)) continue

            val wasNewlyReady = lobby.markPlayerReady(playerId)
            if (wasNewlyReady) {
                logger.info("AI ${playerState.identity.playerName} auto-ready for next round")
                tryStartMatchForPlayer(lobby, tournament, playerState.identity)
            }
        }
    }

    fun broadcastReadyStatus(lobby: TournamentLobby, identity: PlayerIdentity) {
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
                ctx.sender.send(ws, readyMessage)
            }
        }
    }

    fun completeTournament(lobbyId: String) {
        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return

        logger.info("Tournament complete for lobby $lobbyId")
        lobby.completeTournament()
        ctx.lobbyRepository.saveLobby(lobby)
        ctx.lobbyRepository.saveTournament(lobbyId, tournament)

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
                ctx.sender.send(ws, message)
            }
        }

        lobby.spectators.forEach { (_, spectatorIdentity) ->
            val ws = spectatorIdentity.webSocketSession
            if (ws != null && ws.isOpen) {
                ctx.sender.send(ws, message)
            }
        }
    }
}
