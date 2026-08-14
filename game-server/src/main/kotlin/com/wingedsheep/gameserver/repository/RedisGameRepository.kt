package com.wingedsheep.gameserver.repository

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.config.RedisProperties
import com.wingedsheep.gameserver.persistence.dto.PersistentGameSession
import com.wingedsheep.gameserver.persistence.persistenceJson
import com.wingedsheep.gameserver.persistence.restoreGameSession
import com.wingedsheep.gameserver.persistence.toPersistent
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.SessionRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Redis-backed implementation of GameRepository.
 *
 * Game sessions are persisted to Redis on every state change.
 * On startup, sessions are restored from Redis.
 *
 * Key pattern: {prefix}game:{sessionId}
 */
@Component
@ConditionalOnProperty(name = ["cache.redis.enabled"], havingValue = "true")
@Primary
class RedisGameRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    private val cardRegistry: CardRegistry,
    private val printingRegistry: com.wingedsheep.engine.registry.PrintingRegistry,
    private val tokenArtRegistry: com.wingedsheep.engine.registry.TokenArtRegistry,
    private val sessionRegistry: SessionRegistry,
    private val redisProperties: RedisProperties,
    // Replay recordings are no longer part of the session blob — they live in the replay store, so a
    // session recovered from Redis picks its recording back up from there.
    private val replayService: com.wingedsheep.gameserver.replay.ReplayService,
) : GameRepository {

    private val logger = LoggerFactory.getLogger(RedisGameRepository::class.java)

    // In-memory cache for active sessions (avoiding repeated deserialization)
    private val sessionCache = ConcurrentHashMap<String, GameSession>()
    private val gameToLobby = ConcurrentHashMap<String, String>()

    private val keyPrefix = redisProperties.keyPrefix

    private fun gameKey(sessionId: String) = "${keyPrefix}game:$sessionId"
    private fun lobbyLinkKey(sessionId: String) = "${keyPrefix}game-lobby:$sessionId"

    override fun save(gameSession: GameSession) {
        sessionCache[gameSession.sessionId] = gameSession

        val lobbyId = gameToLobby[gameSession.sessionId]

        try {
            val persistent = gameSession.toPersistent(lobbyId)
            val json = persistenceJson.encodeToString(PersistentGameSession.serializer(), persistent)

            redisTemplate.opsForValue().set(
                gameKey(gameSession.sessionId),
                json,
                Duration.ofMinutes(redisProperties.ttlMinutes)
            )
            logger.debug("Persisted game session ${gameSession.sessionId} to Redis")
        } catch (e: Exception) {
            logger.error("Failed to persist game session ${gameSession.sessionId}", e)
        }
    }

    override fun findById(sessionId: String): GameSession? {
        // Check in-memory cache first
        sessionCache[sessionId]?.let { return it }

        // Try to load from Redis
        return try {
            val json = redisTemplate.opsForValue().get(gameKey(sessionId)) ?: return null
            val persistent = persistenceJson.decodeFromString(PersistentGameSession.serializer(), json)
            val (session, _) = restoreGameSession(persistent, cardRegistry, printingRegistry, tokenArtRegistry)
            resumeReplayRecording(session)

            // Cache the restored session
            sessionCache[sessionId] = session

            // Restore lobby link if present
            persistent.lobbyId?.let { gameToLobby[sessionId] = it }

            logger.debug("Loaded game session $sessionId from Redis")
            session
        } catch (e: Exception) {
            logger.error("Failed to load game session $sessionId from Redis", e)
            null
        }
    }

    override fun remove(sessionId: String): GameSession? {
        val session = sessionCache.remove(sessionId)
        gameToLobby.remove(sessionId)

        try {
            redisTemplate.delete(gameKey(sessionId))
            redisTemplate.delete(lobbyLinkKey(sessionId))
            logger.debug("Removed game session $sessionId from Redis")
        } catch (e: Exception) {
            logger.error("Failed to remove game session $sessionId from Redis", e)
        }

        return session
    }

    override fun findAll(): Collection<GameSession> {
        // Return cached sessions - for full enumeration, use loadAllFromRedis()
        return sessionCache.values
    }

    override fun linkToLobby(gameSessionId: String, lobbyId: String) {
        gameToLobby[gameSessionId] = lobbyId

        try {
            redisTemplate.opsForValue().set(
                lobbyLinkKey(gameSessionId),
                lobbyId,
                Duration.ofMinutes(redisProperties.ttlMinutes)
            )
        } catch (e: Exception) {
            logger.error("Failed to persist lobby link for game $gameSessionId", e)
        }

        // Re-save the game session to include the lobby link
        sessionCache[gameSessionId]?.let { save(it) }
    }

    override fun getLobbyForGame(gameSessionId: String): String? {
        gameToLobby[gameSessionId]?.let { return it }

        return try {
            redisTemplate.opsForValue().get(lobbyLinkKey(gameSessionId))?.also {
                gameToLobby[gameSessionId] = it
            }
        } catch (e: Exception) {
            logger.error("Failed to get lobby link for game $gameSessionId", e)
            null
        }
    }

    override fun removeLobbyLink(gameSessionId: String): String? {
        val lobbyId = gameToLobby.remove(gameSessionId)

        try {
            redisTemplate.delete(lobbyLinkKey(gameSessionId))
        } catch (e: Exception) {
            logger.error("Failed to remove lobby link for game $gameSessionId", e)
        }

        return lobbyId
    }

    /**
     * Pick a recovered session's replay recording back up from the replay store.
     *
     * The store's copy can be a few actions behind the recovered live state (it is flushed on a
     * timer, not per action), so [GameSession.restoreReplayRecording] compares the fingerprint
     * written with the flush against the recovered position and declines to continue when they
     * disagree — a short honest replay beats a long fictional one.
     */
    private fun resumeReplayRecording(session: GameSession) {
        val stored = replayService.findStored(session.sessionId) ?: return
        if (stored.status != com.wingedsheep.gameserver.replay.ReplayStatus.IN_PROGRESS) return
        val resumed = session.restoreReplayRecording(stored.replay, stored.resumeFingerprint)
        logger.info(
            "Replay recording for ${session.sessionId}: ${if (resumed) "resumed" else "stopped"} " +
                "at ${stored.replay.actions.size} recorded actions"
        )
        // Nothing more will ever be appended, so close the partial record out now rather than
        // leaving it mid-write for the lifetime of a game that is still being played.
        if (!resumed) replayService.finalizePartial(session.sessionId)
    }

    /**
     * Load all game sessions from Redis.
     * Called during startup recovery.
     *
     * @return List of (GameSession, List<PlayerIdentity>) pairs
     */
    fun loadAllFromRedis(): List<Pair<GameSession, List<PlayerIdentity>>> {
        val results = mutableListOf<Pair<GameSession, List<PlayerIdentity>>>()

        try {
            val keys = redisTemplate.keys("${keyPrefix}game:*") ?: return results

            for (key in keys) {
                try {
                    val json = redisTemplate.opsForValue().get(key) ?: continue
                    val persistent = persistenceJson.decodeFromString(PersistentGameSession.serializer(), json)
                    val (session, identities) = restoreGameSession(persistent, cardRegistry, printingRegistry, tokenArtRegistry)
                    resumeReplayRecording(session)

                    sessionCache[session.sessionId] = session
                    persistent.lobbyId?.let { gameToLobby[session.sessionId] = it }

                    results.add(session to identities)
                    logger.info("Restored game session ${session.sessionId} from Redis")
                } catch (e: Exception) {
                    logger.error("Failed to restore game session from key $key", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to enumerate game sessions from Redis", e)
        }

        return results
    }
}
