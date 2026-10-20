package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Two or more cards put back on a library always ask for their order, even when they are copies of
 * one card and a selection in the same resolution already showed them (#3340). Whether a question
 * is pending is visible to every seat, and the cards' identities are not, so skipping the question
 * for copies would tell the opponent what the library holds. A single card still skips.
 */
class IdenticalCardsLibraryOrderScenarioTest : ScenarioTestBase() {
    private fun nameOf(game: TestGame, id: com.wingedsheep.sdk.model.EntityId) =
        game.state.getEntity(id)?.get<CardComponent>()?.name

    init {
        test("scry 2 keeping two copies of one card on top asks for their order") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Preordain")
                .withCardOnBattlefield(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Preordain").error.shouldBeNull()
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options.size shouldBe 2
            game.skipSelection().error.shouldBeNull()

            val order = game.state.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
            order.cards.map { nameOf(game, it) } shouldBe listOf("Forest", "Forest")
            game.keepLibraryOrder().error.shouldBeNull()

            game.state.pendingDecision.shouldBeNull()
            game.state.stack shouldBe emptyList()
            game.findCardsInHand(1, "Forest").size shouldBe 1
            game.state.getLibrary(game.player1Id).map { nameOf(game, it) } shouldBe listOf("Forest", "Swamp")
        }

        test("scry 1 keeping its card on top asks nothing more") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Opt")
                .withCardOnBattlefield(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Opt").error.shouldBeNull()
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options.size shouldBe 1
            game.skipSelection().error.shouldBeNull()

            game.state.pendingDecision.shouldBeNull()
            game.state.stack shouldBe emptyList()
            game.isInHand(1, "Forest") shouldBe true
            game.state.getLibrary(game.player1Id).map { nameOf(game, it) } shouldBe listOf("Swamp")
        }

        test("Impulse sending three copies of one card to the bottom asks for their order") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Impulse")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val library = game.state.getLibrary(game.player1Id)
            val kept = library[1]
            val forests = listOf(library[0], library[2], library[3])
            val swamp = library[4]
            game.castSpell(1, "Impulse").error.shouldBeNull()
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldBe library.take(4)
            game.selectCards(listOf(kept)).error.shouldBeNull()

            val order = game.state.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
            order.cards.toSet() shouldBe forests.toSet()
            val chosen = forests.reversed()
            game.submitDecision(OrderedResponse(order.id, chosen)).error.shouldBeNull()

            game.state.pendingDecision.shouldBeNull()
            game.state.stack shouldBe emptyList()
            game.state.getHand(game.player1Id) shouldContain kept
            game.state.getLibrary(game.player1Id) shouldBe listOf(swamp) + chosen
            game.isInGraveyard(1, "Impulse") shouldBe true
        }
    }
}
