package com.wingedsheep.ai.arena

import com.wingedsheep.ai.assist.DeckBuildRequest
import com.wingedsheep.ai.assist.DraftsimDeckBuildAdvisor
import com.wingedsheep.ai.engine.generateSeededSealedPool
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Does the synergy-aware sealed build make **stronger decks**? Deck strength is only measurable by
 * playing (card ratings alone don't separate decks), so: for each seeded sealed pool, build the deck
 * the AI would build ([DraftsimDeckBuildAdvisor], the AI's own builder) with the pool's synergy
 * reading and without it, and play the two against each other with the live AI on both seats, seats
 * swapped within a pair. A pool whose two builds come out identical can't differ and is skipped.
 *
 * ```
 * just arena-synergy-decks 200 BLB
 * ```
 *
 * Disabled unless `-DarenaSynergyDecks=true`, so a normal `:ai:test` run never pays for it.
 */
class SynergyDeckBenchmark : FunSpec({

    val enabled = System.getProperty("arenaSynergyDecks") == "true"
    val games = System.getProperty("arenaGames")?.toIntOrNull() ?: 200
    val seed = System.getProperty("arenaSeed")?.toLongOrNull() ?: ArenaConfig.DEFAULT_SEED
    val setCode = System.getProperty("arenaSet") ?: "BLB"
    val threads = System.getProperty("arenaThreads")?.toIntOrNull() ?: Runtime.getRuntime().availableProcessors()
    val gameTimeout: Duration? = (System.getProperty("arenaGameTimeoutSec")?.toLongOrNull() ?: 600L)
        .takeIf { it > 0 }?.seconds

    test("synergy-aware sealed builds vs the same pools built without synergy").config(enabled = enabled) {
        val set = MtgSetCatalog.requireByCode(setCode)
        val registry = harnessRegistry(set)
        val agent = ArenaAgents.resolve("live")
        val pairs = (games + 1) / 2

        fun deckOf(pool: List<CardDefinition>, synergy: Boolean): Deck? {
            val build = DraftsimDeckBuildAdvisor.buildDeck(DeckBuildRequest(pool = pool, setCodes = listOf(setCode)), synergy)
            val list = build.builds.firstOrNull()?.deckList ?: return null
            return Deck(list.flatMap { (name, n) -> List(n) { name } }.sorted())
        }

        // Pools whose two builds differ, in seed order, until there are enough for the run.
        val matchups = mutableListOf<Triple<Long, Deck, Deck>>()
        var identical = 0
        var poolId = 0L
        while (matchups.size < pairs && poolId < pairs * 10L) {
            val pairSeed = mixSeed(seed, poolId++)
            val pool = generateSeededSealedPool(set.cards, Random(pairSeed))
            val on = deckOf(pool, synergy = true) ?: continue
            val off = deckOf(pool, synergy = false) ?: continue
            if (on.cards == off.cards) identical++ else matchups += Triple(pairSeed, on, off)
        }
        println("=== SYNERGY DECKS: $setCode, ${matchups.size} pools with differing builds " +
            "($identical identical skipped), ${matchups.size * 2} games, live vs live ===")

        val finished = AtomicInteger(0)
        val winsSoFar = AtomicInteger(0)
        val decidedSoFar = AtomicInteger(0)
        val executor = Executors.newFixedThreadPool(threads)
        val results: List<Future<Pair<Int, Int>>> = try {
            matchups.mapIndexed { index, (pairSeed, on, off) ->
                executor.submit<Pair<Int, Int>> {
                    // Game A: synergy deck in seat 0; game B: in seat 1. Count (synergy wins, decided games).
                    val a = ArenaGameRunner.play(registry, agent, agent, on, off, pairSeed, index, 0, gameTimeout = gameTimeout)
                    val b = ArenaGameRunner.play(registry, agent, agent, off, on, pairSeed, index, 1, gameTimeout = gameTimeout)
                    val wins = (if (a.winnerSeat == 0) 1 else 0) + (if (b.winnerSeat == 1) 1 else 0)
                    val decided = listOf(a, b).count { it.winnerSeat != null }
                    val done = finished.incrementAndGet()
                    val totalWins = winsSoFar.addAndGet(wins)
                    val totalDecided = decidedSoFar.addAndGet(decided)
                    println("[$done/${matchups.size}] pool $index: synergy won $wins of $decided " +
                        "(${a.turns}+${b.turns} turns, ${(a.durationMs + b.durationMs) / 1000}s) — " +
                        "running $totalWins/$totalDecided")
                    wins to decided
                }
            }.also { futures -> futures.forEach { it.get() } }
        } finally {
            executor.shutdown()
        }

        val wins = results.sumOf { it.get().first }
        val decided = results.sumOf { it.get().second }
        if (decided == 0) {
            println("No decided games.")
        } else {
            val rate = wins.toDouble() / decided
            val stderr = sqrt(rate * (1 - rate) / decided)
            println("Synergy-aware build won $wins of $decided decided games: ${pct(rate)} ± ${pct(1.96 * stderr)} (95%)")
        }
    }
})
