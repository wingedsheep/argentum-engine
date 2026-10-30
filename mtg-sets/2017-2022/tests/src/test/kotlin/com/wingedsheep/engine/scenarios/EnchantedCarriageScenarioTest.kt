package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.EnchantedCarriage
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Enchanted Carriage (ELD #218) — "When this Vehicle enters, create two 1/1 white Mouse creature
 * tokens. Crew 2"
 */
class EnchantedCarriageScenarioTest : FunSpec({

    test("entering creates two 1/1 white Mouse tokens") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EnchantedCarriage))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val carriage = driver.putCardInHand(me, "Enchanted Carriage")
        driver.giveColorlessMana(me, 5)
        driver.castSpell(me, carriage).error shouldBe null
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        val mice = driver.getCreatures(me).filter { driver.getCardName(it) == "Mouse Token" }
        mice shouldHaveSize 2
        mice.forEach {
            driver.state.projectedState.getPower(it) shouldBe 1
            driver.state.projectedState.getToughness(it) shouldBe 1
        }
        // Uncrewed, the Vehicle itself isn't a creature.
        driver.state.projectedState.isCreature(driver.findPermanent(me, "Enchanted Carriage")!!) shouldBe false
    }
})
