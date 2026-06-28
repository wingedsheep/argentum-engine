package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.auth.AdminAuthService
import com.wingedsheep.gameserver.auth.UserAdminService
import com.wingedsheep.gameserver.persistence.MatchResultRepository
import com.wingedsheep.gameserver.stats.CardStat
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Admin view of the registered accounts: list everyone with their lifetime record, drill into one
 * account's full stats, and grant/revoke admin access. Auth goes through [AdminAuthService] (the same
 * password-or-admin-account gate as the rest of the dashboard). Mounted only when accounts are enabled
 * — the data lives in Postgres.
 */
@RestController
@RequestMapping("/api/admin/users")
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class AdminUsersController(
    private val adminAuth: AdminAuthService,
    private val statsQuery: StatsQueryService,
    private val userAdmin: UserAdminService,
    private val matchResults: MatchResultRepository,
) {
    data class StatsDto(val games: Long, val wins: Long, val losses: Long, val winRate: Double)

    /** One account's full profile + stats, for the player detail view. */
    data class UserDetailDto(
        val id: UUID,
        val email: String,
        val displayName: String,
        val isAdmin: Boolean,
        val createdAt: String,
        val stats: StatsDto,
        val colors: List<StatBucket>,
        val modes: List<StatBucket>,
        val opponents: List<HeadToHead>,
        val topCards: List<CardStat>,
        val tournaments: List<UserTournamentEntry>,
        val recentGames: List<GameHistoryEntry>,
    )

    data class SetAdminBody(val isAdmin: Boolean)

    @GetMapping
    fun list(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        ResponseEntity.ok(statsQuery.allUsersWithStats())
    }

    @GetMapping("/{id}")
    fun detail(
        @PathVariable id: UUID,
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        val user = userAdmin.get(id)
            ?: return@guard ResponseEntity.status(404).body(mapOf("error" to "User not found"))
        val games = matchResults.countGamesForUser(id)
        val wins = matchResults.countWinsForUser(id)
        ResponseEntity.ok(
            UserDetailDto(
                id = user.id!!,
                email = user.email,
                displayName = user.displayName,
                isAdmin = user.isAdmin,
                createdAt = user.createdAt.toString(),
                stats = StatsDto(
                    games = games,
                    wins = wins,
                    losses = games - wins,
                    winRate = if (games > 0) wins.toDouble() / games else 0.0,
                ),
                colors = statsQuery.colorBreakdown(id),
                modes = statsQuery.modeBreakdown(id),
                opponents = statsQuery.headToHead(id),
                topCards = statsQuery.topCardsForUser(id, 20),
                tournaments = statsQuery.tournamentHistory(id, 25),
                recentGames = statsQuery.recentGames(id, 25, 0),
            )
        )
    }

    @PostMapping("/{id}/admin")
    fun setAdmin(
        @PathVariable id: UUID,
        @RequestBody body: SetAdminBody,
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        val updated = userAdmin.setAdmin(id, body.isAdmin)
            ?: return@guard ResponseEntity.status(404).body(mapOf("error" to "User not found"))
        ResponseEntity.ok(mapOf("id" to updated.id, "isAdmin" to updated.isAdmin))
    }
}
