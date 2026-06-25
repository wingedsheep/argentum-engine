package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.conditions.NumberMatches
import com.wingedsheep.sdk.scripting.conditions.NumberProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Unit coverage for the unary numeric-predicate condition `NumberMatches(amount, property)`.
 * Drives the evaluator over a `DynamicAmount.Fixed(n)` so each [NumberProperty] arithmetic branch
 * (prime / even / odd / multiple-of) is pinned independently of any card. The prime edge cases
 * (0 and 1 are not prime; 2 is the smallest prime) back the Zimone ruling.
 */
class NumberMatchesConditionTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Forest" to 20), skipMulligans = true)
        return d
    }

    fun GameTestDriver.eval(n: Int, property: NumberProperty): Boolean {
        val context = EffectContext(sourceId = null, controllerId = activePlayer!!, xValue = 0)
        return ConditionEvaluator().evaluate(state, NumberMatches(DynamicAmount.Fixed(n), property), context)
    }

    test("Prime: 0 and 1 are not prime; 2, 3, 5, 7, 11 are; 4, 6, 9 are not") {
        val d = driver()
        d.eval(0, NumberProperty.Prime) shouldBe false
        d.eval(1, NumberProperty.Prime) shouldBe false
        d.eval(2, NumberProperty.Prime) shouldBe true
        d.eval(3, NumberProperty.Prime) shouldBe true
        d.eval(4, NumberProperty.Prime) shouldBe false
        d.eval(5, NumberProperty.Prime) shouldBe true
        d.eval(6, NumberProperty.Prime) shouldBe false
        d.eval(7, NumberProperty.Prime) shouldBe true
        d.eval(9, NumberProperty.Prime) shouldBe false
        d.eval(11, NumberProperty.Prime) shouldBe true
        d.eval(25, NumberProperty.Prime) shouldBe false // 5*5 — past the early-prime shortcut
        d.eval(31, NumberProperty.Prime) shouldBe true
    }

    test("Even: 0 is even; parity alternates") {
        val d = driver()
        d.eval(0, NumberProperty.Even) shouldBe true
        d.eval(1, NumberProperty.Even) shouldBe false
        d.eval(2, NumberProperty.Even) shouldBe true
        d.eval(7, NumberProperty.Even) shouldBe false
    }

    test("Odd: complement of Even") {
        val d = driver()
        d.eval(0, NumberProperty.Odd) shouldBe false
        d.eval(1, NumberProperty.Odd) shouldBe true
        d.eval(2, NumberProperty.Odd) shouldBe false
        d.eval(7, NumberProperty.Odd) shouldBe true
    }

    test("MultipleOf: divisibility, with 0 a multiple of everything") {
        val d = driver()
        d.eval(0, NumberProperty.MultipleOf(3)) shouldBe true
        d.eval(3, NumberProperty.MultipleOf(3)) shouldBe true
        d.eval(6, NumberProperty.MultipleOf(3)) shouldBe true
        d.eval(4, NumberProperty.MultipleOf(3)) shouldBe false
    }
})
