package com.wingedsheep.gameserver.social

import com.wingedsheep.gameserver.lobby.QuickGameLobby
import com.wingedsheep.gameserver.lobby.QuickGameLobbyPlayer
import com.wingedsheep.sdk.core.DeckFormat
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Everything a quick lobby knew when its game started, so a rematch can be the same game again: the
 * same settings and each seat's same deck submission (an empty list still means "roll me a random
 * pool", so a Limited rematch deals fresh pools, as the first game did).
 */
data class RematchRecipe(
    val format: DeckFormat?,
    val ranked: Boolean,
    val momirBasic: Boolean,
    val setCode: String?,
    val seats: List<QuickGameLobbyPlayer>,
    val recordedAt: Long = System.currentTimeMillis(),
    /** Whether the game was played for ante (CR 407). */
    val playingForAnte: Boolean = false,
    /** Whether the game used the Chaos house variant. */
    val chaosRules: Boolean = false,
) {
    companion object {
        /** The recipe for a started [lobby], or null when it isn't a human 1v1 a rematch can replay. */
        fun of(lobby: QuickGameLobby): RematchRecipe? {
            if (lobby.vsAi || lobby.twoHeadedGiant || lobby.players.size != 2) return null
            if (lobby.players.any { it.isAi }) return null
            return RematchRecipe(
                format = lobby.format,
                ranked = lobby.ranked,
                momirBasic = lobby.momirBasic,
                setCode = lobby.setCode,
                seats = lobby.players.map { it.copy(ready = false) },
                playingForAnte = lobby.playingForAnte,
                chaosRules = lobby.chaosRules,
            )
        }
    }
}

/**
 * Rematch recipes by game session id. Written by the quick lobby as its game starts, taken by
 * [PostGameService] when that game ends. The two sides meet here rather than calling each other
 * because the lobby handler already sits below the post-game service in the dependency graph.
 */
@Component
class RematchRecipes {
    private val byGame = ConcurrentHashMap<String, RematchRecipe>()

    fun record(gameId: String, recipe: RematchRecipe) {
        byGame[gameId] = recipe
    }

    fun take(gameId: String): RematchRecipe? = byGame.remove(gameId)

    /** A game that never reached game-over (server-side abandon) leaves its recipe behind. */
    @Scheduled(fixedDelay = SWEEP_MS, initialDelay = SWEEP_MS)
    fun sweep() {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        byGame.entries.removeIf { it.value.recordedAt < cutoff }
    }

    companion object {
        private const val SWEEP_MS = 30 * 60 * 1000L
        private const val MAX_AGE_MS = 12 * 60 * 60 * 1000L
    }
}
