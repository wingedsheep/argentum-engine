package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Inquisition — "Target player reveals their hand. Inquisition deals damage to
 * that player equal to the number of white cards in their hand."
 *
 * The count is a *colour* test on the card, so the hand is stocked with white cards of several
 * types plus non-white ones: a filter that had drifted to "white creatures" or to Plains would score
 * differently on this hand and be caught. The empty case matters too — zero white cards must deal
 * zero damage rather than misfiring.
 */
class InquisitionScenarioTest : ScenarioTestBase() {

    init {
        context("Inquisition — damage equal to white cards in hand") {

            test("counts every white card, whatever its type") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Inquisition")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    // Three white cards across three card types, and two non-white decoys.
                    .withCardInHand(2, "Savannah Lions")
                    .withCardInHand(2, "Pacifism")
                    .withCardInHand(2, "Holy Strength")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Inquisition", 2).error shouldBe null
                game.resolveStack()

                withClue("three white cards -> 3 damage") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("the hand is revealed, not discarded") {
                    game.handSize(2) shouldBe 5
                }
            }

            test("no white cards deals no damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Inquisition")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Inquisition", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
