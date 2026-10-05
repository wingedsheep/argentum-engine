package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Mesa Lynx (ZNR #28) — {1}{W} Creature — Cat, 2/1.
 *
 *   During turns other than yours, this creature gets +0/+2.
 *
 * Exercises ConditionalStaticAbility(ModifyStats(0, 2, Self), Conditions.IsNotYourTurn):
 * absent (1 toughness) during the controller's turn, present (3 toughness) during the
 * opponent's turn.
 */
class MesaLynxScenarioTest : ScenarioTestBase() {

    init {
        context("Mesa Lynx conditional toughness boost") {

            test("is 2/1 during its controller's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mesa Lynx")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lynx = game.findPermanent("Mesa Lynx")!!

                withClue("No boost during its controller's turn (stays 2/1)") {
                    game.state.projectedState.getPower(lynx) shouldBe 2
                    game.state.projectedState.getToughness(lynx) shouldBe 1
                }
            }

            test("is 2/3 during the opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mesa Lynx")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lynx = game.findPermanent("Mesa Lynx")!!

                withClue("Gets +0/+2 during a turn other than its controller's (becomes 2/3)") {
                    game.state.projectedState.getPower(lynx) shouldBe 2
                    game.state.projectedState.getToughness(lynx) shouldBe 3
                }
            }
        }
    }
}
