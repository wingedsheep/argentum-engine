package com.wingedsheep.ai.training

import com.wingedsheep.ai.arena.ArenaAgent
import com.wingedsheep.ai.arena.ArenaAgents
import com.wingedsheep.ai.arena.ArenaTrainingObserver
import com.wingedsheep.ai.arena.TableGameRunner
import com.wingedsheep.ai.arena.TableSetup
import com.wingedsheep.ai.arena.mixSeed
import com.wingedsheep.ai.engine.buildSeededSealedDeck
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random

/** Explicitly enabled offline collector; ordinary tests skip it. */
class EclTrainingBenchmark : FunSpec({
    val enabled = System.getProperty("eclCollect") == "true"

    test("collect clean replayable ECL decision games").config(enabled = enabled) {
        val games = System.getProperty("eclCollectGames")?.toIntOrNull() ?: 100
        val seed = System.getProperty("eclCollectSeed")?.toLongOrNull() ?: 20260801L
        val requestedOutput = Path.of(
            System.getProperty("eclCollectOutput") ?: "benchmarks/training/ecl-corpus.json"
        )
        val baseDir = Path.of(System.getProperty("eclCollectBaseDir") ?: "")
        val output = if (requestedOutput.isAbsolute) requestedOutput else baseDir.resolve(requestedOutput).normalize()
        val runId = System.getProperty("eclCollectRunId") ?: "ecl-$seed"
        val set = MtgSetCatalog.requireByCode("ECL")
        val registry = CardRegistry().apply { register(set.cards); register(set.basicLands) }
        val profiles = listOf("production", "v0", "v0-rollout-determinized")
        val existing = if (Files.isRegularFile(output)) {
            TrainingCorpusFiles.read(output)
        } else {
            TrainingCorpus(emptyList(), emptyList())
        }
        require(existing.games.all { it.runId == runId }) { "existing corpus belongs to another run" }
        var cleanGames = existing.games.size
        var gameIndex = existing.games.maxOfOrNull {
            it.gameId.removePrefix("game-").toInt()
        }?.plus(1) ?: (System.getProperty("eclCollectStartIndex")?.toIntOrNull() ?: 0)
        var attemptedThisRun = 0
        var quarantinedThisRun = 0

        while (cleanGames < games) {
            val gameSeed = mixSeed(seed, gameIndex.toLong() + 1)
            val deck = buildSeededSealedDeck(set.cards, Random(gameSeed))
            val seat0 = ArenaAgents.resolve(profiles[gameIndex % profiles.size])
            val seat1 = ArenaAgents.resolve(profiles[(gameIndex + 1) % profiles.size])
            val gameId = "game-${gameIndex.toString().padStart(6, '0')}"
            val observer = BufferedGameCollector(registry, runId, gameId, gameSeed)
            val outcome = TableGameRunner.play(
                registry, TableSetup.HEADS_UP, listOf(seat0, seat1), listOf(deck, deck), gameSeed,
                groupId = gameIndex / 2, rotation = gameIndex % 2, trainingObserver = observer,
            )
            if (outcome.completed && outcome.illegalActions.isEmpty() && outcome.exception == null) {
                val metadata = observer.metadata(deck, listOf(seat0, seat1), generator = seat0.name)
                TrainingCorpusFiles.appendGameAtomically(output, metadata, observer.records)
                cleanGames++
            } else {
                quarantinedThisRun++
                System.err.println("quarantined $gameId: ${outcome.drawReason} ${outcome.illegalActions} ${outcome.exception}")
            }
            attemptedThisRun++
            gameIndex++
            if (attemptedThisRun % 10 == 0 || cleanGames == games) {
                val filled = (cleanGames * 20 / games).coerceIn(0, 20)
                val bar = "#".repeat(filled) + "-".repeat(20 - filled)
                println("ECL collection [$bar] $cleanGames/$games clean; $quarantinedThisRun quarantined this run")
            }
        }
        val corpus = if (Files.isRegularFile(output)) {
            TrainingCorpusFiles.read(output)
        } else {
            TrainingCorpus(emptyList(), emptyList())
        }
        val report = CorpusReporter.report(corpus, minimumGeneratorCount = 2)
        val reportPath = output.resolveSibling(output.fileName.toString().substringBeforeLast('.') + "-report.json")
        Files.writeString(reportPath, TrainingRecordEncoding.json.encodeToString(report))
        require(report.valid && report.games > 0) {
            (report.errors + "no clean games were retained").distinct().joinToString("; ")
        }
        println("Collected ${report.games} clean games / ${report.decisions} decisions to $output")
    }
})

private class BufferedGameCollector(
    registry: CardRegistry,
    private val runId: String,
    private val gameId: String,
    private val seed: Long,
) : ArenaTrainingObserver {
    private val factory = DecisionRecordFactory(registry)
    private val actions = mutableListOf<String>()
    private var initialDigest: String = ""
    private var seats: List<EntityId> = emptyList()
    private var nextDecisionIndex = 0
    val records = mutableListOf<DecisionTrainingRecord>()

    @Synchronized
    override fun gameStarted(state: GameState, seats: List<EntityId>) {
        this.seats = seats
        val viewerDigests = seats.map { TrainingRecordEncoding.digest(TrainingRecordEncoding.observation(state, it)) }
        initialDigest = TrainingRecordEncoding.sha256(viewerDigests.joinToString("|"))
    }

    @Synchronized
    override fun quietRoot(state: GameState, actingPlayer: EntityId) {
        val identity = DecisionIdentity(runId, gameId, nextDecisionIndex++)
        runCatching {
            factory.capture(
                state, actingPlayer, identity, "Limited", seed,
                actionPrefixDigest = TrainingRecordEncoding.sha256(actions.joinToString("\n")),
                requireMeaningful = true,
            )
        }.onSuccess(records::add)
    }

    @Synchronized
    override fun action(action: GameAction) {
        actions += TrainingRecordEncoding.json.encodeToString<GameAction>(action)
    }

    @Synchronized
    override fun decision(playerId: EntityId, response: DecisionResponse) {
        actions += TrainingRecordEncoding.json.encodeToString<GameAction>(SubmitDecision(playerId, response))
    }

    @Synchronized
    fun metadata(deck: Deck, agents: List<ArenaAgent>, generator: String): TrainingGameMetadata {
        val names = if (deck.cardEntries.isNotEmpty()) deck.cardEntries.map { it.name } else deck.cards
        val deckHash = TrainingRecordEncoding.sha256(names.sorted().joinToString("\n"))
        return TrainingGameMetadata(
            runId = runId, gameId = gameId, setCode = "ECL", format = "Limited",
            deckHashes = List(seats.size) { deckHash }, seed = seed,
            profilesBySeat = agents.map { it.name }, completionReason = "completed",
            generator = generator, initialStateDigest = initialDigest, actionLog = actions,
        )
    }
}
