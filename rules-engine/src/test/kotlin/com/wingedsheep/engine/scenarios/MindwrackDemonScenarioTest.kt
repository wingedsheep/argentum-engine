package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ddq.cards.MindwrackDemon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MindwrackDemonScenarioTest : FunSpec({
    test("ETB mills four; upkeep loses 4 without delirium") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MindwrackDemon)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val beforeGy = driver.getGraveyardCardNames(you).size
        val card = driver.putCardInHand(you, "Mindwrack Demon")
        driver.giveMana(you, Color.BLACK, 2)
        driver.giveColorlessMana(you, 2)
        driver.castSpell(you, card)
        driver.bothPass() // resolve creature
        driver.bothPass() // resolve ETB mill

        driver.getGraveyardCardNames(you).size shouldBe beforeGy + 4
        driver.findPermanent(you, "Mindwrack Demon") shouldNotBe null

        // Advance to your next upkeep (pass through opponent's turn first)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP) // opponent upkeep
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP) // your upkeep
        driver.state.activePlayerId shouldBe you
        driver.state.step shouldBe Step.UPKEEP

        driver.bothPass() // resolve delirium trigger
        driver.getLifeTotal(you) shouldBe 16
    }

    test("with delirium satisfied, the upkeep trigger costs no life") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MindwrackDemon)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val demon = driver.putPermanentOnBattlefield(you, "Mindwrack Demon")
        demon shouldNotBe null

        // Four distinct card types in the graveyard: creature, instant, sorcery, land.
        driver.putCardInGraveyard(you, "Grizzly Bears")
        driver.putCardInGraveyard(you, "Lightning Bolt")
        driver.putCardInGraveyard(you, "Demonic Counsel")
        driver.putCardInGraveyard(you, "Forest")

        val lifeBefore = driver.getLifeTotal(you)

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP) // opponent upkeep
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP) // your upkeep
        driver.state.activePlayerId shouldBe you

        driver.bothPass() // resolve (or fizzle) the delirium trigger

        // "unless there are four or more card types" — delirium is met, so no life is lost.
        driver.getLifeTotal(you) shouldBe lifeBefore
    }
})
