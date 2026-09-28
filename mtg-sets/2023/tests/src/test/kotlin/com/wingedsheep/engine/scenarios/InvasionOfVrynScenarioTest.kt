package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Invasion of Vryn // Overloaded Mage-Ring — "When this Siege enters, draw three cards, then
 * discard a card."
 */
class InvasionOfVrynScenarioTest : ScenarioTestBase() {
    init {
        test("entering draws three cards then discards one") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Vryn")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val gyBefore = game.state.getGraveyard(game.player1Id).size
            game.castSpell(1, "Invasion of Vryn").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            (decision is SelectCardsDecision) shouldBe true
            game.selectCards(listOf((decision as SelectCardsDecision).options.first())).error shouldBe null
            game.resolveStack()

            game.state.getHand(game.player1Id).size shouldBe 2
            game.state.getGraveyard(game.player1Id).size shouldBe gyBefore + 1
        }
    }
}
