package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Erode (SOS #15).
 *
 * "{W} Instant. Destroy target creature or planeswalker. Its controller may search their library
 *  for a basic land card, put it onto the battlefield tapped, then shuffle."
 *
 * Verifies the destroy always happens, and that the optional basic-land search is performed by the
 * *destroyed permanent's controller* (the opponent here), who may decline.
 */
class ErodeScenarioTest : ScenarioTestBase() {

    init {
        context("Erode") {

            test("destroys the creature; its controller may fetch a tapped basic land") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Erode")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Erode", bears)
                game.resolveStack()

                // Its controller (player 2) may search — accept.
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true)
                game.resolveStack()

                // Select the Forest from player 2's library.
                if (game.hasPendingDecision()) {
                    val forest = game.state.getLibrary(game.player2Id).first { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
                    }
                    game.selectCards(listOf(forest))
                    game.resolveStack()
                }

                withClue("Grizzly Bears should be destroyed") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                val forestOnBf = game.state.getBattlefield().firstOrNull { id ->
                    val e = game.state.getEntity(id)
                    e?.get<CardComponent>()?.name == "Forest" &&
                        e.get<ControllerComponent>()?.playerId == game.player2Id
                }
                withClue("Player 2 should have fetched a Forest onto the battlefield") {
                    (forestOnBf != null) shouldBe true
                }
                withClue("The fetched Forest should be tapped") {
                    game.state.getEntity(forestOnBf!!)?.get<TappedComponent>() shouldBe TappedComponent
                }
            }

            test("the controller may decline the search") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Erode")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Erode", bears)
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Grizzly Bears should still be destroyed") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                withClue("No land should have entered the battlefield") {
                    game.state.getBattlefield().none { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
                    } shouldBe true
                }
            }
        }
    }
}
