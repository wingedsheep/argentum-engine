package com.wingedsheep.rulesengine.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class KeywordTest : FunSpec({

    context("fromString") {
        test("parses single word keywords") {
            Keyword.fromString("Flying") shouldBe Keyword.FLYING
            Keyword.fromString("Trample") shouldBe Keyword.TRAMPLE
            Keyword.fromString("Haste") shouldBe Keyword.HASTE
            Keyword.fromString("Vigilance") shouldBe Keyword.VIGILANCE
            Keyword.fromString("Reach") shouldBe Keyword.REACH
            Keyword.fromString("Deathtouch") shouldBe Keyword.DEATHTOUCH
            Keyword.fromString("Lifelink") shouldBe Keyword.LIFELINK
            Keyword.fromString("Changeling") shouldBe Keyword.CHANGELING
        }

        test("parses multi-word keywords") {
            Keyword.fromString("First strike") shouldBe Keyword.FIRST_STRIKE
            Keyword.fromString("Double strike") shouldBe Keyword.DOUBLE_STRIKE
        }

        test("parsing is case insensitive") {
            Keyword.fromString("flying") shouldBe Keyword.FLYING
            Keyword.fromString("FLYING") shouldBe Keyword.FLYING
            Keyword.fromString("Flying") shouldBe Keyword.FLYING
            Keyword.fromString("changeling") shouldBe Keyword.CHANGELING
        }

        test("returns null for unknown keyword") {
            Keyword.fromString("NotAKeyword").shouldBeNull()
            Keyword.fromString("").shouldBeNull()
        }
    }

    context("parseFromOracleText") {
        test("parses single keyword on its own line") {
            val keywords = Keyword.parseFromOracleText("Flying")
            keywords shouldBe setOf(Keyword.FLYING)
        }

        test("parses changeling keyword") {
            val keywords = Keyword.parseFromOracleText("Changeling")
            keywords shouldBe setOf(Keyword.CHANGELING)
        }

        test("parses multiple keywords on separate lines") {
            val oracleText = """
                Flying
                Vigilance
            """.trimIndent()
            val keywords = Keyword.parseFromOracleText(oracleText)
            keywords shouldContainExactlyInAnyOrder listOf(Keyword.FLYING, Keyword.VIGILANCE)
        }

        test("parses comma-separated keywords") {
            val keywords = Keyword.parseFromOracleText("Flying, trample, changeling")
            keywords shouldContainExactlyInAnyOrder listOf(Keyword.FLYING, Keyword.TRAMPLE, Keyword.CHANGELING)
        }

        test("ignores non-keyword text") {
            val oracleText = """
                Flying
                When this creature enters the battlefield, draw a card.
            """.trimIndent()
            val keywords = Keyword.parseFromOracleText(oracleText)
            keywords shouldBe setOf(Keyword.FLYING)
        }

        test("returns empty set for no keywords") {
            val keywords = Keyword.parseFromOracleText("Deal 3 damage to any target.")
            keywords shouldBe emptySet()
        }
    }

    context("displayName") {
        test("keywords have correct display names") {
            Keyword.FLYING.displayName shouldBe "Flying"
            Keyword.FIRST_STRIKE.displayName shouldBe "First strike"
            Keyword.DOUBLE_STRIKE.displayName shouldBe "Double strike"
            Keyword.DEATHTOUCH.displayName shouldBe "Deathtouch"
            Keyword.CHANGELING.displayName shouldBe "Changeling"
        }
    }
})
