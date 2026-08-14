package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.conditions.AnOpponentLifeAtMost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AnOpponentLifeAtMostConditionTest : FunSpec({
    fun createDriver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 20), skipMulligans = true)
    }

    fun GameTestDriver.evaluate(threshold: Int): Boolean = ConditionEvaluator().evaluate(
        state,
        AnOpponentLifeAtMost(threshold),
        EffectContext(sourceId = null, controllerId = activePlayer!!, targets = emptyList(), xValue = 0),
    )

    test("only an opponent at or below the threshold satisfies the condition") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        driver.setLifeTotal(controller, 10)
        driver.setLifeTotal(opponent, 20)
        driver.evaluate(10) shouldBe false

        driver.setLifeTotal(opponent, 10)
        driver.evaluate(10) shouldBe true
    }

    test("the threshold is inclusive") {
        val driver = createDriver()
        val opponent = driver.getOpponent(driver.activePlayer!!)

        driver.setLifeTotal(opponent, 11)
        driver.evaluate(10) shouldBe false
        driver.setLifeTotal(opponent, 10)
        driver.evaluate(10) shouldBe true
    }
})
