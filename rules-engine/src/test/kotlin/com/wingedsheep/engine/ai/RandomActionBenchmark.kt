package com.wingedsheep.engine.ai

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import io.kotest.core.spec.style.FunSpec
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.measureTime

/**
 * Benchmark: pure random actions (no AI evaluation/simulation).
 * Measures raw engine throughput: enumerate legal actions → pick random → process.
 *
 * Disabled by default. Run with:
 *   ./gradlew :rules-engine:test --tests "*.RandomActionBenchmark" -Dbenchmark=true
 *   ./gradlew :rules-engine:test --tests "*.RandomActionBenchmark" -Dbenchmark=true -DbenchmarkGames=100
 */
class RandomActionBenchmark : FunSpec({

    val numGames = System.getProperty("benchmarkGames")?.toIntOrNull() ?: 20
    val benchmarkEnabled = System.getProperty("benchmark") == "true"

    test("benchmark: $numGames random-action games in parallel").config(enabled = benchmarkEnabled) {
        val registry = CardRegistry().apply { register(PortalSet.allCards) }
        val allCards = PortalSet.allCards
        val cores = Runtime.getRuntime().availableProcessors()
        val pool = Executors.newFixedThreadPool(cores)
        val completionService = ExecutorCompletionService<RandomGameResult>(pool)
        val finished = AtomicInteger(0)

        println("=== RANDOM ACTION BENCHMARK: $numGames games on $cores threads ===")

        val wallTime = measureTime {
            (1..numGames).forEach { i ->
                completionService.submit {
                    val deck1 = buildRandomSealedDeck(allCards)
                    val deck2 = buildRandomSealedDeck(allCards)
                    playRandomGame(registry, deck1, deck2, maxTurns = 50).also {
                        val n = finished.incrementAndGet()
                        if (n <= 5 || n % 10 == 0 || n == numGames) {
                            println("  [${n}/${numGames}] ${it.turns} turns, ${it.actions} actions, ${it.durationMs}ms, ${it.winner}")
                        }
                    }
                }
            }

            val results = (1..numGames).map { completionService.take().get() }

            val completed = results.count { it.gameOver }
            val avgTurns = results.map { it.turns }.average()
            val avgActions = results.map { it.actions }.average()
            val avgMs = results.map { it.durationMs }.average()
            val totalActions = results.sumOf { it.actions }
            val totalCpuMs = results.sumOf { it.durationMs }

            println()
            println("--- SUMMARY ($numGames games, random actions) ---")
            println("Completed:  $completed / ${results.size} (${completed * 100 / results.size}%)")
            println("Turns:      avg=${String.format("%.1f", avgTurns)}, min=${results.minOf { it.turns }}, max=${results.maxOf { it.turns }}")
            println("Actions:    avg=${avgActions.roundToInt()}, min=${results.minOf { it.actions }}, max=${results.maxOf { it.actions }}")
            println("CPU/game:   avg=${avgMs.roundToInt()}ms, min=${results.minOf { it.durationMs }}ms, max=${results.maxOf { it.durationMs }}ms")
            if (totalCpuMs > 0) {
                println("Throughput: ~${(totalActions * 1000.0 / totalCpuMs).roundToInt()} actions/sec (per thread)")
                println("Games/sec:  ~${String.format("%.1f", results.size * 1000.0 / totalCpuMs)} (per thread)")
            }

            // Timing breakdown
            val totalEnumerateMs = results.sumOf { it.enumerateNs } / 1_000_000.0
            val totalProcessMs = results.sumOf { it.processNs } / 1_000_000.0
            val totalDecisionMs = results.sumOf { it.decisionNs } / 1_000_000.0
            val totalMeasuredMs = totalEnumerateMs + totalProcessMs + totalDecisionMs
            if (totalMeasuredMs > 0) {
                println()
                println("--- TIME BREAKDOWN (sum across all games) ---")
                println("Enumerate:  ${String.format("%,.0f", totalEnumerateMs)}ms (${String.format("%.1f", totalEnumerateMs * 100 / totalMeasuredMs)}%)")
                println("Process:    ${String.format("%,.0f", totalProcessMs)}ms (${String.format("%.1f", totalProcessMs * 100 / totalMeasuredMs)}%)")
                println("Decisions:  ${String.format("%,.0f", totalDecisionMs)}ms (${String.format("%.1f", totalDecisionMs * 100 / totalMeasuredMs)}%)")
            }
        }

        println("Wall time:  ${wallTime.inWholeMilliseconds}ms ($numGames games on $cores threads)")
        println("Effective:  ~${String.format("%.1f", numGames * 1000.0 / wallTime.inWholeMilliseconds)} games/sec (wall clock)")
        pool.shutdown()
    }
})

private data class RandomGameResult(
    val turns: Int, val actions: Int, val durationMs: Long,
    val winner: String, val gameOver: Boolean,
    val enumerateNs: Long = 0, val processNs: Long = 0, val decisionNs: Long = 0
)

/**
 * Play a game using purely random legal actions. No AI evaluation at all.
 */
private fun playRandomGame(
    registry: CardRegistry, deck1: Deck, deck2: Deck, maxTurns: Int = 50
): RandomGameResult {
    val processor = ActionProcessor(registry)
    val enumerator = LegalActionEnumerator.create(registry)
    val initializer = GameInitializer(registry)
    var actionCount = 0
    var enumerateNs = 0L
    var processNs = 0L
    var decisionNs = 0L

    val result = initializer.initializeGame(
        GameConfig(
            players = listOf(PlayerConfig("P1", deck1), PlayerConfig("P2", deck2)),
            skipMulligans = true, startingPlayerIndex = 0
        )
    )

    val p1 = result.state.turnOrder[0]
    val p2 = result.state.turnOrder[1]
    var state: GameState = result.state
    val rng = Random(System.nanoTime())

    var lastProgressTurn = 0
    var lastProgressAction = 0
    var stuckCount = 0

    val duration = measureTime {
        while (!state.gameOver && state.turnNumber < maxTurns) {
            // Stuck detection
            if (actionCount - lastProgressAction > 1000 && state.turnNumber == lastProgressTurn) {
                stuckCount++
                if (stuckCount >= 3) break
            }
            if (state.turnNumber > lastProgressTurn) {
                lastProgressTurn = state.turnNumber
                lastProgressAction = actionCount
                stuckCount = 0
            }

            val action = if (state.pendingDecision != null) {
                val d = state.pendingDecision
                val t0 = System.nanoTime()
                val response = randomDecisionResponse(d, rng)
                decisionNs += System.nanoTime() - t0
                SubmitDecision(d.playerId, response)
            } else {
                val priorityPlayer = state.priorityPlayerId ?: break
                val t0 = System.nanoTime()
                val legalActions = enumerator.enumerate(state, priorityPlayer)
                enumerateNs += System.nanoTime() - t0
                val affordable = legalActions.filter { it.affordable }
                if (affordable.isEmpty()) break
                affordable[rng.nextInt(affordable.size)].action
            }

            val t0 = System.nanoTime()
            val r = processor.process(state, action).result
            processNs += System.nanoTime() - t0
            if (r.error != null) {
                // If random action failed, just pass priority
                val t1 = System.nanoTime()
                val fallback = processor.process(state, PassPriority(action.playerId)).result
                processNs += System.nanoTime() - t1
                if (fallback.error != null) break
                state = fallback.state
            } else {
                state = r.state
            }
            actionCount++
        }
    }

    return RandomGameResult(
        turns = state.turnNumber,
        actions = actionCount,
        durationMs = duration.inWholeMilliseconds,
        winner = when {
            !state.gameOver -> "timeout"
            state.winnerId == p1 -> "P1"
            state.winnerId == p2 -> "P2"
            else -> "draw"
        },
        gameOver = state.gameOver,
        enumerateNs = enumerateNs,
        processNs = processNs,
        decisionNs = decisionNs
    )
}

/**
 * Generate a random but valid response for any pending decision.
 */
private fun randomDecisionResponse(decision: PendingDecision, rng: Random): DecisionResponse {
    return when (decision) {
        is ChooseTargetsDecision -> {
            val targets = decision.targetRequirements.associate { req ->
                val valid = decision.legalTargets[req.index] ?: emptyList()
                val count = rng.nextInt(req.minTargets, req.maxTargets + 1).coerceAtMost(valid.size)
                req.index to valid.shuffled(rng).take(count)
            }
            TargetsResponse(decision.id, targets)
        }

        is SelectCardsDecision -> {
            val count = rng.nextInt(decision.minSelections, decision.maxSelections + 1)
                .coerceAtMost(decision.options.size)
            CardsSelectedResponse(decision.id, decision.options.shuffled(rng).take(count))
        }

        is YesNoDecision -> YesNoResponse(decision.id, rng.nextBoolean())

        is ChooseModeDecision -> {
            val available = decision.modes.filter { it.available }
            val count = rng.nextInt(decision.minModes, decision.maxModes + 1).coerceAtMost(available.size)
            ModesChosenResponse(decision.id, available.shuffled(rng).take(count).map { it.index })
        }

        is ChooseColorDecision -> {
            val colors = decision.availableColors.toList()
            ColorChosenResponse(decision.id, colors[rng.nextInt(colors.size)])
        }

        is ChooseNumberDecision ->
            NumberChosenResponse(decision.id, rng.nextInt(decision.minValue, decision.maxValue + 1))

        is DistributeDecision -> {
            val dist = mutableMapOf<EntityId, Int>()
            var remaining = decision.totalAmount
            for (target in decision.targets) {
                dist[target] = decision.minPerTarget
                remaining -= decision.minPerTarget
            }
            // Dump remaining onto random targets
            while (remaining > 0 && decision.targets.isNotEmpty()) {
                val target = decision.targets[rng.nextInt(decision.targets.size)]
                dist[target] = (dist[target] ?: 0) + 1
                remaining--
            }
            DistributionResponse(decision.id, dist)
        }

        is OrderObjectsDecision ->
            OrderedResponse(decision.id, decision.objects.shuffled(rng))

        is SplitPilesDecision -> {
            val shuffled = decision.cards.shuffled(rng)
            val splitPoint = if (shuffled.size > 1) rng.nextInt(1, shuffled.size) else 1
            PilesSplitResponse(decision.id, listOf(shuffled.take(splitPoint), shuffled.drop(splitPoint)))
        }

        is ChooseOptionDecision ->
            OptionChosenResponse(decision.id, rng.nextInt(decision.options.size))

        is AssignDamageDecision ->
            DamageAssignmentResponse(decision.id, decision.defaultAssignments)

        is BudgetModalDecision -> {
            val selected = mutableListOf<Int>()
            var budget = decision.budget
            val affordable = decision.modes.withIndex().filter { it.value.cost <= budget }
            if (affordable.isNotEmpty()) {
                val pick = affordable[rng.nextInt(affordable.size)]
                selected.add(pick.index)
                budget -= pick.value.cost
            }
            BudgetModalResponse(decision.id, selected)
        }

        is SearchLibraryDecision -> {
            val count = rng.nextInt(decision.minSelections, decision.maxSelections + 1)
                .coerceAtMost(decision.options.size)
            CardsSelectedResponse(decision.id, decision.options.shuffled(rng).take(count))
        }

        is ReorderLibraryDecision ->
            OrderedResponse(decision.id, decision.cards.shuffled(rng))

        is SelectManaSourcesDecision ->
            ManaSourcesSelectedResponse(decision.id, emptyList(), autoPay = true)
    }
}

private fun buildRandomSealedDeck(allCards: List<CardDefinition>): Deck {
    val pool = generateSealedPool(allCards)
    val deckMap = buildHeuristicSealedDeck(pool)
    return Deck(deckMap.flatMap { (name, count) -> List(count) { name } })
}

private fun generateSealedPool(allCards: List<CardDefinition>): List<CardDefinition> {
    val nonBasics = allCards.filter { !it.typeLine.isBasicLand }
    val commons = nonBasics.filter { it.metadata.rarity == Rarity.COMMON }
    val uncommons = nonBasics.filter { it.metadata.rarity == Rarity.UNCOMMON }
    val rares = nonBasics.filter { it.metadata.rarity == Rarity.RARE }
    val mythics = nonBasics.filter { it.metadata.rarity == Rarity.MYTHIC }

    val pool = mutableListOf<CardDefinition>()
    repeat(6) {
        val usedNames = mutableSetOf<String>()
        fun pick(from: List<CardDefinition>): CardDefinition? {
            val available = from.filter { it.name !in usedNames }
            if (available.isEmpty()) return null
            return available.random().also { usedNames.add(it.name) }
        }
        repeat(11) { pick(commons)?.let { pool.add(it) } }
        repeat(3) { pick(uncommons)?.let { pool.add(it) } }
        val rare = if (mythics.isNotEmpty() && Math.random() < 0.125) pick(mythics) else null
        pool.add(rare ?: pick(rares) ?: pick(uncommons) ?: pick(commons)!!)
    }
    return pool
}
