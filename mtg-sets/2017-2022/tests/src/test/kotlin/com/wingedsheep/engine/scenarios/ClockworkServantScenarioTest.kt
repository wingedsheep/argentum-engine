package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.ClockworkServant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Clockwork Servant — Adamant: "When this creature enters, if at least three mana of the same color
 * was spent to cast it, draw a card." The cost is all generic, so the colors spent on it decide.
 */
class ClockworkServantScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(ClockworkServant))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("three red mana spent on the generic cost: draws a card") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val spell = driver.putCardInHand(p, "Clockwork Servant")
        driver.giveMana(p, Color.RED, 3)
        val handAfterCast = driver.getHandSize(p) - 1

        driver.castSpell(p, spell).outcome shouldBe Outcome.Done
        driver.bothPass() // creature resolves, ETB trigger goes on the stack
        driver.bothPass() // trigger resolves

        driver.getHandSize(p) shouldBe handAfterCast + 1
    }

    test("two red and one green spent: no single color reaches three, no draw") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val spell = driver.putCardInHand(p, "Clockwork Servant")
        driver.giveMana(p, Color.RED, 2)
        driver.giveMana(p, Color.GREEN, 1)
        val handAfterCast = driver.getHandSize(p) - 1

        driver.castSpell(p, spell).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.bothPass()

        driver.getHandSize(p) shouldBe handAfterCast
    }
})
