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
 * Map the Frontier (OTJ #170) — {3}{G} Sorcery.
 *
 * "Search your library for up to two basic land cards and/or Desert cards, put them onto the
 *  battlefield tapped, then shuffle."
 *
 * Verifies the basic-land-or-Desert search filter, the two cards entering tapped, and the
 * "up to two" upper bound (finding fewer is legal).
 */
class MapTheFrontierScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("finds two basic lands and puts them onto the battlefield tapped") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val forest = driver.putCardOnTopOfLibrary(player, "Forest")
        val mountain = driver.putCardOnTopOfLibrary(player, "Mountain")

        val spell = driver.putCardInHand(player, "Map the Frontier")
        driver.giveMana(player, Color.GREEN, 4)

        driver.castSpell(player, spell).isSuccess shouldBe true
        driver.bothPass()

        driver.isPaused shouldBe true
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        // Both basic lands in the library are valid targets.
        decision.options.size shouldBe 2
        decision.maxSelections shouldBe 2

        driver.submitCardSelection(player, listOf(forest, mountain))
        driver.isPaused shouldBe false

        val battlefield = driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD))
        battlefield.contains(forest) shouldBe true
        battlefield.contains(mountain) shouldBe true
        driver.isTapped(forest) shouldBe true
        driver.isTapped(mountain) shouldBe true
    }

    test("may find fewer than two ('up to two')") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val forest = driver.putCardOnTopOfLibrary(player, "Forest")

        val spell = driver.putCardInHand(player, "Map the Frontier")
        driver.giveMana(player, Color.GREEN, 4)

        driver.castSpell(player, spell).isSuccess shouldBe true
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        // Select nothing — finding zero is a legal "up to two" choice.
        driver.submitCardSelection(player, emptyList())
        driver.isPaused shouldBe false

        val battlefield = driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD))
        battlefield.contains(forest) shouldBe false
    }
})
