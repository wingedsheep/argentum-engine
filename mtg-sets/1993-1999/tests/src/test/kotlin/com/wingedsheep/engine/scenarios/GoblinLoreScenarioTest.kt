package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Goblin Lore (P02): "Draw four cards, then discard three cards at random."
 */
class GoblinLoreScenarioTest : ScenarioTestBase() {

    init {
        context("Goblin Lore") {
            test("draws four cards then discards three at random") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Goblin Lore")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val graveBefore = game.graveyardSize(1)
                game.castSpell(1, "Goblin Lore").error shouldBe null
                game.resolveStack()

                withClue("hand: Lore left, +4 drawn, -3 discarded") {
                    game.handSize(1) shouldBe 1
                }
                withClue("Lore itself plus three discards") {
                    game.graveyardSize(1) shouldBe graveBefore + 4
                }
            }
        }
    }
}
