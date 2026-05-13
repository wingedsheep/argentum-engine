package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class KeywordTest : DescribeSpec({

    describe("Keyword.fromString") {
        it("recognises the Fateful Bite ability word case-insensitively") {
            Keyword.fromString("Fateful Bite") shouldBe Keyword.FATEFUL_BITE
            Keyword.fromString("fateful bite") shouldBe Keyword.FATEFUL_BITE
        }

        it("returns null for unknown text") {
            Keyword.fromString("Made-up Keyword") shouldBe null
        }
    }

    describe("Keyword.parseFromOracleText — ability-word prefix on an em-dash line") {
        it("surfaces the new Fateful Bite ability word") {
            val parsed = Keyword.parseFromOracleText(
                "Fateful Bite — {2}, Sacrifice this creature: Search your library for a Spider Hero card.",
            )
            parsed shouldContain Keyword.FATEFUL_BITE
        }

        it("surfaces the pre-existing Eerie ability word (regression guard for the em-dash branch)") {
            val parsed = Keyword.parseFromOracleText(
                "Eerie — Whenever an enchantment you control enters, scry 1.",
            )
            parsed shouldContain Keyword.EERIE
        }

        it("does not invent a keyword when the em-dash prefix is not a known ability word") {
            val parsed = Keyword.parseFromOracleText(
                "Bargain — You may sacrifice an artifact, creature, or land as you cast this spell.",
            )
            parsed shouldNotContain Keyword.FATEFUL_BITE
            parsed shouldNotContain Keyword.EERIE
        }

        it("still parses comma-separated keyword lines alongside an ability-word prefix") {
            val parsed = Keyword.parseFromOracleText(
                """
                Reach, deathtouch
                Fateful Bite — {2}, Sacrifice this creature: Search your library for a Spider Hero card.
                """.trimIndent(),
            )
            parsed shouldContain Keyword.REACH
            parsed shouldContain Keyword.DEATHTOUCH
            parsed shouldContain Keyword.FATEFUL_BITE
        }
    }
})
