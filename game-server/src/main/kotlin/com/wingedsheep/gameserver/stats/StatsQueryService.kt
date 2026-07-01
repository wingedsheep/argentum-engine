package com.wingedsheep.gameserver.stats

import com.fasterxml.jackson.annotation.JsonProperty
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.sdk.core.CardType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID

/** A `(label, count)` bucket for a stats breakdown. */
data class StatBucket(val label: String, val count: Long)

/** One opponent in a head-to-head record. */
data class HeadToHead(
    val opponent: String,
    val opponentUserId: UUID?,
    val isAi: Boolean,
    val wins: Long,
    val losses: Long,
)

/** One opponent seat in a recorded game (so the client can link to a human opponent's profile). */
data class GameOpponent(
    val name: String,
    /** Null for guests and AI seats — only signed-in opponents link to a public profile. */
    val userId: UUID?,
    val isAi: Boolean,
)

/** One row in a user's game history. */
data class GameHistoryEntry(
    val endedAt: String,
    val gameMode: String?,
    val format: String?,
    val colors: String?,
    /** Comma-joined opponent names — kept for callers that just want a label (e.g. the deck modal). */
    val opponents: String?,
    /** Per-seat opponents, so signed-in opponents can be linked to their public profile. */
    val opponentList: List<GameOpponent>,
    val won: Boolean,
    /** This player's ELO *before* this game, when it was a ranked game; null otherwise. */
    val selfRating: Int?,
    /** The opponent's ELO at the time of a ranked game; null otherwise (or for multi-seat games). */
    val opponentRating: Int?,
    /** Stable game id, used to open/share the replay. */
    val gameId: String,
    /** True when a compact replay was stored for this game and can be watched/shared. */
    val hasReplay: Boolean,
)

/** One card line in a stored deck (for the recent-games deck viewer). */
data class DeckCardEntry(val cardName: String, val copies: Int)

/**
 * One card line enriched with the registry metadata the client needs to render a deck the polished
 * way (group by type, draw a mana curve, colour the pips, show the art on hover) — so the deck viewer
 * doesn't have to load the full card catalog just to know a card's cost/type/colour/image. Field names
 * match the client's `DeckStatsCard` shape so `computeDeckStats` consumes them directly.
 */
data class GameDeckCard(
    val cardName: String,
    val copies: Int,
    /** Converted mana cost; 0 when the card can't be resolved. */
    val cmc: Int,
    /** Card type enum names (e.g. ["CREATURE"], ["LAND"]) — empty when unresolved. */
    val cardTypes: List<String>,
    /** The card's own colours as enum names (e.g. ["WHITE","BLUE"]); empty = colourless/unresolved. */
    val colors: List<String>,
    /**
     * Direct CDN art URL for the card's default printing, resolved the same way as the deckbuilder
     * catalog ([CardsController]). `null` when the card can't be resolved, in which case the client
     * falls back to a Scryfall name lookup — slower and rate-limited, so we send this up front.
     */
    val imageUri: String?,
)

/** One seat's recorded deck within a finished game, for the recent-games deck viewer. */
data class GameDeckParticipant(
    val playerName: String,
    val isAi: Boolean,
    /** True for the seat belonging to the requesting user. */
    val isSelf: Boolean,
    val won: Boolean,
    /** Color identity recomputed from the stored deck, canonical WUBRG order; "" = colorless. */
    val colors: String,
    /** The deck's cards, most copies first then alphabetical, each enriched with display metadata. */
    val cards: List<GameDeckCard>,
)

/** Both seats' decks for one finished game the requesting user played. */
data class GameDecks(
    val gameId: String,
    val endedAt: String,
    val gameMode: String?,
    val participants: List<GameDeckParticipant>,
)

/** Global, cross-user totals for the admin dashboard. */
data class GlobalOverview(
    val totalGames: Long,
    val totalPlayers: Long,
    val totalAccounts: Long,
    val totalTournaments: Long,
    val gamesLast24h: Long,
    val gamesLast7d: Long,
)

/** One day's game count for the games-per-day chart. */
data class DailyCount(val day: String, val count: Long)

/** One IP and how many games connected from it (admin-only; resolved to a location elsewhere). */
data class IpCount(val ip: String, val count: Long)

/** A card and how often it has appeared in decks. [imageUri] is set only for per-user top cards. */
data class CardStat(val cardName: String, val copies: Long, val decks: Long, val imageUri: String? = null)

/** A card's win rate: games (decks containing it) and how many of those decks won. */
data class CardWinRate(val cardName: String, val decks: Long, val wins: Long, val winRate: Double)

/** One tournament in a user's tournament history. [id] opens the full tournament detail. */
data class UserTournamentEntry(
    val id: Long,
    /** Completion time, or the start time while the tournament is still in progress. */
    val endedAt: String,
    val name: String?,
    val format: String?,
    val gameMode: String?,
    /** Final placement (1 = winner); 0 while the tournament is still in progress. */
    val placement: Int,
    val playerCount: Int,
    /** IN_PROGRESS / COMPLETED / ABANDONED — see [TournamentStatus]. */
    val status: String,
)

/** One player's final standing in a tournament. */
data class TournamentStanding(
    val placement: Int,
    val playerName: String,
    /** Null for guests and AI seats. */
    val userId: UUID?,
    val isAi: Boolean,
    val wins: Int,
    val losses: Int,
    val draws: Int,
)

/** One seat in a single tournament game. */
data class TournamentGamePlayer(
    val name: String,
    val userId: UUID?,
    val isAi: Boolean,
    val won: Boolean,
)

/** One game played within a tournament, with its replay availability. */
data class TournamentGame(
    val gameId: String,
    val endedAt: String,
    val hasReplay: Boolean,
    val players: List<TournamentGamePlayer>,
)

/**
 * Full, public detail for one finished tournament: every participant's final standing and every game
 * that was played (not just the requesting player's), each linkable to its replay. [games] is empty
 * for tournaments recorded before games carried a lobby id (those show standings only).
 */
data class TournamentDetail(
    val id: Long,
    val name: String?,
    val format: String?,
    val gameMode: String?,
    val setCodes: String?,
    val playerCount: Int,
    val winnerName: String?,
    /** Completion time, or the start time while the tournament is still in progress. */
    val endedAt: String,
    /** IN_PROGRESS / COMPLETED / ABANDONED — see [TournamentStatus]. */
    val status: String,
    val standings: List<TournamentStanding>,
    val games: List<TournamentGame>,
)

/** A registered account plus its lifetime game counts, for the admin Players list. */
data class AdminUserStat(
    val id: UUID,
    val email: String,
    val displayName: String,
    // Pin the wire name (see SetAdminBody) — the admin Players list reads `u.isAdmin` for the ADMIN
    // badge; without this Jackson emits `admin` and the badge never shows for promoted accounts.
    @JsonProperty("isAdmin") val isAdmin: Boolean,
    val createdAt: String,
    val games: Long,
    val wins: Long,
    val lastPlayed: String?,
)

/** A recorded tournament for the admin list. The [id] opens the shared tournament detail view. */
data class TournamentSummary(
    val id: Long,
    /** Completion time, or the start time while the tournament is still in progress. */
    val endedAt: String,
    val name: String?,
    val format: String?,
    val gameMode: String?,
    val playerCount: Int,
    val winnerName: String?,
    /** IN_PROGRESS / COMPLETED / ABANDONED — see [TournamentStatus]. */
    val status: String,
)

/** One seat in a recorded game, for the admin global game list. */
data class AdminGamePlayer(
    val name: String,
    val userId: UUID?,
    val isAi: Boolean,
    val won: Boolean,
    // Coarse connection origin, resolved from the seat's recorded IP via [GeoIpService]. Null for AI
    // seats and when the IP couldn't be resolved. Raw IPs never reach the client — only the location.
    val location: GeoLocation? = null,
)

/** A recorded game for the admin global game list, newest first, with every seat. */
data class AdminRecentGame(
    val gameId: String,
    val endedAt: String,
    val gameMode: String?,
    val format: String?,
    val players: List<AdminGamePlayer>,
    val winnerName: String?,
    val hasReplay: Boolean,
    val tournamentName: String?,
)

/**
 * Aggregate / group-by stat queries over the match-history tables, via [JdbcTemplate]. These are
 * awkward to express through Spring Data JDBC's entity mapping, so they live here as plain SQL. The
 * bean only exists when accounts are enabled (a DataSource is present then). Per-user methods filter
 * by `user_id`; the global methods span every recorded game (guests included, AI excluded).
 */
@Service
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class StatsQueryService(
    private val jdbc: JdbcTemplate,
    private val deckProfiler: DeckProfiler,
    private val cardRegistry: CardRegistry,
    private val printingRegistry: PrintingRegistry,
    private val geoIp: GeoIpService,
) {

    // ---- Per-user ----------------------------------------------------------------------------

    /**
     * How often the user has played each color identity (e.g. "WU"), most-played first. Recomputed
     * from each game's stored deck list (the authoritative source) rather than the denormalized
     * `colors` column, which can be stale/empty for games recorded before a card's color identity
     * was known. Games with no stored deck cards are omitted (we genuinely don't know their colors,
     * so they must not masquerade as "Colorless").
     */
    fun colorBreakdown(userId: UUID): List<StatBucket> {
        val byColors = decksByParticipant(userId).values
            .map { cards -> deckProfiler.profile(cards.map { it.cardName }).colors }
            .groupingBy { it }
            .eachCount()
        return byColors.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { StatBucket(it.key, it.value.toLong()) }
    }

    /** How often the user has played each set, most-played first. Splits the comma-separated codes. */
    fun setBreakdown(userId: UUID): List<StatBucket> = jdbc.query(
        """
        SELECT trim(s) AS label, count(*) AS n
        FROM match_participants p
        CROSS JOIN LATERAL unnest(string_to_array(p.set_codes, ',')) AS s
        WHERE p.user_id = ? AND p.set_codes IS NOT NULL AND p.set_codes <> ''
        GROUP BY trim(s)
        ORDER BY n DESC
        """.trimIndent(),
        { rs, _ -> StatBucket(rs.getString("label"), rs.getLong("n")) },
        userId,
    )

    /**
     * How often the user has played each game mode, most-played first. The label is a composite
     * `"<gameMode>~<format>"` (format may be empty) so the client can render the mode as a hierarchy
     * (e.g. "Tournament › Draft"); the client merges buckets whose display label collapses to the same
     * thing. `~` never occurs in the enum names being concatenated.
     */
    fun modeBreakdown(userId: UUID): List<StatBucket> = jdbc.query(
        """
        SELECT COALESCE(r.game_mode, 'UNKNOWN') || '~' || COALESCE(r.format, '') AS label, count(*) AS n
        FROM match_participants p
        JOIN match_results r ON r.id = p.match_id
        WHERE p.user_id = ?
        GROUP BY COALESCE(r.game_mode, 'UNKNOWN') || '~' || COALESCE(r.format, '')
        ORDER BY n DESC
        """.trimIndent(),
        { rs, _ -> StatBucket(rs.getString("label"), rs.getLong("n")) },
        userId,
    )

    /**
     * Win/loss against each human opponent the user has faced, most-played first. AI seats are
     * excluded — the head-to-head is about the most common *people* you play against, and the
     * built-in AI would otherwise dominate every casual player's list.
     */
    fun headToHead(userId: UUID): List<HeadToHead> = jdbc.query(
        """
        SELECT COALESCE(u.display_name, opp.player_name) AS opponent,
               opp.user_id AS opponent_user_id,
               bool_or(opp.is_ai) AS is_ai,
               SUM(CASE WHEN me.won THEN 1 ELSE 0 END) AS wins,
               SUM(CASE WHEN me.won THEN 0 ELSE 1 END) AS losses
        FROM match_participants me
        JOIN match_participants opp ON opp.match_id = me.match_id AND opp.id <> me.id
        LEFT JOIN users u ON u.id = opp.user_id
        WHERE me.user_id = ? AND opp.is_ai = false
        GROUP BY COALESCE(u.display_name, opp.player_name), opp.user_id
        ORDER BY count(*) DESC, opponent ASC
        """.trimIndent(),
        { rs, _ ->
            HeadToHead(
                opponent = rs.getString("opponent"),
                opponentUserId = rs.getObject("opponent_user_id", UUID::class.java),
                isAi = rs.getBoolean("is_ai"),
                wins = rs.getLong("wins"),
                losses = rs.getLong("losses"),
            )
        },
        userId,
    )

    /** Total number of games the user has played — drives the recent-games pager. */
    fun recentGamesCount(userId: UUID): Long = jdbc.queryForObject(
        "SELECT count(*) FROM match_participants WHERE user_id = ?",
        Long::class.java,
        userId,
    ) ?: 0

    /**
     * Most-recent games the user played, newest first, one page at a time. Each row's deck colors
     * are recomputed from the stored deck list (see [colorBreakdown]) so the table never shows a
     * real deck as "Colorless" because of a stale `colors` column; when no deck was recorded the
     * colors are left null and the client renders a dash.
     */
    fun recentGames(userId: UUID, limit: Int, offset: Int): List<GameHistoryEntry> {
        data class Row(
            val pid: Long,
            val mid: Long,
            val gameId: String,
            val entry: GameHistoryEntry,
        )
        val rows = jdbc.query(
            """
            SELECT me.id AS pid, me.match_id AS mid, r.ended_at AS ended_at, r.game_mode AS game_mode,
                   r.format AS format, me.won AS won, me.colors AS colors, r.game_id AS game_id,
                   (gr.id IS NOT NULL) AS has_replay
            FROM match_participants me
            JOIN match_results r ON r.id = me.match_id
            LEFT JOIN game_replays gr ON gr.game_id = r.game_id
            WHERE me.user_id = ?
            ORDER BY r.ended_at DESC
            LIMIT ? OFFSET ?
            """.trimIndent(),
            { rs, _ ->
                Row(
                    pid = rs.getLong("pid"),
                    mid = rs.getLong("mid"),
                    gameId = rs.getString("game_id"),
                    entry = GameHistoryEntry(
                        endedAt = rs.getTimestamp("ended_at").toInstant().toString(),
                        gameMode = rs.getString("game_mode"),
                        format = rs.getString("format"),
                        colors = rs.getString("colors"),
                        opponents = null,
                        opponentList = emptyList(),
                        won = rs.getBoolean("won"),
                        selfRating = null,
                        opponentRating = null,
                        gameId = rs.getString("game_id"),
                        hasReplay = rs.getBoolean("has_replay"),
                    ),
                )
            },
            userId, limit, offset,
        )
        val cardsByPid = cardsForParticipants(rows.map { it.pid })
        val opponentsByMid = opponentsForMatches(rows.map { it.mid }, userId)
        val ratingByGame = ratingsForGames(rows.map { it.gameId }, userId)
        return rows.map { row ->
            val cards = cardsByPid[row.pid]
            // Recompute from the recorded deck when we have one; otherwise keep whatever was stored.
            val colors = if (cards.isNullOrEmpty()) row.entry.colors
            else deckProfiler.profile(cards.map { it.cardName }).colors
            val opps = opponentsByMid[row.mid].orEmpty()
            val rating = ratingByGame[row.gameId]
            row.entry.copy(
                colors = colors,
                opponents = opps.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name },
                opponentList = opps,
                selfRating = rating?.first,
                opponentRating = rating?.second,
            )
        }
    }

    /** Opponent seats (everyone but [userId]'s own seat) for a set of match ids, keyed by match id. */
    private fun opponentsForMatches(matchIds: List<Long>, userId: UUID): Map<Long, List<GameOpponent>> {
        if (matchIds.isEmpty()) return emptyMap()
        val placeholders = matchIds.joinToString(",") { "?" }
        val out = LinkedHashMap<Long, MutableList<GameOpponent>>()
        jdbc.query(
            """
            SELECT o.match_id AS mid, COALESCE(u.display_name, o.player_name) AS name,
                   o.user_id AS uid, o.is_ai AS is_ai
            FROM match_participants o
            LEFT JOIN users u ON u.id = o.user_id
            WHERE o.match_id IN ($placeholders) AND (o.user_id IS NULL OR o.user_id <> ?)
            ORDER BY o.id
            """.trimIndent(),
            { rs ->
                out.getOrPut(rs.getLong("mid")) { mutableListOf() }.add(
                    GameOpponent(
                        name = rs.getString("name"),
                        userId = rs.getObject("uid", UUID::class.java),
                        isAi = rs.getBoolean("is_ai"),
                    ),
                )
            },
            *buildList<Any> { addAll(matchIds); add(userId) }.toTypedArray(),
        )
        return out
    }

    /**
     * Each player's pre-game ELO and the opponent's ELO for the given ranked games, keyed by game id.
     * `rating_before` is the rating the player carried into the game and `opponent_rating` is the
     * opponent's rating at that time — i.e. both players' ELO "at the time the game was played".
     * Non-ranked games have no row and are simply absent from the map.
     */
    private fun ratingsForGames(gameIds: List<String>, userId: UUID): Map<String, Pair<Int, Int?>> {
        if (gameIds.isEmpty()) return emptyMap()
        val placeholders = gameIds.joinToString(",") { "?" }
        val out = HashMap<String, Pair<Int, Int?>>()
        jdbc.query(
            """
            SELECT game_id, rating_before, opponent_rating
            FROM rating_history
            WHERE user_id = ? AND game_id IN ($placeholders)
            """.trimIndent(),
            { rs ->
                val gameId = rs.getString("game_id") ?: return@query
                val self = Math.round(rs.getDouble("rating_before")).toInt()
                val oppRaw = rs.getObject("opponent_rating")
                val opp = if (oppRaw == null) null else Math.round((oppRaw as Number).toDouble()).toInt()
                out[gameId] = self to opp
            },
            *buildList<Any> { add(userId); addAll(gameIds) }.toTypedArray(),
        )
        return out
    }

    /**
     * Both seats' decks for one finished game the user played, for the recent-games deck viewer.
     * Returns null when the game isn't one the [userId] participated in (so a user can't read a
     * stranger's deck by guessing game ids). Colors are recomputed per seat from its stored deck.
     */
    fun decksForGame(userId: UUID, gameId: String): GameDecks? {
        data class Seat(val pid: Long, val name: String, val isAi: Boolean, val isSelf: Boolean, val won: Boolean)
        val header = jdbc.query(
            """
            SELECT r.ended_at AS ended_at, r.game_mode AS game_mode
            FROM match_results r
            JOIN match_participants me ON me.match_id = r.id AND me.user_id = ?
            WHERE r.game_id = ?
            LIMIT 1
            """.trimIndent(),
            { rs, _ -> rs.getTimestamp("ended_at").toInstant().toString() to rs.getString("game_mode") },
            userId, gameId,
        ).firstOrNull() ?: return null
        val seats = jdbc.query(
            """
            SELECT p.id AS pid, COALESCE(u.display_name, p.player_name) AS name,
                   p.is_ai AS is_ai, COALESCE(p.user_id = ?, false) AS is_self, p.won AS won
            FROM match_participants p
            JOIN match_results r ON r.id = p.match_id
            LEFT JOIN users u ON u.id = p.user_id
            WHERE r.game_id = ?
            ORDER BY COALESCE(p.user_id = ?, false) DESC, p.id
            """.trimIndent(),
            { rs, _ ->
                Seat(rs.getLong("pid"), rs.getString("name"), rs.getBoolean("is_ai"), rs.getBoolean("is_self"), rs.getBoolean("won"))
            },
            userId, gameId, userId,
        )
        val cardsByPid = cardsForParticipants(seats.map { it.pid })
        return GameDecks(
            gameId = gameId,
            endedAt = header.first,
            gameMode = header.second,
            participants = seats.map { seat ->
                val cards = cardsByPid[seat.pid].orEmpty()
                GameDeckParticipant(
                    playerName = seat.name,
                    isAi = seat.isAi,
                    isSelf = seat.isSelf,
                    won = seat.won,
                    colors = deckProfiler.profile(cards.map { it.cardName }).colors,
                    cards = enrichDeckCards(cards),
                )
            },
        )
    }

    /**
     * The creature subtypes (Goblin, Angel, …) the user plays most, by total copies across every
     * recorded deck. Resolved through the card registry since subtypes aren't stored per card.
     */
    fun creatureTypeBreakdown(userId: UUID, limit: Int): List<StatBucket> {
        val byType = HashMap<String, Long>()
        for ((name, copies) in cardCopiesForUser(userId)) {
            val card = lookupCard(name) ?: continue
            if (!card.typeLine.isCreature) continue
            for (sub in card.typeLine.subtypes) byType.merge(sub.value, copies, Long::plus)
        }
        return byType.entries
            .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { StatBucket(it.key, it.value) }
    }

    /**
     * Distribution of the user's cards across the primary card types (Creature, Instant, Land, …),
     * by total copies. Each card is bucketed once, by its dominant type, so the slices sum to the
     * deck size.
     */
    fun cardTypeBreakdown(userId: UUID): List<StatBucket> {
        val byType = HashMap<String, Long>()
        for ((name, copies) in cardCopiesForUser(userId)) {
            val card = lookupCard(name) ?: continue
            byType.merge(primaryTypeLabel(card.typeLine.cardTypes), copies, Long::plus)
        }
        return byType.entries
            .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
            .map { StatBucket(it.key, it.value) }
    }

    /**
     * Mana-value curve of the user's nonland cards, by total copies, bucketed 0..6 with a final
     * "7+" bin. Lands are excluded — a curve is about what you cast.
     */
    fun manaCurve(userId: UUID): List<StatBucket> {
        val byMv = LongArray(8)
        for ((name, copies) in cardCopiesForUser(userId)) {
            val card = lookupCard(name) ?: continue
            if (card.typeLine.isLand) continue
            byMv[card.manaCost.cmc.coerceIn(0, 7)] += copies
        }
        return byMv.mapIndexed { mv, n -> StatBucket(if (mv == 7) "7+" else mv.toString(), n) }
    }

    // ---- Per-user deck-card helpers ----------------------------------------------------------

    /** Card name -> total copies across every recorded deck of [userId]. */
    private fun cardCopiesForUser(userId: UUID): List<Pair<String, Long>> = jdbc.query(
        """
        SELECT c.card_name AS card_name, sum(c.copies) AS copies
        FROM match_participant_cards c
        JOIN match_participants p ON p.id = c.participant_id
        WHERE p.user_id = ?
        GROUP BY c.card_name
        """.trimIndent(),
        { rs, _ -> rs.getString("card_name") to rs.getLong("copies") },
        userId,
    )

    /** Per-game (participant) deck lists for [userId], keyed by participant id. */
    private fun decksByParticipant(userId: UUID): Map<Long, List<DeckCardEntry>> {
        val out = LinkedHashMap<Long, MutableList<DeckCardEntry>>()
        jdbc.query(
            """
            SELECT c.participant_id AS pid, c.card_name AS card_name, c.copies AS copies
            FROM match_participant_cards c
            JOIN match_participants p ON p.id = c.participant_id
            WHERE p.user_id = ?
            """.trimIndent(),
            { rs ->
                out.getOrPut(rs.getLong("pid")) { mutableListOf() }
                    .add(DeckCardEntry(rs.getString("card_name"), rs.getInt("copies")))
            },
            userId,
        )
        return out
    }

    /** Deck cards for a set of participant ids, keyed by participant id. */
    private fun cardsForParticipants(pids: List<Long>): Map<Long, List<DeckCardEntry>> {
        if (pids.isEmpty()) return emptyMap()
        val placeholders = pids.joinToString(",") { "?" }
        val out = LinkedHashMap<Long, MutableList<DeckCardEntry>>()
        jdbc.query(
            "SELECT participant_id AS pid, card_name, copies FROM match_participant_cards " +
                "WHERE participant_id IN ($placeholders)",
            { rs ->
                out.getOrPut(rs.getLong("pid")) { mutableListOf() }
                    .add(DeckCardEntry(rs.getString("card_name"), rs.getInt("copies")))
            },
            *pids.toTypedArray(),
        )
        return out
    }

    /** Registry lookup tolerant of a "name#collector" pin (mirrors [DeckProfiler]). */
    private fun lookupCard(name: String) =
        cardRegistry.getCard(name) ?: cardRegistry.getCard(name.substringBefore('#'))

    /**
     * Enrich bare `(name, copies)` deck lines with the cost/type/colour the client needs to render a
     * deck the polished way (group by type, mana curve, colour pips). Sorted most-copies-first then
     * alphabetically — a stable order the client regroups for display. Unresolved cards keep sensible
     * zero/empty defaults so a deck with an unknown card still renders.
     */
    fun enrichDeckCards(cards: List<DeckCardEntry>): List<GameDeckCard> =
        cards.sortedWith(compareByDescending<DeckCardEntry> { it.copies }.thenBy { it.cardName })
            .map { e ->
                val card = lookupCard(e.cardName)
                GameDeckCard(
                    cardName = e.cardName,
                    copies = e.copies,
                    cmc = card?.cmc ?: 0,
                    cardTypes = card?.typeLine?.cardTypes?.map { it.name } ?: emptyList(),
                    colors = card?.colors?.map { it.name } ?: emptyList(),
                    // Prefer the default printing's art (mirrors CardsController), falling back to the
                    // canonical metadata image — so the deck viewer loads from the CDN directly.
                    imageUri = card?.let {
                        printingRegistry.defaultPrinting(it.name)?.imageUri ?: it.metadata.imageUri
                    },
                )
            }

    /** Enrich a name→copies map (a stored [SharedDeck]'s card list) for the saved-deck viewer. */
    fun enrichDeck(cardCounts: Map<String, Int>): List<GameDeckCard> =
        enrichDeckCards(cardCounts.map { (name, copies) -> DeckCardEntry(name, copies) })

    /** The single dominant card type used to bucket a card in [cardTypeBreakdown]. */
    private fun primaryTypeLabel(types: Set<CardType>): String {
        val order = listOf(
            CardType.CREATURE, CardType.PLANESWALKER, CardType.INSTANT, CardType.SORCERY,
            CardType.ARTIFACT, CardType.ENCHANTMENT, CardType.LAND,
        )
        return order.firstOrNull { it in types }?.displayName ?: "Other"
    }

    /**
     * The user's most-played cards across all their recorded decks. Basic lands are excluded — every
     * deck runs a pile of them, so they'd otherwise crowd out the cards that actually characterize how
     * the player builds. Filtering is done after the group-by (the registry knows which names are
     * basics), so [limit] applies to the non-basic results.
     */
    fun topCardsForUser(userId: UUID, limit: Int): List<CardStat> = jdbc.query(
        """
        SELECT c.card_name AS card_name, sum(c.copies) AS copies, count(*) AS decks
        FROM match_participant_cards c
        JOIN match_participants p ON p.id = c.participant_id
        WHERE p.user_id = ?
        GROUP BY c.card_name
        ORDER BY copies DESC, decks DESC
        """.trimIndent(),
        { rs, _ -> CardStat(rs.getString("card_name"), rs.getLong("copies"), rs.getLong("decks")) },
        userId,
    ).filterNot { lookupCard(it.cardName)?.typeLine?.isBasicLand == true }
        .take(limit)
        // Resolve each card's art the same way as the deck viewer (default printing → canonical
        // metadata) so the most-played-cards section can show a hover preview without a Scryfall hit.
        .map { it.copy(imageUri = imageUriFor(it.cardName)) }

    /** Default-printing art URL for a card name, or null when it can't be resolved (mirrors [enrichDeckCards]). */
    private fun imageUriFor(name: String): String? =
        lookupCard(name)?.let { printingRegistry.defaultPrinting(it.name)?.imageUri ?: it.metadata.imageUri }

    /** The user's tournaments, newest first — including any in-progress or abandoned ones. */
    fun tournamentHistory(userId: UUID, limit: Int): List<UserTournamentEntry> = jdbc.query(
        """
        SELECT t.id AS id, COALESCE(t.ended_at, t.started_at, now()) AS ended_at, t.name AS name,
               t.format AS format, t.game_mode AS game_mode, tp.placement AS placement,
               t.player_count AS player_count, t.status AS status
        FROM tournament_participants tp
        JOIN tournaments t ON t.id = tp.tournament_id
        WHERE tp.user_id = ?
        ORDER BY COALESCE(t.ended_at, t.started_at, now()) DESC
        LIMIT ?
        """.trimIndent(),
        { rs, _ ->
            UserTournamentEntry(
                id = rs.getLong("id"),
                endedAt = rs.getTimestamp("ended_at").toInstant().toString(),
                name = rs.getString("name"),
                format = rs.getString("format"),
                gameMode = rs.getString("game_mode"),
                placement = rs.getInt("placement"),
                playerCount = rs.getInt("player_count"),
                status = rs.getString("status"),
            )
        },
        userId, limit,
    )

    /**
     * Full public detail for one tournament: every participant's standing plus every game played in it
     * (found by the lobby id the games were recorded under), each with replay availability. Returns
     * null when no tournament has that id. Games are empty for tournaments recorded before games
     * carried a lobby id.
     */
    fun tournamentDetail(id: Long): TournamentDetail? {
        val header = jdbc.query(
            """
            SELECT id, lobby_id, name, format, game_mode, set_codes, player_count, winner_name,
                   COALESCE(ended_at, started_at, now()) AS ended_at, status
            FROM tournaments WHERE id = ?
            """.trimIndent(),
            { rs, _ ->
                TournamentDetail(
                    id = rs.getLong("id"),
                    name = rs.getString("name"),
                    format = rs.getString("format"),
                    gameMode = rs.getString("game_mode"),
                    setCodes = rs.getString("set_codes"),
                    playerCount = rs.getInt("player_count"),
                    winnerName = rs.getString("winner_name"),
                    endedAt = rs.getTimestamp("ended_at").toInstant().toString(),
                    status = rs.getString("status"),
                    standings = emptyList(),
                    games = emptyList(),
                ) to rs.getString("lobby_id")
            },
            id,
        ).firstOrNull() ?: return null
        val (detail, lobbyId) = header

        val standings = jdbc.query(
            """
            SELECT tp.placement AS placement, COALESCE(u.display_name, tp.player_name) AS name,
                   tp.user_id AS uid, tp.is_ai AS is_ai, tp.wins AS wins, tp.losses AS losses, tp.draws AS draws
            FROM tournament_participants tp
            LEFT JOIN users u ON u.id = tp.user_id
            WHERE tp.tournament_id = ?
            ORDER BY tp.placement ASC, name ASC
            """.trimIndent(),
            { rs, _ ->
                TournamentStanding(
                    placement = rs.getInt("placement"),
                    playerName = rs.getString("name"),
                    userId = rs.getObject("uid", UUID::class.java),
                    isAi = rs.getBoolean("is_ai"),
                    wins = rs.getInt("wins"),
                    losses = rs.getInt("losses"),
                    draws = rs.getInt("draws"),
                )
            },
            id,
        )

        val games = if (lobbyId.isNullOrEmpty()) emptyList() else tournamentGames(lobbyId)
        return detail.copy(standings = standings, games = games)
    }

    /** Every recorded game for a lobby (the tournament's bracket games), oldest first, with seats. */
    private fun tournamentGames(lobbyId: String): List<TournamentGame> {
        data class GameRow(val mid: Long, val gameId: String, val endedAt: String, val hasReplay: Boolean)
        val games = jdbc.query(
            """
            SELECT r.id AS mid, r.game_id AS game_id, r.ended_at AS ended_at, (gr.id IS NOT NULL) AS has_replay
            FROM match_results r
            LEFT JOIN game_replays gr ON gr.game_id = r.game_id
            WHERE r.lobby_id = ?
            ORDER BY r.ended_at ASC
            """.trimIndent(),
            { rs, _ ->
                GameRow(rs.getLong("mid"), rs.getString("game_id"), rs.getTimestamp("ended_at").toInstant().toString(), rs.getBoolean("has_replay"))
            },
            lobbyId,
        )
        if (games.isEmpty()) return emptyList()
        val placeholders = games.joinToString(",") { "?" }
        val playersByMid = LinkedHashMap<Long, MutableList<TournamentGamePlayer>>()
        jdbc.query(
            """
            SELECT p.match_id AS mid, COALESCE(u.display_name, p.player_name) AS name,
                   p.user_id AS uid, p.is_ai AS is_ai, p.won AS won
            FROM match_participants p
            LEFT JOIN users u ON u.id = p.user_id
            WHERE p.match_id IN ($placeholders)
            ORDER BY p.id
            """.trimIndent(),
            { rs ->
                playersByMid.getOrPut(rs.getLong("mid")) { mutableListOf() }.add(
                    TournamentGamePlayer(
                        name = rs.getString("name"),
                        userId = rs.getObject("uid", UUID::class.java),
                        isAi = rs.getBoolean("is_ai"),
                        won = rs.getBoolean("won"),
                    ),
                )
            },
            *games.map { it.mid }.toTypedArray(),
        )
        return games.map { TournamentGame(it.gameId, it.endedAt, it.hasReplay, playersByMid[it.mid].orEmpty()) }
    }

    // ---- Global (admin) ----------------------------------------------------------------------

    /**
     * Every registered account with its lifetime game/win counts and last-played time, most-active
     * first. A LEFT JOIN keeps accounts that have never played (they show 0 games). Drives the admin
     * Players list; per-account detail reuses the per-user methods above.
     */
    fun allUsersWithStats(): List<AdminUserStat> = jdbc.query(
        """
        SELECT u.id AS id, u.email AS email, u.display_name AS display_name, u.is_admin AS is_admin,
               u.created_at AS created_at,
               count(p.id) AS games,
               count(p.id) FILTER (WHERE p.won) AS wins,
               max(r.ended_at) AS last_played
        FROM users u
        LEFT JOIN match_participants p ON p.user_id = u.id
        LEFT JOIN match_results r ON r.id = p.match_id
        GROUP BY u.id, u.email, u.display_name, u.is_admin, u.created_at
        ORDER BY games DESC, u.created_at ASC
        """.trimIndent(),
    ) { rs, _ ->
        AdminUserStat(
            id = rs.getObject("id", UUID::class.java),
            email = rs.getString("email"),
            displayName = rs.getString("display_name"),
            isAdmin = rs.getBoolean("is_admin"),
            createdAt = rs.getTimestamp("created_at").toInstant().toString(),
            games = rs.getLong("games"),
            wins = rs.getLong("wins"),
            lastPlayed = rs.getTimestamp("last_played")?.toInstant()?.toString(),
        )
    }

    fun overview(): GlobalOverview {
        val totalGames = jdbc.queryForObject("SELECT count(*) FROM match_results", Long::class.java) ?: 0
        val totalAccounts = jdbc.queryForObject(
            "SELECT count(DISTINCT user_id) FROM match_participants WHERE user_id IS NOT NULL",
            Long::class.java,
        ) ?: 0
        val totalPlayers = jdbc.queryForObject(
            """
            SELECT count(DISTINCT COALESCE(CAST(user_id AS TEXT), 'guest:' || player_name))
            FROM match_participants WHERE is_ai = false
            """.trimIndent(),
            Long::class.java,
        ) ?: 0
        val games24h = jdbc.queryForObject(
            "SELECT count(*) FROM match_results WHERE ended_at >= now() - interval '24 hours'",
            Long::class.java,
        ) ?: 0
        val games7d = jdbc.queryForObject(
            "SELECT count(*) FROM match_results WHERE ended_at >= now() - interval '7 days'",
            Long::class.java,
        ) ?: 0
        val totalTournaments = jdbc.queryForObject("SELECT count(*) FROM tournaments", Long::class.java) ?: 0
        return GlobalOverview(totalGames, totalPlayers, totalAccounts, totalTournaments, games24h, games7d)
    }

    /** Games per calendar day (UTC) over the last [days] days, oldest first. */
    fun gamesPerDay(days: Int): List<DailyCount> = jdbc.query(
        """
        SELECT to_char(date_trunc('day', ended_at), 'YYYY-MM-DD') AS day, count(*) AS n
        FROM match_results
        WHERE ended_at >= now() - (? * interval '1 day')
        GROUP BY date_trunc('day', ended_at)
        ORDER BY date_trunc('day', ended_at)
        """.trimIndent(),
        { rs, _ -> DailyCount(rs.getString("day"), rs.getLong("n")) },
        days,
    )

    fun modeDistribution(): List<StatBucket> = jdbc.query(
        """
        SELECT COALESCE(game_mode, 'UNKNOWN') AS label, count(*) AS n
        FROM match_results
        GROUP BY COALESCE(game_mode, 'UNKNOWN')
        ORDER BY n DESC
        """.trimIndent(),
    ) { rs, _ -> StatBucket(rs.getString("label"), rs.getLong("n")) }

    fun colorDistribution(): List<StatBucket> = jdbc.query(
        """
        SELECT COALESCE(colors, '') AS label, count(*) AS n
        FROM match_participants
        WHERE is_ai = false
        GROUP BY COALESCE(colors, '')
        ORDER BY n DESC
        """.trimIndent(),
    ) { rs, _ -> StatBucket(rs.getString("label"), rs.getLong("n")) }

    /** Distinct connecting IPs and their game counts (admin-only; for geolocation). */
    fun ipBreakdown(): List<IpCount> = jdbc.query(
        """
        SELECT client_ip AS ip, count(*) AS n
        FROM match_participants
        WHERE is_ai = false AND client_ip IS NOT NULL AND client_ip <> ''
        GROUP BY client_ip
        ORDER BY n DESC
        """.trimIndent(),
    ) { rs, _ -> IpCount(rs.getString("ip"), rs.getLong("n")) }

    /**
     * Most-played cards across every recorded deck. Basic lands are excluded — every deck runs a pile
     * of them, so they'd otherwise crowd out the cards that actually characterize the metagame (same
     * reasoning as [topCardsForUser]). Filtering is done after the group-by, so [limit] applies to the
     * non-basic results.
     */
    fun topCards(limit: Int): List<CardStat> = jdbc.query(
        """
        SELECT card_name, sum(copies) AS copies, count(*) AS decks
        FROM match_participant_cards
        GROUP BY card_name
        ORDER BY copies DESC, decks DESC
        """.trimIndent(),
        { rs, _ -> CardStat(rs.getString("card_name"), rs.getLong("copies"), rs.getLong("decks")) },
    ).filterNot { lookupCard(it.cardName)?.typeLine?.isBasicLand == true }.take(limit)

    /**
     * Win rate per card: of the decks containing the card, how many won. Restricted to cards seen in
     * at least [minDecks] decks so a single lucky game can't top the chart. Basic lands are excluded
     * (see [topCards]) — they appear in nearly every deck and say nothing about a card's win impact.
     */
    fun cardWinRates(minDecks: Int, limit: Int): List<CardWinRate> = jdbc.query(
        """
        SELECT c.card_name AS card_name,
               count(*) AS decks,
               count(*) FILTER (WHERE p.won) AS wins
        FROM match_participant_cards c
        JOIN match_participants p ON p.id = c.participant_id
        GROUP BY c.card_name
        HAVING count(*) >= ?
        ORDER BY (count(*) FILTER (WHERE p.won))::float / count(*) DESC, decks DESC
        """.trimIndent(),
        { rs, _ ->
            val decks = rs.getLong("decks")
            val wins = rs.getLong("wins")
            CardWinRate(rs.getString("card_name"), decks, wins, if (decks > 0) wins.toDouble() / decks else 0.0)
        },
        minDecks,
    ).filterNot { lookupCard(it.cardName)?.typeLine?.isBasicLand == true }.take(limit)

    /** Recorded tournaments, newest first — including in-progress and abandoned ones. */
    fun recentTournaments(limit: Int): List<TournamentSummary> = jdbc.query(
        """
        SELECT id, COALESCE(ended_at, started_at, now()) AS ended_at, name, format, game_mode,
               player_count, winner_name, status
        FROM tournaments
        ORDER BY COALESCE(ended_at, started_at, now()) DESC
        LIMIT ?
        """.trimIndent(),
        { rs, _ ->
            TournamentSummary(
                id = rs.getLong("id"),
                endedAt = rs.getTimestamp("ended_at").toInstant().toString(),
                name = rs.getString("name"),
                format = rs.getString("format"),
                gameMode = rs.getString("game_mode"),
                playerCount = rs.getInt("player_count"),
                winnerName = rs.getString("winner_name"),
                status = rs.getString("status"),
            )
        },
        limit,
    )

    /** Total number of recorded games across every player — drives the admin global game pager. */
    fun recentGamesGlobalCount(): Long = jdbc.queryForObject(
        "SELECT count(*) FROM match_results",
        Long::class.java,
    ) ?: 0

    /**
     * The most-recent games across every player, newest first, one page at a time. Unlike the
     * per-user [recentGames] this is neutral — it lists every seat (winner flagged) rather than a
     * single viewer's perspective — and tags each game with its tournament name when it came from one.
     */
    fun recentGamesGlobal(limit: Int, offset: Int): List<AdminRecentGame> {
        data class Row(
            val mid: Long,
            val gameId: String,
            val endedAt: String,
            val gameMode: String?,
            val format: String?,
            val lobbyId: String?,
            val hasReplay: Boolean,
        )
        val rows = jdbc.query(
            """
            SELECT r.id AS mid, r.game_id AS game_id, r.ended_at AS ended_at, r.game_mode AS game_mode,
                   r.format AS format, r.lobby_id AS lobby_id, (gr.id IS NOT NULL) AS has_replay
            FROM match_results r
            LEFT JOIN game_replays gr ON gr.game_id = r.game_id
            ORDER BY r.ended_at DESC
            LIMIT ? OFFSET ?
            """.trimIndent(),
            { rs, _ ->
                Row(
                    mid = rs.getLong("mid"),
                    gameId = rs.getString("game_id"),
                    endedAt = rs.getTimestamp("ended_at").toInstant().toString(),
                    gameMode = rs.getString("game_mode"),
                    format = rs.getString("format"),
                    lobbyId = rs.getString("lobby_id"),
                    hasReplay = rs.getBoolean("has_replay"),
                )
            },
            limit, offset,
        )
        if (rows.isEmpty()) return emptyList()
        val playersByMid = playersForMatches(rows.map { it.mid })
        val tournamentNames = tournamentNamesByLobby(rows.mapNotNull { it.lobbyId })
        return rows.map { row ->
            val players = playersByMid[row.mid].orEmpty()
            AdminRecentGame(
                gameId = row.gameId,
                endedAt = row.endedAt,
                gameMode = row.gameMode,
                format = row.format,
                players = players,
                winnerName = players.firstOrNull { it.won }?.name,
                hasReplay = row.hasReplay,
                tournamentName = row.lobbyId?.let { tournamentNames[it] },
            )
        }
    }

    /**
     * Every seat for a set of match ids, keyed by match id (seat order preserved). Each seat's
     * recorded IP is resolved to a coarse location via [GeoIpService] (batched across all seats,
     * cached in-process); the raw IP stays server-side and only the location reaches the DTO.
     */
    private fun playersForMatches(matchIds: List<Long>): Map<Long, List<AdminGamePlayer>> {
        if (matchIds.isEmpty()) return emptyMap()
        val placeholders = matchIds.joinToString(",") { "?" }
        data class Seat(
            val mid: Long,
            val name: String,
            val userId: UUID?,
            val isAi: Boolean,
            val won: Boolean,
            val ip: String?,
        )
        val seats = jdbc.query(
            """
            SELECT p.match_id AS mid, COALESCE(u.display_name, p.player_name) AS name,
                   p.user_id AS uid, p.is_ai AS is_ai, p.won AS won, p.client_ip AS client_ip
            FROM match_participants p
            LEFT JOIN users u ON u.id = p.user_id
            WHERE p.match_id IN ($placeholders)
            ORDER BY p.id
            """.trimIndent(),
            { rs, _ ->
                Seat(
                    mid = rs.getLong("mid"),
                    name = rs.getString("name"),
                    userId = rs.getObject("uid", UUID::class.java),
                    isAi = rs.getBoolean("is_ai"),
                    won = rs.getBoolean("won"),
                    ip = rs.getString("client_ip")?.takeIf { it.isNotBlank() },
                )
            },
            *matchIds.toTypedArray(),
        )
        val locations = geoIp.resolve(seats.mapNotNull { it.ip })
        val out = LinkedHashMap<Long, MutableList<AdminGamePlayer>>()
        for (seat in seats) {
            out.getOrPut(seat.mid) { mutableListOf() }.add(
                AdminGamePlayer(
                    name = seat.name,
                    userId = seat.userId,
                    isAi = seat.isAi,
                    won = seat.won,
                    location = seat.ip?.let { locations[it] },
                ),
            )
        }
        return out
    }

    /** Tournament name for each lobby id that has one, keyed by lobby id. */
    private fun tournamentNamesByLobby(lobbyIds: List<String>): Map<String, String> {
        val distinct = lobbyIds.distinct()
        if (distinct.isEmpty()) return emptyMap()
        val placeholders = distinct.joinToString(",") { "?" }
        val out = HashMap<String, String>()
        jdbc.query(
            "SELECT lobby_id, name FROM tournaments WHERE lobby_id IN ($placeholders)",
            { rs ->
                val lobby = rs.getString("lobby_id")
                val name = rs.getString("name")
                if (lobby != null && name != null) out[lobby] = name
            },
            *distinct.toTypedArray(),
        )
        return out
    }
}
