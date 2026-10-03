package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.WailingGhoul
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wailing Ghoul (EMN #112) — {1}{B} Creature — Zombie 1/3
 * "When this creature enters, mill two cards."
 */
class WailingGhoulScenarioTest : FunSpec({
    test("ETB mills the controller's top two cards") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WailingGhoul)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val yourGyBefore = driver.getGraveyardCardNames(you).size
        val oppGyBefore = driver.getGraveyardCardNames(opponent).size
        val card = driver.putCardInHand(you, "Wailing Ghoul")
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, card)
        driver.bothPass() // resolve creature
        driver.bothPass() // resolve ETB mill

        driver.getGraveyardCardNames(you).size shouldBe yourGyBefore + 2
        driver.getGraveyardCardNames(opponent).size shouldBe oppGyBefore
        driver.findPermanent(you, "Wailing Ghoul") shouldNotBe null
    }
})
