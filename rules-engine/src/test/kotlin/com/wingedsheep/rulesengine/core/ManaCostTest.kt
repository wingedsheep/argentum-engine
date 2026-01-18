package com.wingedsheep.rulesengine.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.assertions.throwables.shouldThrow

class ManaCostTest : FunSpec({

    context("parsing mana costs") {
        test("parse empty string returns ZERO") {
            ManaCost.parse("") shouldBe ManaCost.ZERO
            ManaCost.parse("  ") shouldBe ManaCost.ZERO
        }

        test("parse single colored mana") {
            val cost = ManaCost.parse("{W}")
            cost.cmc shouldBe 1
            cost.colors shouldBe setOf(Color.WHITE)
        }

        test("parse multiple colored mana") {
            val cost = ManaCost.parse("{W}{U}{B}")
            cost.cmc shouldBe 3
            cost.colors shouldContainExactlyInAnyOrder listOf(Color.WHITE, Color.BLUE, Color.BLACK)
        }

        test("parse generic mana") {
            val cost = ManaCost.parse("{3}")
            cost.cmc shouldBe 3
            cost.genericAmount shouldBe 3
            cost.colors shouldBe emptySet()
        }

        test("parse mixed mana cost") {
            val cost = ManaCost.parse("{2}{W}{W}")
            cost.cmc shouldBe 4
            cost.genericAmount shouldBe 2
            cost.colors shouldBe setOf(Color.WHITE)
            cost.colorCount shouldBe mapOf(Color.WHITE to 2)
        }

        test("parse colorless mana") {
            val cost = ManaCost.parse("{C}{C}")
            cost.cmc shouldBe 2
            cost.colors shouldBe emptySet()
        }

        test("parse X mana") {
            val cost = ManaCost.parse("{X}{R}{R}")
            cost.cmc shouldBe 2
            cost.hasX.shouldBeTrue()
            cost.colors shouldBe setOf(Color.RED)
        }

        test("parse complex mana cost") {
            val cost = ManaCost.parse("{4}{W}{W}")
            cost.cmc shouldBe 6
            cost.genericAmount shouldBe 4
            cost.colorCount shouldBe mapOf(Color.WHITE to 2)
        }

        test("parse throws for unknown symbol") {
            shouldThrow<IllegalArgumentException> {
                ManaCost.parse("{Z}")
            }
        }
    }

    context("ManaCost.of factory") {
        test("create from vararg symbols") {
            val cost = ManaCost.of(ManaSymbol.generic(2), ManaSymbol.W, ManaSymbol.W)
            cost.cmc shouldBe 4
            cost.colors shouldBe setOf(Color.WHITE)
        }
    }

    context("toString") {
        test("formats mana cost correctly") {
            val cost = ManaCost.of(ManaSymbol.generic(2), ManaSymbol.W, ManaSymbol.U)
            cost.toString() shouldBe "{2}{W}{U}"
        }

        test("ZERO formats as empty string") {
            ManaCost.ZERO.toString() shouldBe ""
        }
    }

    context("isEmpty") {
        test("ZERO is empty") {
            ManaCost.ZERO.isEmpty().shouldBeTrue()
        }

        test("non-zero cost is not empty") {
            ManaCost.parse("{W}").isEmpty().shouldBeFalse()
        }
    }

    context("colorCount") {
        test("counts each color correctly") {
            val cost = ManaCost.parse("{W}{W}{U}{B}{B}{B}")
            cost.colorCount shouldBe mapOf(
                Color.WHITE to 2,
                Color.BLUE to 1,
                Color.BLACK to 3
            )
        }
    }
})
