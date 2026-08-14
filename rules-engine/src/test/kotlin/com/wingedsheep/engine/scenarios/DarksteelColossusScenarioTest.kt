package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Darksteel Colossus (DST, reprinted in FDN) — {11} Artifact Creature — Golem 11/11, Trample,
 * Indestructible.
 *
 * "If Darksteel Colossus would be put into a graveyard from anywhere, reveal it and shuffle it
 * into its owner's library instead."
 *
 * The redirect is a card-intrinsic replacement effect (CR 113.6b — it says "from anywhere", so it
 * functions from every zone) carried on the card entity via
 * [com.wingedsheep.engine.state.components.identity.SelfZoneRedirectComponent], so it functions in
 * every zone — not just on the battlefield. These tests drive a graveyard-bound move from the
 * battlefield, the library (mill), and the hand (discard) and assert the Colossus ends up shuffled
 * back into its owner's library rather than in the graveyard.
 */
class DarksteelColossusScenarioTest : ScenarioTestBase() {

    private val name = "Darksteel Colossus"

    init {
        test("dies from the battlefield: shuffled into its owner's library, never reaching the graveyard") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, name, summoningSickness = false)
                // Library padding so a shuffle is observable and placement isn't trivially "the only card".
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val colossus = game.findPermanent(name)!!
            val libraryBefore = game.librarySize(1)

            game.state = ZoneTransitionService.moveToZone(game.state, colossus, Zone.GRAVEYARD).state

            withClue("it must not land in the graveyard") {
                game.isInGraveyard(1, name) shouldBe false
            }
            withClue("it must be shuffled back into the library") {
                game.findCardsInLibrary(1, name).size shouldBe 1
                game.librarySize(1) shouldBe libraryBefore + 1
            }
        }

        test("milled from the library: redirected right back into the library") {
            val game = scenario()
                .withPlayers()
                .withCardInLibrary(1, name)
                .withCardInLibrary(1, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val colossus = game.findCardsInLibrary(1, name).single()
            game.state = ZoneTransitionService.moveToZone(game.state, colossus, Zone.GRAVEYARD).state

            withClue("a card put into the graveyard from the library is redirected too") {
                game.isInGraveyard(1, name) shouldBe false
                game.findCardsInLibrary(1, name).size shouldBe 1
            }
        }

        test("discarded from hand: redirected into the library instead of the graveyard") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, name)
                .withCardInLibrary(1, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val colossus = game.findCardsInHand(1, name).single()
            game.state = ZoneTransitionService.moveToZone(game.state, colossus, Zone.GRAVEYARD).state

            withClue("a card put into the graveyard from the hand is redirected too") {
                game.isInGraveyard(1, name) shouldBe false
                game.isInHand(1, name) shouldBe false
                game.findCardsInLibrary(1, name).size shouldBe 1
            }
        }
    }
}
