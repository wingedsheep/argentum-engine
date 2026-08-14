package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.BalambTRexaur
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Balamb T-Rexaur. */
class BalambTRexaurScenarioTest : FunSpec({

    fun createDriver(vararg cards: com.wingedsheep.sdk.model.CardDefinition): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        cards.forEach { driver.registerCard(it) }
        return driver
    }

    // -----------------------------------------------------------------------------------------
    // Balamb T-Rexaur
    // -----------------------------------------------------------------------------------------

    test("Balamb T-Rexaur gains 3 life when it enters") {
        val driver = createDriver(BalambTRexaur)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val lifeBefore = driver.getLifeTotal(active)

        val trex = driver.putCardInHand(active, "Balamb T-Rexaur")
        driver.giveMana(active, Color.GREEN, 6)
        driver.castSpell(active, trex).error shouldBe null

        // Resolve the spell, then the ETB life-gain trigger.
        var safety = 0
        while (driver.stackSize > 0 && safety < 20) {
            driver.bothPass(); safety++
        }

        driver.getLifeTotal(active) shouldBe (lifeBefore + 3)
        driver.state.getBattlefield().contains(trex) shouldBe true
    }
})
