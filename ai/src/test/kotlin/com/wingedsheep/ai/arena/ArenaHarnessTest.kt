package com.wingedsheep.ai.arena

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files

/**
 * Proves the arena measures what it claims to, before anyone reads a win rate off it.
 *
 * The Phase 1 exit criterion is "`just arena v0 v0 300` returns 50% with the CI containing 50%".
 * That is weaker than what is actually true and testable: with the same agent on both seats, the
 * two games of a pair are **the same game**, so the mirror must come out at *exactly* 50% with
 * zero variance. Any deviation is a real defect — a seat leak, a seed leak, or nondeterminism in
 * the AI — and would otherwise hide inside a confidence interval.
 *
 * Portal, not Bloomburrow: simpler cards run faster, and this test asserts structure, not strength.
 */
class ArenaHarnessTest : FunSpec({

    test("resource-backed evaluation vectors are arena-addressable without recompiling") {
        ArenaAgents.resolve("eval-default").profile.evalWeightsId shouldBe "default"
        ArenaAgents.resolve("eval-blind").profile.evalWeightsId shouldBe "blind"
    }

    // Six games. Enough for the structural claims; small enough to stay in the always-on suite.
    val config = ArenaConfig(
        agentA = ArenaAgents.resolve("v0"),
        agentB = ArenaAgents.resolve("v0"),
        games = 6,
        setCode = "POR",
        maxTurns = 25,
    )

    test("a v0 mirror is exactly 50% — the seat/seed-leak detector") {
        val run = Arena.run(config)

        run.pairs.forEach { pair ->
            // Same agent on both seats + same seed + same decks => literally the same game twice.
            withClue("pair ${pair.pairId}") {
                pair.gameA.winnerSeat shouldBe pair.gameB.winnerSeat
                pair.gameA.turns shouldBe pair.gameB.turns
                pair.gameA.actions shouldBe pair.gameB.actions
                pair.score shouldBe 0.0
            }
        }

        run.stats.meanPairScore shouldBe 0.0
        run.stats.pairWinShare shouldBe 0.5
        run.stats.pairScoreCi shouldBe Interval(0.0, 0.0)
        run.stats.aWins shouldBe run.stats.bWins
    }

    test("the same seed replays the same games, across threads") {
        // Two runs of the same config, one of them single-threaded. Any thread-local
        // nondeterminism in the AI or the engine would show up as a divergence here.
        val parallel = Arena.run(config)
        val sequential = Arena.run(config.copy(threads = 1))

        parallel.pairs.zip(sequential.pairs).forEach { (a, b) ->
            withClue("pair ${a.pairId}") {
                a.gameA.winnerSeat shouldBe b.gameA.winnerSeat
                a.gameA.turns shouldBe b.gameA.turns
                a.gameA.actions shouldBe b.gameA.actions
                a.gameA.seat0Life shouldBe b.gameA.seat0Life
                a.gameA.seat1Life shouldBe b.gameA.seat1Life
            }
        }
    }

    test("a different seed plays different games") {
        // Not a strength claim — just that the seed is threaded through to the decks and the
        // shuffles rather than being decorative.
        val original = Arena.run(config)
        val other = Arena.run(config.copy(seed = config.seed + 1))
        val identical = original.pairs.zip(other.pairs).count { (a, b) -> a.gameA.actions == b.gameA.actions }
        withClue("all ${config.pairs} pairs played identically at two different seeds") {
            (identical < config.pairs) shouldBe true
        }
    }

    test("games run clean: no illegal actions, no engine exceptions") {
        val run = Arena.run(config)
        run.stats.illegalActions.keys.shouldBeEmpty()
        run.stats.exceptions.keys.shouldBeEmpty()
    }

    test("feature collection writes labelled raw positions as JSONL") {
        val output = Files.createTempFile("argentum-arena-features-", ".jsonl")
        try {
            Arena.run(config.copy(games = 2, threads = 2, featureOutput = output))
            val rows = Files.readAllLines(output)
            rows.size shouldBeGreaterThan 0
            rows.forEach { line ->
                val row = Json.parseToJsonElement(line).jsonObject
                row.keys shouldBe setOf(
                    "features", "toMove", "turn", "gameId", "setCode", "agent", "result"
                )
                row.getValue("features").jsonObject.containsKey("lifeDifference") shouldBe true
                row.getValue("setCode").jsonPrimitive.content shouldBe "POR"
                row.getValue("agent").jsonPrimitive.content shouldBe "v0"
                (row.getValue("result").jsonPrimitive.content.toInt() in (-1..1)) shouldBe true
            }
        } finally {
            Files.deleteIfExists(output)
        }
    }

    test("every name in gauntlet.json resolves to a real agent") {
        // Catches a typo in the committed gauntlet at :ai:test time rather than 40 minutes into
        // a `just arena-gauntlet` run.
        loadGauntlet().size shouldBe loadGauntlet().map { it.name }.distinct().size
    }
})
