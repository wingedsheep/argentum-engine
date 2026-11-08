package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** [ManaCost.withGenericPaidBy] — "spend only mana of the chosen color" on an ability's cost. */
class ManaCostGenericPaidByTest : FunSpec({

    test("one color turns the generic part into that many pips of the color") {
        ManaCost.parse("{3}").withGenericPaidBy(setOf(Color.BLUE)) shouldBe ManaCost.parse("{U}{U}{U}")
    }

    test("two colors turn it into hybrid pips either color pays") {
        ManaCost.parse("{2}").withGenericPaidBy(setOf(Color.RED, Color.GREEN)) shouldBe ManaCost.parse("{R/G}{R/G}")
    }

    test("colored, colorless and X symbols are untouched; only generic is recolored") {
        ManaCost.parse("{2}{B}{C}").withGenericPaidBy(setOf(Color.BLACK)) shouldBe ManaCost.parse("{B}{C}{B}{B}")
    }

    test("a cost with no generic part is returned unchanged") {
        val cost = ManaCost.parse("{W}{W}")
        cost.withGenericPaidBy(setOf(Color.BLUE)) shouldBe cost
    }

    test("an empty or oversized color set fails closed to colorless pips, never to any mana") {
        ManaCost.parse("{2}").withGenericPaidBy(emptySet()) shouldBe ManaCost.parse("{C}{C}")
        ManaCost.parse("{2}").withGenericPaidBy(setOf(Color.WHITE, Color.BLUE, Color.BLACK)) shouldBe ManaCost.parse("{C}{C}")
    }
})
