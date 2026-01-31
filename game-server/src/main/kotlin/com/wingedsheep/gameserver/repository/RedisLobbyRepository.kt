package com.wingedsheep.gameserver.repository

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.config.RedisProperties
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.persistence.dto.PersistentTournamentLobby
import com.wingedsheep.gameserver.persistence.dto.PersistentSealedSession
import com.wingedsheep.gameserver.persistence.dto.PersistentTournament
import com.wingedsheep.gameserver.persistence.persistenceJson
import com.wingedsheep.gameserver.persistence.restoreTournamentLobby
import com.wingedsheep.gameserver.persistence.restoreTournamentManager
import com.wingedsheep.gameserver.persistence.toPersistent
import com.wingedsheep.gameserver.sealed.SealedSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.tournament.TournamentManager
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Redis-backed implementation of LobbyRepository.
 *
 * Lobbies, sealed sessions, and tournaments are persisted to Redis on every state change.
 * On startup, they are restored from Redis.
 *
 * Key patterns:
 * - {prefix}lobby:{lobbyId}
 * - {prefix}sealed:{sessionId}
 * - {prefix}tournament:{lobbyId}
 */
@Component
@ConditionalOnProperty(name = ["cache.redis.enabled"], havingValue = "true")
@Primary
class RedisLobbyRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    private val cardRegistry: CardRegistry,
    private val redisProperties: RedisProperties
) : LobbyRepository {

    private val logger = LoggerFactory.getLogger(RedisLobbyRepository::class.java)

    // In-memory caches for active entities
    private val lobbyCache = ConcurrentHashMap<String, TournamentLobby>()
    private val sealedSessionCache = ConcurrentHashMap<String, SealedSession>()
    private val tournamentCache = ConcurrentHashMap<String, TournamentManager>()

    private val keyPrefix = redisProperties.keyPrefix

    private fun lobbyKey(lobbyId: String) = "${keyPrefix}lobby:$lobbyId"
    private fun sealedKey(sessionId: String) = "${keyPrefix}sealed:$sessionId"
    private fun tournamentKey(lobbyId: String) = "${keyPrefix}tournament:$lobbyId"

    // =========================================================================
    // Lobby Operations
    // =========================================================================

    override fun saveLobby(lobby: TournamentLobby) {
        lobbyCache[lobby.lobbyId] = lobby

        try {
            val persistent = lobby.toPersistent()
            val json = persistenceJson.encodeToString(PersistentTournamentLobby.serializer(), persistent)

            redisTemplate.opsForValue().set(
                lobbyKey(lobby.lobbyId),
                json,
                redisProperties.ttlMinutes,
                TimeUnit.MINUTES
            )
            logger.debug("Persisted lobby ${lobby.lobbyId} to Redis")
        } catch (e: Exception) {
            logger.error("Failed to persist lobby ${lobby.lobbyId}", e)
        }
    }

    override fun findLobbyById(lobbyId: String): TournamentLobby? {
        lobbyCache[lobbyId]?.let { return it }

        return try {
            val json = redisTemplate.opsForValue().get(lobbyKey(lobbyId)) ?: return null
            val persistent = persistenceJson.decodeFromString(PersistentTournamentLobby.serializer(), json)
            val (lobby, _) = restoreTournamentLobby(persistent, cardRegistry)

            lobbyCache[lobbyId] = lobby
            logger.debug("Loaded lobby $lobbyId from Redis")
            lobby
        } catch (e: Exception) {
            logger.error("Failed to load lobby $lobbyId from Redis", e)
            null
        }
    }

    override fun removeLobby(lobbyId: String): TournamentLobby? {
        val lobby = lobbyCache.remove(lobbyId)

        try {
            redisTemplate.delete(lobbyKey(lobbyId))
            logger.debug("Removed lobby $lobbyId from Redis")
        } catch (e: Exception) {
            logger.error("Failed to remove lobby $lobbyId from Redis", e)
        }

        return lobby
    }

    override fun findAllLobbies(): Collection<TournamentLobby> {
        return lobbyCache.values
    }

    // =========================================================================
    // Sealed Session Operations (Legacy 2-player format)
    // =========================================================================

    override fun saveSealedSession(session: SealedSession) {
        sealedSessionCache[session.sessionId] = session

        try {
            val persistent = session.toPersistent()
            val json = persistenceJson.encodeToString(PersistentSealedSession.serializer(), persistent)

            redisTemplate.opsForValue().set(
                sealedKey(session.sessionId),
                json,
                redisProperties.ttlMinutes,
                TimeUnit.MINUTES
            )
            logger.debug("Persisted sealed session ${session.sessionId} to Redis")
        } catch (e: Exception) {
            logger.error("Failed to persist sealed session ${session.sessionId}", e)
        }
    }

    override fun findSealedSessionById(sessionId: String): SealedSession? {
        sealedSessionCache[sessionId]?.let { return it }

        // Note: Loading from Redis for sealed sessions is more complex because
        // SealedSession requires PlayerSession objects which contain WebSocket refs.
        // For now, return null and let the session be recreated if needed.
        return try {
            val json = redisTemplate.opsForValue().get(sealedKey(sessionId)) ?: return null
            // TODO: Implement full restoration if needed
            logger.debug("Found sealed session $sessionId in Redis (not fully restored)")
            null
        } catch (e: Exception) {
            logger.error("Failed to load sealed session $sessionId from Redis", e)
            null
        }
    }

    override fun removeSealedSession(sessionId: String): SealedSession? {
        val session = sealedSessionCache.remove(sessionId)

        try {
            redisTemplate.delete(sealedKey(sessionId))
            logger.debug("Removed sealed session $sessionId from Redis")
        } catch (e: Exception) {
            logger.error("Failed to remove sealed session $sessionId from Redis", e)
        }

        return session
    }

    // =========================================================================
    // Tournament Operations
    // =========================================================================

    override fun saveTournament(lobbyId: String, tournament: TournamentManager) {
        tournamentCache[lobbyId] = tournament

        try {
            val persistent = tournament.toPersistent(lobbyId)
            val json = persistenceJson.encodeToString(PersistentTournament.serializer(), persistent)

            redisTemplate.opsForValue().set(
                tournamentKey(lobbyId),
                json,
                redisProperties.ttlMinutes,
                TimeUnit.MINUTES
            )
            logger.debug("Persisted tournament for lobby $lobbyId to Redis")
        } catch (e: Exception) {
            logger.error("Failed to persist tournament for lobby $lobbyId", e)
        }
    }

    override fun findTournamentById(lobbyId: String): TournamentManager? {
        tournamentCache[lobbyId]?.let { return it }

        return try {
            val json = redisTemplate.opsForValue().get(tournamentKey(lobbyId)) ?: return null
            val persistent = persistenceJson.decodeFromString(PersistentTournament.serializer(), json)
            val tournament = restoreTournamentManager(persistent)

            tournamentCache[lobbyId] = tournament
            logger.debug("Loaded tournament for lobby $lobbyId from Redis")
            tournament
        } catch (e: Exception) {
            logger.error("Failed to load tournament for lobby $lobbyId from Redis", e)
            null
        }
    }

    override fun removeTournament(lobbyId: String): TournamentManager? {
        val tournament = tournamentCache.remove(lobbyId)

        try {
            redisTemplate.delete(tournamentKey(lobbyId))
            logger.debug("Removed tournament for lobby $lobbyId from Redis")
        } catch (e: Exception) {
            logger.error("Failed to remove tournament for lobby $lobbyId from Redis", e)
        }

        return tournament
    }

    // =========================================================================
    // Bulk Loading for Startup Recovery
    // =========================================================================

    /**
     * Load all lobbies from Redis.
     *
     * @return List of (TournamentLobby, List<PlayerIdentity>) pairs
     */
    fun loadAllLobbiesFromRedis(): List<Pair<TournamentLobby, List<PlayerIdentity>>> {
        val results = mutableListOf<Pair<TournamentLobby, List<PlayerIdentity>>>()

        try {
            val keys = redisTemplate.keys("${keyPrefix}lobby:*") ?: return results

            for (key in keys) {
                try {
                    val json = redisTemplate.opsForValue().get(key) ?: continue
                    val persistent = persistenceJson.decodeFromString(PersistentTournamentLobby.serializer(), json)
                    val (lobby, identities) = restoreTournamentLobby(persistent, cardRegistry)

                    lobbyCache[lobby.lobbyId] = lobby
                    results.add(lobby to identities)
                    logger.info("Restored lobby ${lobby.lobbyId} from Redis")
                } catch (e: Exception) {
                    logger.error("Failed to restore lobby from key $key", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to enumerate lobbies from Redis", e)
        }

        return results
    }

    /**
     * Load all tournaments from Redis.
     *
     * @return Map of lobbyId to TournamentManager
     */
    fun loadAllTournamentsFromRedis(): Map<String, TournamentManager> {
        val results = mutableMapOf<String, TournamentManager>()

        try {
            val keys = redisTemplate.keys("${keyPrefix}tournament:*") ?: return results

            for (key in keys) {
                try {
                    val json = redisTemplate.opsForValue().get(key) ?: continue
                    val persistent = persistenceJson.decodeFromString(PersistentTournament.serializer(), json)
                    val tournament = restoreTournamentManager(persistent)

                    tournamentCache[persistent.lobbyId] = tournament
                    results[persistent.lobbyId] = tournament
                    logger.info("Restored tournament for lobby ${persistent.lobbyId} from Redis")
                } catch (e: Exception) {
                    logger.error("Failed to restore tournament from key $key", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to enumerate tournaments from Redis", e)
        }

        return results
    }
}
