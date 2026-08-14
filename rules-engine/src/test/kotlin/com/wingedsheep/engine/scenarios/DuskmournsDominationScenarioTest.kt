package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Duskmourn's Domination. */
class DuskmournsDominationScenarioTest : ScenarioTestBase() {

    init {
        context("Duskmourn's Domination — control + -3/-0 + loses all abilities") {

            test("attaching the Aura steals control, applies -3/-0, and removes flying/vigilance") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Serra Angel", summoningSickness = false)
                    .withCardAttachedTo(1, "Duskmourn's Domination", "Serra Angel")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val angel = game.findPermanent("Serra Angel")!!

                withClue("Player 1 now controls the enchanted Serra Angel") {
                    game.state.projectedState.getController(angel) shouldBe game.player1Id
                }
                withClue("Serra Angel (4/4) gets -3/-0 → 1/4") {
                    game.state.projectedState.getPower(angel) shouldBe 1
                    game.state.projectedState.getToughness(angel) shouldBe 4
                }
                withClue("Serra Angel loses all abilities, including flying and vigilance") {
                    game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe false
                    game.state.projectedState.hasKeyword(angel, Keyword.VIGILANCE) shouldBe false
                }
            }
        }
    }
}
