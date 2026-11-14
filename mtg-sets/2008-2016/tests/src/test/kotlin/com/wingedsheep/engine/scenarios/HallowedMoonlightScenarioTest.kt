package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

/**
 * Hallowed Moonlight (ORI #16).
 *
 * "Until end of turn, if a creature would enter and it wasn't cast, exile it instead. Draw a card."
 * The instant grants a floating battlefield-entry redirect that outlives it: a reanimated creature
 * and a creature token (never cast — 2015-06-22 ruling) are exiled instead, the token then ceasing to
 * exist, while a creature spell still enters — but a copy of one wasn't cast, so it is exiled. Its
 * creature test reads the token as it would enter, so March of the Machines' animated Treasure is
 * caught too. The replacement ends with the turn.
 */
class HallowedMoonlightScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.tokenCount(): Int =
            state.getBattlefield().count { state.getEntity(it)?.has<TokenComponent>() == true }

        /** Where each token created during [results] went: EXILE when diverted, BATTLEFIELD when it entered. */
        fun tokenDestinations(results: List<ExecutionResult>): List<Zone> =
            results.flatMap { it.events }.filterIsInstance<ZoneChangeEvent>()
                .filter { it.fromZone == null }.map { it.toZone }

        fun TestGame.castMoonlight() {
            castSpell(1, "Hallowed Moonlight").error shouldBe null
            resolveStack()
        }

        test("draws a card, then a reanimated creature is exiled instead") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Hallowed Moonlight")
                .withCardInHand(1, "Zombify")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castMoonlight()
            game.isInHand(1, "Plains") shouldBe true

            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.castSpellTargetingGraveyardCard(1, "Zombify", listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInExile(1, "Grizzly Bears") shouldBe true
        }

        test("creature tokens are exiled instead and cease to exist") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Hallowed Moonlight")
                .withCardInHand(1, "Raise the Alarm")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castMoonlight()
            game.castSpell(1, "Raise the Alarm").error shouldBe null
            val results = game.resolveStack()

            tokenDestinations(results) shouldBe listOf(Zone.EXILE, Zone.EXILE)
            game.tokenCount() shouldBe 0
        }

        test("a creature spell cast this turn still enters") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Hallowed Moonlight")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castMoonlight()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("cast in response, it exiles the opponent's tokens as their spell resolves") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Hallowed Moonlight")
                .withCardInHand(2, "Raise the Alarm")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(2, "Plains", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Raise the Alarm").error shouldBe null
            game.passPriority()
            game.castSpell(1, "Hallowed Moonlight").error shouldBe null
            val results = game.resolveStack()

            tokenDestinations(results) shouldBe listOf(Zone.EXILE, Zone.EXILE)
            game.findCardsInGraveyard(2, "Raise the Alarm").size shouldBe 1
            game.tokenCount() shouldBe 0
        }

        test("a copy of a creature spell wasn't cast — the copy is exiled, the original enters") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Hallowed Moonlight")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Double Major")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castMoonlight()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.castSpellTargetingStackSpell(1, "Double Major", "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.findAllPermanents("Grizzly Bears").size shouldBe 1
            game.tokenCount() shouldBe 0
        }

        test("a Treasure that March of the Machines makes a creature is exiled as it would enter") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Hallowed Moonlight")
                .withCardInHand(1, "Strike It Rich")
                .withCardOnBattlefield(1, "March of the Machines")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castMoonlight()
            game.castSpell(1, "Strike It Rich").error shouldBe null
            val results = game.resolveStack()

            // Exiled instead of entering as a 0/0 artifact creature that state-based actions bin.
            tokenDestinations(results) shouldBe listOf(Zone.EXILE)

            game.findCardsInGraveyard(1, "Strike It Rich").size shouldBe 1
            game.tokenCount() shouldBe 0
        }

        test("the replacement ends with the turn — the opponent's tokens enter next turn") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Hallowed Moonlight")
                .withCardInHand(2, "Raise the Alarm")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(2, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castMoonlight()
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id

            game.castSpell(2, "Raise the Alarm").error shouldBe null
            val results = game.resolveStack()

            tokenDestinations(results) shouldBe listOf(Zone.BATTLEFIELD, Zone.BATTLEFIELD)
            game.tokenCount() shouldBe 2
        }
    }
}
