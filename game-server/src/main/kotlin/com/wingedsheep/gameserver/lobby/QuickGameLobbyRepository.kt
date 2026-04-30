package com.wingedsheep.gameserver.lobby

import com.wingedsheep.sdk.model.EntityId
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory store for [QuickGameLobby] instances with a per-lobby lock.
 *
 * Quick-game lobbies are short-lived (seconds-to-minutes between create and game start) and don't
 * survive a server restart, so they don't need Redis-backed persistence — a `ConcurrentHashMap`
 * is enough. The per-lobby lock fixes the race that the previous `waitingGameSession: @Volatile`
 * field had: two players hitting Create at the same time used to clobber each other.
 *
 * Callers should hold the lock for the *entire* read-mutate-write cycle on a single lobby
 * (selecting a deck, marking ready, evaluating "both ready → start"). [withLock] wraps that.
 */
@Component
class QuickGameLobbyRepository {
    private val lobbies = ConcurrentHashMap<String, QuickGameLobby>()
    private val locks = ConcurrentHashMap<String, Any>()

    fun save(lobby: QuickGameLobby) {
        lobbies[lobby.lobbyId] = lobby
        locks.computeIfAbsent(lobby.lobbyId) { Any() }
    }

    fun findById(lobbyId: String): QuickGameLobby? = lobbies[lobbyId]

    fun remove(lobbyId: String) {
        lobbies.remove(lobbyId)
        locks.remove(lobbyId)
    }

    fun findContainingPlayer(playerId: EntityId): QuickGameLobby? =
        lobbies.values.firstOrNull { lobby -> lobby.players.any { it.playerId == playerId } }

    /**
     * Run [block] while holding the per-lobby lock. The lobby is re-fetched after acquiring the
     * lock so that concurrent removals don't race; if it's gone by then, [block] receives null.
     */
    fun <T> withLock(lobbyId: String, block: (QuickGameLobby?) -> T): T {
        val lock = locks.computeIfAbsent(lobbyId) { Any() }
        return synchronized(lock) {
            block(lobbies[lobbyId])
        }
    }

    fun findAll(): Collection<QuickGameLobby> = lobbies.values
}
