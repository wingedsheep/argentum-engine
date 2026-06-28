package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.auth.AuthSupport
import com.wingedsheep.gameserver.persistence.MatchResultRepository
import com.wingedsheep.gameserver.stats.CardStat
import com.wingedsheep.gameserver.stats.GameHistoryEntry
import com.wingedsheep.gameserver.stats.HeadToHead
import com.wingedsheep.gameserver.stats.StatBucket
import com.wingedsheep.gameserver.stats.StatsQueryService
import com.wingedsheep.gameserver.stats.UserTournamentEntry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
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
    private val authSupport: AuthSupport,
) {
    data class StatsDto(val games: Long, val wins: Long, val losses: Long, val winRate: Double)

    @GetMapping("/me")
    fun me(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): StatsDto {
        val userId = authSupport.requireUser(auth).userId
        val games = matchResults.countGamesForUser(userId)
        val wins = matchResults.countWinsForUser(userId)
        return StatsDto(
            games = games,
            wins = wins,
            losses = games - wins,
            winRate = if (games > 0) wins.toDouble() / games else 0.0,
        )
    }

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

    @GetMapping("/me/history")
    fun history(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestParam(defaultValue = "25") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
    ): List<GameHistoryEntry> =
        statsQuery.recentGames(authSupport.requireUser(auth).userId, limit.coerceIn(1, 100), offset.coerceAtLeast(0))

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
}
