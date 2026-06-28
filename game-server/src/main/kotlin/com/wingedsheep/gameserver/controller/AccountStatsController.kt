package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.auth.AuthSupport
import com.wingedsheep.gameserver.persistence.MatchResultRepository
import com.wingedsheep.gameserver.persistence.RatingHistoryRepository
import com.wingedsheep.gameserver.persistence.UserRatingRepository
import com.wingedsheep.gameserver.persistence.UserRepository
import com.wingedsheep.gameserver.ranking.Elo
import com.wingedsheep.gameserver.ranking.RankedMode
import com.wingedsheep.gameserver.ranking.RatingTier
import com.wingedsheep.gameserver.stats.CardStat
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
    private val matchResults: MatchResultRepository,
    private val statsQuery: StatsQueryService,
    private val userRatings: UserRatingRepository,
    private val ratingHistory: RatingHistoryRepository,
    private val users: UserRepository,
    private val authSupport: AuthSupport,
) {
    data class StatsDto(val games: Long, val wins: Long, val losses: Long, val winRate: Double)

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

    /** Overall win/loss record for [userId]. */
    private fun statsFor(userId: java.util.UUID): StatsDto {
        val games = matchResults.countGamesForUser(userId)
        val wins = matchResults.countWinsForUser(userId)
        return StatsDto(
            games = games,
            wins = wins,
            losses = games - wins,
            winRate = if (games > 0) wins.toDouble() / games else 0.0,
        )
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

    @GetMapping("/me")
    fun me(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): StatsDto =
        statsFor(authSupport.requireUser(auth).userId)

    @GetMapping("/me/colors")
    fun colors(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): List<StatBucket> =
        statsQuery.colorBreakdown(authSupport.requireUser(auth).userId)

    @GetMapping("/me/sets")
    fun sets(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): List<StatBucket> =
        statsQuery.setBreakdown(authSupport.requireUser(auth).userId)

    @GetMapping("/me/modes")
    fun modes(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): List<StatBucket> =
        statsQuery.modeBreakdown(authSupport.requireUser(auth).userId)

    @GetMapping("/me/opponents")
    fun opponents(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): List<HeadToHead> =
        statsQuery.headToHead(authSupport.requireUser(auth).userId)

    /** One page of game history. The total count (for the pager) is returned in `X-Total-Count`. */
    @GetMapping("/me/history")
    fun history(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(defaultValue = "25") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
    ): ResponseEntity<List<GameHistoryEntry>> {
        val userId = authSupport.requireUser(auth).userId
        val page = statsQuery.recentGames(userId, limit.coerceIn(1, 100), offset.coerceAtLeast(0))
        return ResponseEntity.ok()
            .header("X-Total-Count", statsQuery.recentGamesCount(userId).toString())
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
    ): List<StatBucket> =
        statsQuery.creatureTypeBreakdown(authSupport.requireUser(auth).userId, limit.coerceIn(1, 50))

    /** Distribution of the user's cards across primary card types (Creature, Instant, Land, …). */
    @GetMapping("/me/card-types")
    fun cardTypes(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): List<StatBucket> =
        statsQuery.cardTypeBreakdown(authSupport.requireUser(auth).userId)

    /** Mana-value curve of the user's nonland cards (buckets 0..6 then "7+"). */
    @GetMapping("/me/curve")
    fun curve(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): List<StatBucket> =
        statsQuery.manaCurve(authSupport.requireUser(auth).userId)

    @GetMapping("/me/cards")
    fun cards(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(defaultValue = "30") limit: Int,
    ): List<CardStat> =
        statsQuery.topCardsForUser(authSupport.requireUser(auth).userId, limit.coerceIn(1, 200))

    @GetMapping("/me/tournaments")
    fun tournaments(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(defaultValue = "25") limit: Int,
    ): List<UserTournamentEntry> =
        statsQuery.tournamentHistory(authSupport.requireUser(auth).userId, limit.coerceIn(1, 100))

    // ---- Public profiles --------------------------------------------------------------------

    /** Everything a read-only public profile shows for another player, in one request. */
    data class PublicProfileDto(
        val userId: String,
        val displayName: String,
        val stats: StatsDto,
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
     */
    @GetMapping("/users/{userId}")
    fun publicProfile(@PathVariable userId: java.util.UUID): ResponseEntity<PublicProfileDto> {
        val user = users.findById(userId).orElse(null) ?: return ResponseEntity.notFound().build()
        val id = user.id ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            PublicProfileDto(
                userId = id.toString(),
                displayName = user.displayName,
                stats = statsFor(id),
                ratings = ratingsFor(id),
                ratingHistory = ratingHistoryFor(id, RankedMode.entries),
                colors = statsQuery.colorBreakdown(id),
                cardTypes = statsQuery.cardTypeBreakdown(id),
                curve = statsQuery.manaCurve(id),
                creatureTypes = statsQuery.creatureTypeBreakdown(id, 12),
                modes = statsQuery.modeBreakdown(id),
                sets = statsQuery.setBreakdown(id),
                topCards = statsQuery.topCardsForUser(id, 24),
                opponents = statsQuery.headToHead(id),
                tournaments = statsQuery.tournamentHistory(id, 15),
                recentGames = statsQuery.recentGames(id, 10, 0),
            ),
        )
    }
}
