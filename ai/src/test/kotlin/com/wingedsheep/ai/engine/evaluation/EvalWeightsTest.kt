package com.wingedsheep.ai.engine.evaluation

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.doubles.shouldBeExactly
import kotlinx.serialization.encodeToString

class EvalWeightsTest : StringSpec({

    "bundled default reproduces the compiled fallback" {
        EvalWeights.resolve("default") shouldBe EvaluationWeights.DEFAULT
    }

    "bundled profiles are selectable without recompiling" {
        EvalWeights.resolve("blind") shouldBe EvaluationWeights.BLIND
        // `concave-hand*` differ from `default` only in `topdeckPenalty`: -1.0 and -2.0 against
        // the historical -3.0. Both are arena-measured; -2.0 is the one production takes, because
        // -1.0 also starts spending the last card on `respond-02`'s negative control.
        EvalWeights.ids shouldBe setOf("default", "blind", "concave-hand", "concave-hand-2")
    }

    "unknown profile safely uses the compiled fallback" {
        EvalWeights.resolve("does-not-exist") shouldBe EvaluationWeights.DEFAULT
    }

    "resource shape decodes profile ids to vectors" {
        EvalWeights.decode(
            """{"candidate":{"life":2.0,"boardPresence":3.0,"cardAdvantage":4.0,"threatAssessment":5.0,"tempo":6.0}}"""
        )["candidate"] shouldBe EvaluationWeights(2.0, 3.0, 4.0, 5.0, 6.0)
    }

    "malformed tuning artifact is ignored" {
        EvalWeights.decodeOrEmpty("""{"candidate":""") shouldBe emptyMap()
    }

    "raw profile decodes and scores the complete feature schema" {
        val weights = RawBoardFeatures.names.associateWith { 0.0 }.toMutableMap().apply {
            this["lifeDifference"] = 2.0
            this["isMyTurn"] = -0.25
        }
        val encoded = """{"candidate":{"intercept":1.5,"weights":${
            kotlinx.serialization.json.Json.encodeToString(weights)
        },"winProbabilityScale":1.0}}"""
        val profile = EvalWeights.decodeRaw(encoded).getValue("candidate")

        profile.isValid() shouldBe true
        profile.evaluate(zeroFeatures.copy(lifeDifference = 3, isMyTurn = 1)) shouldBeExactly 7.25
    }

    "raw profile rejects an incomplete or non-finite vector" {
        RawEvaluationWeights(0.0, mapOf("lifeDifference" to 1.0)).isValid() shouldBe false
        RawEvaluationWeights(
            0.0,
            RawBoardFeatures.names.associateWith { if (it == "myLife") Double.NaN else 0.0 },
        ).isValid() shouldBe false
    }
})

private val zeroFeatures = RawBoardFeatures(
    myLife = 0, opponentLife = 0, lifeDifference = 0,
    myBurnRangeLife = 0, opponentBurnRangeLife = 0,
    creatureCountDifference = 0, totalPowerDifference = 0, totalToughnessDifference = 0,
    evasiveCreatureDifference = 0, untappedCreatureDifference = 0,
    artifactCountDifference = 0, enchantmentCountDifference = 0, planeswalkerCountDifference = 0,
    landCountDifference = 0, planeswalkerLoyaltyDifference = 0,
    handSizeDifference = 0, myHandSize = 0, opponentHandSize = 0,
    untappedLandDifference = 0, graveyardSizeDifference = 0,
    summoningSickCreatureDifference = 0, librarySizeDifference = 0,
    removalInHandDifference = 0, threatsInPlayDifference = 0,
    turnNumber = 0, isMyTurn = 0,
)
