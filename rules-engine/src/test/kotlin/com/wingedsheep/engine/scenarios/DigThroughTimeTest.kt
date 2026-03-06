package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khans.cards.DigThroughTime
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DigThroughTimeTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DigThroughTime)
        return driver
    }

    test("Dig Through Time: look at 7, keep 2 in hand, rest on bottom of library") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put specific cards on top of library
        val card1 = driver.putCardOnTopOfLibrary(activePlayer, "Island")
        val card2 = driver.putCardOnTopOfLibrary(activePlayer, "Forest")
        val card3 = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        val card4 = driver.putCardOnTopOfLibrary(activePlayer, "Plains")
        val card5 = driver.putCardOnTopOfLibrary(activePlayer, "Swamp")
        val card6 = driver.putCardOnTopOfLibrary(activePlayer, "Island")
        val card7 = driver.putCardOnTopOfLibrary(activePlayer, "Forest")

        val spellCard = driver.putCardInHand(activePlayer, "Dig Through Time")
        driver.giveMana(activePlayer, Color.BLUE, 8)

        val initialHandSize = driver.getHandSize(activePlayer)

        val castResult = driver.castSpell(activePlayer, spellCard)
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // SelectFromCollection pauses for a decision
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()

        val selectDecision = decision as SelectCardsDecision
        selectDecision.options.size shouldBe 7
        selectDecision.minSelections shouldBe 2
        selectDecision.maxSelections shouldBe 2

        // Player selects card7 and card5 to keep
        val selectedCards = listOf(card7, card5)
        driver.submitCardSelection(activePlayer, selectedCards)

        driver.isPaused shouldBe false

        // Selected cards go to hand
        val handZone = driver.state.getZone(ZoneKey(activePlayer, Zone.HAND))
        handZone.contains(card7) shouldBe true
        handZone.contains(card5) shouldBe true

        // Hand: initial - 1 (spell cast) + 2 (kept cards)
        driver.getHandSize(activePlayer) shouldBe initialHandSize - 1 + 2

        // Rest go to bottom of library (not graveyard!)
        val libraryZone = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY))
        libraryZone.contains(card6) shouldBe true
        libraryZone.contains(card4) shouldBe true
        libraryZone.contains(card3) shouldBe true
        libraryZone.contains(card2) shouldBe true
        libraryZone.contains(card1) shouldBe true

        // Graveyard should only have the spell itself (not the non-selected cards)
        val graveyardZone = driver.state.getZone(ZoneKey(activePlayer, Zone.GRAVEYARD))
        graveyardZone.contains(card6) shouldBe false
        graveyardZone.contains(card4) shouldBe false
    }
})
