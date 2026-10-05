package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Pouncing Lynx (WAR #25, reprinted J22 #228) — {1}{W} Creature — Cat, 2/1.
 *
 *   During your turn, this creature has first strike.
 *
 * Exercises ConditionalStaticAbility(GrantKeyword(FIRST_STRIKE, Self), Conditions.IsYourTurn):
 * the keyword is present during its controller's turn and absent during the opponent's.
 */
class PouncingLynxScenarioTest : ScenarioTestBase() {

    init {
        context("Pouncing Lynx conditional first strike") {

            test("has first strike during its controller's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pouncing Lynx")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lynx = game.findPermanent("Pouncing Lynx")!!

                withClue("First strike during its controller's turn") {
                    game.state.projectedState.hasKeyword(lynx, Keyword.FIRST_STRIKE.name) shouldBe true
                }
                game.state.projectedState.getPower(lynx) shouldBe 2
                game.state.projectedState.getToughness(lynx) shouldBe 1
            }

            test("lacks first strike during the opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pouncing Lynx")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lynx = game.findPermanent("Pouncing Lynx")!!

                withClue("No first strike outside its controller's turn") {
                    game.state.projectedState.hasKeyword(lynx, Keyword.FIRST_STRIKE.name) shouldBe false
                }
            }
        }
    }
}
