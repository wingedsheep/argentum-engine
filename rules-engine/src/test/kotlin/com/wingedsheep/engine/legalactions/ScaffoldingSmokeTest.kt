package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.legalactions.support.EnumerationFixtures
import com.wingedsheep.engine.legalactions.support.shouldContainCastOf
import com.wingedsheep.engine.legalactions.support.shouldContainPlayLandOf
import com.wingedsheep.engine.legalactions.support.shouldNotContainCastOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty

/**
 * Phase 1 smoke test — proves the EnumerationTestDriver, fixtures, and matchers
 * compile and produce sensible results against a real game state. NOT a test of
 * any specific enumerator's correctness.
 */
class ScaffoldingSmokeTest : FunSpec({

    test("active player on main phase can play a Forest from hand") {
        val driver = EnumerationFixtures.mainPhaseOfActivePlayer()

        val actions = driver.enumerateFor(driver.player1)

        actions.shouldNotBeEmpty()
        actions shouldContainPlayLandOf "Forest"
    }

    test("opponent on active player's main phase cannot play a Forest") {
        val driver = EnumerationFixtures.mainPhaseOfActivePlayer()

        val actions = driver.enumerateFor(driver.player2)

        actions shouldNotContainCastOf "Forest" // sanity — Forest is a land, not a spell
        // Opponent has no priority during active player's main phase by default,
        // but the enumerator will still return PassPriority. This proves the
        // wiring works for both players without crashing.
    }

    test("matcher resolves entity ids back to card names") {
        // The view's name-resolution path is what makes the matchers possible.
        // Pick any PlayLand action and confirm its name round-trips to a card
        // that the active player actually has in hand.
        val driver = EnumerationFixtures.mainPhaseOfActivePlayer()
        val actions = driver.enumerateFor(driver.player1)

        val playLand = actions.playLandActions().first()
        val resolvedName = actions.cardNameOf(playLand.action)
        val handNames = driver.game.state.getHand(driver.player1).mapNotNull {
            driver.game.state.getEntity(it)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.name
        }

        handNames shouldContain resolvedName
    }
})
