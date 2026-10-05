package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Overwhelmed Apprentice — {U} Creature — Human Wizard 1/2 (ELD; reprinted in J22).
 *
 *   When this creature enters, each opponent mills two cards. Then you scry 2.
 */
class OverwhelmedApprenticeScenarioTest : ScenarioTestBase() {

    init {
        context("Overwhelmed Apprentice enters trigger") {

            test("the opponent mills two, then its controller scries 2 over their own library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Overwhelmed Apprentice")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Plains")
                    .withCardInLibrary(2, "Swamp")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Overwhelmed Apprentice").error shouldBe null
                game.resolveStack() // creature resolves, enters trigger resolves → scry decision

                withClue("Each opponent milled exactly two cards before the scry") {
                    game.graveyardSize(2) shouldBe 2
                    game.isInGraveyard(2, "Plains") shouldBe true
                    game.isInGraveyard(2, "Swamp") shouldBe true
                    game.librarySize(2) shouldBe 1
                }
                withClue("The controller mills nothing") {
                    game.graveyardSize(1) shouldBe 0
                }

                val decision = game.getPendingDecision()
                withClue("Scry 2 looks at the controller's top two cards") {
                    (decision is SelectCardsDecision) shouldBe true
                    (decision as SelectCardsDecision).options.map { it.nameOf(game) }.toSet() shouldBe
                        setOf("Mountain", "Forest")
                }
                game.selectCards((decision as SelectCardsDecision).options).error shouldBe null
                game.resolveStack()

                withClue("Both scried cards went to the bottom; the Island surfaces") {
                    game.librarySize(1) shouldBe 3
                    game.state.getLibrary(game.player1Id).first().nameOf(game) shouldBe "Island"
                }
                game.isOnBattlefield("Overwhelmed Apprentice") shouldBe true
            }

            test("an opponent with one card left mills just that card, and the scry still happens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Overwhelmed Apprentice")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Overwhelmed Apprentice").error shouldBe null
                game.resolveStack()

                withClue("Mill takes as many as it can") {
                    game.isInGraveyard(2, "Plains") shouldBe true
                    game.librarySize(2) shouldBe 0
                }

                val decision = game.getPendingDecision()
                withClue("The scry is not skipped by the short mill") {
                    (decision is SelectCardsDecision) shouldBe true
                    (decision as SelectCardsDecision).options.size shouldBe 2
                }
            }
        }
    }

    private fun EntityId.nameOf(game: TestGame): String? =
        game.state.getEntity(this)?.get<CardComponent>()?.name
}
