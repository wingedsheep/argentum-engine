package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.advisor.modules.BloomburrowAdvisorModule
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.mtg.sets.definitions.bloomburrow.BloomburrowSet
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
 * A/B benchmark: compares AI with Bloomburrow CardAdvisors vs. without.
 *
 * Both players use the same random Bloomburrow sealed deck per game.
 * Player 1 (Advised) gets the BloomburrowAdvisorModule.
 * Player 2 (Generic) uses the default generic AI.
 *
 * Each game is played twice with colors swapped (P1 goes first, then P2)
 * to reduce first-player bias. Win rate is tracked per side.
 *
 * Disabled by default. Run with:
 *   ./gradlew :rules-engine:test --tests "*.AdvisorBenchmark" -Dbenchmark=true
 *   ./gradlew :rules-engine:test --tests "*.AdvisorBenchmark" -Dbenchmark=true -DbenchmarkGames=50
 */
class AdvisorBenchmark : FunSpec({

    val numGames = System.getProperty("benchmarkGames")?.toIntOrNull() ?: 20
    val benchmarkEnabled = System.getProperty("benchmark") == "true"

    test("advisor benchmark: advised vs generic AI ($numGames game pairs)").config(enabled = benchmarkEnabled) {
        val allCards = BloomburrowSet.allCards + BloomburrowSet.basicLands
        val registry = CardRegistry().apply { register(allCards) }
        val cores = Runtime.getRuntime().availableProcessors()
        val pool = Executors.newFixedThreadPool(cores)
        val completionService = ExecutorCompletionService<PairResult>(pool)
        val finished = AtomicInteger(0)

        val csvFile = java.io.File(System.getProperty("java.io.tmpdir"), "advisor-benchmark.csv")
        csvFile.writeText("pair,game,advised_player,turns,actions,duration_ms,winner,advised_life,generic_life,completed,deck_summary\n")

        println("=== ADVISOR BENCHMARK: $numGames game pairs on $cores threads ===")
        println("  Each pair = same decks, swap who goes first")
        println("  Advised = BloomburrowAdvisorModule, Generic = default AI")
        println()

        val wallTime = measureTime {
            (1..numGames).forEach { pairId ->
                completionService.submit {
                    val nonBasics = allCards.filter { !it.typeLine.isBasicLand }
                    val sealedPool = generateBloomburrowPool(nonBasics)
                    val deck = buildRandomSealedDeck(sealedPool)
                    val summary = deckSummary(deck)

                    // Game A: Advised = P1 (goes first)
                    val gameA = playAdvisorGame(registry, deck, deck, pairId * 2 - 1, advisedIsP1 = true)

                    // Game B: Advised = P2 (goes second)
                    val gameB = playAdvisorGame(registry, deck, deck, pairId * 2, advisedIsP1 = false)

                    val n = finished.incrementAndGet()
                    if (n <= 10 || n % 10 == 0 || n == numGames) {
                        val aWin = if (gameA.advisedWon) "advised" else if (gameA.genericWon) "generic" else "draw"
                        val bWin = if (gameB.advisedWon) "advised" else if (gameB.genericWon) "generic" else "draw"
                        println("  [Pair $pairId] ($n/$numGames) A=$aWin B=$bWin [$summary]")
                    }

                    PairResult(pairId, gameA, gameB, summary)
                }
            }

            val results = (1..numGames).map { completionService.take().get() }

            // Write CSV
            for (r in results) {
                for ((game, label) in listOf(r.gameA to "P1", r.gameB to "P2")) {
                    csvFile.appendText("${r.pairId},${game.id},$label,${game.turns},${game.actions}," +
                            "${game.durationMs},${game.winnerLabel},${game.advisedLife},${game.genericLife}," +
                            "${game.gameOver},${r.deckSummary}\n")
                }
            }

            // Aggregate stats
            val allGames = results.flatMap { listOf(it.gameA, it.gameB) }
            val completed = allGames.count { it.gameOver }
            val advisedWins = allGames.count { it.advisedWon }
            val genericWins = allGames.count { it.genericWon }
            val draws = allGames.count { !it.advisedWon && !it.genericWon }
            val totalGames = allGames.size

            val advisedWinRate = if (totalGames > 0) advisedWins * 100.0 / totalGames else 0.0
            val genericWinRate = if (totalGames > 0) genericWins * 100.0 / totalGames else 0.0

            // Going-first stats
            val asP1 = results.map { it.gameA }
            val asP2 = results.map { it.gameB }
            val advisedWinsAsP1 = asP1.count { it.advisedWon }
            val advisedWinsAsP2 = asP2.count { it.advisedWon }

            println()
            println("--- RESULTS (${totalGames} games from $numGames pairs) ---")
            println("Completed:      $completed / $totalGames (${completed * 100 / totalGames}%)")
            println()
            println("Advised wins:   $advisedWins (${String.format("%.1f", advisedWinRate)}%)")
            println("Generic wins:   $genericWins (${String.format("%.1f", genericWinRate)}%)")
            println("Draws:          $draws")
            if (draws > 0) {
                val reasons = allGames.filter { !it.advisedWon && !it.genericWon && it.drawReason.isNotEmpty() }
                    .groupBy { it.drawReason }
                    .mapValues { it.value.size }
                    .entries.sortedByDescending { it.value }
                for ((reason, count) in reasons) {
                    println("  $count × $reason")
                }
            }
            println()
            println("Advised as P1 (first):  $advisedWinsAsP1 / ${asP1.size} wins")
            println("Advised as P2 (second): $advisedWinsAsP2 / ${asP2.size} wins")
            println()
            println("Avg turns:    ${String.format("%.1f", allGames.map { it.turns }.average())}")
            println("Avg actions:  ${allGames.map { it.actions }.average().roundToInt()}")
            println("Avg duration: ${allGames.map { it.durationMs }.average().roundToInt()}ms")
            println()
            println("CSV: ${csvFile.absolutePath}")
        }

        println("Wall time: ${wallTime.inWholeMilliseconds}ms")
        pool.shutdown()
    }
})

// ═════════════════════════════════════════════════════════════════════════════
// Game runner
// ═════════════════════════════════════════════════════════════════════════════

private data class PairResult(
    val pairId: Int,
    val gameA: GameResultAdvisor,
    val gameB: GameResultAdvisor,
    val deckSummary: String
)

private data class GameResultAdvisor(
    val id: Int, val turns: Int, val actions: Int, val durationMs: Long,
    val advisedWon: Boolean, val genericWon: Boolean,
    val advisedLife: Int, val genericLife: Int,
    val gameOver: Boolean, val winnerLabel: String,
    val drawReason: String = ""
)

private fun playAdvisorGame(
    registry: CardRegistry, deck1: Deck, deck2: Deck, id: Int,
    advisedIsP1: Boolean, maxTurns: Int = 50
): GameResultAdvisor {
    val processor = ActionProcessor(registry)
    val initializer = GameInitializer(registry)
    var actionCount = 0
    var turns = 0

    val result = initializer.initializeGame(
        GameConfig(
            players = listOf(PlayerConfig("Advised", deck1), PlayerConfig("Generic", deck2)),
            skipMulligans = true, startingPlayerIndex = 0
        )
    )

    val p1 = result.state.turnOrder[0]
    val p2 = result.state.turnOrder[1]

    val advisedId = if (advisedIsP1) p1 else p2
    val genericId = if (advisedIsP1) p2 else p1

    val advisedAi = AIPlayer.create(registry, advisedId, advisorModules = listOf(BloomburrowAdvisorModule()))
    val genericAi = AIPlayer.create(registry, genericId)

    fun aiFor(playerId: EntityId) = if (playerId == advisedId) advisedAi else genericAi

    var state: GameState = result.state
    var lastProgressTurn = 0
    var lastProgressAction = 0
    var drawReason = ""
    val recentActions = ArrayDeque<String>(32)

    val duration = measureTime {
        while (!state.gameOver && turns < maxTurns) {
            if (actionCount - lastProgressAction > 200 && turns == lastProgressTurn) {
                val step = state.step.name
                val phase = state.phase.name
                val decision = state.pendingDecision?.javaClass?.simpleName ?: "none"
                val priority = if (state.priorityPlayerId == advisedId) "advised" else "generic"
                drawReason = "stuck turn=$turns step=$step decision=$decision priority=$priority"
                println("  [STUCK] Game $id: turn=$turns phase=$phase step=$step decision=$decision priority=$priority actions=$actionCount")
                println("  [STUCK] Last 20 actions:")
                recentActions.takeLast(20).forEach { println("    $it") }
                // Dump battlefield state
                val proj = state.projectedState
                for (pid in listOf(p1, p2)) {
                    val label = if (pid == advisedId) "Advised" else "Generic"
                    val creatures = proj.getBattlefieldControlledBy(pid)
                        .filter { proj.isCreature(it) }
                        .map { eid ->
                            val card = state.getEntity(eid)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                            val tapped = state.getEntity(eid)?.has<TappedComponent>() == true
                            val attacking = state.getEntity(eid)?.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() == true
                            val pt = "${proj.getPower(eid) ?: "?"}/${proj.getToughness(eid) ?: "?"}"
                            val kw = proj.getKeywords(eid).joinToString(",")
                            "${card?.name ?: "?"} $pt${if (tapped) " T" else ""}${if (attacking) " ATK" else ""}${if (kw.isNotEmpty()) " [$kw]" else ""}"
                        }
                    println("  [STUCK] $label creatures: $creatures")
                }
                // Dump legal actions
                val legalActions = try { advisedAi.let { _ ->
                    val sim = GameSimulator(registry)
                    val priorityPid = state.priorityPlayerId
                    if (priorityPid != null) sim.getLegalActions(state, priorityPid) else emptyList()
                } } catch (_: Exception) { emptyList() }
                println("  [STUCK] Legal actions for priority player: ${legalActions.map { la ->
                    val desc = la.actionType
                    val extra = when {
                        la.validAttackers != null -> " validAtk=${la.validAttackers!!.size} mandatoryAtk=${la.mandatoryAttackers?.size ?: 0}"
                        la.validBlockers != null -> " validBlk=${la.validBlockers!!.size} mandatoryBlk=${la.mandatoryBlockerAssignments?.size ?: 0}"
                        else -> ""
                    }
                    "$desc$extra"
                }}")
                break
            }
            if (turns > lastProgressTurn) {
                lastProgressTurn = turns
                lastProgressAction = actionCount
            }

            val d = state.pendingDecision
            val next: GameState? = if (d != null) {
                actionCount++
                val ai = aiFor(d.playerId)
                val response = ai.respondToDecision(state, d)
                recentActions.addLast("#$actionCount Decision(${d.javaClass.simpleName} player=${if (d.playerId == advisedId) "adv" else "gen"}) -> ${response.javaClass.simpleName}")
                if (recentActions.size > 30) recentActions.removeFirst()
                val r = processor.process(state, SubmitDecision(d.playerId, response)).result
                if (r.error != null) {
                    recentActions.addLast("  ERROR: ${r.error}")
                    drawReason = "error(${r.error})"
                    null
                } else r.state
            } else when (val prioId = state.priorityPlayerId) {
                null -> { drawReason = "noPriority(turn=$turns)"; null }
                else -> {
                    actionCount++
                    val label = if (prioId == advisedId) "adv" else "gen"
                    val ai = aiFor(prioId)
                    val action = ai.chooseAction(state)
                    val actionDesc = action.javaClass.simpleName
                    recentActions.addLast("#$actionCount Action($label) step=${state.step.name} -> $actionDesc")
                    if (recentActions.size > 50) recentActions.removeFirst()
                    val r = processor.process(state, action).result
                    if (r.error != null) {
                        recentActions.addLast("  ERROR: ${r.error}")
                        // Try safe fallback depending on step
                        val blockersAlreadyDeclared = state.getEntity(prioId)
                            ?.has<com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent>() == true
                        val attackersAlreadyDeclared = state.getEntity(prioId)
                            ?.has<com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent>() == true
                        val fallbackAction: GameAction = when {
                            state.step == Step.DECLARE_BLOCKERS && state.activePlayerId != prioId && !blockersAlreadyDeclared -> {
                                val sim = GameSimulator(registry)
                                val la = sim.getLegalActions(state, prioId).find { it.actionType == "DeclareBlockers" }
                                val mandatory = la?.mandatoryBlockerAssignments ?: emptyMap()
                                DeclareBlockers(prioId, mandatory.mapValues { (_, targets) ->
                                    if (targets.isNotEmpty()) listOf(targets.first()) else emptyList()
                                })
                            }
                            state.step == Step.DECLARE_ATTACKERS && state.activePlayerId == prioId && !attackersAlreadyDeclared -> {
                                val sim = GameSimulator(registry)
                                val la = sim.getLegalActions(state, prioId).find { it.actionType == "DeclareAttackers" }
                                val mandatory = la?.mandatoryAttackers ?: emptyList()
                                val opponentId = state.turnOrder.firstOrNull { it != prioId }
                                DeclareAttackers(prioId, if (mandatory.isNotEmpty() && opponentId != null) mandatory.associateWith { opponentId } else emptyMap())
                            }
                            else -> PassPriority(prioId)
                        }
                        val fallback = processor.process(state, fallbackAction).result
                        if (fallback.error != null) {
                            drawReason = "error(${r.error} + fallback: ${fallback.error})"
                            null
                        } else fallback.state
                    } else {
                        recentActions.addLast("  -> step=${r.state.step.name} phase=${r.state.phase.name} pending=${r.state.pendingDecision?.javaClass?.simpleName ?: "none"}")
                        r.state
                    }
                }
            }
            if (next == null) break
            if (next === state) {
                // State didn't change — avoid infinite loop
                drawReason = "noProgress turn=$turns step=${state.step.name} decision=${state.pendingDecision?.javaClass?.simpleName ?: "none"}"
                println("  [NO_PROGRESS] Game $id: $drawReason")
                println("  [NO_PROGRESS] Last 10 actions:")
                recentActions.takeLast(10).forEach { println("    $it") }
                break
            }
            state = next
            if (state.turnNumber > turns) turns = state.turnNumber
        }
        if (!state.gameOver && drawReason.isEmpty()) {
            drawReason = "maxTurns($maxTurns)"
        }
    }

    val advisedLife = state.getEntity(advisedId)?.get<LifeTotalComponent>()?.life ?: 0
    val genericLife = state.getEntity(genericId)?.get<LifeTotalComponent>()?.life ?: 0
    val advisedWon = state.gameOver && state.winnerId == advisedId
    val genericWon = state.gameOver && state.winnerId == genericId

    return GameResultAdvisor(
        id = id, turns = turns, actions = actionCount, durationMs = duration.inWholeMilliseconds,
        advisedWon = advisedWon, genericWon = genericWon,
        advisedLife = advisedLife, genericLife = genericLife,
        gameOver = state.gameOver,
        winnerLabel = when {
            advisedWon -> "advised"
            genericWon -> "generic"
            else -> "draw"
        },
        drawReason = drawReason
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// Deck building (Bloomburrow sealed pool)
// ═════════════════════════════════════════════════════════════════════════════

private fun generateBloomburrowPool(nonBasics: List<CardDefinition>): List<CardDefinition> {
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
        val rare = if (mythics.isNotEmpty() && Random.nextDouble() < 0.125) pick(mythics) else null
        pool.add(rare ?: pick(rares) ?: pick(uncommons) ?: pick(commons)!!)
    }
    return pool
}

private fun buildRandomSealedDeck(pool: List<CardDefinition>): Deck {
    val deckMap = buildHeuristicSealedDeck(pool)
    return Deck(deckMap.flatMap { (name, count) -> List(count) { name } })
}

private fun deckSummary(deck: Deck): String {
    val basics = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
    val landCount = deck.cards.count { it in basics }
    val colors = deck.cards.filter { it in basics }.groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }.map { it.key.first() }
    return "${colors.joinToString("")} ${deck.size - landCount}sp"
}
