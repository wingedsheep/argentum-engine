package com.wingedsheep.ai.training

import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.AiProfileSelector
import com.wingedsheep.ai.engine.evaluation.RawBoardFeatures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class EclTrainingInfrastructureTest : FunSpec({
    fun game(id: String, generator: String = "production") = TrainingGameMetadata(
        runId = "ecl-run", gameId = id, setCode = "ECL", format = "Limited",
        deckHashes = listOf("deck-a", "deck-b"), seed = id.hashCode().toLong(),
        profilesBySeat = listOf("production", "production"), completionReason = "completed",
        generator = generator,
    )

    test("whole-game split is deterministic and keeps paired games together") {
        val games = listOf(game("pair-1-a"), game("pair-1-b"), game("pair-2-a"))
        val first = CorpusSplitter.split(games, 17) { it.gameId.substringBeforeLast('-') }
        val second = CorpusSplitter.split(games.reversed(), 17) { it.gameId.substringBeforeLast('-') }
        first shouldBe second
        first.assignments.filter { it.gameId.contains("pair-1") }.map { it.split }.distinct().size shouldBe 1
    }

    test("atomic corpus file accepts one clean game and rejects recovered games") {
        val directory = Files.createTempDirectory("ecl-corpus-test")
        val path = directory.resolve("corpus.json")
        TrainingCorpusFiles.appendGameAtomically(path, game("one"), emptyList())
        TrainingCorpusFiles.read(path).games shouldBe listOf(game("one"))
        shouldThrow<IllegalArgumentException> {
            TrainingCorpusFiles.appendGameAtomically(path, game("bad").copy(recoveredIllegalAction = true), emptyList())
        }
    }

    test("artifact loader fails closed on wrong set schema and non-finite values") {
        val names = RawBoardFeatures.names.sorted()
        val valid = ApprenticeArtifact(
            modelId = "ecl-apprentice", setCode = "ECL", featureNames = names,
            sharedCoefficients = List(names.size) { 0.0 },
        )
        val json = kotlinx.serialization.json.Json.encodeToString(ApprenticeArtifact.serializer(), valid)
        ApprenticeArtifactLoader.decodeOrNull(json, "ECL") shouldBe valid
        ApprenticeArtifactLoader.decodeOrNull(json, "BLB") shouldBe null
        valid.copy(sharedCoefficients = listOf(Double.NaN)).validationErrors() shouldContain "shared coefficient count mismatch"
    }

    test("ECL profile selection cannot leak to another set") {
        AiProfileSelector.select("ECL", AiProfile.ECL_APPRENTICE) shouldBe AiProfile.ECL_APPRENTICE
        // Whatever is live, not a fixed profile: this pins that a set-scoped request falls back to
        // the production agent, so it moves with every promotion.
        AiProfileSelector.select("BLB", AiProfile.ECL_APPRENTICE) shouldBe
            AiProfile.PRODUCTION_CANDIDATE_COUNTERPATIENCE
        AiProfileSelector.select("BLB", AiProfile.CURRENT) shouldBe AiProfile.CURRENT
    }

    test("corpus report exposes coverage dimensions") {
        val report = CorpusReporter.report(
            TrainingCorpus(listOf(game("a", "production"), game("b", "v0")), emptyList())
        )
        report.valid shouldBe true
        report.gamesByGenerator shouldBe mapOf("production" to 1, "v0" to 1)
        report.decisions shouldBe 0
    }
})
