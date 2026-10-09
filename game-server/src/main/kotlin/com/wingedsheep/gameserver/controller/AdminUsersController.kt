package com.wingedsheep.gameserver.controller

import com.fasterxml.jackson.annotation.JsonProperty
import com.wingedsheep.gameserver.auth.AdminAuthService
import com.wingedsheep.gameserver.auth.UserAdminService
import com.wingedsheep.gameserver.persistence.DeckRepository
import com.wingedsheep.gameserver.persistence.MatchResultRepository
import com.wingedsheep.gameserver.stats.CardStat
import com.wingedsheep.gameserver.stats.GameDeckCard
import com.wingedsheep.gameserver.stats.GameHistoryEntry
import com.wingedsheep.gameserver.stats.HeadToHead
import com.wingedsheep.gameserver.stats.StatBucket
import com.wingedsheep.gameserver.stats.StatsQueryService
import com.wingedsheep.gameserver.stats.UserTournamentEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
    private val decks: DeckRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    data class StatsDto(val games: Long, val wins: Long, val losses: Long, val winRate: Double)

    /** One of the account's saved decks, enriched so the admin deck viewer renders it the polished way. */
    data class AdminDeckDto(
        val id: Long,
        val name: String,
        val format: String?,
        val updatedAt: String,
        val cards: List<GameDeckCard>,
    )

    /** One account's full profile + stats, for the player detail view. */
    data class UserDetailDto(
        val id: UUID,
        val email: String,
        val displayName: String,
        // Pin the wire name (see SetAdminBody) — the client reads `detail.isAdmin` for the badge
        // and the promote/revoke toggle; without this Jackson emits `admin` and both misbehave.
        @JsonProperty("isAdmin") val isAdmin: Boolean,
        val avatar: String?,
        val createdAt: String,
        val stats: StatsDto,
        val colors: List<StatBucket>,
        val modes: List<StatBucket>,
        val opponents: List<HeadToHead>,
        val topCards: List<CardStat>,
        val tournaments: List<UserTournamentEntry>,
        val recentGames: List<GameHistoryEntry>,
    )

    /**
     * `@JsonProperty` pins the wire name to `isAdmin`. Without it, Jackson treats the `is`-prefixed
     * Kotlin property as the getter for a property named `admin`, so the JSON key `isAdmin` the client
     * sends no longer maps to the constructor parameter and deserialization fails with 400.
     */
    data class SetAdminBody(@JsonProperty("isAdmin") val isAdmin: Boolean)

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
                avatar = user.avatar,
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

    /**
     * A page of the account's games, newest first, with the total in `X-Total-Count` so the admin UI
     * can page through every game (the detail view only embeds the most recent handful). Reuses the
     * same per-user history query as the player's own profile.
     */
    @GetMapping("/{id}/games")
    fun games(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "25") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        val entries = statsQuery.recentGames(id, limit.coerceIn(1, 100), offset.coerceAtLeast(0))
        ResponseEntity.ok()
            .header("X-Total-Count", statsQuery.recentGamesCount(id).toString())
            .body(entries)
    }

    /**
     * Both seats' decks for one of the account's games. Scoped to [id] (who must be a participant),
     * mirroring the player-facing deck viewer — so colours and the `isSelf` seat are from this
     * account's perspective. 404 when the game isn't one this account played.
     */
    @GetMapping("/{id}/games/{gameId}/decks")
    fun gameDecks(
        @PathVariable id: UUID,
        @PathVariable gameId: String,
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        val decks = statsQuery.decksForGame(id, gameId)
            ?: return@guard ResponseEntity.status(404).body(mapOf("error" to "Game not found for this user"))
        ResponseEntity.ok(decks)
    }

    /**
     * The account's saved decks, newest first, each enriched with per-card cost/type/colour so the
     * admin deck viewer renders them the polished way. The stored body is the client's `SharedDeck`
     * JSON; we pull its `cards` map (plus the commander) and enrich through the registry.
     */
    @GetMapping("/{id}/decks")
    fun savedDecks(
        @PathVariable id: UUID,
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        val rows = decks.findByUserIdOrderByUpdatedAtDesc(id)
        val dtos = rows.map { row ->
            AdminDeckDto(
                id = row.id!!,
                name = row.name,
                format = row.format,
                updatedAt = row.updatedAt.toString(),
                cards = statsQuery.enrichDeck(deckCardCounts(row.data)),
            )
        }
        ResponseEntity.ok(dtos)
    }

    /** Extract the `name -> copies` card map from a stored SharedDeck JSON body (commander included). */
    private fun deckCardCounts(body: String): Map<String, Int> = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val cards = LinkedHashMap<String, Int>()
        root["cards"]?.jsonObject?.forEach { (name, count) ->
            count.jsonPrimitive.intOrNull?.let { if (it > 0) cards[name] = it }
        }
        // The commander lives in the command zone (CR 903.6a) and isn't in `cards` — include it so the
        // deck the admin sees matches what the player built.
        root["commander"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?.let { cards.putIfAbsent(it, 1) }
        cards
    }.getOrDefault(emptyMap())
}
