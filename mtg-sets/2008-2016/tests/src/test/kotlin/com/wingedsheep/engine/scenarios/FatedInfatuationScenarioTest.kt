package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Fated Infatuation (BNG #39) — "Create a token that's a copy of target creature you control.
 * If it's your turn, scry 2."
 *
 * The scry is gated on whose turn it is at resolution: on the opponent's turn the copy is made
 * without a scry decision.
 */
class FatedInfatuationScenarioTest : ScenarioTestBase() {

    private fun board(activePlayer: Int) = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Fated Infatuation")
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(activePlayer)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("on your turn, copies the target creature and scries 2") {
            val game = board(activePlayer = 1)
            val bears = game.findPermanent("Grizzly Bears")!!
            val topTwo = game.state.getLibrary(game.player1Id).take(2)

            game.castSpell(1, "Fated Infatuation", bears).error shouldBe null
            game.resolveStack()

            val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("scry 2 looks at the top two cards") { scry.options.toSet() shouldBe topTwo.toSet() }
            game.selectCards(topTwo).error shouldBe null
            game.resolveStack()

            withClue("a token copy of Grizzly Bears was created") {
                game.findAllPermanents("Grizzly Bears").size shouldBe 2
            }
            withClue("both scried cards went to the bottom") {
                game.state.getLibrary(game.player1Id).takeLast(2).toSet() shouldBe topTwo.toSet()
            }
        }

        test("on an opponent's turn, copies the creature without a scry") {
            val game = board(activePlayer = 2)
            val bears = game.findPermanent("Grizzly Bears")!!
            val libraryBefore = game.state.getLibrary(game.player1Id)

            game.castSpell(1, "Fated Infatuation", bears).error shouldBe null
            game.resolveStack()

            withClue("no scry decision off your turn") { game.hasPendingDecision() shouldBe false }
            game.findAllPermanents("Grizzly Bears").size shouldBe 2
            game.state.getLibrary(game.player1Id) shouldBe libraryBefore
        }
    }
}
