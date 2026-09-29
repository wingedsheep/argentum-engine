package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class GrimTutorScenarioTest : ScenarioTestBase() {
    init {
        test("search requires one card, keeps it private, shuffles and then loses three life") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Grim Tutor")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val library = game.state.getLibrary(game.player1Id)
            val chosen = game.findCardsInLibrary(1, "Grizzly Bears").single()

            game.castSpell(1, "Grim Tutor").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision as SelectCardsDecision
            decision.options.toSet() shouldBe library.toSet()
            decision.minSelections shouldBe 1
            decision.maxSelections shouldBe 1
            game.getLifeTotal(1) shouldBe 20

            val result = game.selectCards(listOf(chosen))
            result.error shouldBe null
            result.events.filterIsInstance<CardsRevealedEvent>().isEmpty() shouldBe true
            result.events.filterIsInstance<LibraryShuffledEvent>().size shouldBe 1
            game.resolveStack()
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.state.getLibrary(game.player1Id).toSet() shouldBe (library.toSet() - chosen)
            game.getLifeTotal(1) shouldBe 17
            game.getLifeTotal(2) shouldBe 20
            game.isInGraveyard(1, "Grim Tutor") shouldBe true
        }

        test("empty library still loses three life") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Grim Tutor")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.state.getLibrary(game.player1Id).size shouldBe 0
            game.castSpell(1, "Grim Tutor").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.getLifeTotal(1) shouldBe 17
            game.getLifeTotal(2) shouldBe 20
            game.isInGraveyard(1, "Grim Tutor") shouldBe true
        }
    }
}
