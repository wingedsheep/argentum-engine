package com.wingedsheep.ai.training

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Serializable
data class TrainingCorpusEnvelope(
    val games: List<TrainingGameMetadata>,
    val records: List<DecisionTrainingRecord>,
)

/** Atomic whole-corpus persistence. A crash can leave a temporary file, never a half-accepted game. */
object TrainingCorpusFiles {
    private val json = Json { encodeDefaults = true; classDiscriminator = "type" }

    fun writeAtomically(path: Path, corpus: TrainingCorpus, minimumGeneratorCount: Int = 2) {
        val validation = TrainingCorpusValidator.validate(corpus, minimumGeneratorCount)
        require(validation.valid) { validation.errors.joinToString("; ") }
        Files.createDirectories(path.toAbsolutePath().parent)
        val staged = Files.createTempFile(path.toAbsolutePath().parent, path.fileName.toString(), ".tmp")
        try {
            Files.writeString(staged, json.encodeToString(TrainingCorpusEnvelope(corpus.games, corpus.records)))
            runCatching {
                Files.move(staged, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            }.getOrElse {
                Files.move(staged, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(staged)
        }
    }

    fun appendGameAtomically(
        path: Path,
        game: TrainingGameMetadata,
        records: List<DecisionTrainingRecord>,
    ) {
        require(records.all { it.identity.runId == game.runId && it.identity.gameId == game.gameId })
        val existing = if (Files.isRegularFile(path)) read(path) else TrainingCorpus(emptyList(), emptyList())
        writeAtomically(path, TrainingCorpus(existing.games + game, existing.records + records), minimumGeneratorCount = 1)
    }

    fun read(path: Path): TrainingCorpus {
        val envelope = json.decodeFromString<TrainingCorpusEnvelope>(Files.readString(path))
        return TrainingCorpus(envelope.games, envelope.records)
    }
}

@Serializable
enum class CorpusSplit { TRAIN, VALIDATION, TEST }

@Serializable
data class SplitAssignment(val gameId: String, val split: CorpusSplit)

@Serializable
data class CorpusSplitManifest(val seed: Long, val assignments: List<SplitAssignment>)

object CorpusSplitter {
    /** Deterministic whole-game split. Adjacent paired games can share [pairKey] to prevent leakage. */
    fun split(
        games: List<TrainingGameMetadata>,
        seed: Long,
        pairKey: (TrainingGameMetadata) -> String = { it.globallyUniqueId },
    ): CorpusSplitManifest {
        val grouped = games.groupBy(pairKey).toSortedMap()
        val assignments = grouped.flatMap { (key, members) ->
            val bucket = Math.floorMod(TrainingRecordEncoding.sha256("$seed:$key").take(8).toLong(16), 100)
            val split = when { bucket < 70 -> CorpusSplit.TRAIN; bucket < 85 -> CorpusSplit.VALIDATION; else -> CorpusSplit.TEST }
            members.map { SplitAssignment(it.globallyUniqueId, split) }
        }.sortedBy { it.gameId }
        return CorpusSplitManifest(seed, assignments)
    }
}
