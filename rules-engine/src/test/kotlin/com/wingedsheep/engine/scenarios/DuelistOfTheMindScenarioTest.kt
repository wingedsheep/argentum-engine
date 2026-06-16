package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.Divination
import com.wingedsheep.mtg.sets.definitions.otj.cards.DuelistOfTheMind
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Duelist of the Mind (OTJ #45).
 *
 * {1}{U} star/3 Human Advisor. Flying, vigilance.
 * - Power equals the number of cards you've drawn this turn (CDA via the new
 *   [com.wingedsheep.sdk.scripting.values.TurnTracker.CARDS_DRAWN] tracker); toughness is a
 *   printed 3.
 * - Whenever you commit a crime, you may draw a card. If you do, discard a card. Triggers only
 *   once each turn.
 */
class DuelistOfTheMindScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(DuelistOfTheMind, Divination))
        return driver
    }

    test("power equals cards drawn this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val duelist = driver.putCreatureOnBattlefield(me, "Duelist of the Mind")

        // No cards drawn yet this turn -> power 0, toughness 3.
        projector.getProjectedPower(driver.state, duelist) shouldBe 0
        projector.getProjectedToughness(driver.state, duelist) shouldBe 3

        // Draw two via Divination -> power becomes 2, toughness still 3.
        val divination = driver.putCardInHand(me, "Divination")
        driver.giveMana(me, Color.BLUE, 3)
        driver.castSpell(me, divination).isSuccess shouldBe true
        driver.bothPass() // resolve Divination -> draw 2

        driver.isPaused shouldBe false
        projector.getProjectedPower(driver.state, duelist) shouldBe 2
        projector.getProjectedToughness(driver.state, duelist) shouldBe 3
    }

    test("crime trigger loots once each turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40, "Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Duelist of the Mind")

        // First crime: Lightning Bolt the opponent.
        val bolt1 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt1, targets = listOf(opp))
        driver.bothPass() // resolve Bolt -> commit-crime -> loot trigger on stack
        driver.bothPass() // begin resolving loot trigger

        val handBefore = driver.getHandSize(me)

        // The optional loot: accept the "may draw" then discard the drawn card.
        (driver.pendingDecision is YesNoDecision) shouldBe true
        driver.submitYesNo(me, true)
        // Drawing then discarding nets zero cards in hand.
        driver.submitCardSelection(me, listOf(driver.getHand(me).last()))

        driver.isPaused shouldBe false
        driver.getHandSize(me) shouldBe handBefore

        // Second crime the same turn: the once-per-turn ability does NOT trigger again,
        // so no may-draw decision is raised.
        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt2, targets = listOf(opp))
        driver.bothPass()
        driver.bothPass()

        driver.isPaused shouldBe false
        (driver.pendingDecision is YesNoDecision) shouldBe false
    }
})
