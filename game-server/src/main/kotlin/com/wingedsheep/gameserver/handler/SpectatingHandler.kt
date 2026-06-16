package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.gameserver.tournament.TournamentManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

@Component
class SpectatingHandler(
    private val ctx: LobbySharedContext
) {
    private val logger = LoggerFactory.getLogger(SpectatingHandler::class.java)

    fun handleSpectateGame(session: WebSocketSession, message: ClientMessage.SpectateGame) {
        val token = ctx.sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { ctx.sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            ctx.sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val playerSession = ctx.sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            ctx.sender.sendError(session, ErrorCode.NOT_CONNECTED, "Player session not found")
            return
        }

        val gameSession = ctx.gameRepository.findById(message.gameSessionId)
        if (gameSession == null) {
            ctx.sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Game not found")
            return
        }

        gameSession.addSpectator(playerSession)
        identity.currentSpectatingGameId = message.gameSessionId
        ctx.broadcastSpectatorCount(gameSession)

        val playerNames = gameSession.getPlayerNames()
        if (playerNames.size >= 2) {
            ctx.sender.send(session, ServerMessage.SpectatingStarted(
                gameSessionId = message.gameSessionId,
                player1Name = playerNames[0],
                player2Name = playerNames[1]
            ))
        }

        val spectatorState = gameSession.buildSpectatorState()
        if (spectatorState != null) {
            ctx.sender.send(session, spectatorState)
        }

        logger.info("Player ${identity.playerName} started spectating game ${message.gameSessionId}")
    }

    fun handleStopSpectating(session: WebSocketSession) {
        val token = ctx.sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { ctx.sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            ctx.sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val playerSession = ctx.sessionRegistry.getPlayerSession(session.id)
        if (playerSession == null) {
            return
        }

        val gameSessionId = identity.currentSpectatingGameId
        if (gameSessionId != null) {
            val gameSession = ctx.gameRepository.findById(gameSessionId)
            gameSession?.removeSpectator(playerSession)
            if (gameSession != null) {
                ctx.broadcastSpectatorCount(gameSession)
            }
            identity.currentSpectatingGameId = null

            logger.info("Player ${identity.playerName} stopped spectating game $gameSessionId")
        }

        ctx.sender.send(session, ServerMessage.SpectatingStopped)

        sendActiveMatchesToPlayer(identity, session)
    }

    fun restoreSpectating(
        identity: PlayerIdentity,
        playerSession: PlayerSession,
        session: WebSocketSession,
        gameSessionId: String
    ) {
        val gameSession = ctx.gameRepository.findById(gameSessionId)
        if (gameSession == null || gameSession.isGameOver()) {
            identity.currentSpectatingGameId = null
            sendActiveMatchesToPlayer(identity, session)
            return
        }

        gameSession.addSpectator(playerSession)
        ctx.broadcastSpectatorCount(gameSession)

        val playerNames = gameSession.getPlayerNames()
        if (playerNames.size >= 2) {
            ctx.sender.send(session, ServerMessage.SpectatingStarted(
                gameSessionId = gameSessionId,
                player1Name = playerNames[0],
                player2Name = playerNames[1]
            ))
        }

        val spectatorState = gameSession.buildSpectatorState()
        if (spectatorState != null) {
            ctx.sender.send(session, spectatorState)
        }

        logger.info("Restored spectating for ${identity.playerName} to game $gameSessionId")
    }

    fun broadcastSpectatorUpdate(gameSession: GameSession) {
        val spectatorState = gameSession.buildSpectatorState() ?: return

        for (spectator in gameSession.getSpectators()) {
            if (spectator.webSocketSession.isOpen) {
                ctx.sender.send(spectator.webSocketSession, spectatorState)
            }
        }
    }

    fun sendActiveMatchesToPlayer(identity: PlayerIdentity, session: WebSocketSession) {
        val lobbyId = identity.currentLobbyId ?: return
        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        val activeMatches = buildActiveMatchesList(tournament)

        ctx.sender.send(session, ServerMessage.ActiveMatches(
            lobbyId = lobbyId,
            round = tournament.currentRound?.roundNumber ?: 0,
            matches = activeMatches,
            standings = tournament.getStandingsInfo(connectedIds)
        ))
    }

    fun broadcastActiveMatchesToWaitingPlayers(lobbyId: String) {
        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return

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

        for ((playerId, playerState) in lobby.players) {
            val identity = playerState.identity
            val ws = identity.webSocketSession ?: continue
            if (!ws.isOpen) continue

            if (identity.currentGameSessionId != null) continue
            if (identity.currentSpectatingGameId != null) continue

            ctx.sender.send(ws, message)
        }

        for ((_, spectatorIdentity) in lobby.spectators) {
            val ws = spectatorIdentity.webSocketSession ?: continue
            if (!ws.isOpen) continue
            if (spectatorIdentity.currentSpectatingGameId != null) continue
            ctx.sender.send(ws, message)
        }
    }

    private fun buildActiveMatchesList(tournament: TournamentManager): List<ServerMessage.ActiveMatchInfo> {
        val matches = tournament.getAllInProgressMatches()
        return matches.mapNotNull { match ->
            val gameSessionId = match.gameSessionId ?: return@mapNotNull null
            val gameSession = ctx.gameRepository.findById(gameSessionId) ?: return@mapNotNull null

            // ActiveMatchInfo is the tournament overview tile — tournament matches are 2-player.
            val playerNames = gameSession.getPlayerNames()
            val lifeTotals = gameSession.getLifeTotals()
            if (playerNames.size < 2 || lifeTotals.size < 2) return@mapNotNull null

            ServerMessage.ActiveMatchInfo(
                gameSessionId = gameSessionId,
                player1Name = playerNames[0],
                player2Name = playerNames[1],
                player1Life = lifeTotals[0],
                player2Life = lifeTotals[1]
            )
        }
    }
}
