package com.wingedsheep.gameserver.session

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Component
class SessionRegistry {

    private val logger = LoggerFactory.getLogger(SessionRegistry::class.java)

    /** Player identities indexed by token */
    private val playerIdentities = ConcurrentHashMap<String, PlayerIdentity>()

    /** WebSocket session ID → player token */
    private val wsToToken = ConcurrentHashMap<String, String>()

    /** Legacy player sessions indexed by WebSocket session ID */
    private val playerSessions = ConcurrentHashMap<String, PlayerSession>()

    /** Per-session locks for thread-safe WebSocket writes */
    private val sessionLocks = ConcurrentHashMap<String, Any>()

    /** Scheduler for disconnect grace period timers */
    val disconnectScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    /** Grace period before treating a disconnect as abandonment */
    val disconnectGracePeriodMinutes = 5L

    fun register(identity: PlayerIdentity, session: WebSocketSession, playerSession: PlayerSession) {
        identity.webSocketSession = session
        playerIdentities[identity.token] = identity
        wsToToken[session.id] = identity.token
        playerSessions[session.id] = playerSession
    }

    fun getIdentityByToken(token: String): PlayerIdentity? = playerIdentities[token]

    fun getIdentityByWsId(wsId: String): PlayerIdentity? {
        val token = wsToToken[wsId] ?: return null
        return playerIdentities[token]
    }

    fun getTokenByWsId(wsId: String): String? = wsToToken[wsId]

    fun getPlayerSession(wsId: String): PlayerSession? = playerSessions[wsId]

    fun mapWsToToken(wsId: String, token: String) {
        wsToToken[wsId] = token
    }

    fun setPlayerSession(wsId: String, playerSession: PlayerSession) {
        playerSessions[wsId] = playerSession
    }

    fun removeByWsId(wsId: String): Pair<String?, PlayerSession?> {
        sessionLocks.remove(wsId)
        val token = wsToToken.remove(wsId)
        val playerSession = playerSessions.remove(wsId)
        return token to playerSession
    }

    fun removeIdentity(token: String): PlayerIdentity? = playerIdentities.remove(token)

    fun getAllIdentities(): Collection<PlayerIdentity> = playerIdentities.values

    fun getSessionLock(wsId: String): Any = sessionLocks.computeIfAbsent(wsId) { Any() }

    fun removeOldWsMapping(token: String) {
        val oldWsId = wsToToken.entries.find { it.value == token }?.key
        if (oldWsId != null) {
            playerSessions.remove(oldWsId)
            wsToToken.remove(oldWsId)
            sessionLocks.remove(oldWsId)
        }
    }
}
