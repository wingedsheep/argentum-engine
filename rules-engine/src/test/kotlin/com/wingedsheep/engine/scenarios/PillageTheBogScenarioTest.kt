package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pillage the Bog (OTJ #224) — {B}{G} Sorcery.
 *
 * "Look at the top X cards of your library, where X is twice the number of lands you control.
 *  Put one of them into your hand and the rest on the bottom of your library in a random order."
 *
 * Verifies X = 2 × lands you control, keeping exactly one card in hand, and the rest going to
 * the bottom of the library (not the graveyard).
 */
class PillageTheBogScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("looks at twice the lands you control, keeps one, rest to bottom") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Three lands controlled -> X = 6.
        driver.putLandOnBattlefield(player, "Swamp")
        driver.putLandOnBattlefield(player, "Forest")
        driver.putLandOnBattlefield(player, "Swamp")

        // Seed the top of the library with six known cards (last pushed = topmost).
        val c1 = driver.putCardOnTopOfLibrary(player, "Mountain")
        val c2 = driver.putCardOnTopOfLibrary(player, "Plains")
        val c3 = driver.putCardOnTopOfLibrary(player, "Island")
        val c4 = driver.putCardOnTopOfLibrary(player, "Forest")
        val c5 = driver.putCardOnTopOfLibrary(player, "Swamp")
        val c6 = driver.putCardOnTopOfLibrary(player, "Mountain")

        val spell = driver.putCardInHand(player, "Pillage the Bog")
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveMana(player, Color.GREEN, 1)

        val handBefore = driver.getHandSize(player)

        driver.castSpell(player, spell).isSuccess shouldBe true
        driver.bothPass()

        driver.isPaused shouldBe true
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options.size shouldBe 6
        decision.minSelections shouldBe 1
        decision.maxSelections shouldBe 1

        driver.submitCardSelection(player, listOf(c6))
        driver.isPaused shouldBe false

        // Kept card is in hand (hand: before - 1 spell + 1 kept).
        val hand = driver.state.getZone(ZoneKey(player, Zone.HAND))
        hand.contains(c6) shouldBe true
        driver.getHandSize(player) shouldBe handBefore - 1 + 1

        // The other five looked-at cards are in the library, not the graveyard.
        val library = driver.state.getZone(ZoneKey(player, Zone.LIBRARY))
        listOf(c1, c2, c3, c4, c5).forEach { library.contains(it) shouldBe true }
        val graveyard = driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD))
        listOf(c1, c2, c3, c4, c5).forEach { graveyard.contains(it) shouldBe false }
    }
})
