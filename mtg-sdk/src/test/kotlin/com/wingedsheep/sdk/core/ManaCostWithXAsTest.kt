package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * [ManaCost.withXAs] — substituting an announced X (CR 107.3a) into a cost, so downstream payment
 * code never has to special-case an `{X}` symbol. Driven by X-cost cycling (Webstrike Elite's
 * "Cycling {X}{G}{G}"), where the handler resolves the cost once and then pays it like any other.
 */
class ManaCostWithXAsTest : StringSpec({

    fun sub(cost: String, x: Int) = ManaCost.parse(cost).withXAs(x)

    "{X}{G}{G} with X=2 becomes {2}{G}{G}" {
        sub("{X}{G}{G}", 2) shouldBe ManaCost.parse("{2}{G}{G}")
    }
    "{X}{2}{W} with X=3 folds into the printed generic as {5}{W}" {
        sub("{X}{2}{W}", 3) shouldBe ManaCost.parse("{5}{W}")
    }
    "X=0 drops the symbol entirely rather than leaving {0}" {
        sub("{X}{G}{G}", 0) shouldBe ManaCost.parse("{G}{G}")
    }
    "a bare {X} with X=0 is the empty cost" {
        sub("{X}", 0) shouldBe ManaCost.ZERO
    }
    "each X symbol is charged separately — {X}{X}{R} with X=3 is {6}{R}" {
        sub("{X}{X}{R}", 3) shouldBe ManaCost.parse("{6}{R}")
    }
    "a cost with no X is returned unchanged" {
        sub("{2}{G}", 4) shouldBe ManaCost.parse("{2}{G}")
    }
    "a negative X is clamped to 0 rather than underflowing the generic" {
        sub("{X}{G}{G}", -1) shouldBe ManaCost.parse("{G}{G}")
    }
    "the substituted cost carries no X left for downstream payment" {
        sub("{X}{2}{W}", 3).hasX shouldBe false
    }
    "mana value reflects the announced X" {
        sub("{X}{G}{G}", 4).cmc shouldBe 6
    }
})
