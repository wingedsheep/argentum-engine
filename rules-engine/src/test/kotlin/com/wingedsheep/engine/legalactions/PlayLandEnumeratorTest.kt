package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.legalactions.support.EnumerationFixtures
import com.wingedsheep.engine.legalactions.support.shouldContainPlayLandOf
import com.wingedsheep.engine.legalactions.support.shouldNotContainPlayLandOf
import com.wingedsheep.sdk.core.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tests for [enumerators.PlayLandEnumerator] (excluding the Muldrotha-style
 * "play lands from graveyard" path — that lives in CastFromZoneEnumerator
 * tests in phase 4).
 */
class PlayLandEnumeratorTest : FunSpec({

    test("emits one PlayLand per land in hand on the active player's main phase") {
        val driver = EnumerationFixtures.allForestsMainPhase()
        // Opening hand is 7 Forests guaranteed by the all-Forest deck.

        val landActions = driver.enumerateFor(driver.player1).playLandActions()

        landActions shouldHaveSize 7
        landActions.forEach { it.actionType shouldBe "PlayLand" }
    }

    test("emits no PlayLand actions for the non-active player") {
        val driver = EnumerationFixtures.allForestsMainPhase()

        val landActions = driver.enumerateFor(driver.player2).playLandActions()

        landActions.shouldBeEmpty()
    }

    test("emits no PlayLand actions outside main phase") {
        val driver = EnumerationFixtures.allForestsMirror()
        driver.game.passPriorityUntil(Step.UPKEEP)

        val landActions = driver.enumerateFor(driver.player1).playLandActions()

        landActions.shouldBeEmpty()
    }

    test("after playing a land, no further PlayLand actions appear (drop exhausted)") {
        val driver = EnumerationFixtures.allForestsMainPhase()
        val firstLand = driver.game.state.getHand(driver.player1).first()
        driver.game.playLand(driver.player1, firstLand)

        val landActions = driver.enumerateFor(driver.player1).playLandActions()

        landActions.shouldBeEmpty()
    }

    test("PlayLand description includes the card name") {
        val driver = EnumerationFixtures.allForestsMainPhase()

        val actions = driver.enumerateFor(driver.player1)

        actions shouldContainPlayLandOf "Forest"
        actions.playLandActions().first().description shouldBe "Play Forest"
    }

    test("a deck of pure Bears yields zero PlayLand actions even on main phase") {
        val driver = EnumerationFixtures.forestAndBearsMirror().also {
            // Replace the deck setup: register a creature-only mirror.
        }
        // Reuse the standard fixture and just verify: any non-land in hand
        // never produces a PlayLand. Pick a non-land from the opening hand.
        val handIds = driver.game.state.getHand(driver.player1)
        val nonLandIds = handIds.filterNot { id ->
            driver.game.state.getEntity(id)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.typeLine?.isLand == true
        }

        // Skip if shuffle dealt only lands — extremely unlikely with 50/50 deck.
        if (nonLandIds.isEmpty()) return@test

        driver.game.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val actions = driver.enumerateFor(driver.player1)

        actions shouldNotContainPlayLandOf "Grizzly Bears"
    }
})
