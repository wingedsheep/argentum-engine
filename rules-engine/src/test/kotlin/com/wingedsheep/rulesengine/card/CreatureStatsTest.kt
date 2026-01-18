package com.wingedsheep.rulesengine.card

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

class CreatureStatsTest : FunSpec({

    context("creation") {
        test("creates stats with valid power and toughness") {
            val stats = CreatureStats(3, 4)
            stats.basePower shouldBe 3
            stats.baseToughness shouldBe 4
        }

        test("allows zero power") {
            val stats = CreatureStats(0, 1)
            stats.basePower shouldBe 0
        }

        test("allows zero toughness") {
            val stats = CreatureStats(1, 0)
            stats.baseToughness shouldBe 0
        }

        test("throws for negative power") {
            shouldThrow<IllegalArgumentException> {
                CreatureStats(-1, 1)
            }
        }

        test("throws for negative toughness") {
            shouldThrow<IllegalArgumentException> {
                CreatureStats(1, -1)
            }
        }
    }

    context("parse") {
        test("parses valid power/toughness strings") {
            val stats = CreatureStats.parse("3", "4")
            stats.basePower shouldBe 3
            stats.baseToughness shouldBe 4
        }

        test("throws for non-numeric power") {
            shouldThrow<IllegalArgumentException> {
                CreatureStats.parse("*", "4")
            }
        }

        test("throws for non-numeric toughness") {
            shouldThrow<IllegalArgumentException> {
                CreatureStats.parse("3", "*")
            }
        }
    }

    context("of factory") {
        test("creates stats using of") {
            val stats = CreatureStats.of(2, 2)
            stats.basePower shouldBe 2
            stats.baseToughness shouldBe 2
        }
    }

    context("toString") {
        test("formats as power/toughness") {
            CreatureStats(3, 4).toString() shouldBe "3/4"
            CreatureStats(0, 1).toString() shouldBe "0/1"
            CreatureStats(10, 10).toString() shouldBe "10/10"
        }
    }
})
