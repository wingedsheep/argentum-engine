package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class TypeLineTest : DescribeSpec({

    describe("TypeLine.parse") {
        it("preserves hyphens inside subtype names") {
            val parsed = TypeLine.parse("Artifact Creature — Assembly-Worker")

            parsed.cardTypes shouldBe setOf(CardType.ARTIFACT, CardType.CREATURE)
            parsed.subtypes shouldBe setOf(Subtype("Assembly-Worker"))
        }

        it("splits Urza's Power-Plant into two land types, keeping the hyphen") {
            val parsed = TypeLine.parse("Land — Urza's Power-Plant")

            parsed.cardTypes shouldBe setOf(CardType.LAND)
            parsed.subtypes shouldBe setOf(
                Subtype("Urza's"),
                Subtype("Power-Plant"),
            )
        }

        it("still accepts an ASCII hyphen as a spaced type-subtype separator") {
            val parsed = TypeLine.parse("Artifact Creature - Assembly-Worker")

            parsed.cardTypes shouldBe setOf(CardType.ARTIFACT, CardType.CREATURE)
            parsed.subtypes shouldBe setOf(Subtype("Assembly-Worker"))
        }
    }

    describe("isFreelyAddedBasicLand") {
        it("is true for the five nonsnow basic land types") {
            TypeLine.parse("Basic Land — Plains").isFreelyAddedBasicLand shouldBe true
            TypeLine.parse("Basic Land — Forest").isFreelyAddedBasicLand shouldBe true
        }

        it("is false for pack-only basics: snow basics and Wastes") {
            TypeLine.parse("Basic Snow Land — Island").isFreelyAddedBasicLand shouldBe false
            TypeLine.parse("Basic Land").isFreelyAddedBasicLand shouldBe false
            TypeLine.parse("Basic Snow Land").isFreelyAddedBasicLand shouldBe false
        }

        it("is false for nonbasic lands with a basic land type") {
            TypeLine.parse("Land — Mountain Forest").isFreelyAddedBasicLand shouldBe false
        }
    }
})
