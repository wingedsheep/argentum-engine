package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * The Three Seasons (KHM #231).
 *
 *   III — Choose three cards in each graveyard. Their owners shuffle those cards into their libraries.
 *
 * Pins that the Saga's controller makes the choice in every graveyard (the opponent's too), that
 * exactly three are taken from a graveyard holding more, that a graveyard holding fewer gives up
 * all of them, that each card lands in its owner's library, and that a player with an empty
 * graveyard doesn't shuffle (ruling).
 */
class TheThreeSeasonsScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Seasons", "Opponent")
            .withCardOnBattlefield(1, "The Three Seasons")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(6) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Island") } }

        /** Set the Saga at two lore counters and advance to the main phase so chapter III triggers. */
        fun TestGame.triggerChapterThree() {
            val saga = findPermanent("The Three Seasons")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, 2))
                    .with(SagaComponent(triggeredChapters = setOf(1, 2)))
            }
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        }

        /** Resolve chapter III, answering every graveyard prompt with its first three options. */
        fun TestGame.resolveChoosingFirstThree(): List<List<EntityId>> {
            resolveStack()
            val picks = mutableListOf<List<EntityId>>()
            while (true) {
                val decision = getPendingDecision() as? SelectCardsDecision ?: break
                withClue("the Saga's controller chooses in every graveyard") {
                    decision.playerId shouldBe player1Id
                }
                withClue("exactly three must be chosen") {
                    decision.minSelections shouldBe 3
                    decision.maxSelections shouldBe 3
                }
                val pick = decision.options.take(3)
                picks += pick
                selectCards(pick).error shouldBe null
                resolveStack()
            }
            return picks
        }

        test("three cards from each larger graveyard go back into their owners' libraries") {
            val game = builder()
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Plains")
                .withCardInGraveyard(1, "Swamp")
                .withCardInGraveyard(1, "Mountain")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInGraveyard(2, "Hill Giant")
                .withCardInGraveyard(2, "Plains")
                .withCardInGraveyard(2, "Swamp")
                .build()

            game.triggerChapterThree()
            val library1Before = game.librarySize(1)
            val library2Before = game.librarySize(2)
            val picks = game.resolveChoosingFirstThree()

            withClue("one prompt per graveyard, both answered by the Saga's controller") { picks.size shouldBe 2 }
            game.graveyardSize(1) shouldBe 2 + 1 // two unchosen cards, plus the sacrificed Saga
            game.graveyardSize(2) shouldBe 1
            game.librarySize(1) shouldBe library1Before + 3
            game.librarySize(2) shouldBe library2Before + 3
            withClue("each chosen card is in its owner's library") {
                game.state.getLibrary(game.player1Id) shouldContainAll picks[0]
                game.state.getLibrary(game.player2Id) shouldContainAll picks[1]
            }
        }

        test("a graveyard with three or fewer cards gives up all of them, and an empty one doesn't shuffle") {
            val game = builder()
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .build()

            game.triggerChapterThree()
            val library1Before = game.librarySize(1)
            val library2Order = game.state.getLibrary(game.player2Id).toList()
            val picks = game.resolveChoosingFirstThree()

            withClue("nothing to choose between — no prompt") { picks.size shouldBe 0 }
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            game.isInGraveyard(1, "Hill Giant") shouldBe false
            game.librarySize(1) shouldBe library1Before + 2
            withClue("the opponent's library is untouched — no shuffle for an empty graveyard") {
                game.state.getLibrary(game.player2Id).toList() shouldBe library2Order
            }
        }
    }
}
