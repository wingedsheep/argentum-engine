package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Unfathomable Truths.
 *
 * Card reference:
 * - Unfathomable Truths ({4}{U}): Instant, devoid
 *   "Draw three cards and create a 0/1 colorless Eldrazi Spawn creature token with
 *    'Sacrifice this token: Add {C}.'"
 */
class UnfathomableTruthsScenarioTest : ScenarioTestBase() {

    init {
        context("Unfathomable Truths") {
            test("draws three cards and creates an Eldrazi Spawn token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Unfathomable Truths")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Unfathomable Truths")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }

                game.resolveStack()

                withClue("Three cards should be drawn") {
                    game.state.getZone(game.player1Id, Zone.HAND).size shouldBe 3
                    game.state.getZone(game.player1Id, Zone.LIBRARY).size shouldBe 1
                }
                withClue("An Eldrazi Spawn token should be created") {
                    game.findPermanents("Eldrazi Spawn") shouldHaveSize 1
                }
                withClue("Unfathomable Truths goes to the graveyard") {
                    game.isInGraveyard(1, "Unfathomable Truths") shouldBe true
                }
            }
        }
    }
}
