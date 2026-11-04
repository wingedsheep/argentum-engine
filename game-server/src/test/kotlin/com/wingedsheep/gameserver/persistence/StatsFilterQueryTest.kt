package com.wingedsheep.gameserver.persistence

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.gameserver.stats.AiBucket
import com.wingedsheep.gameserver.stats.DeckProfiler
import com.wingedsheep.gameserver.stats.EngineAiHistory
import com.wingedsheep.gameserver.stats.GameFilter
import com.wingedsheep.gameserver.stats.GeoIpService
import com.wingedsheep.gameserver.stats.NotCountedReason
import com.wingedsheep.gameserver.stats.OpponentFilter
import com.wingedsheep.gameserver.stats.StatsQueryService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.flywaydb.core.Flyway
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.UUID

/**
 * The stats filters against real PostgreSQL: who a game was against, how recent it was, and which
 * results count at all (a seat that never took an action — see `StatsEligibility`).
 *
 * Alice's history, newest first:
 *  - g1 beat Bob (human)                       counted, vs humans
 *  - g2 lost to the AI                          counted, vs AI
 *  - g3 beat the AI                             counted, vs AI
 *  - g4 lost to Bob, never took an action       not counted
 *  - g5 beat Bob 40 days ago                    counted, vs humans
 *  - g6 4-player pod with Bob and an AI, won    counted, mixed (neither vs humans nor vs AI)
 *
 * Self-skips when Docker is unavailable, like [FlywayMigrationTest].
 */
class StatsFilterQueryTest : FunSpec({

    val dockerAvailable = runCatching { DockerClientFactory.instance().isDockerAvailable }.getOrDefault(false)
    val alice = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val bob = UUID.fromString("22222222-2222-2222-2222-222222222222")

    lateinit var postgres: PostgreSQLContainer
    lateinit var stats: StatsQueryService

    beforeSpec {
        if (!dockerAvailable) return@beforeSpec
        postgres = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine")).also { it.start() }
        Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        val jdbc = JdbcTemplate(DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password))
        jdbc.update("INSERT INTO users(id, email, display_name) VALUES (?, 'a@test', 'Alice'), (?, 'b@test', 'Bob')", alice, bob)
        data class Seat(val user: UUID?, val name: String, val won: Boolean, val ai: Boolean = false, val acted: Boolean? = true, val counts: Boolean = true)
        var seatId = 100L
        fun game(id: Long, daysAgo: Int, vararg seats: Seat) {
            jdbc.update(
                "INSERT INTO match_results(id, game_id, game_mode, ended_at) VALUES (?, ?, 'CASUAL', now() - (? * interval '1 day'))",
                id, "g$id", daysAgo,
            )
            for (s in seats) {
                jdbc.update(
                    "INSERT INTO match_participants(id, match_id, user_id, player_name, won, is_ai, took_action, counts_in_stats) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    seatId++, id, s.user, s.name, s.won, s.ai, s.acted, s.counts,
                )
            }
        }
        game(1, 1, Seat(alice, "Alice", true), Seat(bob, "Bob", false))
        game(2, 2, Seat(alice, "Alice", false), Seat(null, "AI", true, ai = true))
        game(3, 3, Seat(alice, "Alice", true), Seat(null, "AI", false, ai = true))
        game(4, 4, Seat(alice, "Alice", false, acted = false, counts = false), Seat(bob, "Bob", true, counts = false))
        game(5, 40, Seat(alice, "Alice", true), Seat(bob, "Bob", false))
        game(
            6, 5,
            Seat(alice, "Alice", true), Seat(bob, "Bob", false),
            Seat(null, "AI 1", false, ai = true), Seat(null, "AI 2", false, ai = true),
        )
        // A no-show against the AI: the AI's "win" must not reach the AI-vs-human report.
        game(7, 2, Seat(bob, "Bob", false, acted = false, counts = false), Seat(null, "AI", true, ai = true, counts = false))
        stats = StatsQueryService(
            jdbc = jdbc,
            deckProfiler = DeckProfiler(CardRegistry()),
            cardRegistry = CardRegistry(),
            printingRegistry = PrintingRegistry(),
            geoIp = GeoIpService(),
            engineAiHistory = EngineAiHistory(),
        )
    }

    afterSpec { if (dockerAvailable) postgres.stop() }

    test("the record leaves out games nobody played and splits the rest by opponent").config(enabled = dockerAvailable) {
        val record = stats.playerRecord(alice)
        record.games shouldBe 5
        record.wins shouldBe 4
        record.notCounted shouldBe 1
        record.vsHumans.games shouldBe 2
        record.vsHumans.wins shouldBe 2
        record.vsAi.games shouldBe 2
        record.vsAi.wins shouldBe 1
    }

    test("an opponent filter narrows the record, and the split stays whole").config(enabled = dockerAvailable) {
        val vsHumans = stats.playerRecord(alice, GameFilter(OpponentFilter.HUMANS))
        vsHumans.games shouldBe 2
        vsHumans.winRate shouldBe 1.0
        vsHumans.notCounted shouldBe 1
        vsHumans.vsAi.games shouldBe 2

        val vsAi = stats.playerRecord(alice, GameFilter(OpponentFilter.AI))
        vsAi.games shouldBe 2
        vsAi.notCounted shouldBe 0
    }

    test("a period filter drops older games from the record and the split").config(enabled = dockerAvailable) {
        val month = stats.playerRecord(alice, GameFilter(days = 30))
        month.games shouldBe 4
        month.vsHumans.games shouldBe 1
    }

    test("history lists the uncounted game, marked with whose inaction voided it").config(enabled = dockerAvailable) {
        val all = stats.recentGames(alice, 10, 0)
        all.size shouldBe 6
        stats.recentGamesCount(alice) shouldBe 6
        all.first { it.gameId == "g4" }.notCounted shouldBe NotCountedReason.NO_ACTION
        all.filter { it.gameId != "g4" }.all { it.notCounted == null } shouldBe true

        stats.recentGames(bob, 10, 0).first { it.gameId == "g4" }.notCounted shouldBe NotCountedReason.OPPONENT_NO_ACTION
        stats.recentGamesCount(alice, GameFilter(OpponentFilter.AI)) shouldBe 2
    }

    test("head-to-head counts only played games, and is empty against the AI").config(enabled = dockerAvailable) {
        val h2h = stats.headToHead(alice).single()
        h2h.opponent shouldBe "Bob"
        h2h.wins shouldBe 3
        h2h.losses shouldBe 0
        stats.headToHead(alice, GameFilter(OpponentFilter.AI)).shouldBeEmpty()
    }

    test("the admin roster and overview count played games only").config(enabled = dockerAvailable) {
        val roster = stats.allUsersWithStats(GameFilter(OpponentFilter.HUMANS)).associateBy { it.id }
        roster.getValue(alice).games shouldBe 2
        roster.getValue(bob).games shouldBe 2

        val overview = stats.overview()
        overview.totalGames shouldBe 5
        overview.notCountedGames shouldBe 2
        overview.gamesVsHumans shouldBe 2
        overview.gamesVsAi shouldBe 2
    }

    test("the AI-vs-human report leaves out a game the person never played").config(enabled = dockerAvailable) {
        val report = stats.aiVsHuman(days = null, bucket = AiBucket.WEEK)
        // g2, g3 and the pod g6; g7 (a no-show) is reported as left out instead of as an AI win.
        report.overall.games shouldBe 3
        report.overall.aiWins shouldBe 1
        report.notCounted shouldBe 1
    }
})
