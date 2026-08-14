package com.wingedsheep.ai.training

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContain

class TrainingCorpusValidatorTest : FunSpec({
    fun game(id: String, generator: String = "production") = TrainingGameMetadata(
        runId = "run", gameId = id, setCode = "POR", format = "Standard",
        deckHashes = listOf("a", "b"), seed = 1, profilesBySeat = listOf("production", "production"),
        completionReason = "completed", generator = generator,
    )

    test("rejects duplicates invalid completions recovery and missing generator diversity") {
        val recovered = game("bad").copy(recoveredIllegalAction = true)
        val result = TrainingCorpusValidator.validate(
            TrainingCorpus(listOf(game("same"), game("same"), recovered), emptyList())
        )
        result.valid.shouldBeFalse()
        result.errors shouldContain "duplicate runId/gameId"
        result.errors shouldContain "invalid game run/bad"
        result.errors shouldContain "missing generator diversity"
    }
})
