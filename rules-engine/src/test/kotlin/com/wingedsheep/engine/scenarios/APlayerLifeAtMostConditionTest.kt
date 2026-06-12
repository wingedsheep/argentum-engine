package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.conditions.APlayerLifeAtMost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the existential life-threshold condition used by Razortrap Gorge
 * ("This land enters tapped unless a player has 13 or less life").
 *
 * The condition is true whenever ANY player in `state.turnOrder` has life ≤ N —
 * distinct from `LifeAtMost` (which is `Player.You` only).
 */
class APlayerLifeAtMostConditionTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.evalAtMost(threshold: Int): Boolean {
        val controller = activePlayer!!
        val context = EffectContext(
            sourceId = null,
            controllerId = controller,
            targets = emptyList(),
            xValue = 0
        )
        return ConditionEvaluator().evaluate(state, APlayerLifeAtMost(threshold), context)
    }

    test("false when both players are at full life") {
        val driver = createDriver()
        // Default starting life is 20 in standard.
        driver.evalAtMost(13) shouldBe false
    }

    test("true when you are at or below the threshold") {
        val driver = createDriver()
        driver.setLifeTotal(driver.activePlayer!!, 13)
        driver.evalAtMost(13) shouldBe true
    }

    test("true when an opponent is at or below the threshold") {
        val driver = createDriver()
        val opponent = driver.getOpponent(driver.activePlayer!!)
        driver.setLifeTotal(opponent, 5)
        driver.evalAtMost(13) shouldBe true
    }

    test("boundary — true at exactly the threshold, false at threshold + 1") {
        val driver = createDriver()
        val opponent = driver.getOpponent(driver.activePlayer!!)
        driver.setLifeTotal(driver.activePlayer!!, 14)
        driver.setLifeTotal(opponent, 14)
        driver.evalAtMost(13) shouldBe false
        driver.setLifeTotal(opponent, 13)
        driver.evalAtMost(13) shouldBe true
    }
})
