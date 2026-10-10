package com.wingedsheep.ai.arena

import com.wingedsheep.ai.engine.buildSeededSealedDeck
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The recoverable half of an arena run: a run that never finishes must still leave a report behind,
 * and a game that will not end must not hold the run hostage.
 *
 * Fast on purpose — synthetic outcomes for the file round trip, and an already-expired deadline for
 * the timeout, so nothing here waits on a real long game.
 */
class ArenaResultsFileTest : FunSpec({

    fun game(pairId: Int, gameIndex: Int, winnerSeat: Int?, drawReason: String = "") = ArenaGameOutcome(
        pairId = pairId, gameIndex = gameIndex,
        seat0Agent = if (gameIndex == 0) "alpha" else "beta",
        seat1Agent = if (gameIndex == 0) "beta" else "alpha",
        seed = 1000L + pairId, winnerSeat = winnerSeat, turns = 12, actions = 300, durationMs = 450,
        seat0Life = 7, seat1Life = 0, completed = winnerSeat != null, drawReason = drawReason,
        exception = null, illegalActions = emptyMap(), actionStreamHash = null,
    )

    val pairs = listOf(
        ArenaPair(1, game(1, 0, 0), game(1, 1, 0)),
        ArenaPair(2, game(2, 0, null, "timeout(600s,turn=40,actions=900)"), game(2, 1, 1)),
        ArenaPair(3, game(3, 0, 1), game(3, 1, null, "maxTurns(50)")),
    )

    fun config(games: Int) = ArenaConfig(
        agentA = ArenaAgents.resolve("v0"), agentB = ArenaAgents.resolve("v0-blind"),
        games = games, setCode = "POR", threads = 2, gameTimeout = 600.seconds,
    )

    fun tempDir(): File = Files.createTempDirectory("argentum-arena-results-").toFile()

    test("a results file round-trips to the same statistics as the in-memory pairs") {
        val dir = tempDir()
        try {
            ArenaResultsFile.open(dir, config(games = 6)).use { file -> pairs.forEach(file::append) }
            val rows = File(dir, ArenaResultsFile.RESULTS).readLines().drop(1)
            ArenaStats.of("alpha", "beta", ArenaResultsFile.parsePairs(rows)) shouldBe
                ArenaStats.of("alpha", "beta", pairs)
        } finally {
            dir.deleteRecursively()
        }
    }

    test("a killed run reports the whole pairs it finished, says PARTIAL, and counts its timeouts") {
        val dir = tempDir()
        try {
            ArenaResultsFile.open(dir, config(games = 20)).use { file -> pairs.forEach(file::append) }
            // A kill mid-write leaves a torn row and, at worst, half a pair. Neither may count.
            File(dir, ArenaResultsFile.RESULTS).appendText("4,0,alpha,beta,1004,0,12,300,450,7,0,true,0,,\n4,1,alp")

            val report = ArenaResultsFile.report(dir)
            report shouldContain "PARTIAL RUN:  3 of 10 pairs finished"
            report shouldContain "Games:        6 (3 pairs), set=POR"
            report shouldContain "TIMEOUTS:     1 game(s) hit the 600s wall-clock cap"
            report shouldContain "1x timeout"
            // The results file alone is enough — the directory and the CSV path both work.
            ArenaResultsFile.report(File(dir, ArenaResultsFile.RESULTS)) shouldBe report
        } finally {
            dir.deleteRecursively()
        }
    }

    test("a finished run's report is not marked partial") {
        val dir = tempDir()
        try {
            ArenaResultsFile.open(dir, config(games = 6)).use { file -> pairs.forEach(file::append) }
            ArenaResultsFile.report(dir) shouldNotContain "PARTIAL"
        } finally {
            dir.deleteRecursively()
        }
    }

    test("a pod results file round-trips whole rotation groups and drops a torn one") {
        val table = TableSetup.FFA3
        fun podGame(groupId: Int, rotation: Int, winnerSeat: Int?) = PodGame(
            aSeat = rotation,
            outcome = TableGameOutcome(
                groupId = groupId, rotation = rotation, setup = table,
                seatAgents = List(3) { if (it == rotation) "alpha" else "beta" },
                seed = 7L, winnerSeat = winnerSeat, winnerTeam = winnerSeat, turns = 30, actions = 900,
                durationMs = 1200, lifeBySeat = listOf(3, 0, 0), completed = winnerSeat != null,
                drawReason = if (winnerSeat == null) "maxTurns(30)" else "", exception = null,
                illegalActions = emptyMap(), actionStreamHash = null,
            ),
        )
        val groups = listOf(
            PodGroup(1, listOf(podGame(1, 0, 0), podGame(1, 1, 2), podGame(1, 2, null))),
            PodGroup(2, listOf(podGame(2, 0, 1), podGame(2, 1, 1), podGame(2, 2, 2))),
        )
        val dir = tempDir()
        try {
            val podConfig = PodArenaConfig(
                agentA = ArenaAgents.resolve("v0"), agentB = ArenaAgents.resolve("v0-blind"),
                table = table, groups = 5, setCode = "POR",
            )
            ArenaResultsFile.open(dir, podConfig).use { file ->
                groups.forEach(file::append)
                // Group 3 got one rotation in before the kill — it must not enter the sample.
                file.append(PodGroup(3, listOf(podGame(3, 0, 0))))
            }
            val rows = File(dir, ArenaResultsFile.RESULTS).readLines().drop(1)
            PodArenaStats.of("alpha", "beta", table, ArenaResultsFile.parseGroups(rows, table)) shouldBe
                PodArenaStats.of("alpha", "beta", table, groups)
            ArenaResultsFile.report(dir) shouldContain "PARTIAL RUN:  2 of 5 rotation groups finished"
        } finally {
            dir.deleteRecursively()
        }
    }

    test("an expired per-game deadline ends the game as a timeout draw between actions") {
        val set = MtgSetCatalog.requireByCode("POR")
        val registry = harnessRegistry(set)
        val seed = mixSeed(ArenaConfig.DEFAULT_SEED, 1L)
        val deck = buildSeededSealedDeck(set.cards, Random(seed))
        val v0 = ArenaAgents.resolve("v0")

        // Already past the deadline before the first action: the loop's between-action check must
        // fire, rather than the game playing on until a turn or action cap.
        val outcome = ArenaGameRunner.play(
            registry, v0, v0, deck, deck, seed = seed, pairId = 1, gameIndex = 0,
            gameTimeout = Duration.ZERO,
        )
        outcome.drawReason shouldStartWith "${TableGameRunner.TIMEOUT_REASON}("
        outcome.completed shouldBe false
        outcome.winnerSeat shouldBe null
        outcome.exception shouldBe null

        val stats = ArenaStats.of("v0", "v0", listOf(ArenaPair(1, outcome, outcome)))
        stats.timeouts shouldBe 2
        stats.drawReasons[TableGameRunner.TIMEOUT_REASON] shouldBe 2
    }
})
