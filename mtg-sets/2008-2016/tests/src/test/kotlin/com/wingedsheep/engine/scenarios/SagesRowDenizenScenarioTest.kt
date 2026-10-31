package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Sage's Row Denizen (GTC #46) — {2}{U} 2/3 Creature — Vedalken Wizard.
 *
 *   Whenever another blue creature you control enters, target player mills two cards.
 */
class SagesRowDenizenScenarioTest : ScenarioTestBase() {
    /** Answers the trigger's target prompt if the engine asks for one. */
    private fun TestGame.chooseTarget(target: EntityId) {
        if (state.pendingDecision is ChooseTargetsDecision) selectTargets(listOf(target)).error shouldBe null
    }

    init {
        context("Sage's Row Denizen") {
            test("another blue creature entering mills the target player two; a non-blue one doesn't trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sage's Row Denizen")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Wind Drake")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.state.pendingDecision shouldBe null
                game.librarySize(2) shouldBe 3

                game.castSpell(1, "Wind Drake").error shouldBe null
                game.resolveStack()
                game.chooseTarget(game.player2Id)
                game.resolveStack()

                game.librarySize(2) shouldBe 1
                game.graveyardSize(2) shouldBe 2
            }
        }
    }
}
