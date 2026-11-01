package com.wingedsheep.gameserver.stats

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import kotlin.math.sqrt

/**
 * The admin "AI vs humans" report: how the built-in AI fares against human players, over time, per
 * deployed build and per game mode — the number to watch while improving the AI.
 *
 * The population is every recorded game with at least one AI seat *and* one human seat. Each game has
 * one [AiMatchOutcome]: the AI won (only AI seats won), the humans won (only human seats won), or a
 * draw — no winner, or an AI and a human that won together (a mixed Two-Headed Giant team). Win rates
 * are over *decisive* games only, so draws neither help nor hurt the AI.
 */
enum class AiMatchOutcome { AI, HUMAN, DRAW }

/** One AI-vs-human game, as read from the match-history tables. */
data class AiMatchGame(
    val matchId: Long,
    val endedAt: Instant,
    val gameMode: String?,
    val format: String?,
    /** The deploy that played it (`game_replays.engine_version`); null when no replay was stored. */
    val engineVersion: String?,
    val outcome: AiMatchOutcome,
    /**
     * The sets the game's card pool came from — the draft/sealed sets of the lobby or tournament that
     * produced it. Empty for games outside a set-based lobby (quick games, constructed, cubes), whose
     * decks span whatever sets their cards' canonical printings sit in.
     */
    val setCodes: List<String> = emptyList(),
    /** The colour identity of each AI seat's deck (canonical WUBRG, "" colourless); null when none was recorded. */
    val aiColors: List<String?> = emptyList(),
    /** `match_results.game_id` when the game's replay is stored, so the report can link to it; else null. */
    val replayGameId: String? = null,
)

/**
 * Narrows the [AiVsHumanReport] to a slice of the games. Every aggregate — overall, previous window,
 * series, per-mode, per-build, recent, humans — is computed over the games that pass it.
 *
 * Humans are excluded by *game*: a game any excluded human sat in drops out entirely, so a pod with a
 * heavy player in it doesn't leave a half-counted record behind. [excludeTopHumans] picks the humans
 * with the most games in the range (after the mode/format/set filters), so "discount the regulars"
 * stays meaningful as the range or the slice changes.
 */
data class AiVsHumanFilter(
    val gameMode: String? = null,
    val format: String? = null,
    val setCode: String? = null,
    /** [AiMatchHumanSeat.playerKey]s to leave out. */
    val excludeHumans: Set<String> = emptySet(),
    val excludeTopHumans: Int = 0,
) {
    fun matchesSlice(game: AiMatchGame): Boolean =
        (gameMode == null || game.gameMode == gameMode) &&
            (format == null || game.format == format) &&
            (setCode == null || setCode in game.setCodes)

    companion object {
        val NONE = AiVsHumanFilter()
    }
}

/** A human seat in an AI-vs-human game. [playerKey] is the account id, or the guest's name. */
data class AiMatchHumanSeat(
    val matchId: Long,
    val playerKey: String,
    val displayName: String,
    val isGuest: Boolean,
    val won: Boolean,
    /** The seat's deck colour identity (canonical WUBRG, "" colourless); null when none was recorded. */
    val colors: String? = null,
)

/** A win/loss/draw tally from the AI's side, with its decisive win rate and a 95% Wilson interval. */
data class AiRecord(val games: Long, val aiWins: Long, val humanWins: Long, val draws: Long) {
    private val decisive: Long get() = aiWins + humanWins

    /** AI wins over decisive games; null when there were none. */
    val aiWinRate: Double? get() = if (decisive == 0L) null else aiWins.toDouble() / decisive

    /** Lower bound of the 95% Wilson score interval around [aiWinRate]. */
    val aiWinRateLow: Double? get() = wilson(-1)

    /** Upper bound of the 95% Wilson score interval around [aiWinRate]. */
    val aiWinRateHigh: Double? get() = wilson(+1)

    // Wilson rather than the normal approximation: the per-week samples here are small, and the
    // normal interval collapses to zero width at 0% / 100% — exactly where a sparse week lands.
    private fun wilson(sign: Int): Double? {
        val n = decisive.toDouble()
        if (n == 0.0) return null
        val p = aiWins / n
        val z = 1.96
        val z2 = z * z
        val centre = p + z2 / (2 * n)
        val margin = z * sqrt(p * (1 - p) / n + z2 / (4 * n * n))
        return ((centre + sign * margin) / (1 + z2 / n)).coerceIn(0.0, 1.0)
    }

    companion object {
        val EMPTY = AiRecord(0, 0, 0, 0)

        fun of(outcomes: Iterable<AiMatchOutcome>): AiRecord {
            var ai = 0L
            var human = 0L
            var draw = 0L
            for (o in outcomes) when (o) {
                AiMatchOutcome.AI -> ai++
                AiMatchOutcome.HUMAN -> human++
                AiMatchOutcome.DRAW -> draw++
            }
            return AiRecord(ai + human + draw, ai, human, draw)
        }
    }
}

/**
 * One time bucket of the series; [start] is the bucket's first UTC day (`YYYY-MM-DD`). [aiChanges]
 * counts the engine AI changes merged to main in the bucket.
 */
data class AiPeriodRecord(val start: String, val record: AiRecord, val aiChanges: Int = 0)

data class AiModeRecord(val gameMode: String?, val format: String?, val record: AiRecord)

/**
 * One game mode's own series, on the same buckets as [AiVsHumanReport.series] — the small multiples
 * that show whether a dip is the AI getting weaker or the game mix moving toward a harder mode.
 */
data class AiModeSeries(val gameMode: String?, val format: String?, val series: List<AiPeriodRecord>)

/** A value a filter can take, with how many AI-vs-human games in the range carry it (before any filter). */
data class AiModeOption(val gameMode: String?, val format: String?, val games: Long)

data class AiSetOption(val code: String, val name: String?, val games: Long)

data class AiFilterOptions(
    /** Most games first. */
    val modes: List<AiModeOption>,
    /** Most games first. A multi-set lobby counts toward each of its sets. */
    val sets: List<AiSetOption>,
    /** Games in the range with no recorded set (quick games, constructed, cubes). */
    val gamesWithoutSet: Long,
)

/** The filter the report was computed under, echoed back with the humans it left out. */
data class AiAppliedFilter(
    val gameMode: String?,
    val format: String?,
    val setCode: String?,
    val excludeTopHumans: Int,
    /** Every human left out — named or among the top — with their record in the range's slice. */
    val excludedHumans: List<AiHumanRecord>,
    /** Games the human exclusions dropped from the range. */
    val excludedGames: Long,
)

/**
 * A deployed build's record. [firstSeen] / [lastSeen] bound the games it played in the range.
 * [newAiChanges] counts the engine AI changes it carried that the build before it did not — null when
 * the build can't be placed on main's history (a `dev` build) or no earlier build is known to compare.
 */
data class AiBuildRecord(
    val version: String,
    val firstSeen: String,
    val lastSeen: String,
    val record: AiRecord,
    val newAiChanges: Int? = null,
)

/**
 * An engine AI change merged to main. [shippedIn] is the first deployed build that carried it and
 * [shippedAt] when that build played its first AI-vs-human game; both null when no recorded game has
 * run a build with it yet.
 */
data class AiChangeEntry(
    val sha: String,
    val committedAt: String,
    val pr: Int?,
    val subject: String,
    val shippedIn: String?,
    val shippedAt: String?,
    /** The series bucket ([AiPeriodRecord.start]) the change was merged in. */
    val bucketStart: String,
)

/** The engine AI changes within the report's time axis, newest first. */
data class AiChangeLog(
    /** False when the running build had no git history to read, so changes can't be shown at all. */
    val available: Boolean,
    val changes: List<AiChangeEntry>,
)

/**
 * One AI-vs-human game as the report lists it: who played, with which colours, and how it went.
 * [matchId] opens both seats' decks (`/api/stats/admin/matches/{matchId}/decks`); [replayGameId]
 * opens the replay, null when none was stored.
 */
data class AiRecentGame(
    val matchId: Long,
    val endedAt: String,
    val gameMode: String?,
    val format: String?,
    val outcome: AiMatchOutcome,
    val humans: List<AiRecentHuman> = emptyList(),
    val aiColors: List<String?> = emptyList(),
    val replayGameId: String? = null,
)

/** A human seat of an [AiRecentGame]. [key] is the [AiHumanRecord.key] it belongs to. */
data class AiRecentHuman(
    val key: String,
    val name: String,
    @get:JsonProperty("isGuest") val isGuest: Boolean,
    val won: Boolean,
    val colors: String?,
)

/** One human's games against the AI in a slice of the range, newest first — the human's drawer on the report. */
data class AiHumanGames(
    /** The human's record over the slice; null when they have no games in it. */
    val human: AiHumanRecord?,
    /** Newest first, at most [AiVsHumanAggregator.MAX_HUMAN_GAMES]. */
    val games: List<AiRecentGame>,
)

/** A human's record against the AI, from the human's side. */
data class AiHumanRecord(
    /** Stable id to pass back as `excludeHuman`: `user:<uuid>` for an account, `guest:<name>` for a guest. */
    val key: String,
    val name: String,
    // Pinned: Jackson would otherwise emit `guest` for an `is`-prefixed Boolean.
    @get:JsonProperty("isGuest") val isGuest: Boolean,
    val games: Long,
    val wins: Long,
    val losses: Long,
    val draws: Long,
)

data class AiVsHumanReport(
    /** Range length in days; null for all time. */
    val days: Int?,
    val bucket: AiBucket,
    val overall: AiRecord,
    /** The equally long window just before the range — null for all time. */
    val previous: AiRecord?,
    /** Oldest first, with empty buckets filled so the time axis is continuous. */
    val series: List<AiPeriodRecord>,
    /** Most games first. */
    val byMode: List<AiModeRecord>,
    /** The [MAX_MODE_SERIES][AiVsHumanAggregator.MAX_MODE_SERIES] most-played modes, each with its own series. */
    val seriesByMode: List<AiModeSeries>,
    /** Newest build first, at most [AiVsHumanAggregator.MAX_BUILDS]; games with no known build are left out. */
    val byBuild: List<AiBuildRecord>,
    /** The latest games, oldest first, at most [AiVsHumanAggregator.RECENT_GAMES]. */
    val recent: List<AiRecentGame>,
    /** The humans who have played the AI most, at most [AiVsHumanAggregator.MAX_HUMANS]. */
    val topHumans: List<AiHumanRecord>,
    val aiChanges: AiChangeLog,
    val filter: AiAppliedFilter,
    val options: AiFilterOptions,
)

enum class AiBucket {
    DAY, WEEK, MONTH;

    /** The first UTC day of the bucket holding [day]. Weeks start on Monday. */
    fun startOf(day: LocalDate): LocalDate = when (this) {
        DAY -> day
        WEEK -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        MONTH -> day.withDayOfMonth(1)
    }

    fun next(start: LocalDate): LocalDate = when (this) {
        DAY -> start.plusDays(1)
        WEEK -> start.plusWeeks(1)
        MONTH -> start.plusMonths(1)
    }
}

/** Folds AI-vs-human games into an [AiVsHumanReport]. Pure, so it's tested without a database. */
object AiVsHumanAggregator {
    const val MAX_BUILDS = 10
    const val RECENT_GAMES = 40
    const val MAX_HUMANS = 10
    const val MAX_MODE_SERIES = 6
    const val MAX_HUMAN_GAMES = 100

    /**
     * @param games every AI-vs-human game since `now - 2 × days` (the range plus the window before
     *   it, for [AiVsHumanReport.previous]), or all of them when [days] is null.
     * @param humanSeats the human seats of those games.
     * @param timeline the engine AI's change history, to mark changes on the series and builds.
     * @param filter the slice to report on; [AiVsHumanReport.options] are counted before it applies.
     * @param setName a set's display name by code, for [AiFilterOptions.sets].
     */
    fun aggregate(
        allGames: List<AiMatchGame>,
        humanSeats: List<AiMatchHumanSeat>,
        days: Int?,
        bucket: AiBucket,
        now: Instant,
        timeline: EngineAiTimeline = EngineAiTimeline.UNAVAILABLE,
        filter: AiVsHumanFilter = AiVsHumanFilter.NONE,
        setName: (String) -> String? = { null },
    ): AiVsHumanReport {
        val rangeStart = days?.let { now.minusSeconds(it * 86_400L) }
        fun inRange(g: AiMatchGame) = rangeStart == null || !g.endedAt.isBefore(rangeStart)
        val options = options(allGames.filter(::inRange), setName)

        // The slice (mode / format / set), then the humans it excludes — named, or the busiest in range.
        val sliced = allGames.filter(filter::matchesSlice)
        val slicedIds = sliced.mapTo(HashSet()) { it.matchId }
        val slicedInRangeIds = sliced.filter(::inRange).mapTo(HashSet()) { it.matchId }
        val slicedSeats = humanSeats.filter { it.matchId in slicedIds }
        val outcomeBySlicedId = sliced.associate { it.matchId to it.outcome }
        val rangeHumans = humanRecords(slicedSeats.filter { it.matchId in slicedInRangeIds }, outcomeBySlicedId)
        // The busiest humans are counted on top of the named ones, so "leave out the busiest" still drops
        // someone after the busiest is already left out by name.
        val excludedKeys = filter.excludeHumans + rangeHumans.filter { it.key !in filter.excludeHumans }
            .take(filter.excludeTopHumans.coerceAtLeast(0)).map { it.key }
        val droppedIds = slicedSeats.filter { it.playerKey in excludedKeys }.mapTo(HashSet()) { it.matchId }
        val games = sliced.filter { it.matchId !in droppedIds }

        val inRange = games.filter(::inRange).sortedBy { it.endedAt }
        val previous = rangeStart?.let { start ->
            val prevStart = start.minusSeconds(days * 86_400L)
            AiRecord.of(games.filter { !it.endedAt.isBefore(prevStart) && it.endedAt.isBefore(start) }.map { it.outcome })
        }
        val inRangeIds = inRange.mapTo(HashSet()) { it.matchId }
        val outcomeById = inRange.associate { it.matchId to it.outcome }
        val series = series(inRange, bucket, rangeStart, now)
        val axisStart = series.firstOrNull()?.let { LocalDate.parse(it.start).atStartOfDay(ZoneOffset.UTC).toInstant() }
        val changesOnAxis = if (axisStart == null) emptyList() else timeline.changes.filter { !it.committedAt.isBefore(axisStart) }
        val changesPerBucket = changesOnAxis.groupingBy { bucket.startOf(it.committedAt.utcDay()).toString() }.eachCount()
        // Every build the fetched games ran — the window before the range too — placed on main's
        // history, oldest first: the baseline that says which AI changes each build added.
        val placedBuilds = games.filter { !it.engineVersion.isNullOrBlank() }
            .groupBy { it.engineVersion!! }
            .mapNotNull { (version, g) -> timeline.ordinalOf(version)?.let { PlacedBuild(version, it, g.minOf { x -> x.endedAt }) } }
            .sortedBy { it.ordinal }

        return AiVsHumanReport(
            days = days,
            bucket = bucket,
            overall = AiRecord.of(inRange.map { it.outcome }),
            previous = previous,
            series = series.map { it.copy(aiChanges = changesPerBucket[it.start] ?: 0) },
            byMode = inRange.groupBy { it.gameMode to it.format }
                .map { (key, g) -> AiModeRecord(key.first, key.second, AiRecord.of(g.map { it.outcome })) }
                .sortedWith(compareByDescending<AiModeRecord> { it.record.games }.thenBy { it.gameMode }),
            seriesByMode = inRange.groupBy { it.gameMode to it.format }
                .entries
                .sortedWith(compareByDescending<Map.Entry<Pair<String?, String?>, List<AiMatchGame>>> { it.value.size }.thenBy { it.key.first })
                .take(MAX_MODE_SERIES)
                .map { (key, g) -> AiModeSeries(key.first, key.second, seriesFrom(g, bucket, series.firstOrNull()?.start, now)) },
            byBuild = inRange.filter { !it.engineVersion.isNullOrBlank() }
                .groupBy { it.engineVersion!! }
                .map { (version, g) ->
                    AiBuildRecord(
                        version = version,
                        firstSeen = g.first().endedAt.toString(),
                        lastSeen = g.last().endedAt.toString(),
                        record = AiRecord.of(g.map { it.outcome }),
                        newAiChanges = newChangesIn(version, placedBuilds, timeline),
                    )
                }
                .sortedByDescending { it.firstSeen }
                .take(MAX_BUILDS),
            recent = inRange.takeLast(RECENT_GAMES).let { recent ->
                val ids = recent.mapTo(HashSet()) { it.matchId }
                val seatsById = humanSeats.filter { it.matchId in ids }.groupBy { it.matchId }
                recent.map { recentGame(it, seatsById[it.matchId].orEmpty()) }
            },
            topHumans = humanRecords(humanSeats.filter { it.matchId in inRangeIds }, outcomeById).take(MAX_HUMANS),
            filter = AiAppliedFilter(
                gameMode = filter.gameMode,
                format = filter.format,
                setCode = filter.setCode,
                excludeTopHumans = filter.excludeTopHumans.coerceAtLeast(0),
                excludedHumans = rangeHumans.filter { it.key in excludedKeys },
                excludedGames = slicedInRangeIds.count { it in droppedIds }.toLong(),
            ),
            options = options,
            aiChanges = AiChangeLog(
                available = timeline.available,
                changes = changesOnAxis.sortedByDescending { it.ordinal }.map { change ->
                    // The first build on main at or after the change is the first to carry it.
                    val shipped = placedBuilds.firstOrNull { it.ordinal >= change.ordinal }
                    AiChangeEntry(
                        sha = change.sha,
                        committedAt = change.committedAt.toString(),
                        pr = change.pr,
                        subject = change.subject,
                        shippedIn = shipped?.version,
                        shippedAt = shipped?.firstSeen?.toString(),
                        bucketStart = bucket.startOf(change.committedAt.utcDay()).toString(),
                    )
                },
            ),
        )
    }

    /**
     * [playerKey]'s games against the AI in the range, under [filter]'s mode/format/set slice — the
     * same games their row in [AiVsHumanReport.topHumans] counts. The filter's human exclusions don't
     * apply: the drawer is about this one human.
     */
    fun humanGames(
        allGames: List<AiMatchGame>,
        humanSeats: List<AiMatchHumanSeat>,
        playerKey: String,
        days: Int?,
        now: Instant,
        filter: AiVsHumanFilter = AiVsHumanFilter.NONE,
    ): AiHumanGames {
        val rangeStart = days?.let { now.minusSeconds(it * 86_400L) }
        val ownIds = humanSeats.filter { it.playerKey == playerKey }.mapTo(HashSet()) { it.matchId }
        val games = allGames
            .filter { it.matchId in ownIds && filter.matchesSlice(it) && (rangeStart == null || !it.endedAt.isBefore(rangeStart)) }
            .sortedByDescending { it.endedAt }
        val ids = games.mapTo(HashSet()) { it.matchId }
        val seats = humanSeats.filter { it.matchId in ids }
        val seatsById = seats.groupBy { it.matchId }
        return AiHumanGames(
            human = humanRecords(seats.filter { it.playerKey == playerKey }, games.associate { it.matchId to it.outcome }).firstOrNull(),
            games = games.take(MAX_HUMAN_GAMES).map { recentGame(it, seatsById[it.matchId].orEmpty()) },
        )
    }

    private fun recentGame(game: AiMatchGame, seats: List<AiMatchHumanSeat>) = AiRecentGame(
        matchId = game.matchId,
        endedAt = game.endedAt.toString(),
        gameMode = game.gameMode,
        format = game.format,
        outcome = game.outcome,
        humans = seats.map { AiRecentHuman(it.playerKey, it.displayName, it.isGuest, it.won, it.colors) },
        aiColors = game.aiColors,
        replayGameId = game.replayGameId,
    )

    /** Each human's record over [seats], most games first. */
    private fun humanRecords(seats: List<AiMatchHumanSeat>, outcomeById: Map<Long, AiMatchOutcome>): List<AiHumanRecord> =
        seats.groupBy { it.playerKey }
            .map { (key, own) ->
                // A drawn game is a draw for every human in it; otherwise a seat that didn't win
                // lost — in a pod, to the AI or to another human, either way not a win against it.
                val draws = own.count { outcomeById[it.matchId] == AiMatchOutcome.DRAW && !it.won }.toLong()
                val wins = own.count { it.won }.toLong()
                AiHumanRecord(
                    key = key,
                    name = own.last().displayName,
                    isGuest = own.last().isGuest,
                    games = own.size.toLong(),
                    wins = wins,
                    losses = own.size - wins - draws,
                    draws = draws,
                )
            }
            .sortedWith(compareByDescending<AiHumanRecord> { it.games }.thenBy { it.name.lowercase() })

    private fun options(inRange: List<AiMatchGame>, setName: (String) -> String?): AiFilterOptions = AiFilterOptions(
        modes = inRange.groupingBy { it.gameMode to it.format }.eachCount()
            .map { (key, n) -> AiModeOption(key.first, key.second, n.toLong()) }
            .sortedWith(compareByDescending<AiModeOption> { it.games }.thenBy { it.gameMode }.thenBy { it.format }),
        sets = inRange.flatMap { it.setCodes.distinct() }.groupingBy { it }.eachCount()
            .map { (code, n) -> AiSetOption(code, setName(code), n.toLong()) }
            .sortedWith(compareByDescending<AiSetOption> { it.games }.thenBy { it.code }),
        gamesWithoutSet = inRange.count { it.setCodes.isEmpty() }.toLong(),
    )

    private data class PlacedBuild(val version: String, val ordinal: Int, val firstSeen: Instant)

    /** The AI changes [version] carried past the newest known build before it on main. */
    private fun newChangesIn(version: String, placed: List<PlacedBuild>, timeline: EngineAiTimeline): Int? {
        val ordinal = timeline.ordinalOf(version) ?: return null
        val before = placed.lastOrNull { it.ordinal < ordinal } ?: return null
        return timeline.changes.count { it.ordinal > before.ordinal && it.ordinal <= ordinal }
    }

    private fun series(games: List<AiMatchGame>, bucket: AiBucket, rangeStart: Instant?, now: Instant): List<AiPeriodRecord> =
        seriesFrom(games, bucket, (rangeStart ?: games.firstOrNull()?.endedAt)?.utcDay()?.toString(), now)

    /** [games] bucketed from the bucket holding [firstDayIso] (`YYYY-MM-DD`) to now, gaps filled. */
    private fun seriesFrom(games: List<AiMatchGame>, bucket: AiBucket, firstDayIso: String?, now: Instant): List<AiPeriodRecord> {
        val firstDay = LocalDate.parse(firstDayIso ?: return emptyList())
        val byBucket = games.groupBy { bucket.startOf(it.endedAt.utcDay()) }
        val last = bucket.startOf(now.utcDay())
        val out = ArrayList<AiPeriodRecord>()
        var cursor = bucket.startOf(firstDay)
        while (!cursor.isAfter(last)) {
            val outcomes = byBucket[cursor].orEmpty().map { it.outcome }
            out += AiPeriodRecord(cursor.toString(), if (outcomes.isEmpty()) AiRecord.EMPTY else AiRecord.of(outcomes))
            cursor = bucket.next(cursor)
        }
        return out
    }

    private fun Instant.utcDay(): LocalDate = atOffset(ZoneOffset.UTC).toLocalDate()
}
