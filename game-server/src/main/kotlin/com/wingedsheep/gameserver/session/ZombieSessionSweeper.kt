package com.wingedsheep.gameserver.session

import com.wingedsheep.gameserver.handler.GamePlayHandler
import com.wingedsheep.gameserver.handler.LobbySharedContext
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ZombieSessionSweeper(
    private val sessionRegistry: SessionRegistry,
    private val gameRepository: GameRepository,
    private val lobbyRepository: LobbyRepository,
    private val gamePlayHandler: GamePlayHandler,
    private val lobbySharedContext: LobbySharedContext
) {
    private val logger = LoggerFactory.getLogger(ZombieSessionSweeper::class.java)

    companion object {
        /** Keep completed tournaments for 30 minutes so the host can add extra rounds. */
        const val TOURNAMENT_COMPLETE_GRACE_PERIOD_MS = 30L * 60 * 1000
    }

    @Scheduled(fixedRate = 60_000)
    fun sweep() {
        sweepFinishedGames()
        sweepEmptyLobbies()
        sweepDisconnectedIdentities()
        sweepStaleTrackingEntries()
    }

    private fun sweepFinishedGames() {
        val finished = gameRepository.findAll().filter { it.isGameOver() }
        for (game in finished) {
            logger.info("Sweeping finished game: ${game.sessionId}")
            gameRepository.remove(game.sessionId)
            gameRepository.removeLobbyLink(game.sessionId)
        }
    }

    private fun sweepEmptyLobbies() {
        val now = System.currentTimeMillis()
        val empty = lobbyRepository.findAllLobbies().filter { lobby ->
            lobby.playerCount == 0 ||
                (lobby.state == LobbyState.TOURNAMENT_COMPLETE &&
                    lobby.completedAt != null &&
                    now - lobby.completedAt!! > TOURNAMENT_COMPLETE_GRACE_PERIOD_MS)
        }
        for (lobby in empty) {
            logger.info("Sweeping lobby: ${lobby.lobbyId} (state=${lobby.state}, players=${lobby.playerCount})")
            lobbyRepository.removeLobby(lobby.lobbyId)
            lobbyRepository.removeTournament(lobby.lobbyId)
        }
    }

    private fun sweepDisconnectedIdentities() {
        val disconnected = sessionRegistry.getAllIdentities().filter { identity ->
            !identity.isConnected &&
                identity.disconnectTimer == null &&
                identity.currentGameSessionId == null &&
                identity.currentLobbyId == null
        }
        for (identity in disconnected) {
            logger.info("Sweeping orphaned identity: ${identity.playerName} (${identity.token})")
            sessionRegistry.removeIdentity(identity.token)
        }
    }

    private fun sweepStaleTrackingEntries() {
        val activeGameIds = gameRepository.findAll().map { it.sessionId }.toSet()
        val activeLobbyIds = lobbyRepository.findAllLobbies().map { it.lobbyId }.toSet()
        gamePlayHandler.sweepStaleEntries(activeGameIds, activeLobbyIds)
        lobbySharedContext.sweepStaleLocks(activeLobbyIds)
    }
}
