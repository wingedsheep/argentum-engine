package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Hidetsugu and Kairi (MOM #228) — enters: draw three, put two back in any order; dies: exile
 * the top card, target opponent loses life equal to its mana value, and an instant or sorcery
 * exiled this way may be cast for free.
 */
class HidetsuguAndKairiScenarioTest : ScenarioTestBase() {

    private fun TestGame.nameOf(id: com.wingedsheep.sdk.model.EntityId) =
        state.getEntity(id)?.get<CardComponent>()?.name

    /** Kill Hidetsugu and Kairi with Murder, then answer the dies trigger's opponent target. */
    private fun TestGame.murderHidetsugu() {
        castSpell(1, "Murder", findPermanent("Hidetsugu and Kairi")!!).error shouldBe null
        var guard = 0
        while (guard++ < 10) {
            val decision = state.pendingDecision
            when {
                decision is ChooseTargetsDecision ->
                    submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(player2Id)))).error shouldBe null
                decision != null -> return
                state.stack.isEmpty() -> return
                else -> passPriority()
            }
        }
    }

    private fun dyingBoard(topCard: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Hidetsugu and Kairi")
        .withCardInHand(1, "Murder")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInLibrary(1, topCard)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Hidetsugu and Kairi") {
            test("enters: draw three, then put two cards from hand on top of the library") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hidetsugu and Kairi")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hidetsugu and Kairi").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe 3

                val select = game.state.pendingDecision as? SelectCardsDecision
                    ?: error("expected a card selection, got ${game.state.pendingDecision}")
                val putBack = select.options.take(2)
                game.selectCards(putBack).error shouldBe null
                if (game.state.pendingDecision is ReorderLibraryDecision) game.keepLibraryOrder().error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 1
                game.librarySize(1) shouldBe 3
                game.state.getLibrary(game.player1Id).take(2).toSet() shouldBe putBack.toSet()
            }

            test("dies: exiles an instant, opponent loses its mana value, and it can be cast for free") {
                val game = dyingBoard("Divination")
                game.murderHidetsugu()

                // The "may cast" pick over the exiled Divination.
                val pick = game.state.pendingDecision as? SelectCardsDecision
                    ?: error("expected the may-cast pick, got ${game.state.pendingDecision}")
                game.selectCards(pick.options).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.isInGraveyard(1, "Divination") shouldBe true
                // Divination drew the two Islands left under the exiled card.
                game.handSize(1) shouldBe 2
                game.librarySize(1) shouldBe 0
            }

            test("dies: declining the cast leaves the instant or sorcery in exile") {
                val game = dyingBoard("Divination")
                game.murderHidetsugu()

                val pick = game.state.pendingDecision as? SelectCardsDecision
                    ?: error("expected the may-cast pick, got ${game.state.pendingDecision}")
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.isInExile(1, "Divination") shouldBe true
                game.handSize(1) shouldBe 0
            }

            test("dies: a land on top is exiled for zero life loss and no cast") {
                val game = dyingBoard("Forest")
                game.murderHidetsugu()
                game.resolveStack()

                game.state.pendingDecision shouldBe null
                game.getLifeTotal(2) shouldBe 20
                game.isInExile(1, "Forest") shouldBe true
            }
        }
    }
}
