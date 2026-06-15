package com.wingedsheep.gameserver.handler

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.gameserver.deck.EasterEggDeckInjector
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

/**
 * Free-for-All lobby mode (multiplayer.md Phase 4): instead of pairing lobby players into
 * 2-player bracket matches, one N-player [GameSession] seats every lobby player (2-6). The
 * format axis (sealed / draft / premade decks) is untouched — this handler only replaces the
 * "what happens once decks are in" half that [TournamentMatchHandler] covers for bracket mode.
 *
 * Lifecycle: all decks submitted → [maybeStartGame] seats everyone in one session → game runs to
 * completion (mid-game eliminations continue the game, CR 800.4a) → [handleGameComplete] reports
 * standings as the elimination order → players ready up ([handleReadyForNextGame]) → a new game
 * starts with the same pod ("play again"). No [com.wingedsheep.gameserver.tournament.TournamentManager]
 * is ever created for an FFA lobby.
 */
@Component
class FreeForAllHandler(
    private val ctx: LobbySharedContext,
    private val cardRegistry: CardRegistry,
    private val printingRegistry: com.wingedsheep.engine.registry.PrintingRegistry,
    private val gamePlayHandler: GamePlayHandler,
    private val gameProperties: GameProperties,
    private val gameRepository: GameRepository,
) {
    private val logger = LoggerFactory.getLogger(FreeForAllHandler::class.java)

    /**
     * Start one Free-for-All game seating every lobby player, if the pod is ready: no game
     * already running, and every player has a submitted deck. Returns true if a game started.
     * Callers must hold the lobby's round lock.
     */
    fun maybeStartGame(lobby: TournamentLobby): Boolean {
        if (!lobby.isFreeForAll) return false
        if (lobby.ffaGameSessionId != null) return false
        if (lobby.playerCount < 2) return false
        // Two-Headed Giant needs a full pod — exactly four players for two teams of two (CR 810).
        if (lobby.isTwoHeadedGiant && lobby.playerCount != 4) return false
        if (!lobby.allDecksSubmitted()) return false

        val playerStates = lobby.players.values.toList()

        // Commander-shape lobbies (premade Commander/Brawl decks) run under the engine's
        // Commander rules; the wire deck list counts the commander, the engine's library
        // excludes it. Mirrors TournamentMatchHandler.startSingleMatch.
        val isCommanderShape = (lobby.format == com.wingedsheep.gameserver.lobby.TournamentFormat.PREMADE_DECKS &&
            lobby.deckFormat?.isCommanderShape == true) || lobby.format.isCommanderFormat
        if (isCommanderShape) {
            val missing = playerStates.filter { it.commander == null }
            if (missing.isNotEmpty()) {
                logger.warn(
                    "FFA lobby ${lobby.lobbyId}: cannot start commander-shape game — missing commander for " +
                        missing.joinToString(", ") { it.identity.playerName }
                )
                return false
            }
        }

        val gameSession = GameSession(
            cardRegistry = cardRegistry,
            useHandSmoother = gameProperties.handSmoother.enabled,
            debugMode = gameProperties.debugMode,
            printingRegistry = printingRegistry,
            maxPlayers = playerStates.size,
        )
        if (isCommanderShape) {
            gameSession.engineFormat = if (lobby.format.isCommanderFormat) {
                lobby.commanderPreset.toFormat().copy(deckSize = lobby.deckSizeMin)
            } else {
                com.wingedsheep.sdk.core.Format.Commander()
            }
        }
        // CR 802 / 803 — the lobby's chosen attack rule applies to this multiplayer game.
        gameSession.attackMode = lobby.attackMode

        // Two-Headed Giant (CR 810): run under the team format and split the four seats into two
        // teams of two — random by default (re-rolled each game), or the host's manual assignment.
        // Team indices reference the add-player order below (same order used for the partition);
        // GameInitializer seats teammates adjacently (CR 805.1). GameSession.teams flows into
        // GameConfig.teams, which stamps a TeamComponent on each seat — the engine then shares
        // life/turns/combat per team.
        if (lobby.isTwoHeadedGiant) {
            gameSession.engineFormat = com.wingedsheep.sdk.core.Format.TwoHeadedGiant()
            gameSession.teams = com.wingedsheep.gameserver.lobby.TwoHeadedGiantTeams.partition(
                orderedPlayerIds = playerStates.map { it.identity.playerId },
                randomTeams = lobby.randomTeams,
                manualAssignment = lobby.teamAssignments,
            )
        }

        for (playerState in playerStates) {
            val identity = playerState.identity
            val baseDeck = BoosterGenerator.distributeBasicLandVariants(
                lobby.getSubmittedDeck(identity.playerId) ?: return false,
                lobby.allBasicLandVariants
            )
            val deckWithEgg = EasterEggDeckInjector.maybeInjectEasterEggs(identity.playerName, baseDeck)
            val commander = if (isCommanderShape) playerState.commander else null
            val deck = if (commander != null) stripCommanderFromCards(deckWithEgg, commander) else deckWithEgg

            val playerSession = identity.toPlayerSession()
            gameSession.addPlayer(playerSession, deck, commanderCardName = commander)
            gameSession.setPlayerPersistenceInfo(
                playerSession.playerId, playerSession.playerName, identity.token,
                isAi = identity.isAi, aiModelOverride = identity.aiModelOverride
            )
        }

        gameSession.publicSpectate = lobby.isPublic
        gameRepository.save(gameSession)
        gameRepository.linkToLobby(gameSession.sessionId, lobby.lobbyId)
        lobby.ffaGameSessionId = gameSession.sessionId
        lobby.clearReadyState()

        val gameNumber = lobby.ffaGamesPlayed + 1
        logger.info(
            "Starting FFA game $gameNumber for lobby ${lobby.lobbyId} " +
                "(${playerStates.size} players: ${playerStates.joinToString(", ") { it.identity.playerName }})"
        )

        for (playerState in playerStates) {
            val identity = playerState.identity
            ctx.cleanUpSpectatingState(identity)
            identity.currentGameSessionId = gameSession.sessionId
            val ws = identity.webSocketSession
            if (ws != null) {
                ctx.sessionRegistry.getPlayerSession(ws.id)?.currentGameSessionId = gameSession.sessionId
                if (ws.isOpen) {
                    ctx.sender.send(ws, ServerMessage.FreeForAllGameStarting(
                        lobbyId = lobby.lobbyId,
                        gameSessionId = gameSession.sessionId,
                        gameNumber = gameNumber,
                        players = gameSession.seatInfos(identity.playerId),
                    ))
                }
            }
        }
        for ((_, spectatorIdentity) in lobby.spectators) {
            val ws = spectatorIdentity.webSocketSession
            if (ws != null && ws.isOpen) {
                ctx.sender.send(ws, ServerMessage.FreeForAllGameStarting(
                    lobbyId = lobby.lobbyId,
                    gameSessionId = gameSession.sessionId,
                    gameNumber = gameNumber,
                    players = gameSession.seatInfos(),
                ))
            }
        }

        gamePlayHandler.startGame(gameSession)
        ctx.lobbyRepository.saveLobby(lobby)
        return true
    }

    /**
     * The FFA game finished. Standings = elimination order, winner first ([GameSession.getEliminationOrder]
     * reversed; a player never eliminated but not the winner — a simultaneous-loss draw — slots in
     * after the winner). Broadcasts [ServerMessage.FreeForAllGameComplete]; the lobby stays
     * TOURNAMENT_ACTIVE so the pod can ready up and play again.
     *
     * Invoked from [GamePlayHandler.handleGameOver] via the match-result callback, which has
     * already cleared the players' `currentGameSessionId` and looked up the lobby link.
     */
    fun handleGameComplete(lobbyId: String, gameSessionId: String, winnerId: EntityId?) {
        val lock = ctx.roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            val lobby = ctx.lobbyRepository.findLobbyById(lobbyId) ?: return
            if (lobby.ffaGameSessionId != gameSessionId) return

            val gameSession = gameRepository.findById(gameSessionId)
            val standings = buildStandings(lobby, gameSession, winnerId)

            lobby.ffaGameSessionId = null
            lobby.ffaGamesPlayed += 1
            lobby.ffaLastStandings = standings
            lobby.clearReadyState()
            ctx.lobbyRepository.saveLobby(lobby)

            logger.info(
                "FFA game $gameSessionId complete for lobby $lobbyId: " +
                    standings.joinToString(", ") { "${it.placement}. ${it.playerName}" }
            )

            val message = ServerMessage.FreeForAllGameComplete(
                lobbyId = lobbyId,
                standings = standings,
                gamesPlayed = lobby.ffaGamesPlayed,
            )
            broadcastToLobby(lobby, message)
        }
    }

    /**
     * "Play again": mark the player ready; when every connected player is ready (and all decks
     * are still submitted) a new game starts with the same pod.
     */
    fun handleReadyForNextGame(session: WebSocketSession, identity: PlayerIdentity, lobby: TournamentLobby) {
        if (lobby.isSpectator(identity.playerId)) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Spectators cannot ready up")
            return
        }
        val lock = ctx.roundLocks.computeIfAbsent(lobby.lobbyId) { Any() }
        synchronized(lock) {
            if (lobby.ffaGameSessionId != null) {
                ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "A game is already in progress")
                return
            }
            if (!lobby.markPlayerReady(identity.playerId)) return

            logger.info("Player ${identity.playerName} ready for next FFA game in lobby ${lobby.lobbyId}")
            broadcastReadyStatus(lobby, identity)
            ctx.lobbyRepository.saveLobby(lobby)

            if (lobby.areAllPlayersReady()) {
                maybeStartGame(lobby)
            }
        }
    }

    /**
     * A player permanently left the lobby (explicit leave or disconnect-abandon). If they are
     * seated in the running FFA game, concede their seat — the game continues for the rest
     * (CR 800.4a); in a 2-player pod this ends it (the degenerate case).
     */
    fun handlePlayerLeft(lobby: TournamentLobby, playerId: EntityId) {
        val gameSessionId = lobby.ffaGameSessionId ?: return
        val gameSession = gameRepository.findById(gameSessionId) ?: return
        if (gameSession.getPlayerSession(playerId) == null) return
        if (gameSession.isGameOver()) return

        logger.info("FFA lobby ${lobby.lobbyId}: conceding departed player $playerId from game $gameSessionId")
        gameSession.playerConcedes(playerId)
        if (gameSession.isGameOver()) {
            gamePlayHandler.handleGameOver(gameSession, com.wingedsheep.gameserver.protocol.GameOverReason.CONCESSION)
        } else {
            gamePlayHandler.broadcastStateUpdate(gameSession, emptyList())
        }
    }

    /**
     * Restore FFA lobby state for a player reconnecting while the lobby is TOURNAMENT_ACTIVE —
     * the FFA-mode counterpart of LobbyHandler.sendTournamentActiveState. Rejoins a running game
     * if one exists; otherwise re-sends the latest standings + ready status.
     */
    fun sendReconnectionState(
        session: WebSocketSession,
        identity: PlayerIdentity,
        playerSession: PlayerSession?,
        lobby: TournamentLobby,
    ) {
        val gameSessionId = lobby.ffaGameSessionId
        val gameSession = gameSessionId?.let { gameRepository.findById(it) }

        if (gameSession != null && gameSession.isStarted && !gameSession.isGameOver() &&
            gameSession.getPlayerSession(identity.playerId) != null && playerSession != null
        ) {
            identity.currentGameSessionId = gameSessionId
            playerSession.currentGameSessionId = gameSessionId
            ctx.sender.send(session, ServerMessage.FreeForAllGameStarting(
                lobbyId = lobby.lobbyId,
                gameSessionId = gameSessionId,
                gameNumber = lobby.ffaGamesPlayed + 1,
                players = gameSession.seatInfos(identity.playerId),
            ))
            if (gameSession.getPlayerSession(identity.playerId) != null) {
                gameSession.removePlayer(identity.playerId)
            }
            gameSession.associatePlayer(playerSession)
            when {
                gameSession.isAwaitingBottomCards(identity.playerId) -> {
                    val hand = gameSession.getHand(identity.playerId)
                    val cardsToBottom = gameSession.getCardsToBottom(identity.playerId)
                    ctx.sender.send(session, ServerMessage.ChooseBottomCards(hand, cardsToBottom))
                }
                gameSession.isMulliganPhase && !gameSession.hasMulliganComplete(identity.playerId) -> {
                    ctx.sender.send(session, gameSession.getMulliganDecision(identity.playerId))
                }
                gameSession.isMulliganPhase -> {
                    ctx.sender.send(session, ServerMessage.WaitingForOpponentMulligan)
                }
                else -> {
                    gameSession.clearLastSentState(identity.playerId)
                    gamePlayHandler.broadcastStateUpdate(gameSession, emptyList())
                }
            }
            return
        }

        // Between games: re-send last standings (if any) and the current ready roster.
        val standings = lobby.ffaLastStandings
        if (standings != null) {
            ctx.sender.send(session, ServerMessage.FreeForAllGameComplete(
                lobbyId = lobby.lobbyId,
                standings = standings,
                gamesPlayed = lobby.ffaGamesPlayed,
            ))
        }
        val readyPlayerIds = lobby.getReadyPlayerIds()
        if (readyPlayerIds.isNotEmpty()) {
            val connectedCount = lobby.players.values.count { it.identity.isConnected }
            ctx.sender.send(session, ServerMessage.PlayerReadyForRound(
                lobbyId = lobby.lobbyId,
                playerId = identity.playerId.value,
                playerName = identity.playerName,
                readyPlayerIds = readyPlayerIds.map { it.value },
                totalConnectedPlayers = connectedCount,
            ))
        }
    }

    private fun broadcastReadyStatus(lobby: TournamentLobby, identity: PlayerIdentity) {
        val connectedPlayers = lobby.players.values.filter { it.identity.isConnected }
        val message = ServerMessage.PlayerReadyForRound(
            lobbyId = lobby.lobbyId,
            playerId = identity.playerId.value,
            playerName = identity.playerName,
            readyPlayerIds = lobby.getReadyPlayerIds().map { it.value },
            totalConnectedPlayers = connectedPlayers.size,
        )
        broadcastToLobby(lobby, message)
    }

    private fun broadcastToLobby(lobby: TournamentLobby, message: ServerMessage) {
        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) ctx.sender.send(ws, message)
        }
        lobby.spectators.forEach { (_, spectatorIdentity) ->
            val ws = spectatorIdentity.webSocketSession
            if (ws != null && ws.isOpen) ctx.sender.send(ws, message)
        }
    }

    /**
     * Placement order: winner first, then any never-eliminated non-winners (simultaneous-loss
     * draws), then the eliminated players latest-first. Falls back to lobby seat order if the
     * session is already gone (defensive — the result callback runs before session removal).
     */
    private fun buildStandings(
        lobby: TournamentLobby,
        gameSession: GameSession?,
        winnerId: EntityId?,
    ): List<ServerMessage.FfaStandingInfo> {
        val seatedIds = gameSession?.getPlayers()?.map { it.playerId }
            ?: lobby.players.keys.toList()
        val eliminated = gameSession?.getEliminationOrder() ?: emptyList()

        val placementOrder = buildList {
            if (winnerId != null) add(winnerId)
            addAll(seatedIds.filter { it != winnerId && it !in eliminated })
            addAll(eliminated.reversed().filter { it != winnerId })
        }

        return placementOrder.mapIndexed { index, playerId ->
            val identity = lobby.players[playerId]?.identity
            ServerMessage.FfaStandingInfo(
                playerId = playerId.value,
                playerName = identity?.playerName
                    ?: gameSession?.getPlayerSession(playerId)?.playerName
                    ?: "Unknown",
                placement = index + 1,
                isConnected = identity?.isConnected ?: false,
            )
        }
    }

    /** Mirrors TournamentMatchHandler.stripCommanderFromCards — wire decks count the commander, the library doesn't. */
    private fun stripCommanderFromCards(deckList: Map<String, Int>, commander: String): Map<String, Int> {
        val current = deckList[commander] ?: return deckList
        val next = deckList.toMutableMap()
        if (current <= 1) next.remove(commander) else next[commander] = current - 1
        return next
    }
}
