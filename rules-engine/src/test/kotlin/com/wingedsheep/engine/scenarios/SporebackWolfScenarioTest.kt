package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Sporeback Wolf (VOW #223) — {1}{G} Creature — Wolf, 2/2.
 *
 *   During your turn, this creature gets +0/+2.
 *
 * Exercises the ConditionalStaticAbility(ModifyStats(0, 2, Self), Conditions.IsYourTurn):
 * present (4 toughness) during the controller's turn, absent (2 toughness) during the
 * opponent's turn.
 */
class SporebackWolfScenarioTest : ScenarioTestBase() {

    init {
        context("Sporeback Wolf conditional toughness boost") {

            test("is 2/4 during its controller's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sporeback Wolf")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolf = game.findPermanent("Sporeback Wolf")!!

                withClue("Gets +0/+2 during its controller's turn (becomes 2/4)") {
                    game.state.projectedState.getPower(wolf) shouldBe 2
                    game.state.projectedState.getToughness(wolf) shouldBe 4
                }
            }

            test("is 2/2 during the opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sporeback Wolf")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolf = game.findPermanent("Sporeback Wolf")!!

                withClue("No boost outside its controller's turn (stays 2/2)") {
                    game.state.projectedState.getPower(wolf) shouldBe 2
                    game.state.projectedState.getToughness(wolf) shouldBe 2
                }
            }
        }
    }
}
