package com.wingedsheep.gameserver.stats

import com.fasterxml.jackson.annotation.JsonProperty
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

/** One row in a user's game history. */
data class GameHistoryEntry(
    val endedAt: String,
    val gameMode: String?,
    val format: String?,
    val colors: String?,
    val opponents: String?,
    val won: Boolean,
    /** Stable game id, used to open/share the replay. */
    val gameId: String,
    /** True when a compact replay was stored for this game and can be watched/shared. */
    val hasReplay: Boolean,
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

/** A card and how often it has appeared in decks. */
data class CardStat(val cardName: String, val copies: Long, val decks: Long)

/** A card's win rate: games (decks containing it) and how many of those decks won. */
data class CardWinRate(val cardName: String, val decks: Long, val wins: Long, val winRate: Double)

/** One tournament in a user's tournament history. */
data class UserTournamentEntry(
    val endedAt: String,
    val name: String?,
    val format: String?,
    val gameMode: String?,
    val placement: Int,
    val playerCount: Int,
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

/** A recorded tournament for the admin list. */
data class TournamentSummary(
    val endedAt: String,
    val name: String?,
    val format: String?,
    val gameMode: String?,
    val playerCount: Int,
    val winnerName: String?,
)

/**
 * Aggregate / group-by stat queries over the match-history tables, via [JdbcTemplate]. These are
 * awkward to express through Spring Data JDBC's entity mapping, so they live here as plain SQL. The
 * bean only exists when accounts are enabled (a DataSource is present then). Per-user methods filter
 * by `user_id`; the global methods span every recorded game (guests included, AI excluded).
 */
@Service
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class StatsQueryService(private val jdbc: JdbcTemplate) {

    // ---- Per-user ----------------------------------------------------------------------------

    /** How often the user has played each color identity (e.g. "WU"), most-played first. */
    fun colorBreakdown(userId: UUID): List<StatBucket> = jdbc.query(
        """
        SELECT COALESCE(colors, '') AS label, count(*) AS n
        FROM match_participants
        WHERE user_id = ?
        GROUP BY COALESCE(colors, '')
        ORDER BY n DESC
        """.trimIndent(),
        { rs, _ -> StatBucket(rs.getString("label"), rs.getLong("n")) },
        userId,
    )

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

    /** How often the user has played each game mode, most-played first. */
    fun modeBreakdown(userId: UUID): List<StatBucket> = jdbc.query(
        """
        SELECT COALESCE(r.game_mode, 'UNKNOWN') AS label, count(*) AS n
        FROM match_participants p
        JOIN match_results r ON r.id = p.match_id
        WHERE p.user_id = ?
        GROUP BY COALESCE(r.game_mode, 'UNKNOWN')
        ORDER BY n DESC
        """.trimIndent(),
        { rs, _ -> StatBucket(rs.getString("label"), rs.getLong("n")) },
        userId,
    )

    /** Win/loss against each opponent the user has faced, most-played first. */
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
        WHERE me.user_id = ?
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

    /** Most-recent games the user played, newest first. */
    fun recentGames(userId: UUID, limit: Int, offset: Int): List<GameHistoryEntry> = jdbc.query(
        """
        SELECT r.ended_at AS ended_at, r.game_mode AS game_mode, r.format AS format,
               me.won AS won, me.colors AS colors, r.game_id AS game_id,
               (gr.id IS NOT NULL) AS has_replay,
               (SELECT string_agg(COALESCE(u2.display_name, o.player_name), ', ')
                  FROM match_participants o
                  LEFT JOIN users u2 ON u2.id = o.user_id
                  WHERE o.match_id = r.id AND o.id <> me.id) AS opponents
        FROM match_participants me
        JOIN match_results r ON r.id = me.match_id
        LEFT JOIN game_replays gr ON gr.game_id = r.game_id
        WHERE me.user_id = ?
        ORDER BY r.ended_at DESC
        LIMIT ? OFFSET ?
        """.trimIndent(),
        { rs, _ ->
            GameHistoryEntry(
                endedAt = rs.getTimestamp("ended_at").toInstant().toString(),
                gameMode = rs.getString("game_mode"),
                format = rs.getString("format"),
                colors = rs.getString("colors"),
                opponents = rs.getString("opponents"),
                won = rs.getBoolean("won"),
                gameId = rs.getString("game_id"),
                hasReplay = rs.getBoolean("has_replay"),
            )
        },
        userId, limit, offset,
    )

    /** The user's most-played cards across all their recorded decks. */
    fun topCardsForUser(userId: UUID, limit: Int): List<CardStat> = jdbc.query(
        """
        SELECT c.card_name AS card_name, sum(c.copies) AS copies, count(*) AS decks
        FROM match_participant_cards c
        JOIN match_participants p ON p.id = c.participant_id
        WHERE p.user_id = ?
        GROUP BY c.card_name
        ORDER BY copies DESC, decks DESC
        LIMIT ?
        """.trimIndent(),
        { rs, _ -> CardStat(rs.getString("card_name"), rs.getLong("copies"), rs.getLong("decks")) },
        userId, limit,
    )

    /** The user's tournament finishes, newest first. */
    fun tournamentHistory(userId: UUID, limit: Int): List<UserTournamentEntry> = jdbc.query(
        """
        SELECT t.ended_at AS ended_at, t.name AS name, t.format AS format, t.game_mode AS game_mode,
               tp.placement AS placement, t.player_count AS player_count
        FROM tournament_participants tp
        JOIN tournaments t ON t.id = tp.tournament_id
        WHERE tp.user_id = ?
        ORDER BY t.ended_at DESC
        LIMIT ?
        """.trimIndent(),
        { rs, _ ->
            UserTournamentEntry(
                endedAt = rs.getTimestamp("ended_at").toInstant().toString(),
                name = rs.getString("name"),
                format = rs.getString("format"),
                gameMode = rs.getString("game_mode"),
                placement = rs.getInt("placement"),
                playerCount = rs.getInt("player_count"),
            )
        },
        userId, limit,
    )

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

    /** Most-played cards across every recorded deck. */
    fun topCards(limit: Int): List<CardStat> = jdbc.query(
        """
        SELECT card_name, sum(copies) AS copies, count(*) AS decks
        FROM match_participant_cards
        GROUP BY card_name
        ORDER BY copies DESC, decks DESC
        LIMIT ?
        """.trimIndent(),
        { rs, _ -> CardStat(rs.getString("card_name"), rs.getLong("copies"), rs.getLong("decks")) },
        limit,
    )

    /**
     * Win rate per card: of the decks containing the card, how many won. Restricted to cards seen in
     * at least [minDecks] decks so a single lucky game can't top the chart.
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
        LIMIT ?
        """.trimIndent(),
        { rs, _ ->
            val decks = rs.getLong("decks")
            val wins = rs.getLong("wins")
            CardWinRate(rs.getString("card_name"), decks, wins, if (decks > 0) wins.toDouble() / decks else 0.0)
        },
        minDecks, limit,
    )

    /** Recorded tournaments, newest first. */
    fun recentTournaments(limit: Int): List<TournamentSummary> = jdbc.query(
        """
        SELECT ended_at, name, format, game_mode, player_count, winner_name
        FROM tournaments
        ORDER BY ended_at DESC
        LIMIT ?
        """.trimIndent(),
        { rs, _ ->
            TournamentSummary(
                endedAt = rs.getTimestamp("ended_at").toInstant().toString(),
                name = rs.getString("name"),
                format = rs.getString("format"),
                gameMode = rs.getString("game_mode"),
                playerCount = rs.getInt("player_count"),
                winnerName = rs.getString("winner_name"),
            )
        },
        limit,
    )
}
