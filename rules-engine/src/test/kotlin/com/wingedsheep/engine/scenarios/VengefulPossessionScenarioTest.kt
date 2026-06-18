package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Vengeful Possession (DSK #162).
 *
 * Vengeful Possession — {2}{R} Sorcery
 *   "Gain control of target creature until end of turn. Untap it. It gains haste until end of
 *    turn. You may discard a card. If you do, draw a card."
 *
 * Verifies the threaten effect (control swap + untap) and the optional loot rider.
 */
class VengefulPossessionScenarioTest : ScenarioTestBase() {

    init {
        context("Vengeful Possession") {

            test("gains control of the target, untaps it, and loots when you choose to discard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Vengeful Possession")
                    .withCardInHand(1, "Hill Giant") // discard fodder for the loot rider
                    .withLandsOnBattlefield(1, "Mountain", 3) // {2}{R}
                    // Tapped so we can confirm the untap clause works.
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true) // the creature to steal
                    .withCardInLibrary(1, "Plains") // something to draw
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpell(1, "Vengeful Possession", targetId = bears)
                withClue("Casting Vengeful Possession should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Player 1 now controls the stolen creature (via the until-end-of-turn control effect)") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }
                withClue("The stolen creature is untapped") {
                    (game.state.getEntity(bears)?.get<TappedComponent>() != null) shouldBe false
                }

                // Optional loot: choose to discard a card.
                game.answerYesNo(true)
                // Discard Hill Giant (the discard fodder in hand).
                val toDiscard = game.findCardsInHand(1, "Hill Giant")
                if (toDiscard.isNotEmpty()) {
                    game.selectCards(toDiscard)
                }
                game.resolveStack()

                withClue("Discarded card is in the graveyard") {
                    game.findCardsInGraveyard(1, "Hill Giant").size shouldBe 1
                }
                withClue("Drew a card after discarding") {
                    game.findCardsInHand(1, "Plains").size shouldBe 1
                }
            }
        }
    }
}
