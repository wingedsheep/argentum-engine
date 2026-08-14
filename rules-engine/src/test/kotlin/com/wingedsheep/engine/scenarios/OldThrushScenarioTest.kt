package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Old Thrush (HOB) — {2} Creature — Bird 1/2 with flying.
 * "When this creature enters, you gain 2 life. You may search your library for a basic land card,
 *  reveal it, then shuffle and put that card on top."
 *
 * The life gain is unconditional while the search is a *may*, so declining must still leave the
 * life gained. When accepted, the fetched land goes on *top of the library* — not to hand.
 */
class OldThrushScenarioTest : ScenarioTestBase() {

    init {
        context("Old Thrush") {

            test("it is a 1/2 flier") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Old Thrush")
                    .build()

                val thrush = game.findPermanent("Old Thrush")!!
                game.state.projectedState.getPower(thrush) shouldBe 1
                game.state.projectedState.getToughness(thrush) shouldBe 2
                game.state.projectedState.hasKeyword(thrush, Keyword.FLYING) shouldBe true
            }

            test("its ETB gains 2 life and may put a basic land on top of the library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Old Thrush")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Centaur Courser")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)
                game.castSpell(1, "Old Thrush").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the life gain is unconditional and happens first") {
                    game.getLifeTotal(1) shouldBe 22
                }

                withClue("the search is optional") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null

                val decision = game.getPendingDecision()
                withClue("accepting raises a library search") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                val options = (decision as SelectCardsDecision).options
                withClue("only basic land cards are offered") {
                    options.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
                        .shouldContainExactlyInAnyOrder("Forest")
                }
                game.selectCards(listOf(options.single())).error shouldBe null
                game.resolveStack()

                withClue("the card goes on top of the library, not to hand") {
                    game.cardNameAtTop(1) shouldBe "Forest"
                    game.isInHand(1, "Forest") shouldBe false
                    game.librarySize(1) shouldBe libraryBefore
                }
            }

            test("declining the search still gains the 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Old Thrush")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Old Thrush").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                (game.getPendingDecision() is YesNoDecision) shouldBe true
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("life is gained regardless of the may") {
                    game.getLifeTotal(1) shouldBe 22
                }
                withClue("no search happened — the top of the library is untouched") {
                    game.cardNameAtTop(1) shouldBe "Forest"
                    game.librarySize(1) shouldBe 2
                }
                withClue("the Bird still entered") {
                    game.isOnBattlefield("Old Thrush") shouldBe true
                }
            }
        }
    }

    private fun TestGame.libraryIds(playerNumber: Int): List<com.wingedsheep.sdk.model.EntityId> =
        state.getLibrary(if (playerNumber == 1) player1Id else player2Id)

    private fun TestGame.cardNameAtTop(playerNumber: Int): String? =
        state.getEntity(libraryIds(playerNumber).first())?.get<CardComponent>()?.name

    private fun TestGame.cardNameAtBottom(playerNumber: Int): String? =
        state.getEntity(libraryIds(playerNumber).last())?.get<CardComponent>()?.name
}
