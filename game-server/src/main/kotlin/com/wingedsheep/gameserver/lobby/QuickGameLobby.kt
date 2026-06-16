package com.wingedsheep.gameserver.lobby

import com.wingedsheep.sdk.core.DeckFormat
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
    /**
     * If true the lobby is listed by `GET /api/quick-games/public` so other players can find it
     * from the home screen without needing the invite code. AI lobbies cannot be public — there
     * is no second seat for a stranger to join.
     */
    @Volatile var isPublic: Boolean = false,
    /**
     * Optional deck-format restriction. When set, every submitted deck is validated against the
     * format's per-card legality. Null = no restriction (existing behaviour). Host-controlled.
     */
    @Volatile var format: DeckFormat? = null,
    /**
     * When true this is a Momir Basic lobby: no deckbuilding (every seat gets a fixed 60-basic
     * deck), the avatar starts in the command zone, and the random creature pool is every creature
     * across all sets. Host-controlled via the lobby's Format dropdown ("Momir Basic" lives under
     * its Custom-formats group), so it can be toggled after creation. Mutually exclusive with
     * [format].
     */
    @Volatile var momirBasic: Boolean = false,
    /**
     * When true this is a Two-Headed Giant lobby (CR 810): four seats forming two teams of two
     * (join order 0+1 vs 2+3), played under [com.wingedsheep.sdk.core.Format.TwoHeadedGiant].
     * Fixed at creation. Human-only (the built-in AI is not team-aware yet — Phase 8).
     */
    val twoHeadedGiant: Boolean = false,
) {
    val players: MutableList<QuickGameLobbyPlayer> = mutableListOf()

    @Volatile
    var started: Boolean = false

    /** Seats this lobby fills before it can start: 4 for Two-Headed Giant, else the default 2. */
    val maxPlayers: Int get() = if (twoHeadedGiant) TWO_HEADED_GIANT_PLAYERS else MAX_PLAYERS

    val isFull: Boolean get() = players.size >= maxPlayers

    fun findPlayer(playerId: EntityId): QuickGameLobbyPlayer? =
        players.firstOrNull { it.playerId == playerId }

    fun allReady(): Boolean = players.size == maxPlayers && players.all { it.ready }

    /**
     * The team partition for [Format.TwoHeadedGiant], as seat indices into join order: seats 0+1
     * are team 0, seats 2+3 team 1. Null in a non-2HG lobby (each player plays alone). Forwarded
     * to [com.wingedsheep.gameserver.session.GameSession.teams] → `GameConfig.teams`.
     */
    fun teamAssignment(): List<List<Int>>? =
        if (twoHeadedGiant) listOf(listOf(0, 1), listOf(2, 3)) else null

    /** The team index of the seat at [seatIndex] (join order), or null in a non-2HG lobby. */
    fun teamIndexOf(seatIndex: Int): Int? =
        teamAssignment()?.indexOfFirst { seatIndex in it }?.takeIf { it >= 0 }

    companion object {
        const val MAX_PLAYERS = 2
        const val TWO_HEADED_GIANT_PLAYERS = 4

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
    /**
     * Designated commander card name for commander-shape formats (Commander / Brawl / Standard
     * Brawl). Null when [deckList] is empty (random pool) or the lobby format isn't commander-
     * shape. Resubmitted by the client alongside the deck list.
     */
    var commander: String? = null,
)
