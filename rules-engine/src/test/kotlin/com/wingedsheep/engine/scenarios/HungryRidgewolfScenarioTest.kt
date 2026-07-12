package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Hungry Ridgewolf (VOW #161) — {1}{R} Creature — Wolf, 2/2.
 *
 *   As long as you control another Wolf or Werewolf, this creature gets +1/+0 and has trample.
 *
 * Exercises the split "as long as" condition (stats + keyword, same condition): with no other
 * Wolf/Werewolf, Hungry Ridgewolf is a vanilla 2/2 without trample; controlling another Wolf turns
 * on both the +1/+0 (3/2) and trample.
 */
class HungryRidgewolfScenarioTest : ScenarioTestBase() {

    init {
        context("Hungry Ridgewolf — conditional pump + trample") {

            test("without another Wolf or Werewolf, it stays a vanilla 2/2 without trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hungry Ridgewolf", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolf = game.findPermanent("Hungry Ridgewolf")!!

                withClue("no other Wolf/Werewolf: base 2/2, no trample") {
                    game.state.projectedState.getPower(wolf) shouldBe 2
                    game.state.projectedState.getToughness(wolf) shouldBe 2
                    game.state.projectedState.hasKeyword(wolf, Keyword.TRAMPLE) shouldBe false
                }
            }

            test("with another Wolf you control, it gets +1/+0 and gains trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hungry Ridgewolf", summoningSickness = false)
                    .withCardOnBattlefield(1, "Lightning Wolf", summoningSickness = false) // another Wolf
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ridgewolf = game.findPermanent("Hungry Ridgewolf")!!

                withClue("controlling another Wolf turns on the pump") {
                    game.state.projectedState.getPower(ridgewolf) shouldBe 3
                    game.state.projectedState.getToughness(ridgewolf) shouldBe 2
                }
                withClue("controlling another Wolf grants trample") {
                    game.state.projectedState.hasKeyword(ridgewolf, Keyword.TRAMPLE) shouldBe true
                }
            }
        }
    }
}
