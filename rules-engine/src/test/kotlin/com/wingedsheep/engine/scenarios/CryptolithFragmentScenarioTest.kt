package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.conditions.EachPlayerLifeAtMost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Covers Cryptolith Fragment's "if each player has 10 or less life" condition. */
class CryptolithFragmentScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 20), skipMulligans = true)
    }

    fun GameTestDriver.conditionIsMet(): Boolean = ConditionEvaluator().evaluate(
        state,
        EachPlayerLifeAtMost(10),
        EffectContext(
            sourceId = null,
            controllerId = activePlayer!!,
            targets = emptyList(),
            xValue = 0,
        ),
    )

    test("condition is true when every player has 10 or less life") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.setLifeTotal(active, 10)
        driver.setLifeTotal(driver.getOpponent(active), 7)

        driver.conditionIsMet() shouldBe true
    }

    test("condition is false when any player has more than 10 life") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.setLifeTotal(active, 10)
        driver.setLifeTotal(driver.getOpponent(active), 11)

        driver.conditionIsMet() shouldBe false
    }
})
