package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * The Necrobloom (MH3 #194) — {1}{W}{B}{G} Legendary Creature — Plant 2/7.
 * Landfall makes a 0/1 Plant, or a 2/2 Zombie with seven or more differently named lands;
 * land cards in your graveyard have dredge 2.
 */
class TheNecrobloomScenarioTest : ScenarioTestBase() {
    init {
        fun main() = scenario().withPlayers("P1", "P2")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withCardOnBattlefield(1, "The Necrobloom")

        test("landfall creates a 0/1 Plant with fewer than seven land names") {
            val game = main().withLandsOnBattlefield(1, "Forest", 6)
                .withCardInHand(1, "Plains").build()
            val plains = game.findCardsInHand(1, "Plains").single()
            game.execute(PlayLand(game.player1Id, plains)).error shouldBe null
            game.resolveStack()
            game.findPermanents("Plant Token").size shouldBe 1
            game.findPermanents("Zombie Token").size shouldBe 0
        }

        test("landfall creates a 2/2 Zombie instead with seven differently named lands") {
            val game = main().withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(1, "Island")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Mountain")
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Terramorphic Expanse")
                .withCardInHand(1, "Evolving Wilds").build()
            val wilds = game.findCardsInHand(1, "Evolving Wilds").single()
            game.execute(PlayLand(game.player1Id, wilds)).error shouldBe null
            game.resolveStack()
            game.findPermanents("Zombie Token").size shouldBe 1
            game.findPermanents("Plant Token").size shouldBe 0
        }

        test("land cards in your graveyard have dredge 2 for the draw step") {
            val game = scenario().withPlayers("P1", "P2")
                .withActivePlayer(1).withTurnNumber(3)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .withCardOnBattlefield(1, "The Necrobloom")
                .withCardInGraveyard(1, "Forest")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Swamp").build()
            game.passPriority().error shouldBe null
            game.passPriority().error shouldBe null
            val decision = game.state.pendingDecision as YesNoDecision
            decision.context.sourceName shouldBe "Forest"
            game.answerYesNo(true).error shouldBe null
            game.isInHand(1, "Forest") shouldBe true
            game.isInGraveyard(1, "Plains") shouldBe true
            game.isInGraveyard(1, "Island") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.state.getLibrary(game.player1Id).size shouldBe 1
        }
    }
}
