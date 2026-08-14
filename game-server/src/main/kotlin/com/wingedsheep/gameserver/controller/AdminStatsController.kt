package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.auth.AdminAuthService
import com.wingedsheep.gameserver.stats.GeoIpService
import com.wingedsheep.gameserver.stats.StatsQueryService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Admin-only global stats for the dashboard: totals, games-per-day, mode/color distributions, the
 * sets played most in tournaments, and an IP-based geolocation estimate. Auth uses the shared
 * `X-Admin-Password` / admin-account gate.
 * Mounted only when accounts are enabled (the stats live in Postgres); the geolocation endpoint
 * resolves raw IPs server-side and returns only aggregated locations — raw IPs never reach the client.
 */
@RestController
@RequestMapping("/api/stats/admin")
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class AdminStatsController(
    private val statsQuery: StatsQueryService,
    private val geoIp: GeoIpService,
    private val adminAuth: AdminAuthService,
) {
    /** One resolved location and how many games connected from it. */
    data class GeoBucket(
        val country: String?,
        val countryCode: String?,
        val region: String?,
        val city: String?,
        val games: Long,
    )

    @GetMapping("/overview")
    fun overview(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) { ResponseEntity.ok(statsQuery.overview()) }

    @GetMapping("/games-per-day")
    fun gamesPerDay(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
        @RequestParam(defaultValue = "30") days: Int,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        ResponseEntity.ok(statsQuery.gamesPerDay(days.coerceIn(1, 365)))
    }

    @GetMapping("/modes")
    fun modes(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) { ResponseEntity.ok(statsQuery.modeDistribution()) }

    @GetMapping("/tournament-sets")
    fun tournamentSets(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        ResponseEntity.ok(statsQuery.tournamentSetDistribution())
    }

    @GetMapping("/colors")
    fun colors(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) { ResponseEntity.ok(statsQuery.colorDistribution()) }

    @GetMapping("/cards")
    fun cards(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        ResponseEntity.ok(statsQuery.topCards(limit.coerceIn(1, 500)))
    }

    @GetMapping("/cards/win-rates")
    fun cardWinRates(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
        @RequestParam(defaultValue = "10") minDecks: Int,
        @RequestParam(defaultValue = "50") limit: Int,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        ResponseEntity.ok(statsQuery.cardWinRates(minDecks.coerceAtLeast(1), limit.coerceIn(1, 500)))
    }

    @GetMapping("/tournaments")
    fun tournaments(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        ResponseEntity.ok(statsQuery.recentTournaments(limit.coerceIn(1, 200)))
    }

    /**
     * A page of the most-recent games across every player, newest first, with the total in
     * `X-Total-Count` so the admin UI can page through every recorded game. Neutral (lists every
     * seat, winner flagged) — unlike the per-user history which is a single viewer's perspective.
     */
    @GetMapping("/recent-games")
    fun recentGames(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
        @RequestParam(defaultValue = "25") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        val entries = statsQuery.recentGamesGlobal(limit.coerceIn(1, 100), offset.coerceAtLeast(0))
        ResponseEntity.ok()
            .header("X-Total-Count", statsQuery.recentGamesGlobalCount().toString())
            .body(entries)
    }

    /**
     * Storage overview of the accounts database: total size plus per-table row counts and on-disk
     * footprint. Row counts are exact, so this does a scan per table — fetch it on demand, don't poll.
     */
    @GetMapping("/database")
    fun database(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) { ResponseEntity.ok(statsQuery.databaseStats()) }

    @GetMapping("/geo")
    fun geo(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        val ipCounts = statsQuery.ipBreakdown()
        val locations = geoIp.resolve(ipCounts.map { it.ip })
        // Aggregate game counts by resolved location (raw IPs are dropped here).
        val byLocation = ipCounts.groupBy { locations[it.ip] }
            .map { (loc, rows) ->
                GeoBucket(
                    country = loc?.country,
                    countryCode = loc?.countryCode,
                    region = loc?.region,
                    city = loc?.city,
                    games = rows.sumOf { it.count },
                )
            }
            .sortedByDescending { it.games }
        ResponseEntity.ok(byLocation)
    }
}
