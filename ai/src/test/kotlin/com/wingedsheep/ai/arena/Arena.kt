package com.wingedsheep.ai.arena

import com.wingedsheep.ai.engine.buildSeededSealedDeck
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.MtgSet
import java.util.Locale
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.nio.file.Path
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * A head-to-head arena run: agent A vs agent B over N paired games.
 *
 * @param games total games. Rounded **up** to an even number, because a pair is the unit — a
 *   half-played pair would reintroduce exactly the seat bias pairing exists to remove.
 * @param seed the run seed. Everything downstream — decks, shuffles, turn order, every "at random"
 *   choice in the engine — derives from it, so two runs at the same seed play the same games.
 * @param setCode the sealed pool both seats build from. Both seats get the **same 40-card
 *   decklist** in a pair (they still draw different shuffles of it), which is the lowest-variance
 *   design and matches what `AdvisorBenchmark` measured.
 */
data class ArenaConfig(
    val agentA: ArenaAgent,
    val agentB: ArenaAgent,
    val games: Int,
    val seed: Long = DEFAULT_SEED,
    val setCode: String = "BLB",
    val maxTurns: Int = 50,
    val threads: Int = Runtime.getRuntime().availableProcessors(),
    val featureOutput: Path? = null,
) {
    val pairs: Int get() = (games + 1) / 2

    companion object {
        /** Fixed so `just arena` with no seed argument is still reproducible. */
        const val DEFAULT_SEED = 20260727L
    }
}

/** A completed run: the raw pairs, the derived statistics, and the wall clock it took. */
data class ArenaRun(
    val config: ArenaConfig,
    val pairs: List<ArenaPair>,
    val stats: ArenaStats,
    val wallClock: Duration,
)

object Arena {

    fun run(config: ArenaConfig, onProgress: (completed: Int, total: Int, pair: ArenaPair) -> Unit = { _, _, _ -> }): ArenaRun {
        val set = MtgSetCatalog.requireByCode(config.setCode)
        val registry = CardRegistry().apply {
            register(set.cards)
            register(set.basicLands)
        }
        val featureCollector = config.featureOutput?.let {
            ArenaFeatureCollector(it, registry, config.setCode)
        }

        val pool = Executors.newFixedThreadPool(config.threads)
        try {
            val completionService = ExecutorCompletionService<ArenaPair>(pool)
            val finished = AtomicInteger(0)
            var pairs: List<ArenaPair>
            val wallClock = measureTime {
                // Submitted as pairs, never as individual games: a partial run then always holds a
                // whole number of pairs, so an interrupted arena is still an unbiased sample.
                (1..config.pairs).forEach { pairId ->
                    completionService.submit { playPair(registry, set, config, pairId, featureCollector) }
                }
                pairs = (1..config.pairs).map {
                    completionService.take().get().also { pair ->
                        onProgress(finished.incrementAndGet(), config.pairs, pair)
                    }
                }.sortedBy { it.pairId }
            }
            return ArenaRun(
                config = config,
                pairs = pairs,
                stats = ArenaStats.of(config.agentA.name, config.agentB.name, pairs),
                wallClock = wallClock,
            )
        } finally {
            pool.shutdown()
        }
    }

    /**
     * One pair: identical decks, identical game seed, both seat orders.
     *
     * The seed is per pair, not per game, so game A and game B share seat 0's library, seat 1's
     * library and the turn order. The only thing that differs is which agent sits where.
     */
    private fun playPair(
        registry: CardRegistry,
        set: MtgSet,
        config: ArenaConfig,
        pairId: Int,
        featureCollector: ArenaFeatureCollector?,
    ): ArenaPair {
        val pairSeed = mixSeed(config.seed, pairId.toLong())
        val deck = buildSeededSealedDeck(set.cards, Random(pairSeed))

        val gameA = ArenaGameRunner.play(
            registry, seat0 = config.agentA, seat1 = config.agentB,
            seat0Deck = deck, seat1Deck = deck,
            seed = pairSeed, pairId = pairId, gameIndex = 0, maxTurns = config.maxTurns,
            featureCollector = featureCollector,
        )
        val gameB = ArenaGameRunner.play(
            registry, seat0 = config.agentB, seat1 = config.agentA,
            seat0Deck = deck, seat1Deck = deck,
            seed = pairSeed, pairId = pairId, gameIndex = 1, maxTurns = config.maxTurns,
            featureCollector = featureCollector,
        )
        return ArenaPair(pairId, gameA, gameB)
    }
}

/**
 * SplitMix64 finalizer over `(runSeed, pairId)`.
 *
 * `runSeed + pairId` would be wrong: two runs one apart in seed would share most of their games and
 * look far more correlated than they are.
 */
internal fun mixSeed(runSeed: Long, pairId: Long): Long {
    var z = runSeed * -0x61c8864680b583ebL + pairId * -0x7ee3623a03d3c83fL
    z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
    z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
    return z xor (z ushr 31)
}

/** `0.532` → `"53.2%"`. Locale-independent, so committed numbers read the same everywhere. */
internal fun pct(value: Double): String = "${(value * 1000).roundToInt() / 10.0}%"

/**
 * `String.format` pinned to [Locale.ROOT].
 *
 * Not a nicety: on a machine with a comma decimal separator the default locale renders a pair
 * score as `+0,000` and a CSV column as `11,4`, which silently corrupts every `results.csv` the
 * arena writes.
 */
internal fun fmt(format: String, value: Double): String = String.format(Locale.ROOT, format, value)
