package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.composite.ChooseNumberThenExecutor
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.effects.ChooseNumberThenEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [ChooseNumberThenEffect] with `random = true` — Oracle's "choose 1, 2, or 3 at random"
 * (Tibalt's Trickery). The engine draws the number off the game's seeded RNG instead of prompting:
 * every value in the bounds is reachable and nothing outside them is, the draw never pauses for a
 * decision, it is reproducible from the seed, and it is not a die roll (no events of its own — the
 * only events are whatever the inner effect emits).
 */
class ChooseNumberAtRandomTest : FunSpec({

    val inner: Effect = GainLifeEffect(DynamicAmount.XValue)

    fun setup(): Pair<GameTestDriver, EffectContext> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        return driver to EffectContext(sourceId = null, controllerId = driver.activePlayer!!)
    }

    /** Runs the executor with an inner-effect spy, returning the X the inner effect saw (or null). */
    fun runOnce(driver: GameTestDriver, context: EffectContext, effect: ChooseNumberThenEffect, seed: Long): Pair<Int?, EffectResult> {
        var seen: Int? = null
        val executor = ChooseNumberThenExecutor(DecisionHandler(), driver.services.predicateEvaluator.amounts) { state, _, ctx ->
            seen = ctx.xValue
            EffectResult.success(state)
        }
        val result = executor.execute(driver.state.copy(rng = GameRng.seeded(seed)), effect, context)
        return seen to result
    }

    test("draws every number in the bounds and none outside them, without a decision") {
        val (driver, context) = setup()
        val effect = ChooseNumberThenEffect(inner, minValue = 1, maxValue = DynamicAmount.Fixed(3), random = true)
        val seen = (0L until 200L).map { seed ->
            val (x, result) = runOnce(driver, context, effect, seed)
            result.error shouldBe null
            result.state.pendingDecision shouldBe null
            result.events.shouldBeEmpty()
            x!!
        }
        seen.toSet() shouldBe setOf(1, 2, 3)
    }

    test("honours a non-zero minimum and a dynamic maximum") {
        val (driver, context) = setup()
        val effect = ChooseNumberThenEffect(
            inner, minValue = 2, maxValue = DynamicAmount.Add(DynamicAmount.Fixed(2), DynamicAmount.Fixed(2)), random = true
        )
        val seen = (0L until 200L).map { runOnce(driver, context, effect, it).first!! }.toSet()
        seen shouldBe setOf(2, 3, 4)
    }

    test("is reproducible from the seed and advances the RNG") {
        val (driver, context) = setup()
        val effect = ChooseNumberThenEffect(inner, minValue = 1, maxValue = DynamicAmount.Fixed(3), random = true)
        val (first, result) = runOnce(driver, context, effect, 42L)
        runOnce(driver, context, effect, 42L).first shouldBe first
        result.state.rng shouldNotBe GameRng.seeded(42L)
    }

    test("a single-value range always yields that value") {
        val (driver, context) = setup()
        val effect = ChooseNumberThenEffect(inner, minValue = 2, maxValue = DynamicAmount.Fixed(2), random = true)
        (0L until 20L).forEach { runOnce(driver, context, effect, it).first shouldBe 2 }
    }

    test("an empty range does nothing") {
        val (driver, context) = setup()
        val effect = ChooseNumberThenEffect(inner, minValue = 3, maxValue = DynamicAmount.Fixed(1), random = true)
        val (x, result) = runOnce(driver, context, effect, 7L)
        x shouldBe null
        result.error shouldBe null
    }

    test("the controller-chosen form still pauses for a number decision") {
        val (driver, context) = setup()
        val effect = ChooseNumberThenEffect(inner, minValue = 1, maxValue = DynamicAmount.Fixed(3))
        val (x, result) = runOnce(driver, context, effect, 7L)
        x shouldBe null
        result.state.pendingDecision shouldNotBe null
    }
})
