package com.wingedsheep.ai.arena

import com.wingedsheep.ai.engine.evaluation.RawBoardFeatures
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/** Buffers sampled positions until the game result is known, then appends labelled JSONL rows. */
class ArenaFeatureCollector(
    private val path: Path,
    registry: CardRegistry,
    private val setCode: String,
) {
    private val intents = IntentCatalog.of(registry)
    private val json = Json { encodeDefaults = true }

    fun newGame(gameId: String, agentsByPlayer: Map<EntityId, String>): Game =
        Game(gameId, agentsByPlayer)

    inner class Game(
        private val gameId: String,
        private val agentsByPlayer: Map<EntityId, String>,
    ) {
        private val rows = mutableListOf<BufferedRow>()
        private var quietStates = 0

        fun observe(state: GameState, toMove: EntityId) {
            quietStates++
            if (quietStates % SAMPLE_EVERY != 0) return
            rows += BufferedRow(
                features = RawBoardFeatures.extract(state, state.projectedState, toMove, intents),
                toMove = toMove.value,
                turn = state.turnNumber,
            )
        }

        fun finish(winner: EntityId?) {
            val diskRows = rows.map { row ->
                DiskRow(
                    features = row.features,
                    toMove = row.toMove,
                    turn = row.turn,
                    gameId = gameId,
                    setCode = setCode,
                    agent = agentsByPlayer[EntityId(row.toMove)]
                        ?: error("No arena agent recorded for player ${row.toMove}"),
                    result = when (winner?.value) {
                        null -> 0
                        row.toMove -> 1
                        else -> -1
                    },
                )
            }
            append(diskRows)
        }
    }

    @Synchronized
    private fun append(rows: List<DiskRow>) {
        if (rows.isEmpty()) return
        path.toAbsolutePath().parent?.let(Files::createDirectories)
        Files.newBufferedWriter(
            path,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        ).use { writer ->
            rows.forEach {
                writer.write(json.encodeToString(it))
                writer.newLine()
            }
        }
    }

    private data class BufferedRow(val features: RawBoardFeatures, val toMove: String, val turn: Int)

    @Serializable
    private data class DiskRow(
        val features: RawBoardFeatures,
        val toMove: String,
        val turn: Int,
        val gameId: String,
        val setCode: String,
        val agent: String,
        val result: Int,
    )

    companion object { private const val SAMPLE_EVERY = 8 }
}
