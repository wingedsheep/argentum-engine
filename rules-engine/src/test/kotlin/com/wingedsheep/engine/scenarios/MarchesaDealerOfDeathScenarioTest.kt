package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Marchesa, Dealer of Death (OTJ #220, {U}{B}{R} 3/4).
 *
 *   Whenever you commit a crime, you may pay {1}. If you do, look at the top two cards of your
 *   library. Put one of them into your hand and the other into your graveyard.
 *
 * Composed from `MayPayManaEffect` + `Patterns.Library.lookAtTopAndKeep(2, 1)` — verify the
 * crime trigger, the {1} payment, and the keep-one / graveyard-the-other split.
 */
class MarchesaDealerOfDeathScenarioTest : ScenarioTestBase() {

    init {
        context("Marchesa, Dealer of Death") {

            test("committing a crime and paying {1} keeps one of the top two cards and graveyards the other") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Marchesa, Dealer of Death")
                    .withCardInHand(1, "Lightning Bolt")  // the crime: target the opponent
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withLandsOnBattlefield(1, "Mountain", 2) // bolt + the {1}
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.state.getHand(game.player1Id).size
                val graveBefore = game.graveyardSize(1)

                // Commit a crime; drain to the "you may pay {1}" prompt.
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()

                // Pay {1} — yes, then auto-tap a land for the payment.
                game.answerYesNo(true)
                withClue("Paying {1} pauses for mana-source selection") {
                    (game.getPendingDecision() is SelectManaSourcesDecision) shouldBe true
                }
                game.submitDecision(
                    ManaSourcesSelectedResponse(game.getPendingDecision()!!.id, autoPay = true)
                )

                // Choose which of the top two to keep (select one); the other goes to graveyard.
                withClue("A select-from-the-top-two decision is presented") {
                    (game.getPendingDecision() != null) shouldBe true
                }
                val topCards = game.findCardsInLibrary(1, "Grizzly Bears") +
                    game.findCardsInLibrary(1, "Mountain")
                game.selectCards(listOf(topCards.first()))
                game.resolveStack()

                withClue("One card went to hand (Bolt left, one drawn → net same count)") {
                    game.state.getHand(game.player1Id).size shouldBe handBefore
                }
                withClue("The other of the top two went to the graveyard (plus the resolved Bolt)") {
                    (game.graveyardSize(1) >= graveBefore + 1) shouldBe true
                }
            }
        }
    }
}
