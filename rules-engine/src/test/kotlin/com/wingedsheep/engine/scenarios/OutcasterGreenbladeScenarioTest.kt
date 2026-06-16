package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Outcaster Greenblade (OTJ #172) — {2}{G} 1/2 Creature — Human Mercenary.
 *
 * "When this creature enters, search your library for a basic land card or a Desert card,
 *  reveal it, put it into your hand, then shuffle.
 *  This creature gets +1/+1 for each Desert you control."
 *
 * Verifies the basic-or-Desert library search ETB and the live Desert-counting self-buff.
 */
class OutcasterGreenbladeScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("ETB searches for a basic land or Desert and puts it into hand") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val forest = driver.putCardOnTopOfLibrary(player, "Forest")
        val desert = driver.putCardOnTopOfLibrary(player, "Eroded Canyon") // Land — Desert

        val greenblade = driver.putCardInHand(player, "Outcaster Greenblade")
        driver.giveMana(player, Color.GREEN, 3)
        driver.castSpell(player, greenblade).isSuccess shouldBe true
        // Resolve the creature spell, then its ETB trigger (which pauses for the search).
        var guard = 0
        while (driver.stackSize > 0 && !driver.isPaused && guard++ < 10) driver.bothPass()

        driver.isPaused shouldBe true
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        // Both the basic land and the Desert are eligible.
        decision.options.toSet() shouldBe setOf(forest, desert)
        driver.submitCardSelection(player, listOf(desert))
        driver.isPaused shouldBe false

        driver.getHand(player).contains(desert) shouldBe true
    }

    test("gets +1/+1 for each Desert you control") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val greenblade = driver.putCreatureOnBattlefield(player, "Outcaster Greenblade")

        // Base 1/2 with no Deserts.
        projector.getProjectedPower(driver.state, greenblade) shouldBe 1
        projector.getProjectedToughness(driver.state, greenblade) shouldBe 2

        driver.putPermanentOnBattlefield(player, "Eroded Canyon")
        projector.getProjectedPower(driver.state, greenblade) shouldBe 2
        projector.getProjectedToughness(driver.state, greenblade) shouldBe 3

        driver.putPermanentOnBattlefield(player, "Festering Gulch")
        projector.getProjectedPower(driver.state, greenblade) shouldBe 3
        projector.getProjectedToughness(driver.state, greenblade) shouldBe 4
    }
})
