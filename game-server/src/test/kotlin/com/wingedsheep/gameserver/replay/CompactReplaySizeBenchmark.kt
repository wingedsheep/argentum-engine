package com.wingedsheep.gameserver.replay

import com.wingedsheep.ai.engine.buildHeuristicSealedDeck
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.persistence.persistenceJson
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession
import java.time.Instant
import java.util.Base64
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Benchmark: play full games with PURELY RANDOM actions through the real [GameSession] recording
 * path, build the [CompactReplay] each produces, and measure how large that durable record is —
 * raw JSON bytes and the stored gzip+base64 form ([ReplayCodec]) — plus bytes-per-action and
 * bytes-per-frame. This answers "how big are compact replays in practice?" with real games rather
 * than a hand-crafted scenario.
 *
 * Disabled by default (it plays whole games). Run with:
 *   ./gradlew :game-server:test --tests "*.CompactReplaySizeBenchmark" -Dbenchmark=true
 *   ./gradlew :game-server:test --tests "*.CompactReplaySizeBenchmark" -Dbenchmark=true -DbenchmarkGames=50
 *   ./gradlew :game-server:test --tests "*.CompactReplaySizeBenchmark" -Dbenchmark=true -DbenchmarkGames=50 -DbenchmarkSet=BLB
 */
class CompactReplaySizeBenchmark : ScenarioTestBase() {

    private fun mockWs(id: String): WebSocketSession =
        mockk(relaxed = true) { every { this@mockk.id } returns id }

    private val numGames = System.getProperty("benchmarkGames")?.toIntOrNull() ?: 20
    private val enabled = System.getProperty("benchmark") == "true"
    private val setCode = System.getProperty("benchmarkSet") ?: "POR"

    init {
        test("compact replay size over $numGames random games ($setCode)").config(enabled = enabled) {
            val set = MtgSetCatalog.requireByCode(setCode)
            val nonBasics = set.cards.filter { !it.typeLine.isBasicLand }
            val enumerator = LegalActionEnumerator.create(cardRegistry)
            val rng = Random(0x5EED)

            println("=== COMPACT REPLAY SIZE BENCHMARK: $numGames random games on $setCode ===")

            val rows = mutableListOf<ReplaySizeRow>()

            for (i in 0 until numGames) {
                val deck1 = buildHeuristicSealedDeck(generateSealedPool(nonBasics, rng))
                val deck2 = buildHeuristicSealedDeck(generateSealedPool(nonBasics, rng))

                val session = GameSession(cardRegistry = cardRegistry, maxPlayers = 2)
                val p1 = EntityId.of("p1-game$i")
                val p2 = EntityId.of("p2-game$i")
                session.addPlayer(PlayerSession(mockWs("ws1-$i"), p1, "Alice"), deck1)
                session.addPlayer(PlayerSession(mockWs("ws2-$i"), p2, "Bob"), deck2)
                session.startGame()
                session.keepHand(p1)
                session.keepHand(p2)

                val outcome = playRandomGame(session, enumerator, rng, maxTurns = 40)

                val setup = session.getReplaySetup()
                if (setup == null) {
                    println("  [game $i] not replayable (no recorded setup) — skipping")
                    continue
                }
                val actions = session.getRecordedActions()
                val replay = CompactReplay(
                    gameId = session.sessionId,
                    players = session.getPlayers().map { ReplayPlayerInfo(it.playerId.value, it.playerName) },
                    startedAt = Instant.now().toString(),
                    endedAt = Instant.now().toString(),
                    winnerName = null,
                    setup = setup,
                    actions = actions,
                )

                // Sanity: the stored form round-trips losslessly (same guarantee the codec test proves).
                ReplayCodec.decode(ReplayCodec.encode(replay)) shouldBe replay

                val json = persistenceJson.encodeToString(CompactReplay.serializer(), replay)
                val jsonBytes = json.toByteArray(Charsets.UTF_8).size
                val encoded = ReplayCodec.encode(replay)
                val gzipBytes = Base64.getDecoder().decode(encoded).size

                val row = ReplaySizeRow(
                    actions = actions.size,
                    frames = replay.frameCount,
                    turns = outcome.turns,
                    jsonBytes = jsonBytes,
                    gzipBytes = gzipBytes,
                    base64Chars = encoded.length,
                )
                rows.add(row)
                if (i < 5 || (i + 1) % 10 == 0 || i == numGames - 1) {
                    println(
                        "  [${i + 1}/$numGames] ${row.turns} turns, ${row.actions} actions -> " +
                            "json=${fmtBytes(row.jsonBytes)}, gzip+b64=${fmtBytes(row.gzipBytes)} " +
                            "(${row.bytesPerAction().roundToInt()} B/action)"
                    )
                }
            }

            if (rows.isEmpty()) {
                println("No replayable games were recorded.")
                return@config
            }

            fun List<Int>.avg() = average()
            println()
            println("--- SUMMARY (${rows.size} games) ---")
            println("Turns:        avg=${"%.1f".format(rows.map { it.turns }.avg())}, max=${rows.maxOf { it.turns }}")
            println("Actions:      avg=${rows.map { it.actions }.avg().roundToInt()}, min=${rows.minOf { it.actions }}, max=${rows.maxOf { it.actions }}")
            println()
            println("Compact replay size:")
            println("  JSON (raw):   avg=${fmtBytes(rows.map { it.jsonBytes }.avg().roundToInt())}, max=${fmtBytes(rows.maxOf { it.jsonBytes })}")
            println("  gzip+base64:  avg=${fmtBytes(rows.map { it.gzipBytes }.avg().roundToInt())}, max=${fmtBytes(rows.maxOf { it.gzipBytes })}  (the stored form)")
            val avgJson = rows.map { it.jsonBytes }.avg()
            val avgGzip = rows.map { it.gzipBytes }.avg()
            println("  compression:  ~${(100 - avgGzip * 100 / avgJson).roundToInt()}% smaller than raw JSON")
            println("  per action:   ~${(avgGzip / rows.map { it.actions }.avg()).roundToInt()} B/action (stored)")
            println("  total stored: ${fmtBytes(rows.sumOf { it.gzipBytes })} for ${rows.size} games")
        }
    }
}

private data class ReplaySizeRow(
    val actions: Int,
    val frames: Int,
    val turns: Int,
    val jsonBytes: Int,
    val gzipBytes: Int,
    val base64Chars: Int,
) {
    fun bytesPerAction(): Double = if (actions == 0) 0.0 else gzipBytes.toDouble() / actions
}

private data class GameOutcome(val turns: Int, val actions: Int, val gameOver: Boolean)

/**
 * Drive a started, past-mulligan [GameSession] to completion using purely random legal actions and
 * random decision responses. Everything goes through the session's recording path, so the resulting
 * [CompactReplay] is a real one.
 */
private fun playRandomGame(
    session: GameSession,
    enumerator: LegalActionEnumerator,
    rng: Random,
    maxTurns: Int,
): GameOutcome {
    var actionCount = 0
    var lastProgressTurn = 0
    var lastProgressAction = 0
    var stuckCount = 0

    while (true) {
        val state = session.getStateForTesting() ?: break
        if (state.gameOver || state.turnNumber >= maxTurns) break

        // Stuck detection: a random policy can loop a no-op corner forever.
        if (actionCount - lastProgressAction > 1000 && state.turnNumber == lastProgressTurn) {
            stuckCount++
            if (stuckCount >= 3) break
        }
        if (state.turnNumber > lastProgressTurn) {
            lastProgressTurn = state.turnNumber
            lastProgressAction = actionCount
            stuckCount = 0
        }

        val pendingDecision = state.pendingDecision
        val action: GameAction = if (pendingDecision != null) {
            SubmitDecision(pendingDecision.playerId, randomDecisionResponse(pendingDecision, rng))
        } else {
            val priorityPlayer = state.priorityPlayerId ?: break
            val affordable = enumerator.enumerate(state, priorityPlayer).filter { it.affordable }
            if (affordable.isEmpty()) break
            affordable[rng.nextInt(affordable.size)].action
        }

        val result = session.executeAction(action.playerId, action)
        if (result is GameSession.ActionResult.Failure) {
            // Random action was rejected — fall back to passing priority so the game advances.
            val fallback = session.executeAutoPass(action.playerId)
            if (fallback is GameSession.ActionResult.Failure) break
        }
        actionCount++
    }

    val finalState = session.getStateForTesting()
    return GameOutcome(
        turns = finalState?.turnNumber ?: 0,
        actions = actionCount,
        gameOver = finalState?.gameOver ?: false,
    )
}

/** A random but valid response for any pending decision. */
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

        is BatchYesNoDecision -> BatchYesNoResponse(decision.id, choice = rng.nextBoolean(), applyToAll = true)

        is ChooseReplacementDecision -> {
            val fromIndex = rng.nextInt(decision.fromOptions.size)
            val allowed = decision.allowedToByFrom.getOrNull(fromIndex)
            val toIndex = if (allowed != null && allowed.isNotEmpty()) allowed[rng.nextInt(allowed.size)]
            else rng.nextInt(decision.toOptions.size)
            ReplacementChosenResponse(decision.id, fromIndex, toIndex)
        }

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

        is CombatResolutionDecision ->
            CombatResolutionResponse(decision.id, decision.edges.map { DamageEdgeAmount(it.id, it.amount) })

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

/** Six-booster sealed pool, mirroring the AI benchmark's generator. */
private fun generateSealedPool(nonBasics: List<CardDefinition>, rng: Random): List<CardDefinition> {
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
            return available[rng.nextInt(available.size)].also { usedNames.add(it.name) }
        }
        repeat(11) { pick(commons)?.let { pool.add(it) } }
        repeat(3) { pick(uncommons)?.let { pool.add(it) } }
        val rare = if (mythics.isNotEmpty() && rng.nextDouble() < 0.125) pick(mythics) else null
        (rare ?: pick(rares) ?: pick(uncommons) ?: pick(commons))?.let { pool.add(it) }
    }
    return pool
}

private fun fmtBytes(bytes: Int): String =
    if (bytes < 1024) "$bytes B" else "%.1f KB".format(bytes / 1024.0)
