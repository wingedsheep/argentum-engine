package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.auth.AuthSupport
import com.wingedsheep.gameserver.persistence.RatingHistoryRepository
import com.wingedsheep.gameserver.persistence.UserRatingRepository
import com.wingedsheep.gameserver.persistence.UserRepository
import com.wingedsheep.gameserver.ranking.Elo
import com.wingedsheep.gameserver.ranking.RankedMode
import com.wingedsheep.gameserver.ranking.RatingTier
import com.wingedsheep.gameserver.stats.CardStat
import com.wingedsheep.gameserver.stats.GameFilter
import com.wingedsheep.gameserver.stats.PlayerRecord
import com.wingedsheep.gameserver.stats.GameDecks
import com.wingedsheep.gameserver.stats.GameHistoryEntry
import com.wingedsheep.gameserver.stats.HeadToHead
import com.wingedsheep.gameserver.stats.StatBucket
import com.wingedsheep.gameserver.stats.StatsQueryService
import com.wingedsheep.gameserver.stats.UserTournamentEntry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Stats for the signed-in user, computed on demand from the match-history tables (no denormalized
 * stats table). Lives under /api/stats alongside the admin dashboard stats. Only mounted when
 * accounts are enabled.
 */
@RestController
@RequestMapping("/api/stats")
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class AccountStatsController(
    private val statsQuery: StatsQueryService,
    private val userRatings: UserRatingRepository,
    private val ratingHistory: RatingHistoryRepository,
    private val users: UserRepository,
    private val authSupport: AuthSupport,
    private val anteSpoils: com.wingedsheep.gameserver.persistence.AnteSpoilRepository,
) {
    /** Current ranked standing in one mode. Unrated modes report the starting rating / Provisional. */
    data class RatingDto(
        val mode: String,
        val rating: Int,
        val tier: String,
        val provisional: Boolean,
        val gamesPlayed: Int,
        val wins: Int,
        val losses: Int,
        val draws: Int,
        val peakRating: Int,
    )

    /** One point on the rating-over-time chart. */
    data class RatingPointDto(
        val mode: String,
        val endedAt: String,
        val ratingAfter: Int,
        val delta: Int,
        val result: String,
    )

    /** Ranked standings for [userId], every queue present (unplayed ones at the starting rating). */
    private fun ratingsFor(userId: java.util.UUID): List<RatingDto> {
        val byMode = userRatings.findByUserId(userId).associateBy { it.mode }
        return RankedMode.entries.map { mode ->
            val row = byMode[mode.name]
            val rating = row?.rating ?: Elo.STARTING_RATING
            val games = row?.gamesPlayed ?: 0
            val tier = Elo.tier(rating, games)
            RatingDto(
                mode = mode.name,
                rating = Math.round(rating).toInt(),
                tier = tier.displayName,
                provisional = tier == RatingTier.PROVISIONAL,
                gamesPlayed = games,
                wins = row?.wins ?: 0,
                losses = row?.losses ?: 0,
                draws = row?.draws ?: 0,
                peakRating = Math.round(row?.peakRating ?: Elo.STARTING_RATING).toInt(),
            )
        }
    }

    /** Rating-over-time points for [userId] across the given [modes], oldest first. */
    private fun ratingHistoryFor(userId: java.util.UUID, modes: List<RankedMode>): List<RatingPointDto> =
        modes.flatMap { m ->
            ratingHistory.findByUserIdAndModeOrderByCreatedAtAsc(userId, m.name).map { row ->
                RatingPointDto(
                    mode = m.name,
                    endedAt = row.createdAt.toString(),
                    ratingAfter = Math.round(row.ratingAfter).toInt(),
                    delta = Math.round(row.delta).toInt(),
                    result = row.result,
                )
            }
        }

    /** All three ranked queues for the signed-in user, unplayed ones shown at the starting rating. */
    @GetMapping("/me/ratings")
    fun ratings(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): List<RatingDto> =
        ratingsFor(authSupport.requireUser(auth).userId)

    /**
     * Rating-over-time points for the chart. Without [mode], returns every mode's history (the client
     * draws one line per mode); with [mode], just that queue. Oldest first.
     */
    @GetMapping("/me/ratings/history")
    fun ratingsHistory(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(required = false) mode: String?,
    ): List<RatingPointDto> {
        val userId = authSupport.requireUser(auth).userId
        val modes = mode
            ?.let { listOfNotNull(runCatching { RankedMode.valueOf(it.uppercase()) }.getOrNull()) }
            ?: RankedMode.entries
        return ratingHistoryFor(userId, modes)
    }

    data class SpoilDto(
        val cardName: String,
        val imageUri: String?,
        val opponentName: String,
        val gameId: String?,
        val at: String,
    )

    data class SpoilsDto(val won: List<SpoilDto>, val lost: List<SpoilDto>)

    /** Cards won and lost in games played for ante (CR 407) — the profile's Spoils panel. */
    @GetMapping("/me/spoils")
    fun spoils(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): SpoilsDto {
        val rows = anteSpoils.findTop200ByUserIdOrderByCreatedAtDesc(authSupport.requireUser(auth).userId)
        val (won, lost) = rows.partition { it.won }
        fun dto(row: com.wingedsheep.gameserver.persistence.AnteSpoilRow) =
            SpoilDto(row.cardName, row.imageUri, row.opponentName, row.gameId, row.createdAt.toString())
        return SpoilsDto(won.map(::dto), lost.map(::dto))
    }

    // Every game-derived endpoint below takes the same optional slice: `vs` = all / humans / ai and
    // `days` (absent or 0 = all time) — see [GameFilter]. Results whose seat never took an action are
    // left out of every figure; the history list shows them, marked.

    /** Win/loss record under the slice, plus the period's split by opponent type. */
    @GetMapping("/me")
    fun me(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(required = false) vs: String?,
        @RequestParam(required = false) days: Int?,
    ): PlayerRecord = statsQuery.playerRecord(authSupport.requireUser(auth).userId, GameFilter.of(vs, days))

    @GetMapping("/me/colors")
    fun colors(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(required = false) vs: String?,
        @RequestParam(required = false) days: Int?,
    ): List<StatBucket> = statsQuery.colorBreakdown(authSupport.requireUser(auth).userId, GameFilter.of(vs, days))

    @GetMapping("/me/sets")
    fun sets(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(required = false) vs: String?,
        @RequestParam(required = false) days: Int?,
    ): List<StatBucket> = statsQuery.setBreakdown(authSupport.requireUser(auth).userId, GameFilter.of(vs, days))

    @GetMapping("/me/modes")
    fun modes(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(required = false) vs: String?,
        @RequestParam(required = false) days: Int?,
    ): List<StatBucket> = statsQuery.modeBreakdown(authSupport.requireUser(auth).userId, GameFilter.of(vs, days))

    @GetMapping("/me/opponents")
    fun opponents(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(required = false) vs: String?,
        @RequestParam(required = false) days: Int?,
    ): List<HeadToHead> = statsQuery.headToHead(authSupport.requireUser(auth).userId, GameFilter.of(vs, days))

    /** One page of game history. The total count (for the pager) is returned in `X-Total-Count`. */
    @GetMapping("/me/history")
    fun history(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(defaultValue = "25") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(required = false) vs: String?,
        @RequestParam(required = false) days: Int?,
    ): ResponseEntity<List<GameHistoryEntry>> {
        val userId = authSupport.requireUser(auth).userId
        val filter = GameFilter.of(vs, days)
        val page = statsQuery.recentGames(userId, limit.coerceIn(1, 100), offset.coerceAtLeast(0), filter)
        return ResponseEntity.ok()
            .header("X-Total-Count", statsQuery.recentGamesCount(userId, filter).toString())
            .body(page)
    }

    /** Both seats' decklists for one of the user's finished games, for the recent-games deck viewer. */
    @GetMapping("/me/history/{gameId}/decks")
    fun gameDecks(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @PathVariable gameId: String,
    ): ResponseEntity<GameDecks> {
        val decks = statsQuery.decksForGame(authSupport.requireUser(auth).userId, gameId)
        return if (decks == null) ResponseEntity.notFound().build() else ResponseEntity.ok(decks)
    }

    /** Creature subtypes the user plays most, by total copies across all recorded decks. */
    @GetMapping("/me/creature-types")
    fun creatureTypes(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(defaultValue = "15") limit: Int,
        @RequestParam(required = false) vs: String?,
        @RequestParam(required = false) days: Int?,
    ): List<StatBucket> =
        statsQuery.creatureTypeBreakdown(authSupport.requireUser(auth).userId, limit.coerceIn(1, 50), GameFilter.of(vs, days))

    /** Distribution of the user's cards across primary card types (Creature, Instant, Land, …). */
    @GetMapping("/me/card-types")
    fun cardTypes(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(required = false) vs: String?,
        @RequestParam(required = false) days: Int?,
    ): List<StatBucket> = statsQuery.cardTypeBreakdown(authSupport.requireUser(auth).userId, GameFilter.of(vs, days))

    /** Mana-value curve of the user's nonland cards (buckets 0..6 then "7+"). */
    @GetMapping("/me/curve")
    fun curve(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(required = false) vs: String?,
        @RequestParam(required = false) days: Int?,
    ): List<StatBucket> = statsQuery.manaCurve(authSupport.requireUser(auth).userId, GameFilter.of(vs, days))

    @GetMapping("/me/cards")
    fun cards(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(defaultValue = "30") limit: Int,
        @RequestParam(required = false) vs: String?,
        @RequestParam(required = false) days: Int?,
    ): List<CardStat> =
        statsQuery.topCardsForUser(authSupport.requireUser(auth).userId, limit.coerceIn(1, 200), GameFilter.of(vs, days))

    @GetMapping("/me/tournaments")
    fun tournaments(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(defaultValue = "25") limit: Int,
    ): List<UserTournamentEntry> =
        statsQuery.tournamentHistory(authSupport.requireUser(auth).userId, limit.coerceIn(1, 100))

    /**
     * Public detail for one tournament: final standings for every participant and every game played in
     * it (all players', not just the viewer's), each linkable to its public replay. No auth required —
     * the same data is already public on each player's profile — but the tournament must exist (404).
     */
    @GetMapping("/tournaments/{id}")
    fun tournamentDetail(@PathVariable id: Long): ResponseEntity<com.wingedsheep.gameserver.stats.TournamentDetail> {
        val detail = statsQuery.tournamentDetail(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(detail)
    }

    // ---- Public profiles --------------------------------------------------------------------

    /** Everything a read-only public profile shows for another player, in one request. */
    data class PublicProfileDto(
        val userId: String,
        val displayName: String,
        val avatar: String?,
        val stats: PlayerRecord,
        val ratings: List<RatingDto>,
        val ratingHistory: List<RatingPointDto>,
        val colors: List<StatBucket>,
        val cardTypes: List<StatBucket>,
        val curve: List<StatBucket>,
        val creatureTypes: List<StatBucket>,
        val modes: List<StatBucket>,
        val sets: List<StatBucket>,
        val topCards: List<CardStat>,
        val opponents: List<HeadToHead>,
        val tournaments: List<UserTournamentEntry>,
        val recentGames: List<GameHistoryEntry>,
    )

    /**
     * A public, read-only profile for any account, bundled into one response so a visitor's page is a
     * single round-trip. No auth required — profiles are public — but the account must exist (404
     * otherwise). The deck viewer stays `/me`-only, so this never exposes another player's decklists.
     * Takes the same `vs` / `days` slice as the `/me` endpoints; ratings and tournaments ignore it.
     */
    @GetMapping("/users/{userId}")
    fun publicProfile(
        @PathVariable userId: java.util.UUID,
        @RequestParam(required = false) vs: String?,
        @RequestParam(required = false) days: Int?,
    ): ResponseEntity<PublicProfileDto> {
        val user = users.findById(userId).orElse(null) ?: return ResponseEntity.notFound().build()
        val id = user.id ?: return ResponseEntity.notFound().build()
        val filter = GameFilter.of(vs, days)
        return ResponseEntity.ok(
            PublicProfileDto(
                userId = id.toString(),
                displayName = user.displayName,
                avatar = user.avatar,
                stats = statsQuery.playerRecord(id, filter),
                ratings = ratingsFor(id),
                ratingHistory = ratingHistoryFor(id, RankedMode.entries),
                colors = statsQuery.colorBreakdown(id, filter),
                cardTypes = statsQuery.cardTypeBreakdown(id, filter),
                curve = statsQuery.manaCurve(id, filter),
                creatureTypes = statsQuery.creatureTypeBreakdown(id, 12, filter),
                modes = statsQuery.modeBreakdown(id, filter),
                sets = statsQuery.setBreakdown(id, filter),
                topCards = statsQuery.topCardsForUser(id, 24, filter),
                opponents = statsQuery.headToHead(id, filter),
                tournaments = statsQuery.tournamentHistory(id, 15),
                recentGames = statsQuery.recentGames(id, 10, 0, filter),
            ),
        )
    }
}
