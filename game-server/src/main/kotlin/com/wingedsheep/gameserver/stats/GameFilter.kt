package com.wingedsheep.gameserver.stats

/**
 * Who a game was played against, from one seat's point of view. A multiplayer game with both human
 * and AI opponents is neither [HUMANS] nor [AI] — it shows only under [ALL].
 */
enum class OpponentFilter {
    ALL,

    /** Every opponent was a person (signed-in or guest). */
    HUMANS,

    /** Every opponent was the built-in AI. */
    AI;

    companion object {
        /** `humans` / `ai` (any case); anything else — including null — is [ALL]. */
        fun parse(value: String?): OpponentFilter = when (value?.trim()?.lowercase()) {
            "humans", "human" -> HUMANS
            "ai" -> AI
            else -> ALL
        }
    }
}

/**
 * The slice of recorded games a stats view shows: who they were against and how recent. Every view
 * also leaves out seats whose result doesn't count ([StatsEligibility]) unless it lists games, where
 * they are shown and marked instead.
 *
 * Renders as a SQL predicate over one `match_participants` alias via [seatSql]; the arguments it binds
 * come back alongside, in order, so callers splice both into their own query.
 */
data class GameFilter(
    val opponents: OpponentFilter = OpponentFilter.ALL,
    /** Only games that ended in the last [days] days; null = all time. */
    val days: Int? = null,
) {
    /** A predicate fragment plus the arguments it binds, in order. */
    data class Sql(val clause: String, val args: List<Any>)

    /**
     * The predicate for seat alias [seat] (a `match_participants` row). [countedOnly] drops seats whose
     * result doesn't count; game lists pass false and mark those rows instead.
     */
    fun seatSql(seat: String, countedOnly: Boolean = true): Sql {
        val parts = mutableListOf<String>()
        val args = mutableListOf<Any>()
        if (countedOnly) parts += "$seat.counts_in_stats"
        when (opponents) {
            OpponentFilter.ALL -> Unit
            OpponentFilter.HUMANS -> parts += "NOT EXISTS (SELECT 1 FROM match_participants gf_o " +
                "WHERE gf_o.match_id = $seat.match_id AND gf_o.id <> $seat.id AND gf_o.is_ai)"
            OpponentFilter.AI -> parts += "NOT EXISTS (SELECT 1 FROM match_participants gf_o " +
                "WHERE gf_o.match_id = $seat.match_id AND gf_o.id <> $seat.id AND NOT gf_o.is_ai)"
        }
        if (days != null) {
            parts += "EXISTS (SELECT 1 FROM match_results gf_r WHERE gf_r.id = $seat.match_id " +
                "AND gf_r.ended_at >= now() - (? * interval '1 day'))"
            args += days
        }
        return Sql(if (parts.isEmpty()) "true" else parts.joinToString(" AND "), args)
    }

    companion object {
        val ALL_TIME = GameFilter()

        /** From request parameters: `vs` = `all` / `humans` / `ai`, `days` (0 or absent = all time). */
        fun of(vs: String?, days: Int?): GameFilter =
            GameFilter(OpponentFilter.parse(vs), days?.takeIf { it > 0 }?.coerceAtMost(3650))
    }
}

/** A win/loss count. Draws (no winner) count as losses, as they always have here. */
data class WinLoss(val games: Long, val wins: Long, val losses: Long, val winRate: Double) {
    companion object {
        fun of(games: Long, wins: Long) =
            WinLoss(games, wins, games - wins, if (games > 0) wins.toDouble() / games else 0.0)
    }
}

/**
 * A player's record under a [GameFilter]: the counted games, the same period split by who they were
 * against (whatever the filter's opponents), and how many games in the slice don't count.
 */
data class PlayerRecord(
    val games: Long,
    val wins: Long,
    val losses: Long,
    val winRate: Double,
    /** Counted games in the period where every opponent was a person. */
    val vsHumans: WinLoss,
    /** Counted games in the period where every opponent was the AI. */
    val vsAi: WinLoss,
    /** Games in the slice whose result doesn't count — someone never took an action ([StatsEligibility]). */
    val notCounted: Long,
)

/**
 * The [GameFilter] predicate over a whole game (`match_results` alias [match]) rather than one seat,
 * for the admin's global views. A game counts when any seat's result does; [OpponentFilter.HUMANS] is
 * a game with no AI seat, [OpponentFilter.AI] one where a single person played the AI — the games the
 * per-seat filter puts under those names from the human's side.
 */
fun GameFilter.matchSql(match: String, countedOnly: Boolean = true): GameFilter.Sql {
    val parts = mutableListOf<String>()
    if (countedOnly) parts += countedMatchSql(match)
    val args = mutableListOf<Any>()
    when (opponents) {
        OpponentFilter.ALL -> Unit
        OpponentFilter.HUMANS -> parts += "NOT EXISTS (SELECT 1 FROM match_participants gf_a " +
            "WHERE gf_a.match_id = $match.id AND gf_a.is_ai)"
        OpponentFilter.AI -> parts += "EXISTS (SELECT 1 FROM match_participants gf_a " +
            "WHERE gf_a.match_id = $match.id AND gf_a.is_ai) AND (SELECT count(*) FROM match_participants gf_h " +
            "WHERE gf_h.match_id = $match.id AND NOT gf_h.is_ai) = 1"
    }
    if (days != null) {
        parts += "$match.ended_at >= now() - (? * interval '1 day')"
        args += days
    }
    return GameFilter.Sql(if (parts.isEmpty()) "true" else parts.joinToString(" AND "), args)
}

/** A game at least one seat's result counts in — see [StatsEligibility]. */
fun countedMatchSql(match: String): String =
    "EXISTS (SELECT 1 FROM match_participants gf_c WHERE gf_c.match_id = $match.id AND gf_c.counts_in_stats)"
