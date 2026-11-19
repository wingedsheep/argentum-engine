package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Fated Retribution (BNG #11) — "Destroy all creatures and planeswalkers. If it's your turn, scry 2."
 *
 * The scry is gated on whose turn it is at resolution, so casting this instant on the opponent's
 * turn wipes the board without a scry decision.
 */
class FatedRetributionScenarioTest : ScenarioTestBase() {

    private fun board(activePlayer: Int) = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Fated Retribution")
        .withLandsOnBattlefield(1, "Plains", 7)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Jace, Unraveler of Secrets")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(activePlayer)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { game ->
            val jace = game.findPermanent("Jace, Unraveler of Secrets")!!
            game.state = game.state.updateEntity(jace) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 5))
            }
        }

    private fun ScenarioTestBase.TestGame.assertBoardWiped() {
        withClue("all creatures destroyed") {
            isInGraveyard(1, "Grizzly Bears") shouldBe true
            isInGraveyard(2, "Hill Giant") shouldBe true
        }
        withClue("planeswalker destroyed") {
            isInGraveyard(2, "Jace, Unraveler of Secrets") shouldBe true
        }
    }

    init {
        test("on your turn, destroys all creatures and planeswalkers, then scries 2") {
            val game = board(activePlayer = 1)
            val topTwo = game.state.getLibrary(game.player1Id).take(2)

            game.castSpell(1, "Fated Retribution").error shouldBe null
            game.resolveStack()

            val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("scry 2 looks at the top two cards") { scry.options.toSet() shouldBe topTwo.toSet() }
            game.selectCards(topTwo).error shouldBe null
            game.resolveStack()

            game.assertBoardWiped()
            withClue("both scried cards went to the bottom") {
                game.state.getLibrary(game.player1Id).takeLast(2).toSet() shouldBe topTwo.toSet()
            }
        }

        test("on an opponent's turn, destroys everything without a scry") {
            val game = board(activePlayer = 2)
            val libraryBefore = game.state.getLibrary(game.player1Id)

            game.castSpell(1, "Fated Retribution").error shouldBe null
            game.resolveStack()

            withClue("no scry decision off your turn") { game.hasPendingDecision() shouldBe false }
            game.assertBoardWiped()
            game.state.getLibrary(game.player1Id) shouldBe libraryBefore
        }
    }
}
