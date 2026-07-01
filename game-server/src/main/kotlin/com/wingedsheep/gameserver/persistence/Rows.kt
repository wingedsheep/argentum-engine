package com.wingedsheep.gameserver.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * Spring Data JDBC aggregate roots and entities backing the accounts subsystem.
 *
 * These are plain data classes — no JPA, no lazy loading, no dirty-checking. A null [Id] means
 * "new" (Spring Data JDBC inserts); a non-null id updates. Cross-aggregate references are by id
 * (e.g. [DeckRow.userId]), never an object graph; only genuinely-owned children
 * ([MatchResultRow.participants]) use @MappedCollection.
 */
@Table("users")
data class UserRow(
    /**
     * UUID primary key (DB default `gen_random_uuid()`): null means "new" (Spring Data JDBC inserts
     * and reads back the generated value). The id is also the account's shareable, non-guessable
     * "friend code" — you invite a friend by handing them this id, never your email.
     */
    @Id val id: UUID? = null,
    val email: String,
    val displayName: String,
    /** Grants access to the admin dashboard via this account's normal auth token (set from the dashboard). */
    val isAdmin: Boolean = false,
    /** When true the account appears offline to its friends even while connected (presence opt-out). */
    val hidePresence: Boolean = false,
    val createdAt: Instant = Instant.now(),
)

@Table("login_tokens")
data class LoginTokenRow(
    @Id val id: Long? = null,
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: Instant,
    val consumedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
)

@Table("decks")
data class DeckRow(
    @Id val id: Long? = null,
    val userId: UUID,
    val name: String,
    val format: String? = null,
    /** Full deck JSON (the client's SharedDeck shape). */
    val data: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

@Table("match_results")
data class MatchResultRow(
    @Id val id: Long? = null,
    val gameId: String,
    val format: String? = null,
    val tournamentName: String? = null,
    /**
     * The lobby that produced this game, when it came from one (tournament bracket / multiplayer pod).
     * Joins to [TournamentRow.lobbyId] so a tournament's full game list can be found. Null for
     * non-lobby games (quick game / casual) and games recorded before this column existed.
     */
    val lobbyId: String? = null,
    /** Matchmaking context: a [LobbyGameMode] name, or QUICK_GAME / CASUAL / HOTSEAT for non-lobby games. */
    val gameMode: String? = null,
    /** True for a ranked (ELO-adjusting) game. */
    val ranked: Boolean = false,
    /** Replay frame count at game-over — the activity measure behind the recording gate. */
    val frameCount: Int = 0,
    /** GameState.turnNumber at game-over. */
    val turnCount: Int = 0,
    val startedAt: Instant? = null,
    val endedAt: Instant = Instant.now(),
    @MappedCollection(idColumn = "match_id")
    val participants: Set<MatchParticipantRow> = emptySet(),
)

@Table("match_participants")
data class MatchParticipantRow(
    @Id val id: Long? = null,
    /** Null for guests and AI seats. */
    val userId: UUID? = null,
    val playerName: String,
    val won: Boolean,
    /** Deck color identity, canonical WUBRG order (e.g. "WU"); empty = colorless. Null if unknown. */
    val colors: String? = null,
    /** Comma-separated distinct set codes in the deck (e.g. "DSK,BLB"). Null if unknown. */
    val setCodes: String? = null,
    /** True for AI seats (which also have null userId). */
    val isAi: Boolean = false,
    /** Raw connecting IP, admin-only; never sent to clients. Null for AI / unknown. */
    val clientIp: String? = null,
    /** This seat's deck contents — one row per distinct card — for card-level stats. */
    @MappedCollection(idColumn = "participant_id")
    val cards: Set<MatchCardRow> = emptySet(),
)

@Table("match_participant_cards")
data class MatchCardRow(
    @Id val id: Long? = null,
    val cardName: String,
    val copies: Int,
)

/**
 * A durable, compact replay of a finished game — the reproducible setup plus the input stream,
 * gzip+base64-encoded in [data] (see [com.wingedsheep.gameserver.replay.ReplayCodec]). Keyed by
 * [gameId] (the same id the live in-memory cache and the spectator/share links use). The metadata
 * columns mirror [com.wingedsheep.gameserver.replay.CompactReplay] so a history list never has to
 * decode + re-simulate just to render a row.
 */
@Table("game_replays")
data class GameReplayRow(
    @Id val id: Long? = null,
    val gameId: String,
    val format: String? = null,
    val winnerName: String? = null,
    val tournamentName: String? = null,
    val startedAt: Instant? = null,
    val endedAt: Instant = Instant.now(),
    /** Reconstructable frames (initial + one per action) — the activity measure for the UI. */
    val frameCount: Int = 0,
    /** Player display names in seat order, comma-joined — enough for a summary row. */
    val playerNames: String = "",
    /** gzip+base64-encoded CompactReplay JSON. */
    val data: String,
)

@Table("tournaments")
data class TournamentRow(
    @Id val id: Long? = null,
    val lobbyId: String,
    val name: String? = null,
    /** TournamentFormat name (SEALED / DRAFT / PREMADE_DECKS / ...). */
    val format: String? = null,
    /** LobbyGameMode name (TOURNAMENT / FREE_FOR_ALL / ...). */
    val gameMode: String? = null,
    val setCodes: String? = null,
    val playerCount: Int = 0,
    val rounds: Int = 0,
    val gamesPerMatch: Int = 0,
    val winnerName: String? = null,
    /** Lifecycle status: IN_PROGRESS while the bracket is live, COMPLETED when it finishes, or ABANDONED
     *  if the lobby is torn down before completing. See [com.wingedsheep.gameserver.stats.TournamentStatus]. */
    val status: String = "COMPLETED",
    val startedAt: Instant? = null,
    /** Null while [status] is IN_PROGRESS; set when the tournament completes or is abandoned. */
    val endedAt: Instant? = null,
    @MappedCollection(idColumn = "tournament_id")
    val participants: Set<TournamentParticipantRow> = emptySet(),
)

@Table("tournament_participants")
data class TournamentParticipantRow(
    @Id val id: Long? = null,
    /** Null for guests and AI seats. */
    val userId: UUID? = null,
    val playerName: String,
    val isAi: Boolean = false,
    /** Final placement (1 = winner). */
    val placement: Int,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
)

/**
 * A friend relationship between two accounts. A single directed row carries both phases:
 *  - [status] == "PENDING": an outstanding request from [requesterId] to [addresseeId].
 *  - [status] == "ACCEPTED": a symmetric friendship (queried in both directions).
 *
 * Declining, cancelling, and unfriending all delete the row.
 */
@Table("friendships")
data class FriendshipRow(
    @Id val id: UUID? = null,
    val requesterId: UUID,
    val addresseeId: UUID,
    val status: String = FriendshipStatus.PENDING.name,
    val createdAt: Instant = Instant.now(),
    val respondedAt: Instant? = null,
)

enum class FriendshipStatus { PENDING, ACCEPTED }

/**
 * A signed-in account's current ELO rating in one ranked [mode] (RankedMode name). Created lazily on
 * the account's first ranked game in that mode; absence means "unrated" (treated as the starting
 * rating). [gamesPlayed] drives the provisional placement window and the displayed tier.
 */
@Table("user_ratings")
data class UserRatingRow(
    @Id val id: Long? = null,
    val userId: UUID,
    val mode: String,
    val rating: Double = 1200.0,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val peakRating: Double = 1200.0,
    val updatedAt: Instant = Instant.now(),
)

/** One ranked game's rating change for one player — the data behind the "rating over time" chart. */
@Table("rating_history")
data class RatingHistoryRow(
    @Id val id: Long? = null,
    val userId: UUID,
    val mode: String,
    val ratingBefore: Double,
    val ratingAfter: Double,
    val delta: Double,
    /** WIN / LOSS / DRAW. */
    val result: String,
    /** Null if the opponent was later deleted. */
    val opponentUserId: UUID? = null,
    val opponentRating: Double? = null,
    val gameId: String? = null,
    val createdAt: Instant = Instant.now(),
)
