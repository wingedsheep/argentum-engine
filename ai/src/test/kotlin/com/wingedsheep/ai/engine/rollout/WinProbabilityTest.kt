package com.wingedsheep.ai.engine.rollout

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * The scale conversion the whole rollout evaluator rests on.
 *
 * The properties that matter are not "squash returns a number" but the two that make averaging
 * possible at all: a proven win must stay **finite** so it can be averaged, and it must stay far
 * enough above any real board score that averaging never hides it.
 */
class WinProbabilityTest : FunSpec({

    test("an even position is a coin flip") {
        WinProbability.squash(0.0) shouldBe (0.5 plusOrMinus 1e-12)
    }

    test("squash is monotone and symmetric about zero") {
        val ahead = WinProbability.squash(6.0)
        val behind = WinProbability.squash(-6.0)
        ahead shouldBeGreaterThan 0.5
        behind shouldBeLessThan 0.5
        (ahead + behind) shouldBe (1.0 plusOrMinus 1e-12)
        WinProbability.squash(12.0) shouldBeGreaterThan ahead
    }

    test("logit round-trips squash over the ordinary board-score range") {
        listOf(-20.0, -6.0, -1.0, 0.0, 1.0, 6.0, 20.0).forEach { score ->
            WinProbability.logit(WinProbability.squash(score)) shouldBe (score plusOrMinus 1e-9)
        }
    }

    test("a fitted scale round-trips independently of the legacy default") {
        val fittedScale = 1.75
        val score = 3.25
        WinProbability.logit(
            WinProbability.squash(score, fittedScale), fittedScale
        ) shouldBe (score plusOrMinus 1e-9)
    }

    test("the static evaluator's terminal sentinel squashes without overflowing") {
        // `CompositeBoardEvaluator` returns ±Double.MAX_VALUE / 2 for a decided game. Averaging
        // that with anything is meaningless, which is the reason this file exists.
        val won = WinProbability.squash(Double.MAX_VALUE / 2)
        val lost = WinProbability.squash(-Double.MAX_VALUE / 2)
        won shouldBeGreaterThan 0.999
        lost shouldBeLessThan 0.001
        won.isFinite() shouldBe true
    }

    test("a certain win converts back to a large but finite score") {
        val win = WinProbability.logit(WinProbability.WIN)
        win.isFinite() shouldBe true
        // Far above anything the board features can produce — a full board plus a 20-life lead is
        // roughly 30 raw points — so a proven win still dominates every comparison.
        win shouldBeGreaterThan 50.0
        WinProbability.logit(WinProbability.LOSS) shouldBe (-win plusOrMinus 1e-9)
        WinProbability.logit(WinProbability.DRAW) shouldBe (0.0 plusOrMinus 1e-12)
    }

    test("one win in four beats four even boards, which is the whole point") {
        // The averaging the rollout evaluator does, on the two rows that motivated probability
        // space: in raw space the first would be MAX_VALUE/8 and unusable.
        val oneWinInFour = (WinProbability.WIN + WinProbability.DRAW * 3) / 4
        val allEven = WinProbability.DRAW
        oneWinInFour shouldBe (0.625 plusOrMinus 1e-12)
        WinProbability.logit(oneWinInFour) shouldBeGreaterThan WinProbability.logit(allEven)
    }
})
