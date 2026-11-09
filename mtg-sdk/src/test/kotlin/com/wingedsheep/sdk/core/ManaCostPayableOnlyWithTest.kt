package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** [ManaCost.payableOnlyWith] — "spend only mana of the chosen color" on an ability's cost. */
class ManaCostPayableOnlyWithTest : FunSpec({

    test("one color turns the generic part into that many pips of the color") {
        ManaCost.parse("{3}").payableOnlyWith(setOf(Color.BLUE)) shouldBe ManaCost.parse("{U}{U}{U}")
    }

    test("two colors turn it into hybrid pips either color pays") {
        ManaCost.parse("{2}").payableOnlyWith(setOf(Color.RED, Color.GREEN)) shouldBe ManaCost.parse("{R/G}{R/G}")
    }

    test("a pip of an allowed color stays; a hybrid narrows to its allowed half") {
        ManaCost.parse("{1}{B}").payableOnlyWith(setOf(Color.BLACK)) shouldBe ManaCost.parse("{B}{B}")
        ManaCost.parse("{U/R}").payableOnlyWith(setOf(Color.BLUE)) shouldBe ManaCost.parse("{U}")
        ManaCost.parse("{U/R}").payableOnlyWith(setOf(Color.BLUE, Color.RED)) shouldBe ManaCost.parse("{U/R}")
    }

    test("a pip no allowed mana can pay makes the cost unpayable") {
        ManaCost.parse("{1}{R}").payableOnlyWith(setOf(Color.BLUE)) shouldBe null
        ManaCost.parse("{1}{C}").payableOnlyWith(setOf(Color.BLUE)) shouldBe null
        ManaCost.parse("{X}").payableOnlyWith(setOf(Color.BLUE)) shouldBe null
        ManaCost.parse("{G/W}").payableOnlyWith(setOf(Color.BLUE)) shouldBe null
    }

    test("an empty or oversized color set makes a cost with mana unpayable, never payable by any mana") {
        ManaCost.parse("{2}").payableOnlyWith(emptySet()) shouldBe null
        ManaCost.parse("{2}").payableOnlyWith(setOf(Color.WHITE, Color.BLUE, Color.BLACK)) shouldBe null
    }

    test("a cost with no symbols is returned unchanged") {
        ManaCost.ZERO.payableOnlyWith(emptySet()) shouldBe ManaCost.ZERO
    }
})
