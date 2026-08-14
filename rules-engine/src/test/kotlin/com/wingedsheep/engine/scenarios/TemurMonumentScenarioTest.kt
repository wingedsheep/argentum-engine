package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Temur Monument. */
class TemurMonumentScenarioTest : ScenarioTestBase() {

    init {
        context("Temur Monument") {
            test("ETB tutors a basic Forest/Island/Mountain into hand and shuffles") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Temur Monument")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Temur Monument")
                withClue("Casting Temur Monument should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // ETB: search for a basic Forest/Island/Mountain — only the Island qualifies.
                withClue("ETB search should prompt a card selection") {
                    game.hasPendingDecision() shouldBe true
                }
                val decision = game.getPendingDecision() as SelectCardsDecision
                val options = decision.options
                withClue("Only the Island should be a legal choice (Swamp is excluded)") {
                    options.size shouldBe 1
                }
                game.selectCards(listOf(options.first()))
                game.resolveStack()

                withClue("Temur Monument should be on the battlefield") {
                    game.isOnBattlefield("Temur Monument") shouldBe true
                }
                withClue("The Island should be in hand") {
                    game.findCardsInHand(1, "Island").size shouldBe 1
                }
                withClue("The Island should have left the library") {
                    game.findCardsInLibrary(1, "Island").size shouldBe 0
                }
            }
        }
    }
}
