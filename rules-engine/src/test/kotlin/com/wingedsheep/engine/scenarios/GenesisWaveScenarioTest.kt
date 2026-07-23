package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.som.cards.GenesisWave
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Genesis Wave {X}{G}{G}{G} — Sorcery
 *   Reveal the top X cards of your library. You may put any number of permanent cards with mana
 *   value X or less from among them onto the battlefield. Then put all cards revealed this way
 *   that weren't put onto the battlefield into your graveyard.
 *
 * Proves both X readings (how many cards are revealed, and the mana-value cap on what may be put
 * onto the battlefield), that non-permanent and too-expensive cards are not offered, and that
 * everything left over — including a permanent card the player declined — hits the graveyard.
 */
class GenesisWaveScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GenesisWave))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("X=3 reveals three, offers only permanent cards with mana value 3 or less, rest are milled") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // Top of library (topmost last): Savannah Lions {W} (permanent, MV 1 — eligible),
        // Force of Nature {3}{G}{G} (permanent, MV 5 — too expensive), Lightning Bolt {R}
        // (MV 1 but not a permanent card).
        val lions = driver.putCardOnTopOfLibrary(me, "Savannah Lions")
        val forceOfNature = driver.putCardOnTopOfLibrary(me, "Force of Nature")
        val bolt = driver.putCardOnTopOfLibrary(me, "Lightning Bolt")

        val wave = driver.putCardInHand(me, "Genesis Wave")
        driver.giveMana(me, Color.GREEN, 6)
        driver.castXSpell(me, wave, xValue = 3).error shouldBe null
        driver.bothPass()

        val decision = driver.state.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        decision as SelectCardsDecision
        // Only the MV-1 permanent card is a legal pick.
        decision.options.toSet() shouldBe setOf(lions)

        driver.submitCardSelection(me, listOf(lions))
        while (driver.stackSize > 0) driver.bothPass()

        driver.getPermanents(me).contains(lions) shouldBe true
        val graveyard = driver.getGraveyard(me)
        graveyard.contains(forceOfNature) shouldBe true
        graveyard.contains(bolt) shouldBe true
        graveyard.contains(lions) shouldBe false
    }

    test("declining an eligible permanent card sends it to the graveyard too") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val lions = driver.putCardOnTopOfLibrary(me, "Savannah Lions")
        val bolt = driver.putCardOnTopOfLibrary(me, "Lightning Bolt")

        val wave = driver.putCardInHand(me, "Genesis Wave")
        driver.giveMana(me, Color.GREEN, 5)
        driver.castXSpell(me, wave, xValue = 2).error shouldBe null
        driver.bothPass()

        val decision = driver.state.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        driver.submitCardSelection(me, emptyList())
        while (driver.stackSize > 0) driver.bothPass()

        driver.getPermanents(me).contains(lions) shouldBe false
        driver.getGraveyard(me).contains(lions) shouldBe true
        driver.getGraveyard(me).contains(bolt) shouldBe true
    }
})
