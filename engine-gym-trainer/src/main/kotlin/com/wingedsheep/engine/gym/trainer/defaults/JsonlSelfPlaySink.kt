package com.wingedsheep.engine.gym.trainer.defaults

import com.wingedsheep.engine.gym.trainer.spi.SelfPlaySink
import com.wingedsheep.engine.gym.trainer.spi.SlotEncoding
import com.wingedsheep.engine.gym.trainer.spi.TrainerContext
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Writes self-play rows as one JSON object per line (JSONL / NDJSON).
 *
 * Rows are buffered in memory for the duration of a game; `endGame`
 * back-patches the final outcome label onto every row and flushes them in
 * one append. This way every trainer row already carries the z-label
 * AlphaZero training expects without a second pass.
 *
 * Each row is:
 * ```json
 * {
 *   "game_id": "...",
 *   "acting_player": "...",
 *   "head": "actions",
 *   "legal_slots": [{"head": "actions", "slot": 3}],
 *   "visits": [12, 8, 0],
 *   "mcts_value": 0.12,
 *   "outcome": 1.0,
 *   "features": <user-serialized features JSON>
 * }
 * ```
 *
 * Works for any feature type `T` that has a [KSerializer]. Swap it out for
 * an HDF5 / NPZ / Parquet sink when performance matters.
 */
class JsonlSelfPlaySink<T>(
    private val path: Path,
    private val featureSerializer: KSerializer<T>,
    private val json: Json = DEFAULT_JSON,
    private val append: Boolean = true
) : SelfPlaySink<T> {

    private var writer: BufferedWriter? = null
    private val buffered: MutableList<BufferedRow<T>> = mutableListOf()
    private var currentGameId: String = ""

    override fun beginGame(gameId: String, players: List<EntityId>) {
        ensureWriter()
        buffered.clear()
        currentGameId = gameId
    }

    override fun recordStep(
        features: T,
        ctx: TrainerContext,
        actingPlayer: EntityId,
        headUsed: String,
        legalSlots: List<SlotEncoding>,
        visits: IntArray,
        mctsValue: Float
    ) {
        buffered += BufferedRow(
            gameId = currentGameId,
            actingPlayer = actingPlayer.value,
            head = headUsed,
            slots = legalSlots.map { SlotDto(it.head, it.slot) },
            visits = visits.toList(),
            mctsValue = mctsValue,
            features = features
        )
    }

    override fun endGame(winner: EntityId?) {
        val w = writer ?: return
        val winnerIdStr = winner?.value
        for (row in buffered) {
            val outcome: Float = when (winnerIdStr) {
                null -> 0f
                row.actingPlayer -> 1f
                else -> -1f
            }
            val diskRow = DiskRow(
                gameId = row.gameId,
                actingPlayer = row.actingPlayer,
                head = row.head,
                legalSlots = row.slots,
                visits = row.visits,
                mctsValue = row.mctsValue,
                outcome = outcome,
                features = json.encodeToJsonElement(featureSerializer, row.features).toString()
            )
            w.write(json.encodeToString(DiskRow.serializer(), diskRow))
            w.newLine()
        }
        w.flush()
        buffered.clear()
    }

    override fun close() {
        writer?.flush()
        writer?.close()
        writer = null
    }

    private fun ensureWriter() {
        if (writer != null) return
        Files.createDirectories(path.parent ?: path.toAbsolutePath().parent)
        val options = if (append) arrayOf(StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        else arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
        writer = Files.newBufferedWriter(path, *options)
    }

    private data class BufferedRow<T>(
        val gameId: String,
        val actingPlayer: String,
        val head: String,
        val slots: List<SlotDto>,
        val visits: List<Int>,
        val mctsValue: Float,
        val features: T
    )

    @Serializable
    private data class DiskRow(
        val gameId: String,
        val actingPlayer: String,
        val head: String,
        val legalSlots: List<SlotDto>,
        val visits: List<Int>,
        val mctsValue: Float,
        val outcome: Float,
        val features: String
    )

    @Serializable
    private data class SlotDto(val head: String, val slot: Int)

    companion object {
        val DEFAULT_JSON: Json = Json {
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
