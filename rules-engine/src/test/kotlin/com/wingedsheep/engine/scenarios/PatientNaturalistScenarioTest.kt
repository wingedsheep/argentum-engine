package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Patient Naturalist (OTJ #174) — {2}{G} 2/3 Creature.
 *
 * "When this creature enters, mill three cards. Put a land card from among the milled cards
 *  into your hand. If you can't, create a Treasure token."
 *
 * Verifies: top three cards are milled to the graveyard; a land among them is chosen and put
 * into hand; when no land was milled, a Treasure token is created instead.
 */
class PatientNaturalistScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.Treasure)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("mills three, puts a land into hand from among them") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Top three (topmost is the last put): two nonland, one Forest.
        val bears1 = driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
        val forest = driver.putCardOnTopOfLibrary(player, "Forest")
        val bears2 = driver.putCardOnTopOfLibrary(player, "Grizzly Bears")

        val naturalist = driver.putCardInHand(player, "Patient Naturalist")
        driver.giveMana(player, Color.GREEN, 3)
        driver.castSpell(player, naturalist).isSuccess shouldBe true
        var guard = 0
        while (driver.stackSize > 0 && !driver.isPaused && guard++ < 10) driver.bothPass()

        // Exactly one land was milled → it's auto-chosen (single option) and put into hand.
        if (driver.isPaused) {
            val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options shouldBe listOf(forest)
            driver.submitCardSelection(player, listOf(forest))
        }

        val hand = driver.getHand(player)
        hand.contains(forest) shouldBe true
        val graveyard = driver.getGraveyard(player)
        graveyard.contains(bears1) shouldBe true
        graveyard.contains(bears2) shouldBe true
        // The land went to hand, not the graveyard.
        graveyard.contains(forest) shouldBe false
    }

    test("creates a Treasure token when no land was milled") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Top three are all nonland creatures — no land to put into hand.
        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")

        val naturalist = driver.putCardInHand(player, "Patient Naturalist")
        driver.giveMana(player, Color.GREEN, 3)
        driver.castSpell(player, naturalist).isSuccess shouldBe true
        var guard = 0
        while (driver.stackSize > 0 && !driver.isPaused && guard++ < 10) driver.bothPass()

        driver.isPaused shouldBe false

        val battlefield = driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD))
        val treasures = battlefield.count { id ->
            driver.getCardName(id)?.equals("Treasure", ignoreCase = true) == true
        }
        treasures shouldBe 1

        // All three milled cards are in the graveyard.
        driver.getGraveyard(player).size shouldBe 3
    }
})
