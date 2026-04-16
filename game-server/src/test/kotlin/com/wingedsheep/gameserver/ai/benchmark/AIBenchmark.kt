package com.wingedsheep.gameserver.ai.benchmark

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.advisor.modules.BloomburrowAdvisorModule
import com.wingedsheep.ai.engine.advisor.modules.OnslaughtAdvisorModule
import com.wingedsheep.ai.engine.buildHeuristicSealedDeck
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.ai.llm.*
import com.wingedsheep.gameserver.ai.*
import com.wingedsheep.gameserver.config.AiProperties
import com.wingedsheep.engine.view.ClientEventTransformer
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.view.LegalActionEnricher
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.mtg.sets.definitions.bloomburrow.BloomburrowSet
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import io.kotest.core.spec.style.FunSpec
import kotlin.time.Duration.Companion.hours
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.measureTime

/**
 * Configurable AI benchmark.
 *
 * Engine-vs-engine uses the fast synchronous path (same as AdvisorBenchmark).
 * LLM players use the production [AiController] interface with [AiPlayerController].
 *
 * Disabled by default. Run with system properties:
 *
 * ```bash
 * # Engine vs Engine (default), 10 game pairs
 * ./gradlew :game-server:test --tests "*.AIBenchmark" -Dbenchmark=true
 *
 * # Engine vs LLM, 5 game pairs
 * ./gradlew :game-server:test --tests "*.AIBenchmark" -Dbenchmark=true \
 *   -DbenchmarkGames=5 \
 *   -Dp2Type=llm -Dp2Model=anthropic/claude-sonnet-4 \
 *   -Dp2BaseUrl=https://openrouter.ai/api/v1 -Dp2ApiKey=sk-...
 *
 * # LLM vs LLM with different models
 * ./gradlew :game-server:test --tests "*.AIBenchmark" -Dbenchmark=true \
 *   -Dp1Type=llm -Dp1Model=anthropic/claude-sonnet-4 \
 *   -Dp2Type=llm -Dp2Model=openai/gpt-4o \
 *   -Dp1BaseUrl=https://openrouter.ai/api/v1 -Dp1ApiKey=sk-... \
 *   -Dp2BaseUrl=https://openrouter.ai/api/v1 -Dp2ApiKey=sk-...
 *
 * # LLM builds its own deck from sealed pool
 * ./gradlew :game-server:test --tests "*.AIBenchmark" -Dbenchmark=true \
 *   -Dp1Type=llm -Dp1Model=... -Dp1DeckBuilder=llm ...
 * ```
 *
 * System properties:
 * - `benchmark=true` — required to run
 * - `benchmarkGames=N` — game pairs (default 10; each pair = same decks, swap starting player)
 * - `p1Type` / `p2Type` — `engine` (default) or `llm`
 * - `p1Model` / `p2Model` — LLM model name
 * - `p1BaseUrl` / `p2BaseUrl` — LLM API base URL (default: OpenRouter)
 * - `p1ApiKey` / `p2ApiKey` — LLM API key
 * - `p1DeckBuilder` / `p2DeckBuilder` — `engine` (default) or `llm`
 * - `p1ReasoningEffort` / `p2ReasoningEffort` — LLM reasoning effort
 * - `benchmarkMaxTurns` — max turns before draw (default 50)
 * - `benchmarkOutputDir` — output directory (default: java.io.tmpdir)
 */
class AIBenchmark : FunSpec({

    val benchmarkEnabled = System.getProperty("benchmark") == "true"
    val numGames = System.getProperty("benchmarkGames")?.toIntOrNull() ?: 10
    val maxTurns = System.getProperty("benchmarkMaxTurns")?.toIntOrNull() ?: 50

    fun playerConfig(prefix: String): PlayerType {
        val type = System.getProperty("${prefix}Type") ?: "engine"
        return if (type.equals("llm", ignoreCase = true)) {
            PlayerType.Llm(
                model = System.getProperty("${prefix}Model") ?: "qwen/qwen3.6-plus:free",
                baseUrl = System.getProperty("${prefix}BaseUrl") ?: "https://openrouter.ai/api/v1",
                apiKey = System.getProperty("${prefix}ApiKey") ?: "",
                reasoningEffort = System.getProperty("${prefix}ReasoningEffort") ?: "low",
                deckBuilder = System.getProperty("${prefix}DeckBuilder") ?: "engine"
            )
        } else {
            PlayerType.Engine(deckBuilder = System.getProperty("${prefix}DeckBuilder") ?: "engine")
        }
    }

    test("AI benchmark ($numGames game pairs)").config(enabled = benchmarkEnabled, timeout = 4.hours) {
        val p1Config = playerConfig("p1")
        val p2Config = playerConfig("p2")

        val allCards = BloomburrowSet.allCards + BloomburrowSet.basicLands
        val registry = CardRegistry().apply { register(allCards) }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val baseDir = System.getProperty("benchmarkOutputDir") ?: System.getProperty("java.io.tmpdir")
        val outputDir = File(baseDir, "ai-benchmark-$timestamp")
        outputDir.mkdirs()
        val csvFile = File(outputDir, "results.csv")
        csvFile.writeText("pair,game,p1_type,p2_type,first_player,turns,actions,duration_ms,winner,p1_life,p2_life,completed,draw_reason,deck_summary\n")

        println("=== AI BENCHMARK: $numGames game pairs ===")
        println("  Player 1: $p1Config")
        println("  Player 2: $p2Config")
        println("  Output: ${outputDir.absolutePath}")
        println()

        val results = mutableListOf<PairResult>()

        val wallTime = measureTime {
            for (pairId in 1..numGames) {
                val nonBasics = allCards.filter { !it.typeLine.isBasicLand }
                val sealedPool = generateSealedPool(nonBasics)
                val deck1 = buildDeck(p1Config, sealedPool, registry)
                val deck2 = buildDeck(p2Config, sealedPool, registry)
                val summary = deckSummary(deck1)

                val gameA = playGame(registry, deck1, deck2, p1Config, p2Config, pairId * 2 - 1, maxTurns)
                File(outputDir, "game-${pairId * 2 - 1}.log").writeText(gameA.log)

                val gameB = playGame(registry, deck2, deck1, p2Config, p1Config, pairId * 2, maxTurns)
                File(outputDir, "game-${pairId * 2}.log").writeText(gameB.log)

                val pair = PairResult(pairId, gameA.result, gameB.result, summary)
                results.add(pair)

                for ((game, firstPlayer) in listOf(gameA.result to "P1", gameB.result to "P2")) {
                    csvFile.appendText("${pairId},${game.id},${p1Config.label},${p2Config.label},$firstPlayer,${game.turns},${game.actions},${game.durationMs},${game.winnerLabel},${game.p1Life},${game.p2Life},${game.completed},${game.drawReason},${summary}\n")
                }

                println("  [Pair $pairId/$numGames] A=${gameA.result.winnerLabel} B=${gameB.result.winnerLabel} [$summary]")
            }
        }

        val allGames = results.flatMap { listOf(it.gameA, it.gameB) }
        val completed = allGames.count { it.completed }
        val p1Wins = allGames.count { it.winnerLabel == "P1" }
        val p2Wins = allGames.count { it.winnerLabel == "P2" }
        val draws = allGames.count { it.winnerLabel == "draw" }
        val totalGames = allGames.size

        println()
        println("--- RESULTS ($totalGames games from $numGames pairs) ---")
        println("Completed:  $completed / $totalGames (${if (totalGames > 0) completed * 100 / totalGames else 0}%)")
        println()
        println("P1 wins:    $p1Wins (${String.format("%.1f", if (totalGames > 0) p1Wins * 100.0 / totalGames else 0.0)}%) — ${p1Config.label}")
        println("P2 wins:    $p2Wins (${String.format("%.1f", if (totalGames > 0) p2Wins * 100.0 / totalGames else 0.0)}%) — ${p2Config.label}")
        println("Draws:      $draws")
        if (draws > 0) {
            allGames.filter { it.winnerLabel == "draw" }
                .groupBy { it.drawReason }.mapValues { it.value.size }
                .entries.sortedByDescending { it.value }
                .forEach { (reason, count) -> println("  $count × $reason") }
        }
        println()
        println("Avg turns:    ${String.format("%.1f", allGames.map { it.turns }.average())}")
        println("Avg actions:  ${allGames.map { it.actions }.average().roundToInt()}")
        println("Avg duration: ${allGames.map { it.durationMs }.average().roundToInt()}ms")
        println()
        println("Output: ${outputDir.absolutePath}")
        println("Wall time: ${wallTime.inWholeMilliseconds}ms")
    }
})

// ═════════════════════════════════════════════════════════════════════════════
// Player types
// ═════════════════════════════════════════════════════════════════════════════

sealed interface PlayerType {
    val label: String
    val deckBuilder: String

    data class Engine(override val deckBuilder: String = "engine") : PlayerType {
        override val label get() = "engine"
        override fun toString() = "Engine AI (deck=$deckBuilder)"
    }

    data class Llm(
        val model: String, val baseUrl: String, val apiKey: String,
        val reasoningEffort: String = "low", override val deckBuilder: String = "engine"
    ) : PlayerType {
        override val label get() = "llm:$model"
        override fun toString() = "LLM ($model, deck=$deckBuilder)"
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Results
// ═════════════════════════════════════════════════════════════════════════════

private data class PairResult(val pairId: Int, val gameA: GameResult, val gameB: GameResult, val deckSummary: String)
private data class GameResult(
    val id: Int, val turns: Int, val actions: Int, val durationMs: Long,
    val p1Life: Int, val p2Life: Int, val completed: Boolean,
    val winnerLabel: String, val drawReason: String = ""
)
private data class GameRunResult(val result: GameResult, val log: String)

// ═════════════════════════════════════════════════════════════════════════════
// Game runner — synchronous loop using AiController.chooseAction() directly
//
// Engine players use EngineAiController (wraps AIPlayer — same as production).
// LLM players use AiPlayerController (formats state, queries LLM — same as production).
// Both go through the AiController interface, the same code path as real games,
// just called synchronously instead of through AiWebSocketSession's async coroutines.
// ═════════════════════════════════════════════════════════════════════════════

private fun playGame(
    registry: CardRegistry,
    deck1: Deck, deck2: Deck,
    p1Config: PlayerType, p2Config: PlayerType,
    gameId: Int, maxTurns: Int
): GameRunResult {
    val log = StringBuilder()
    log.appendLine("=== Game $gameId ===")
    log.appendLine("P1: ${p1Config.label}")
    log.appendLine("P2: ${p2Config.label}")
    log.appendLine("Deck 1: ${deckSummary(deck1)}")
    log.appendLine("Deck 2: ${deckSummary(deck2)}")

    val processor = ActionProcessor(registry)
    val initializer = GameInitializer(registry)
    val enumerator = LegalActionEnumerator.create(registry)
    val stateTransformer = ClientStateTransformer(registry)
    val enricher = LegalActionEnricher(ManaSolver(registry), registry)

    val initResult = initializer.initializeGame(
        GameConfig(
            players = listOf(PlayerConfig("Player 1", deck1), PlayerConfig("Player 2", deck2)),
            skipMulligans = true, startingPlayerIndex = 0
        )
    )

    val p1 = initResult.state.turnOrder[0]
    val p2 = initResult.state.turnOrder[1]
    var state = initResult.state

    // Create controllers — same production classes, called synchronously
    val p1Controller = createController(p1Config, registry, p1) { state }
    val p2Controller = createController(p2Config, registry, p2) { state }
    fun controllerFor(id: EntityId) = if (id == p1) p1Controller else p2Controller
    fun label(id: EntityId) = if (id == p1) "P1" else "P2"

    p1Controller.setDeckList(deck1.cards.groupingBy { it }.eachCount())
    p2Controller.setDeckList(deck2.cards.groupingBy { it }.eachCount())

    // Game log for LLM context (production AiWebSocketSession accumulates this)
    val recentGameLog = mutableListOf<String>()
    val maxLogSize = 30

    var turns = 0
    var actionCount = 0
    var drawReason = ""
    var lastProgressTurn = 0
    var lastProgressAction = 0

    val duration = measureTime {
        while (!state.gameOver && turns < maxTurns) {
            if (actionCount - lastProgressAction > 300 && turns == lastProgressTurn) {
                drawReason = "stuck(turn=$turns,step=${state.step.name})"
                log.appendLine("[STUCK] $drawReason")
                break
            }
            if (state.turnNumber > turns) {
                lastProgressTurn = turns
                lastProgressAction = actionCount
                turns = state.turnNumber
            }

            val decision = state.pendingDecision
            val priorityPlayer = state.priorityPlayerId

            if (decision == null && priorityPlayer == null) {
                drawReason = "noPriority(turn=$turns)"
                break
            }

            // Determine who needs to act and build their view
            val actingPlayer = decision?.playerId ?: priorityPlayer!!
            val controller = controllerFor(actingPlayer)
            val clientState = stateTransformer.transform(state, actingPlayer)
            val legalActions = if (decision == null) {
                enricher.enrich(enumerator.enumerate(state, actingPlayer), state, actingPlayer)
            } else {
                emptyList<LegalActionInfo>()
            }

            // Call the controller (same interface as production)
            actionCount++
            val response = controller.chooseAction(clientState, legalActions, decision, recentGameLog.toList())

            // Convert to GameAction (same as production AiWebSocketSession.submitResponse)
            val gameAction = when (response) {
                is ActionResponse.SubmitAction -> response.action
                is ActionResponse.SubmitDecision -> SubmitDecision(response.playerId, response.response)
            }

            log.appendLine("T$turns [${state.step.name}] ${label(actingPlayer)}: ${describeAction(gameAction)}")

            // Execute through production ActionProcessor
            val result = processor.process(state, gameAction).result
            if (result.error != null) {
                log.appendLine("  ERROR: ${result.error}")
                // Fallback: if a decision was pending, use engine AI to answer it;
                // otherwise try PassPriority (same as production handleAiAction)
                val fallbackAction = if (decision != null) {
                    val engineAi = AIPlayer.create(registry, actingPlayer,
                        listOf(BloomburrowAdvisorModule(), OnslaughtAdvisorModule()))
                    val engineResponse = engineAi.respondToDecision(state, decision)
                    log.appendLine("  *** ENGINE FALLBACK: ${label(actingPlayer)} failed ${decision::class.simpleName}, engine answered with ${engineResponse::class.simpleName} ***")
                    SubmitDecision(actingPlayer, engineResponse)
                } else {
                    log.appendLine("  *** ENGINE FALLBACK: ${label(actingPlayer)} action failed, passing priority ***")
                    PassPriority(actingPlayer)
                }
                val fallback = processor.process(state, fallbackAction).result
                if (fallback.error != null) {
                    log.appendLine("  FALLBACK FAILED: ${fallback.error}")
                    drawReason = "error(${result.error})"
                    break
                }
                accumulateLog(fallback.events, actingPlayer, recentGameLog, maxLogSize)
                state = fallback.state
            } else {
                accumulateLog(result.events, actingPlayer, recentGameLog, maxLogSize)
                logEvents(result.events, log)
                if (gameAction !is PassPriority) {
                    logBoardState(state, p1, p2, log)
                }
                state = result.state
            }
        }
        if (!state.gameOver && drawReason.isEmpty()) drawReason = "maxTurns($maxTurns)"
    }

    val p1Life = state.getEntity(p1)?.get<LifeTotalComponent>()?.life ?: 0
    val p2Life = state.getEntity(p2)?.get<LifeTotalComponent>()?.life ?: 0
    val winner = when {
        state.gameOver && state.winnerId == p1 -> "P1"
        state.gameOver && state.winnerId == p2 -> "P2"
        else -> "draw"
    }

    log.appendLine("\n=== Result ===")
    log.appendLine("Winner: $winner | P1: ${p1Life}hp | P2: ${p2Life}hp | Turns: $turns | Actions: $actionCount")
    if (drawReason.isNotEmpty()) log.appendLine("Draw reason: $drawReason")

    return GameRunResult(
        GameResult(gameId, turns, actionCount, duration.inWholeMilliseconds,
            p1Life, p2Life, state.gameOver, winner, drawReason),
        log.toString()
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// Controller factory — uses the same production classes
// ═════════════════════════════════════════════════════════════════════════════

private fun createController(
    config: PlayerType, registry: CardRegistry, playerId: EntityId,
    stateProvider: () -> GameState?
): AiController = when (config) {
    is PlayerType.Engine -> EngineAiController(registry, playerId, stateProvider)
    is PlayerType.Llm -> {
        val aiConfig = AiConfig(
            mode = "llm", baseUrl = config.baseUrl, apiKey = config.apiKey,
            model = config.model, reasoningEffort = config.reasoningEffort,
            maxRetries = 2, timeoutMs = 300000
        )
        AiPlayerController(aiConfig, LlmClient(aiConfig), playerId)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Deck building
// ═════════════════════════════════════════════════════════════════════════════

private fun buildDeck(config: PlayerType, pool: List<CardDefinition>, registry: CardRegistry): Deck {
    val deckMap = if (config.deckBuilder == "llm" && config is PlayerType.Llm) {
        val props = AiProperties(
            mode = "llm", baseUrl = config.baseUrl, apiKey = config.apiKey,
            model = config.model, reasoningEffort = config.reasoningEffort, timeoutMs = 300000
        )
        val aiConfig = AiConfig(
            mode = "llm", baseUrl = config.baseUrl, apiKey = config.apiKey,
            model = config.model, reasoningEffort = config.reasoningEffort, timeoutMs = 300000
        )
        try {
            AiDeckBuilder(props, LlmClient(aiConfig), pool, BloomburrowSet.basicLands, listOf("BLB")).build().deckList
        } catch (e: Exception) {
            println("  [WARN] LLM deck build failed: ${e.message}, falling back to heuristic")
            buildHeuristicSealedDeck(pool)
        }
    } else {
        buildHeuristicSealedDeck(pool)
    }
    return Deck(deckMap.flatMap { (name, count) -> List(count) { name } })
}

// ═════════════════════════════════════════════════════════════════════════════
// Sealed pool generation (same as AdvisorBenchmark)
// ═════════════════════════════════════════════════════════════════════════════

private fun generateSealedPool(nonBasics: List<CardDefinition>): List<CardDefinition> {
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

private fun deckSummary(deck: Deck): String {
    val basics = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
    val landCount = deck.cards.count { it in basics }
    val colors = deck.cards.filter { it in basics }.groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }.map { it.key.first() }
    return "${colors.joinToString("")} ${deck.size - landCount}sp"
}

// ═════════════════════════════════════════════════════════════════════════════
// Logging helpers
// ═════════════════════════════════════════════════════════════════════════════

private fun accumulateLog(events: List<GameEvent>, viewingPlayer: EntityId, log: MutableList<String>, maxSize: Int) {
    val clientEvents = ClientEventTransformer.transform(events, viewingPlayer)
    for (event in clientEvents) {
        if (event is com.wingedsheep.engine.view.ClientEvent.PermanentTapped) continue
        if (event is com.wingedsheep.engine.view.ClientEvent.PermanentUntapped) continue
        if (event is com.wingedsheep.engine.view.ClientEvent.ManaAdded) continue
        log.add(event.description)
        if (log.size > maxSize) log.removeFirst()
    }
}

private fun describeAction(action: GameAction): String = when (action) {
    is CastSpell -> "CastSpell"
    is PassPriority -> "Pass"
    is PlayLand -> "PlayLand"
    is DeclareAttackers -> "DeclareAttackers(${action.attackers.size})"
    is DeclareBlockers -> "DeclareBlockers(${action.blockers.size})"
    is ActivateAbility -> "ActivateAbility"
    is SubmitDecision -> "Decision(${action.response::class.simpleName})"
    else -> action::class.simpleName ?: "Unknown"
}

private fun logEvents(events: List<GameEvent>, log: StringBuilder) {
    val clientEvents = ClientEventTransformer.transform(events, EntityId("spectator"))
    for (event in clientEvents) {
        if (event is com.wingedsheep.engine.view.ClientEvent.PermanentTapped) continue
        if (event is com.wingedsheep.engine.view.ClientEvent.PermanentUntapped) continue
        if (event is com.wingedsheep.engine.view.ClientEvent.ManaAdded) continue
        log.appendLine("  → ${event.description}")
    }
}

private fun logBoardState(state: GameState, p1: EntityId, p2: EntityId, log: StringBuilder) {
    val projected = state.projectedState
    fun boardSummary(playerId: EntityId): String {
        val creatures = projected.getBattlefieldControlledBy(playerId)
            .filter { projected.isCreature(it) }
            .map { eid ->
                val name = state.getEntity(eid)?.get<CardComponent>()?.name ?: "?"
                "${name} ${projected.getPower(eid) ?: 0}/${projected.getToughness(eid) ?: 0}"
            }
        return if (creatures.isEmpty()) "(empty)" else creatures.joinToString(", ")
    }
    val p1Life = state.getEntity(p1)?.get<LifeTotalComponent>()?.life ?: 0
    val p2Life = state.getEntity(p2)?.get<LifeTotalComponent>()?.life ?: 0
    log.appendLine("  State: P1=${p1Life}hp ${boardSummary(p1)} | P2=${p2Life}hp ${boardSummary(p2)}")
}
