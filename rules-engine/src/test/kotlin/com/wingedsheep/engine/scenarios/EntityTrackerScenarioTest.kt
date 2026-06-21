package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Entity Tracker (DSK #53) — {2}{U} Creature — Human Scout 2/3.
 *
 * "Flash
 *  Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room,
 *  draw a card."
 *
 * Exercises the enchantment-enters Eerie branch drawing a card.
 */
class EntityTrackerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("an enchantment entering under your control draws a card via Eerie") {
        val driver = newDriver()
        val player = driver.player1

        driver.putCreatureOnBattlefield(player, "Entity Tracker")

        // Cast a Test Enchantment — its ETB fires the Eerie trigger (draw a card).
        val enchantment = driver.putCardInHand(player, "Test Enchantment")
        driver.giveMana(player, Color.WHITE, 1)
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, enchantment).isSuccess shouldBe true
        // Hand size right after the enchantment is on the stack (no longer in hand).
        val handAfterCast = driver.getHandSize(player)
        driver.bothPass() // resolve the enchantment — ETB queues the Eerie trigger
        driver.bothPass() // resolve the Eerie draw

        driver.isPaused shouldBe false
        // The Eerie trigger drew exactly one card on top of the post-cast hand.
        driver.getHandSize(player) shouldBe handAfterCast + 1
    }
})
