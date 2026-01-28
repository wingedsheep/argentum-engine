package com.wingedsheep.gameserver.persistence

import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.gameserver.repository.RedisGameRepository
import com.wingedsheep.gameserver.repository.RedisLobbyRepository
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.SessionRegistry
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

/**
 * Service responsible for recovering game sessions and lobbies from Redis on startup.
 *
 * When Redis caching is enabled, this service loads all persisted sessions
 * and registers the player identities (without WebSocket connections).
 * Players reconnect and re-associate with their identities via their tokens.
 */
@Service
@ConditionalOnProperty(name = ["cache.redis.enabled"], havingValue = "true")
class SessionRecoveryService(
    private val gameRepository: GameRepository,
    private val lobbyRepository: LobbyRepository,
    private val sessionRegistry: SessionRegistry
) {
    private val logger = LoggerFactory.getLogger(SessionRecoveryService::class.java)

    @PostConstruct
    fun recoverSessions() {
        logger.info("Starting session recovery from Redis...")

        var gamesRecovered = 0
        var lobbiesRecovered = 0
        var tournamentsRecovered = 0
        var playersRecovered = 0

        // Recover game sessions
        if (gameRepository is RedisGameRepository) {
            val gameSessions = gameRepository.loadAllFromRedis()
            for ((_, identities) in gameSessions) {
                for (identity in identities) {
                    registerIdentityIfNew(identity)
                    playersRecovered++
                }
                gamesRecovered++
            }
        }

        // Recover lobbies
        if (lobbyRepository is RedisLobbyRepository) {
            val lobbies = lobbyRepository.loadAllLobbiesFromRedis()
            for ((_, identities) in lobbies) {
                for (identity in identities) {
                    registerIdentityIfNew(identity)
                    playersRecovered++
                }
                lobbiesRecovered++
            }

            // Recover tournaments
            val tournaments = lobbyRepository.loadAllTournamentsFromRedis()
            tournamentsRecovered = tournaments.size
        }

        logger.info(
            "Session recovery complete: " +
                "$gamesRecovered games, " +
                "$lobbiesRecovered lobbies, " +
                "$tournamentsRecovered tournaments, " +
                "$playersRecovered player identities"
        )
    }

    /**
     * Register a player identity if one doesn't already exist with that token.
     * Restored identities have no WebSocket connection - they reconnect later.
     */
    private fun registerIdentityIfNew(identity: PlayerIdentity) {
        val existing = sessionRegistry.getIdentityByToken(identity.token)
        if (existing == null) {
            // Register identity without WebSocket session
            // The player will reconnect and their WebSocket will be associated then
            logger.debug(
                "Recovered player identity: {} ({})",
                identity.playerName,
                identity.playerId.value
            )
        }
    }
}
