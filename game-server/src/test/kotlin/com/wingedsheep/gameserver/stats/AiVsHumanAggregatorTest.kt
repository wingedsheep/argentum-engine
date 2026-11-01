package com.wingedsheep.gameserver.stats

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant

class AiVsHumanAggregatorTest : FunSpec({

    // A Wednesday, so day / week / month buckets all differ.
    val now = Instant.parse("2026-10-14T12:00:00Z")
    fun daysAgo(d: Long) = now.minusSeconds(d * 86_400L)

    fun game(
        id: Long,
        at: Instant,
        outcome: AiMatchOutcome,
        mode: String = "CASUAL",
        version: String? = "abc",
        format: String? = null,
        sets: List<String> = emptyList(),
    ) = AiMatchGame(id, at, mode, format, version, outcome, sets)

    test("win rate is over decisive games only, with a Wilson interval around it") {
        val r = AiRecord.of(listOf(AiMatchOutcome.AI, AiMatchOutcome.HUMAN, AiMatchOutcome.HUMAN, AiMatchOutcome.DRAW))
        r shouldBe AiRecord(games = 4, aiWins = 1, humanWins = 2, draws = 1)
        r.aiWinRate!! shouldBe (1.0 / 3 plusOrMinus 1e-9)
        r.aiWinRateLow!! shouldBe (0.0615 plusOrMinus 1e-3)
        r.aiWinRateHigh!! shouldBe (0.7923 plusOrMinus 1e-3)
    }

    test("an all-draw record has no win rate") {
        AiRecord.of(listOf(AiMatchOutcome.DRAW)).aiWinRate.shouldBeNull()
        AiRecord.EMPTY.aiWinRateLow.shouldBeNull()
    }

    test("a perfect record still has a non-degenerate interval") {
        val r = AiRecord.of(List(3) { AiMatchOutcome.AI })
        r.aiWinRateHigh shouldBe 1.0
        (r.aiWinRateLow!! < 0.5) shouldBe true
    }

    test("the range and the window before it are split, and the series is gap-filled") {
        val games = listOf(
            game(1, daysAgo(12), AiMatchOutcome.HUMAN), // previous window
            game(2, daysAgo(5), AiMatchOutcome.AI),
            game(3, daysAgo(5), AiMatchOutcome.HUMAN),
            game(4, daysAgo(1), AiMatchOutcome.AI),
        )
        val report = AiVsHumanAggregator.aggregate(games, emptyList(), days = 7, bucket = AiBucket.DAY, now = now)

        report.overall shouldBe AiRecord(3, 2, 1, 0)
        report.previous shouldBe AiRecord(1, 0, 1, 0)
        report.series.map { it.start }.first() shouldBe "2026-10-07"
        report.series.map { it.start }.last() shouldBe "2026-10-14"
        report.series.size shouldBe 8
        report.series.single { it.start == "2026-10-09" }.record shouldBe AiRecord(2, 1, 1, 0)
        report.series.single { it.start == "2026-10-10" }.record shouldBe AiRecord.EMPTY
    }

    test("all time has no previous window and starts the series at the first game") {
        val games = listOf(game(1, daysAgo(20), AiMatchOutcome.AI), game(2, daysAgo(2), AiMatchOutcome.HUMAN))
        val report = AiVsHumanAggregator.aggregate(games, emptyList(), days = null, bucket = AiBucket.WEEK, now = now)
        report.previous.shouldBeNull()
        report.series.first().start shouldBe "2026-09-21" // the Monday on or before 2026-09-24
        report.series.last().start shouldBe "2026-10-12"
    }

    test("builds are newest first and games without a build are left out of them") {
        val games = listOf(
            game(1, daysAgo(6), AiMatchOutcome.HUMAN, version = "old"),
            game(2, daysAgo(3), AiMatchOutcome.AI, version = "new"),
            game(3, daysAgo(2), AiMatchOutcome.AI, version = null),
        )
        val report = AiVsHumanAggregator.aggregate(games, emptyList(), days = 7, bucket = AiBucket.DAY, now = now)
        report.byBuild.map { it.version } shouldContainExactly listOf("new", "old")
        report.overall.games shouldBe 3
    }

    test("modes are ordered by games played") {
        val games = listOf(
            game(1, daysAgo(1), AiMatchOutcome.AI, mode = "TOURNAMENT"),
            game(2, daysAgo(1), AiMatchOutcome.AI, mode = "QUICK_GAME"),
            game(3, daysAgo(1), AiMatchOutcome.HUMAN, mode = "QUICK_GAME"),
        )
        val report = AiVsHumanAggregator.aggregate(games, emptyList(), days = 7, bucket = AiBucket.DAY, now = now)
        report.byMode.map { it.gameMode } shouldContainExactly listOf("QUICK_GAME", "TOURNAMENT")
    }

    test("a human's record counts a drawn game as a draw and a lost pod as a loss") {
        val games = listOf(
            game(1, daysAgo(1), AiMatchOutcome.HUMAN),
            game(2, daysAgo(1), AiMatchOutcome.AI),
            game(3, daysAgo(1), AiMatchOutcome.DRAW),
            game(4, daysAgo(30), AiMatchOutcome.AI), // outside the range
        )
        val seats = listOf(
            AiMatchHumanSeat(1, "user:a", "Alice", isGuest = false, won = true),
            AiMatchHumanSeat(2, "user:a", "Alice", isGuest = false, won = false),
            AiMatchHumanSeat(3, "user:a", "Alice", isGuest = false, won = false),
            AiMatchHumanSeat(4, "user:a", "Alice", isGuest = false, won = false),
            AiMatchHumanSeat(1, "guest:Bob", "Bob", isGuest = true, won = false),
        )
        val report = AiVsHumanAggregator.aggregate(games, seats, days = 7, bucket = AiBucket.DAY, now = now)
        report.topHumans shouldContainExactly listOf(
            AiHumanRecord("user:a", "Alice", isGuest = false, games = 3, wins = 1, losses = 1, draws = 1),
            AiHumanRecord("guest:Bob", "Bob", isGuest = true, games = 1, wins = 0, losses = 1, draws = 0),
        )
    }

    // Newest first, as the build writes it: builds A and B are plain commits on main; AI changes
    // "first" and "second" land between them, "third" after B with no build running it yet.
    val history = """
        # engine-ai-history v1
        ccc000000003
        aaa222222222	2026-10-12T10:00:00+02:00	12	AI: third
        bbb000000002
        aaa111111111	2026-10-10T10:00:00Z	11	AI: second
        aaa000000000	2026-10-09T10:00:00Z	-	AI: first
        bbb000000001
    """.trimIndent()

    test("a commit with several changelog entries keeps one place on main") {
        val timeline = EngineAiTimeline.parse(
            """
            # engine-ai-history v2
            bbb000000002
            aaa000000000	2026-10-09T10:00:00Z	7	AI: blocks better
            aaa000000000	2026-10-09T10:00:00Z	7	AI: attacks better
            bbb000000001
            """.trimIndent(),
        )
        timeline.changes.map { it.ordinal } shouldContainExactly listOf(1, 1)
        timeline.ordinalOf("bbb000000002") shouldBe 2
    }
    val buildA = "bbb000000001" + "f".repeat(28)
    val buildB = "bbb000000002" + "f".repeat(28)

    test("the history file parses into ordered changes and placeable builds") {
        val timeline = EngineAiTimeline.parse(history)
        timeline.available shouldBe true
        timeline.changes.map { it.subject } shouldContainExactly listOf("AI: first", "AI: second", "AI: third")
        timeline.changes.map { it.pr } shouldContainExactly listOf(null, 11, 12)
        timeline.ordinalOf(buildA) shouldBe 0
        timeline.ordinalOf(buildB.uppercase()) shouldBe 3
        timeline.ordinalOf("dev").shouldBeNull()
        EngineAiTimeline.parse("# unavailable\n").available shouldBe false
    }

    test("AI changes are counted per bucket, credited to the first build carrying them, and counted per build") {
        val timeline = EngineAiTimeline.parse(history)
        val games = listOf(
            game(1, daysAgo(6), AiMatchOutcome.HUMAN, version = buildA),
            game(2, daysAgo(3), AiMatchOutcome.AI, version = buildB),
            game(3, daysAgo(2), AiMatchOutcome.AI, version = "dev"),
        )
        val report = AiVsHumanAggregator.aggregate(games, emptyList(), 7, AiBucket.DAY, now, timeline)

        report.series.filter { it.aiChanges > 0 }.map { it.start to it.aiChanges } shouldContainExactly
            listOf("2026-10-09" to 1, "2026-10-10" to 1, "2026-10-12" to 1)

        report.aiChanges.available shouldBe true
        report.aiChanges.changes.map { it.subject to it.shippedIn } shouldContainExactly listOf(
            "AI: third" to null,
            "AI: second" to buildB,
            "AI: first" to buildB,
        )
        report.aiChanges.changes.last().shippedAt shouldBe daysAgo(3).toString()

        report.byBuild.associate { it.version to it.newAiChanges } shouldBe
            mapOf(buildB to 2, buildA to null, "dev" to null)
    }

    test("without a history there are no change markers") {
        val report = AiVsHumanAggregator.aggregate(listOf(game(1, daysAgo(1), AiMatchOutcome.AI)), emptyList(), 7, AiBucket.DAY, now)
        report.aiChanges.available shouldBe false
        report.series.sumOf { it.aiChanges } shouldBe 0
    }

    test("recent games keep only the latest, oldest first") {
        val games = (1L..50L).map { game(it, daysAgo(51 - it).plusSeconds(0), AiMatchOutcome.AI) }
        val report = AiVsHumanAggregator.aggregate(games, emptyList(), days = null, bucket = AiBucket.MONTH, now = now)
        report.recent.size shouldBe AiVsHumanAggregator.RECENT_GAMES
        report.recent.first().endedAt shouldBe daysAgo(40).toString()
        report.recent.last().endedAt shouldBe daysAgo(1).toString()
    }

    // A slice of the population as it looked in prod: limited games from one set, casual games, and
    // one regular who beats the AI almost every time.
    val mixed = listOf(
        game(1, daysAgo(2), AiMatchOutcome.HUMAN, mode = "TOURNAMENT", format = "DRAFT", sets = listOf("RFX")),
        game(2, daysAgo(2), AiMatchOutcome.HUMAN, mode = "TOURNAMENT", format = "DRAFT", sets = listOf("RFX")),
        game(3, daysAgo(3), AiMatchOutcome.AI, mode = "TOURNAMENT", format = "SEALED", sets = listOf("RFX", "BLB")),
        game(4, daysAgo(3), AiMatchOutcome.AI, mode = "CASUAL", format = "Standard"),
        game(5, daysAgo(4), AiMatchOutcome.AI, mode = "CASUAL", format = "Standard"),
        game(6, daysAgo(10), AiMatchOutcome.HUMAN, mode = "TOURNAMENT", format = "DRAFT", sets = listOf("RFX")), // previous window
    )
    val mixedSeats = listOf(
        AiMatchHumanSeat(1, "user:reg", "Regular", isGuest = false, won = true),
        AiMatchHumanSeat(2, "user:reg", "Regular", isGuest = false, won = true),
        AiMatchHumanSeat(3, "user:reg", "Regular", isGuest = false, won = false),
        AiMatchHumanSeat(4, "user:new", "Newcomer", isGuest = false, won = false),
        AiMatchHumanSeat(5, "guest:G", "G", isGuest = true, won = false),
        AiMatchHumanSeat(6, "user:reg", "Regular", isGuest = false, won = true),
    )

    test("options count every game in the range, before the filter applies") {
        val report = AiVsHumanAggregator.aggregate(
            mixed, mixedSeats, 7, AiBucket.DAY, now,
            filter = AiVsHumanFilter(gameMode = "CASUAL"),
            setName = { if (it == "RFX") "Reality Fracture" else null },
        )
        report.options.modes.map { Triple(it.gameMode, it.format, it.games) } shouldContainExactly listOf(
            Triple("CASUAL", "Standard", 2L),
            Triple("TOURNAMENT", "DRAFT", 2L),
            Triple("TOURNAMENT", "SEALED", 1L),
        )
        report.options.sets shouldContainExactly listOf(AiSetOption("RFX", "Reality Fracture", 3), AiSetOption("BLB", null, 1))
        report.options.gamesWithoutSet shouldBe 2
    }

    test("a mode and format filter narrows every aggregate, the previous window included") {
        val report = AiVsHumanAggregator.aggregate(
            mixed, mixedSeats, 7, AiBucket.DAY, now,
            filter = AiVsHumanFilter(gameMode = "TOURNAMENT", format = "DRAFT"),
        )
        report.overall shouldBe AiRecord(2, 0, 2, 0)
        report.previous shouldBe AiRecord(1, 0, 1, 0)
        report.byMode.map { it.format } shouldContainExactly listOf("DRAFT")
        report.recent.size shouldBe 2
        report.series.sumOf { it.record.games } shouldBe 2
        report.topHumans.map { it.key to it.games } shouldContainExactly listOf("user:reg" to 2L)
        report.filter.gameMode shouldBe "TOURNAMENT"
    }

    test("a set filter keeps every game whose lobby drew from that set") {
        val report = AiVsHumanAggregator.aggregate(mixed, mixedSeats, 7, AiBucket.DAY, now, filter = AiVsHumanFilter(setCode = "RFX"))
        report.overall shouldBe AiRecord(3, 1, 2, 0)
        AiVsHumanAggregator.aggregate(mixed, mixedSeats, 7, AiBucket.DAY, now, filter = AiVsHumanFilter(setCode = "BLB"))
            .overall shouldBe AiRecord(1, 1, 0, 0)
    }

    test("excluding a human drops every game they sat in") {
        val report = AiVsHumanAggregator.aggregate(
            mixed, mixedSeats, 7, AiBucket.DAY, now,
            filter = AiVsHumanFilter(excludeHumans = setOf("user:reg")),
        )
        report.overall shouldBe AiRecord(2, 2, 0, 0)
        report.previous shouldBe AiRecord.EMPTY
        report.topHumans.map { it.key } shouldContainExactly listOf("guest:G", "user:new")
        report.filter.excludedHumans.map { it.key to it.games } shouldContainExactly listOf("user:reg" to 3L)
        report.filter.excludedGames shouldBe 3
    }

    test("excluding the top humans picks the busiest players within the slice") {
        // Overall the regular is busiest; among casual games every human has one game, so the
        // alphabetical tiebreak picks the guest.
        AiVsHumanAggregator.aggregate(mixed, mixedSeats, 7, AiBucket.DAY, now, filter = AiVsHumanFilter(excludeTopHumans = 1))
            .filter.excludedHumans.map { it.key } shouldContainExactly listOf("user:reg")
        val casual = AiVsHumanAggregator.aggregate(
            mixed, mixedSeats, 7, AiBucket.DAY, now,
            filter = AiVsHumanFilter(gameMode = "CASUAL", excludeTopHumans = 1),
        )
        casual.filter.excludedHumans.map { it.key } shouldContainExactly listOf("guest:G")
        casual.overall shouldBe AiRecord(1, 1, 0, 0)
        // Named exclusions don't use up the top-N count: the next busiest goes too.
        AiVsHumanAggregator.aggregate(
            mixed, mixedSeats, 7, AiBucket.DAY, now,
            filter = AiVsHumanFilter(excludeHumans = setOf("user:reg"), excludeTopHumans = 1),
        ).filter.excludedHumans.map { it.key } shouldContainExactly listOf("user:reg", "guest:G")
    }

    test("each mode gets its own series on the report's buckets, most-played first") {
        val report = AiVsHumanAggregator.aggregate(mixed, mixedSeats, 7, AiBucket.DAY, now)
        report.seriesByMode.map { it.gameMode to it.format } shouldContainExactly listOf(
            "CASUAL" to "Standard",
            "TOURNAMENT" to "DRAFT",
            "TOURNAMENT" to "SEALED",
        )
        report.seriesByMode.forEach { m -> m.series.map { it.start } shouldBe report.series.map { it.start } }
        report.seriesByMode.first { it.format == "DRAFT" }.series.sumOf { it.record.humanWins } shouldBe 2
    }

    test("a recent game names its humans and both sides' colours, so its decks can be opened") {
        val games = listOf(
            AiMatchGame(1, daysAgo(1), "CASUAL", null, "abc", AiMatchOutcome.HUMAN, aiColors = listOf("BR"), replayGameId = "g-1"),
            AiMatchGame(2, daysAgo(1).plusSeconds(60), "CASUAL", null, "abc", AiMatchOutcome.AI, aiColors = listOf(null)),
        )
        val seats = listOf(
            AiMatchHumanSeat(1, "user:a", "Alice", isGuest = false, won = true, colors = "WU"),
            AiMatchHumanSeat(2, "guest:Bob", "Bob", isGuest = true, won = false),
        )
        val recent = AiVsHumanAggregator.aggregate(games, seats, 7, AiBucket.DAY, now).recent
        recent.map { it.matchId } shouldContainExactly listOf(1L, 2L)
        recent[0].humans shouldContainExactly listOf(AiRecentHuman("user:a", "Alice", isGuest = false, won = true, colors = "WU"))
        recent[0].aiColors shouldContainExactly listOf("BR")
        recent[0].replayGameId shouldBe "g-1"
        recent[1].humans.single().isGuest shouldBe true
        recent[1].replayGameId.shouldBeNull()
    }

    test("a human's games are the ones their row counts, newest first, ignoring human exclusions") {
        val result = AiVsHumanAggregator.humanGames(
            mixed, mixedSeats, "user:reg", 7, now,
            filter = AiVsHumanFilter(setCode = "RFX", excludeHumans = setOf("user:reg")),
        )
        result.games.map { it.matchId } shouldContainExactly listOf(1L, 2L, 3L)
        result.human shouldBe AiHumanRecord("user:reg", "Regular", isGuest = false, games = 3, wins = 2, losses = 1, draws = 0)
        val report = AiVsHumanAggregator.aggregate(mixed, mixedSeats, 7, AiBucket.DAY, now, filter = AiVsHumanFilter(setCode = "RFX"))
        report.topHumans.first { it.key == "user:reg" } shouldBe result.human
    }

    test("a human with no games in the slice has no record") {
        val result = AiVsHumanAggregator.humanGames(mixed, mixedSeats, "user:new", 7, now, filter = AiVsHumanFilter(gameMode = "TOURNAMENT"))
        result.human.shouldBeNull()
        result.games shouldBe emptyList()
    }
})
