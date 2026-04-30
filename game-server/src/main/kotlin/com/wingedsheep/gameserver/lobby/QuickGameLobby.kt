package com.wingedsheep.gameserver.lobby

import com.wingedsheep.sdk.model.EntityId
import java.util.UUID

/**
 * In-memory state for a quick-game lobby.
 *
 * A quick-game lobby is the staging area between "I want to play" and "the game has started".
 * Replaces the previous `waitingGameSession: @Volatile` global in `GamePlayHandler` so that
 * two players hitting Create simultaneously can no longer race each other into the same slot,
 * and so that deck selection happens *inside* the lobby rather than on the home screen.
 *
 * AI games are modelled as a 1-human + 1-AI lobby — the AI is added by [QuickGameLobbyHandler]
 * at lobby creation and starts off auto-ready, so the host only has to pick a deck and ready up.
 *
 * Mutations are guarded by a per-lobby lock owned by [QuickGameLobbyRepository]; this class itself
 * holds no synchronization and assumes the caller holds the lock.
 */
class QuickGameLobby(
    val lobbyId: String = generateLobbyCode(),
    val createdAt: Long = System.currentTimeMillis(),
    val vsAi: Boolean,
    /** Set used for "Random" sealed-pool decks. Mutable: the host can change it from the lobby UI. */
    @Volatile var setCode: String?,
) {
    val players: MutableList<QuickGameLobbyPlayer> = mutableListOf()

    @Volatile
    var started: Boolean = false

    val isFull: Boolean get() = players.size >= MAX_PLAYERS

    fun findPlayer(playerId: EntityId): QuickGameLobbyPlayer? =
        players.firstOrNull { it.playerId == playerId }

    fun allReady(): Boolean = players.size == MAX_PLAYERS && players.all { it.ready }

    companion object {
        const val MAX_PLAYERS = 2

        // Use the same UUID format as TournamentLobby so the join-code UX is consistent
        // across both flows (shared invite-box copy interaction).
        fun generateLobbyCode(): String = UUID.randomUUID().toString()
    }
}

/**
 * Per-player state in a quick-game lobby.
 *
 * `deckList = emptyMap()` is meaningful: it means "let the server pick a random sealed pool",
 * matching the existing Quick Game semantics (see `GamePlayHandler.handleCreateGame`). A null
 * `deckList` means the player has not yet committed to anything (initial state).
 */
data class QuickGameLobbyPlayer(
    val playerId: EntityId,
    val playerName: String,
    val isAi: Boolean = false,
    var deckList: Map<String, Int>? = null,
    var ready: Boolean = false,
    /**
     * Per-player set code used when [deckList] is empty (Random pool).
     * null means "any set, server picks one". Each player chooses their own.
     */
    var setCode: String? = null,
)
