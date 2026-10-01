package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Experimental Augury (ONE #49) — {1}{U} Instant.
 *
 * Look at the top three cards of your library. Put one of them into your hand and the rest on
 * the bottom of your library in any order. Proliferate.
 */
class ExperimentalAuguryScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("keeps one of the top three, bottoms the rest, then proliferates") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Experimental Augury")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Swamp")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }
            val p1 = game.player1Id
            val libraryBefore = game.state.getLibrary(p1)
            val topThree = libraryBefore.take(3)
            val fourth = libraryBefore[3]

            game.castSpell(1, "Experimental Augury").error shouldBe null
            game.resolveStack()

            val look = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            look.options shouldContainExactlyInAnyOrder topThree
            val kept = topThree[1]
            game.selectCards(listOf(kept))

            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()

            withClue("proliferate prompts for permanents/players") {
                game.hasPendingDecision() shouldBe true
            }
            game.selectCards(listOf(bears))
            if (!game.hasPendingDecision() && game.state.stack.isNotEmpty()) game.resolveStack()

            withClue("kept card went to hand") { game.state.getHand(p1).contains(kept) shouldBe true }
            val libraryAfter = game.state.getLibrary(p1)
            withClue("the unseen fourth card is now on top") { libraryAfter.first() shouldBe fourth }
            withClue("the other two looked-at cards are on the bottom") {
                libraryAfter.takeLast(2) shouldContainExactlyInAnyOrder (topThree - kept)
            }
            withClue("proliferate added a +1/+1 counter") { plusOnes(game, bears) shouldBe 2 }
            game.isInGraveyard(1, "Experimental Augury") shouldBe true
        }
    }
}
