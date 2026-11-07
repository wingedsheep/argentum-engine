package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Depala, Pilot Exemplar (KLD #178) — "Other Dwarves you control get +1/+1. Each Vehicle you
 * control gets +1/+1 as long as it's a creature. Whenever Depala becomes tapped, you may pay {X}.
 * If you do, reveal the top X cards of your library, put all Dwarf and Vehicle cards from among them
 * into your hand, then put the rest on the bottom of your library in a random order."
 *
 * Covers the Vehicle lord reading projected state (only a crewed Vehicle is buffed), the tap
 * trigger firing off a crew cost, X feeding the reveal count, and the declined "may".
 */
class DepalaPilotExemplarScenarioTest : ScenarioTestBase() {

    private fun depalaBoard() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Depala, Pilot Exemplar", summoningSickness = false)
        .withCardOnBattlefield(1, "Aerial Responder", summoningSickness = false)
        .withCardOnBattlefield(1, "Renegade Freighter")
        .withCardOnBattlefield(2, "Aerial Responder")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardInLibrary(1, "Aerial Responder")
        .withCardInLibrary(1, "Fleetwheel Cruiser")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Depala, Pilot Exemplar") {

            test("buffs other Dwarves you control, but not itself, opposing Dwarves, or an uncrewed Vehicle") {
                val game = depalaBoard()
                val projected = game.state.projectedState
                val depala = game.findPermanent("Depala, Pilot Exemplar")!!
                val (mine, theirs) = game.findAllPermanents("Aerial Responder")
                    .partition { projected.getController(it) == game.player1Id }
                val freighter = game.findPermanent("Renegade Freighter")!!

                withClue("Depala is not an 'other' Dwarf") {
                    projected.getPower(depala) shouldBe 3
                    projected.getToughness(depala) shouldBe 3
                }
                withClue("your Dwarf gets +1/+1") {
                    projected.getPower(mine.single()) shouldBe 3
                    projected.getToughness(mine.single()) shouldBe 4
                }
                withClue("the opponent's Dwarf is unchanged") {
                    projected.getPower(theirs.single()) shouldBe 2
                    projected.getToughness(theirs.single()) shouldBe 3
                }
                projected.isCreature(freighter) shouldBe false
            }

            test("crewing with Depala taps it; paying X = 4 puts every Dwarf and Vehicle into hand, and the crewed Vehicle gets +1/+1") {
                val game = depalaBoard()
                val depala = game.findPermanent("Depala, Pilot Exemplar")!!
                val freighter = game.findPermanent("Renegade Freighter")!!

                game.execute(CrewVehicle(game.player1Id, freighter, listOf(depala))).error shouldBe null
                game.resolveStack()

                val ask = game.getPendingDecision()
                ask.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                val number = game.getPendingDecision()
                number.shouldBeInstanceOf<ChooseNumberDecision>()
                number.maxValue shouldBe 4
                game.chooseNumber(4).error shouldBe null
                game.resolveStack()

                withClue("the Dwarf and the Vehicle were put into hand") {
                    game.isInHand(1, "Aerial Responder") shouldBe true
                    game.isInHand(1, "Fleetwheel Cruiser") shouldBe true
                }
                withClue("the non-matching cards went to the bottom of the library") {
                    game.isInHand(1, "Plains") shouldBe false
                    game.findCardsInLibrary(1, "Plains").size shouldBe 2
                    game.librarySize(1) shouldBe 2
                }

                val projected = game.state.projectedState
                withClue("the crewed Renegade Freighter is a 4/3 creature with +1/+1") {
                    projected.isCreature(freighter) shouldBe true
                    projected.getPower(freighter) shouldBe 5
                    projected.getToughness(freighter) shouldBe 4
                }
            }

            test("declining the payment reveals nothing") {
                val game = depalaBoard()
                val depala = game.findPermanent("Depala, Pilot Exemplar")!!
                val freighter = game.findPermanent("Renegade Freighter")!!

                game.execute(CrewVehicle(game.player1Id, freighter, listOf(depala))).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Aerial Responder") shouldBe false
                game.isInHand(1, "Fleetwheel Cruiser") shouldBe false
                game.librarySize(1) shouldBe 4
                game.findAllPermanents("Mountain").size shouldBe 4
            }
        }
    }
}
