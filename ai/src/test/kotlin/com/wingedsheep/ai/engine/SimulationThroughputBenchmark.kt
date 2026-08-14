package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import java.util.Locale
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.lang.management.ManagementFactory
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.measureTime

/**
 * Measures the primitives a rollout evaluator would spend its budget on, so that
 * Phase 5 of `backlog/engine-ai-improvement.md` has a real number to beat instead
 * of the stale `~404 actions/sec/thread` in `docs/engine-performance.md`.
 *
 * Unlike [RandomActionBenchmark], the games here are driven by the **real
 * [AIPlayer]** on both seats. Random play reaches states no real game reaches
 * (random discards, nonsense attacks), and branching factor and projection cost
 * are both state-dependent, so the state distribution has to be realistic.
 *
 * At each priority window the benchmark measures, on the *live* state:
 *
 * - **`ActionProcessor.process` calls/sec** — one timed `process` per sampled
 *   candidate. This is the atom a playout is built from.
 * - **`GameSimulator.simulate` calls/sec** — the same candidates through
 *   `simulate`, which includes `resolveToQuietState`'s auto-pass loop. This is
 *   what the Strategist actually pays per candidate today.
 * - **`StateProjector.project` ns** — timed on `state.copy()`, whose `by lazy`
 *   projection is unforced. That is deliberately the *zero-cache-hit* case: a
 *   rollout visits each state once, so it never benefits from the per-instance memo.
 * - **branching factor, pre- and post-filter** — enumerator output vs. the
 *   Strategist's current candidate filter (`affordable && !isManaAbility && !pass`).
 *   Phase 4's meaningful-action filter is measured against the post-filter number.
 * - **priority windows per game** — the multiplier that turns per-decision cost
 *   into per-game cost.
 *
 * Measurement roughly triples the cost of a game, so wall-clock here is not
 * comparable to [AdvisorBenchmark]'s. Only the ratios matter.
 *
 * Disabled by default. Run with:
 *   just benchmark-throughput
 *   just benchmark-throughput 40 BLB
 *   ./gradlew :ai:test --tests "*.SimulationThroughputBenchmark" -Dbenchmark=true \
 *       -DbenchmarkGames=20 -DbenchmarkSet=BLB
 */
class SimulationThroughputBenchmark : FunSpec({

    val numGames = System.getProperty("benchmarkGames")?.toIntOrNull() ?: 20
    val benchmarkEnabled = System.getProperty("benchmark") == "true"
    val setCode = System.getProperty("benchmarkSet") ?: "BLB"
    val cores = Runtime.getRuntime().availableProcessors()
    // The measured rates climb steeply until the JIT settles — 20 / 40 / 200-game runs
    // reported 838 / 1299 / 2332 process()/sec for the same code before this existed.
    // Two warmup games per thread, discarded, gets the reported numbers reproducible.
    val warmupGames = System.getProperty("benchmarkWarmupGames")?.toIntOrNull() ?: (cores * 2)

    test("benchmark: simulation throughput over $numGames AI games ($setCode)").config(enabled = benchmarkEnabled) {
        val set = MtgSetCatalog.requireByCode(setCode)
        val registry = CardRegistry().apply {
            register(set.cards)
            register(set.basicLands)
        }
        val pool = Executors.newFixedThreadPool(cores)
        val completionService = ExecutorCompletionService<ThroughputSample>(pool)
        val finished = AtomicInteger(0)

        println("=== SIMULATION THROUGHPUT BENCHMARK: $numGames AI games on $cores threads ($setCode) ===")

        fun submitGame(gameId: Int, total: Int, label: String) {
            // Seeded per game so a rerun measures the same games.
            val rng = Random(gameId.toLong())
            completionService.submit {
                val deck1 = buildSeededSealedDeck(set.cards, rng)
                val deck2 = buildSeededSealedDeck(set.cards, rng)
                measureGame(registry, deck1, deck2, seed = gameId.toLong()).also {
                    val n = finished.incrementAndGet()
                    if (n <= 3 || n % 5 == 0 || n == total) {
                        println(
                            "  $label[$n/$total] ${it.turns} turns, ${it.priorityWindows} windows, " +
                                "${it.processCalls} process, ${it.simulateCalls} simulate" +
                                (it.crashError?.let { e -> ", CRASH $e" } ?: "")
                        )
                    }
                }
            }
        }

        if (warmupGames > 0) {
            println("--- WARMUP: $warmupGames games (discarded) ---")
            // Negative ids so warmup never shares a seed with a measured game.
            (1..warmupGames).forEach { submitGame(-it, warmupGames, "warmup ") }
            repeat(warmupGames) { completionService.take().get() }
            finished.set(0)
        }

        val wallTime = measureTime {
            (1..numGames).forEach { submitGame(it, numGames, "") }
            val samples = (1..numGames).map { completionService.take().get() }
            printReport(samples, numGames)
        }

        println("Wall time:  ${wallTime.inWholeMilliseconds}ms ($numGames games on $cores threads)")
        println("Concurrent measured throughput: ${fmt(numGames * 1000.0 / wallTime.inWholeMilliseconds, 2)} games/sec")
        println("NOTE: wall time includes ~2-3x measurement overhead; only the rates above are meaningful.")
        pool.shutdown()
    }
})

// ─────────────────────────────────────────────────────────────────────────────
// Measurement
// ─────────────────────────────────────────────────────────────────────────────

/** Max candidates per window put through the timed process/simulate probes. */
private const val PROBES_PER_WINDOW = 4

private data class ThroughputSample(
    val turns: Int,
    val gameOver: Boolean,
    val priorityWindows: Int,
    val processCalls: Int,
    val processNs: Long,
    val simulateCalls: Int,
    val simulateNs: Long,
    val projectCount: Int,
    val projectNs: Long,
    val enumerateCalls: Int,
    val enumerateNs: Long,
    val preFilterActions: Long,
    val postFilterActions: Long,
    /** Windows where the Strategist has nothing to score — the auto-pass opportunity. */
    val emptyCandidateWindows: Int,
    /** `process` on the action actually played, i.e. the real action mix a playout replays. */
    val playedProcessCalls: Int,
    val playedProcessNs: Long,
    val decisionCount: Int,
    val chooseActionCount: Int,
    val chooseActionNs: Long,
    val chooseActionSamplesNs: List<Long>,
    val chooseActionAllocatedBytes: Long,
    val peakUsedHeapBytes: Long,
    val crashError: String? = null
)

/**
 * Play one AI-vs-AI game, probing the engine primitives at every priority window.
 *
 * The probes run on the live pre-action state and their results are discarded —
 * they never influence which action is taken, so the measured game is the same
 * game the AI would have played unmeasured.
 */
private fun measureGame(
    registry: CardRegistry,
    deck1: Deck,
    deck2: Deck,
    seed: Long,
    // Turns per player, not rounds — `GameState.turnNumber` counts player turns.
    maxTurns: Int = 100
): ThroughputSample {
    val processor = ActionProcessor(registry)
    val enumerator = LegalActionEnumerator.create(registry)
    val simulator = GameSimulator(registry)
    val initializer = GameInitializer(registry)

    val init = initializer.initializeGame(
        GameConfig(
            players = listOf(PlayerConfig("P1", deck1), PlayerConfig("P2", deck2)),
            skipMulligans = true,
            startingPlayerIndex = 0,
            seed = seed
        )
    )

    val p1 = init.state.turnOrder[0]
    val p2 = init.state.turnOrder[1]
    val ai1 = AIPlayer.create(registry, p1)
    val ai2 = AIPlayer.create(registry, p2)
    fun aiFor(playerId: EntityId) = if (playerId == p1) ai1 else ai2

    var state: GameState = init.state
    var priorityWindows = 0
    var processCalls = 0
    var processNs = 0L
    var simulateCalls = 0
    var simulateNs = 0L
    var projectCount = 0
    var projectNs = 0L
    var enumerateCalls = 0
    var enumerateNs = 0L
    var preFilterActions = 0L
    var postFilterActions = 0L
    var emptyCandidateWindows = 0
    var playedProcessCalls = 0
    var playedProcessNs = 0L
    var decisionCount = 0
    var chooseActionCount = 0
    var chooseActionNs = 0L
    val chooseActionSamplesNs = mutableListOf<Long>()
    var chooseActionAllocatedBytes = 0L
    var peakUsedHeapBytes = 0L
    val allocationBean = (ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean)
        ?.takeIf { it.isThreadAllocatedMemorySupported }
    var crashError: String? = null

    var actionCount = 0
    var lastProgressTurn = 0
    var lastProgressAction = 0

    try {
        while (!state.gameOver && state.turnNumber < maxTurns) {
            // Stuck detection, borrowed from AIBenchmark.
            if (actionCount - lastProgressAction > 300 && state.turnNumber == lastProgressTurn) break
            if (state.turnNumber > lastProgressTurn) {
                lastProgressTurn = state.turnNumber
                lastProgressAction = actionCount
            }

            val decision = state.pendingDecision
            if (decision != null) {
                decisionCount++
                actionCount++
                val response = aiFor(decision.playerId).respondToDecision(state, decision)
                val r = processor.process(state, SubmitDecision(decision.playerId, response)).result
                if (r.error != null) break
                state = r.state
                continue
            }

            val priorityPlayer = state.priorityPlayerId ?: break
            priorityWindows++
            actionCount++

            // ── Probe 1: cold projection cost ──
            // copy() is shallow and gives a fresh `by lazy`, so this times a full
            // project() with no memo hit — the case every rollout state is in.
            val coldState = state.copy()
            val projectStart = System.nanoTime()
            coldState.projectedState
            projectNs += System.nanoTime() - projectStart
            projectCount++

            // ── Probe 2: enumeration + branching factor ──
            val enumStart = System.nanoTime()
            val legalActions = enumerator.enumerate(state, priorityPlayer, EnumerationMode.ACTIONS_ONLY)
            enumerateNs += System.nanoTime() - enumStart
            enumerateCalls++

            preFilterActions += legalActions.size
            val candidates = strategistCandidateFilter(legalActions)
            postFilterActions += candidates.size
            if (candidates.isEmpty()) emptyCandidateWindows++

            // ── Probe 3: process() and simulate() on real candidates ──
            for (candidate in candidates.take(PROBES_PER_WINDOW)) {
                val processStart = System.nanoTime()
                processor.process(state, candidate.action)
                processNs += System.nanoTime() - processStart
                processCalls++

                val simulateStart = System.nanoTime()
                simulator.simulate(state, candidate.action)
                simulateNs += System.nanoTime() - simulateStart
                simulateCalls++
            }

            // ── Advance the game with the real AI ──
            val ai = aiFor(priorityPlayer)
            val allocatedBefore = allocationBean?.getThreadAllocatedBytes(Thread.currentThread().threadId()) ?: -1L
            val chooseStart = System.nanoTime()
            val action = ai.chooseAction(state)
            val chooseElapsed = System.nanoTime() - chooseStart
            chooseActionNs += chooseElapsed
            chooseActionSamplesNs += chooseElapsed
            val allocatedAfter = allocationBean?.getThreadAllocatedBytes(Thread.currentThread().threadId()) ?: -1L
            if (allocatedBefore >= 0 && allocatedAfter >= allocatedBefore) {
                chooseActionAllocatedBytes += allocatedAfter - allocatedBefore
            }
            val heap = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used
            if (heap > peakUsedHeapBytes) peakUsedHeapBytes = heap
            chooseActionCount++

            val playedStart = System.nanoTime()
            val r = processor.process(state, action).result
            playedProcessNs += System.nanoTime() - playedStart
            playedProcessCalls++

            state = if (r.error != null) {
                val fallback = processor.process(state, safeFallbackAction(state, priorityPlayer, enumerator)).result
                if (fallback.error != null) break
                fallback.state
            } else {
                r.state
            }
        }
    } catch (e: Throwable) {
        crashError = "${e::class.simpleName}: ${e.message}"
    }

    return ThroughputSample(
        turns = state.turnNumber,
        gameOver = state.gameOver,
        priorityWindows = priorityWindows,
        processCalls = processCalls,
        processNs = processNs,
        simulateCalls = simulateCalls,
        simulateNs = simulateNs,
        projectCount = projectCount,
        projectNs = projectNs,
        enumerateCalls = enumerateCalls,
        enumerateNs = enumerateNs,
        preFilterActions = preFilterActions,
        postFilterActions = postFilterActions,
        emptyCandidateWindows = emptyCandidateWindows,
        playedProcessCalls = playedProcessCalls,
        playedProcessNs = playedProcessNs,
        decisionCount = decisionCount,
        chooseActionCount = chooseActionCount,
        chooseActionNs = chooseActionNs,
        chooseActionSamplesNs = chooseActionSamplesNs,
        chooseActionAllocatedBytes = chooseActionAllocatedBytes,
        peakUsedHeapBytes = peakUsedHeapBytes,
        crashError = crashError
    )
}

/**
 * The candidate filter the [Strategist] applies today (`Strategist.kt`): affordable,
 * not a mana ability, not the pass. Phase 4's `MeaningfulActionFilter` is measured
 * as a further reduction on this number, so it is replicated rather than shared —
 * if the Strategist's filter changes, this benchmark should be updated with it.
 */
private fun strategistCandidateFilter(actions: List<LegalAction>): List<LegalAction> =
    actions.filter { it.affordable && !it.isManaAbility && it.actionType != "PassPriority" }

// ─────────────────────────────────────────────────────────────────────────────
// Reporting
// ─────────────────────────────────────────────────────────────────────────────

/** Locale-independent so the committed baseline numbers read the same everywhere. */
private fun fmt(value: Double, decimals: Int): String =
    String.format(Locale.ROOT, "%.${decimals}f", value)

private fun printReport(samples: List<ThroughputSample>, numGames: Int) {
    val crashed = samples.filter { it.crashError != null }
    val completed = samples.count { it.gameOver }

    fun ratePerSec(calls: Int, ns: Long): Double = if (ns > 0) calls * 1_000_000_000.0 / ns else 0.0
    fun meanMs(ns: Long, calls: Int): Double = ns / calls.coerceAtLeast(1) / 1_000_000.0

    val processCalls = samples.sumOf { it.processCalls }
    val processNs = samples.sumOf { it.processNs }
    val playedCalls = samples.sumOf { it.playedProcessCalls }
    val playedNs = samples.sumOf { it.playedProcessNs }
    val simulateCalls = samples.sumOf { it.simulateCalls }
    val simulateNs = samples.sumOf { it.simulateNs }
    val projectCount = samples.sumOf { it.projectCount }
    val projectNs = samples.sumOf { it.projectNs }
    val enumerateCalls = samples.sumOf { it.enumerateCalls }
    val enumerateNs = samples.sumOf { it.enumerateNs }
    val windows = samples.sumOf { it.priorityWindows }
    val preFilter = samples.sumOf { it.preFilterActions }
    val postFilter = samples.sumOf { it.postFilterActions }
    val emptyWindows = samples.sumOf { it.emptyCandidateWindows }
    val chooseCount = samples.sumOf { it.chooseActionCount }
    val chooseNs = samples.sumOf { it.chooseActionNs }
    val chooseSamples = samples.flatMap { it.chooseActionSamplesNs }.sorted()
    fun percentile(p: Double): Double = if (chooseSamples.isEmpty()) 0.0 else
        chooseSamples[((chooseSamples.size - 1) * p).roundToInt()] / 1_000_000.0
    val allocatedBytes = samples.sumOf { it.chooseActionAllocatedBytes }

    val candidateProcessPerSec = ratePerSec(processCalls, processNs)
    val playedProcessPerSec = ratePerSec(playedCalls, playedNs)
    val simulatePerSec = ratePerSec(simulateCalls, simulateNs)
    val meanPostFilter = if (windows > 0) postFilter.toDouble() / windows else 0.0
    val nonEmptyWindows = windows - emptyWindows
    val meanWhenNonEmpty = if (nonEmptyWindows > 0) postFilter.toDouble() / nonEmptyWindows else 0.0

    println()
    println("--- SUMMARY ($numGames games, AI on both seats) ---")
    println("Completed:  $completed / ${samples.size} (${completed * 100 / samples.size}%)")
    println("Crashed:    ${crashed.size} / ${samples.size} (engine exceptions)")
    if (crashed.isNotEmpty()) {
        println()
        println("--- ENGINE EXCEPTIONS (distinct, with game count) ---")
        crashed.groupingBy { it.crashError!! }.eachCount()
            .entries.sortedByDescending { it.value }
            .forEach { (msg, n) -> println("  [${n}x] $msg") }
    }
    println("Turns:      avg=${fmt(samples.map { it.turns }.average(), 1)}")

    println()
    println("--- THROUGHPUT (per thread) ---")
    // Two process() mixes, because they differ by ~2x and a rollout pays the second one.
    println("process(), candidate mix: ${candidateProcessPerSec.roundToInt()} calls/sec  " +
        "(${fmt(meanMs(processNs, processCalls), 3)} ms each, n=$processCalls) — casts/activations")
    println("process(), as-played mix: ${playedProcessPerSec.roundToInt()} calls/sec  " +
        "(${fmt(meanMs(playedNs, playedCalls), 3)} ms each, n=$playedCalls) — mostly passes; " +
        "this is the rollout mix")
    println("GameSimulator.simulate:   ${simulatePerSec.roundToInt()} calls/sec  " +
        "(${fmt(meanMs(simulateNs, simulateCalls), 3)} ms each, n=$simulateCalls) — incl. resolveToQuietState")
    println("  → simulate costs ${fmt(if (simulatePerSec > 0) candidateProcessPerSec / simulatePerSec else 0.0, 1)}x " +
        "one candidate process()")
    println("LegalActionEnumerator:    ${ratePerSec(enumerateCalls, enumerateNs).roundToInt()} calls/sec  " +
        "(${fmt(meanMs(enumerateNs, enumerateCalls), 3)} ms each)")

    println()
    println("--- PROJECTION (cold, no memo hit — the rollout case) ---")
    println("StateProjector.project:   ${projectNs / projectCount.coerceAtLeast(1)} ns mean (n=$projectCount)")
    println("  share of one process(): " +
        "${fmt(projectNs.toDouble() / projectCount.coerceAtLeast(1) * 100.0 / (processNs.toDouble() / processCalls.coerceAtLeast(1)), 1)}%")

    println()
    println("--- BRANCHING FACTOR (per priority window) ---")
    println("Priority windows:         $windows total, ${fmt(windows.toDouble() / samples.size, 1)} per game")
    println("Legal actions pre-filter: ${fmt(if (windows > 0) preFilter.toDouble() / windows else 0.0, 2)}")
    println("Candidates post-filter:   ${fmt(meanPostFilter, 2)}   " +
        "(affordable && !manaAbility && !pass — what the Strategist scores)")
    // The single most load-bearing number for Phase 4: a window with no candidate is a
    // window a playout can skip outright.
    println("Windows with 0 candidates: $emptyWindows / $windows " +
        "(${fmt(emptyWindows * 100.0 / windows.coerceAtLeast(1), 1)}%) — the auto-pass opportunity")
    println("Candidates when non-empty: ${fmt(meanWhenNonEmpty, 2)} (n=$nonEmptyWindows windows)")
    println("Decisions per game:       ${fmt(samples.sumOf { it.decisionCount }.toDouble() / samples.size, 1)}")

    println()
    println("--- CURRENT AI DECISION COST ---")
    println("Strategist.chooseAction:  ${fmt(meanMs(chooseNs, chooseCount), 1)} ms mean (n=$chooseCount)")
    println("Decision latency:         p50=${fmt(percentile(0.50), 3)} ms, p95=${fmt(percentile(0.95), 3)} ms")
    println("Decision allocations:     ${fmt(allocatedBytes.toDouble() / chooseCount.coerceAtLeast(1) / 1024.0, 1)} KiB/decision, " +
        "${fmt(if (chooseNs > 0) allocatedBytes * 1_000_000_000.0 / chooseNs / 1024.0 / 1024.0 else 0.0, 1)} MiB/s of AI thinking")
    println("Peak used heap:           ${fmt(samples.maxOfOrNull { it.peakUsedHeapBytes }?.toDouble()?.div(1024.0 * 1024.0) ?: 0.0, 1)} MiB")
    println("Per game:                 ${fmt(chooseNs.toDouble() / samples.size / 1_000_000_000.0, 1)} s of AI thinking")

    // What a rollout budget buys at the measured rate. Phase 5's stated target is
    // 1,500-2,000 process()/sec/thread; this says how far off we are.
    println()
    println("--- ROLLOUT BUDGET IMPLIED BY THESE NUMBERS ---")
    val processIn2s = (playedProcessPerSec * 2.0).roundToInt()
    println("process() calls in a 2s NORMAL budget: ~$processIn2s (at the as-played rate)")
    println("At ~50 actions/rollout (post-auto-pass), that is ~${(processIn2s / 50.0).roundToInt()} rollouts per decision,")
    println("i.e. ~${fmt(processIn2s / 50.0 / meanWhenNonEmpty.coerceAtLeast(1.0), 1)} rollouts per candidate " +
        "across the ${fmt(meanWhenNonEmpty, 1)} candidates a non-trivial window actually offers.")
}

// Seeded deck generation and the illegal-action fallback live in `AiBenchmarkSupport.kt` — the
// arena needs both, and two copies of either would silently drift apart.
