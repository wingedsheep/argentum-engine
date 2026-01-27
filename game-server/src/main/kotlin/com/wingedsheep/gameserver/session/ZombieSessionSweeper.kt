package com.wingedsheep.gameserver.session

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
    private val lobbyRepository: LobbyRepository
) {
    private val logger = LoggerFactory.getLogger(ZombieSessionSweeper::class.java)

    @Scheduled(fixedRate = 60_000)
    fun sweep() {
        sweepFinishedGames()
        sweepEmptyLobbies()
        sweepDisconnectedIdentities()
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
        val empty = lobbyRepository.findAllLobbies().filter { lobby ->
            lobby.playerCount == 0 ||
                (lobby.state == LobbyState.TOURNAMENT_COMPLETE)
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
}
