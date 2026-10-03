package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Territory Culler (MH3 #173) — landfall peeks the top card: a creature may be revealed into hand;
 * a card not put into hand (creature or not) may be put into the graveyard; otherwise it stays on top.
 */
class TerritoryCullerScenarioTest : ScenarioTestBase() {

    private fun setup(topCard: String): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Territory Culler")
            .withCardInHand(1, "Forest")
            .withCardInLibrary(1, topCard)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val forest = game.state.getHand(game.player1Id).first { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
        }
        val played = game.execute(PlayLand(game.player1Id, forest))
        withClue("Playing the land should succeed: ${played.error}") { played.error shouldBe null }
        game.resolveStack()
        return game
    }

    init {
        context("Territory Culler landfall") {
            test("a creature top card can be revealed into hand") {
                val game = setup("Grizzly Bears")
                withClue("Landfall should prompt to take the creature") { game.hasPendingDecision() shouldBe true }
                val decision = game.getPendingDecision() as SelectCardsDecision
                game.selectCards(listOf(decision.options.first()))
                game.resolveStack()

                game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 0
            }

            test("declining a creature then binning it puts it into the graveyard") {
                val game = setup("Grizzly Bears")
                game.hasPendingDecision() shouldBe true
                game.skipSelection()
                game.resolveStack()

                withClue("Second prompt offers the graveyard") { game.hasPendingDecision() shouldBe true }
                val bin = game.getPendingDecision() as SelectCardsDecision
                game.selectCards(listOf(bin.options.first()))
                game.resolveStack()

                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
                game.findCardsInHand(1, "Grizzly Bears").size shouldBe 0
            }

            test("a non-creature top card can only be binned, never taken") {
                val game = setup("Island")
                withClue("Non-creature still gets the graveyard prompt") { game.hasPendingDecision() shouldBe true }
                val bin = game.getPendingDecision() as SelectCardsDecision
                bin.options.size shouldBe 1
                game.selectCards(listOf(bin.options.first()))
                game.resolveStack()

                game.findCardsInGraveyard(1, "Island").size shouldBe 1
                game.findCardsInHand(1, "Island").size shouldBe 0
            }

            test("declining both leaves the card on top of the library") {
                val game = setup("Grizzly Bears")
                game.skipSelection()
                game.resolveStack()
                game.skipSelection()
                game.resolveStack()

                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 0
                game.findCardsInHand(1, "Grizzly Bears").size shouldBe 0
            }
        }
    }
}
