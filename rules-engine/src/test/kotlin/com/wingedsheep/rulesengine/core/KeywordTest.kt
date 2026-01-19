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
            Keyword.fromString("Prowess") shouldBe Keyword.PROWESS
        }

        test("parses multi-word keywords") {
            Keyword.fromString("First strike") shouldBe Keyword.FIRST_STRIKE
            Keyword.fromString("Double strike") shouldBe Keyword.DOUBLE_STRIKE
        }

        test("parsing is case insensitive") {
            Keyword.fromString("flying") shouldBe Keyword.FLYING
            Keyword.fromString("prowess") shouldBe Keyword.PROWESS
        }

        test("returns null for unknown keyword") {
            Keyword.fromString("NotAKeyword").shouldBeNull()
            Keyword.fromString("").shouldBeNull()
        }
    }

    context("parseFromOracleText") {
        test("parses mixed keywords") {
            val keywords = Keyword.parseFromOracleText("Flying, Prowess")
            keywords shouldContainExactlyInAnyOrder listOf(Keyword.FLYING, Keyword.PROWESS)
        }
    }

    context("displayName") {
        test("keywords have correct display names") {
            Keyword.FLYING.displayName shouldBe "Flying"
            Keyword.PROWESS.displayName shouldBe "Prowess"
        }
    }
})
