package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.LibrarySearchedEvent
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class RuinInTheirWakeScenarioTest : ScenarioTestBase() {
    private fun ruinGame(wastesController: Int? = null): TestGame {
        val builder = scenario().withPlayers("P1", "P2")
            .withCardInHand(1, "Ruin in Their Wake")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Wastes")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (wastesController != null) builder.withCardOnBattlefield(wastesController, "Wastes")
        return builder.build()
    }

    init {
        test("controlling Wastes permits the selected basic to enter tapped after being revealed") {
            val game = ruinGame(1)
            val island = game.findCardsInLibrary(1, "Island").single()
            val wastes = game.findCardsInLibrary(1, "Wastes").single()
            game.castSpell(1, "Ruin in Their Wake").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>().options
                .shouldContainExactlyInAnyOrder(listOf(island, wastes))
            val selected = game.selectCards(listOf(island))
            selected.error shouldBe null
            selected.events.filterIsInstance<CardsRevealedEvent>().single().cardIds shouldBe listOf(island)
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            val result = game.answerYesNo(true)
            result.error shouldBe null
            game.findPermanent("Island") shouldBe island
            game.state.getEntity(island)!!.has<TappedComponent>() shouldBe true
            result.events.filterIsInstance<LibraryShuffledEvent>().size shouldBe 1
            result.events.filterIsInstance<LibrarySearchedEvent>().size shouldBe 1
        }

        test("declining the battlefield option puts the revealed land in hand") {
            val game = ruinGame(1)
            val island = game.findCardsInLibrary(1, "Island").single()
            game.castSpell(1, "Ruin in Their Wake").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(island)).error shouldBe null
            game.answerYesNo(false).error shouldBe null
            game.findCardsInHand(1, "Island").single() shouldBe island
            game.hasPendingDecision() shouldBe false
        }

        test("opponent Wastes does not unlock ramp and finding Wastes does not unlock itself") {
            val game = ruinGame(2)
            val wastes = game.findCardsInLibrary(1, "Wastes").single()
            game.castSpell(1, "Ruin in Their Wake").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(wastes)).error shouldBe null
            game.findCardsInHand(1, "Wastes").single() shouldBe wastes
            game.hasPendingDecision() shouldBe false
        }

        test("failing to find skips the may decision and still shuffles and emits the search event") {
            val game = ruinGame(1)
            val library = game.state.getLibrary(game.player1Id)
            game.castSpell(1, "Ruin in Their Wake").error shouldBe null
            game.resolveStack()
            val result = game.selectCards(emptyList())
            result.error shouldBe null
            game.hasPendingDecision() shouldBe false
            game.state.getLibrary(game.player1Id).shouldContainExactlyInAnyOrder(library)
            result.events.filterIsInstance<LibraryShuffledEvent>().size shouldBe 1
            result.events.filterIsInstance<LibrarySearchedEvent>().size shouldBe 1
            result.events.filterIsInstance<CardsRevealedEvent>().size shouldBe 0
        }
    }
}
