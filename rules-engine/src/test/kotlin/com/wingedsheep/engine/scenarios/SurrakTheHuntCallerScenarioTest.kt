package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Surrak, the Hunt Caller (DTK #210; reprinted in Foundations).
 *
 * Formidable — At the beginning of combat on your turn, if creatures you control have total power
 * 8 or greater, target creature you control gains haste until end of turn.
 *
 * These exercise the intervening-if total-power gate (CR 603.4) and the haste grant. The card uses
 * only existing SDK primitives (BeginCombat trigger, CompareAmounts over sumPower, GrantKeyword).
 */
class SurrakTheHuntCallerScenarioTest : ScenarioTestBase() {

    init {
        context("Surrak, the Hunt Caller") {

            test("with total power >= 8, grants haste to a chosen creature you control") {
                // Surrak (5) + two Grizzly Bears (2 each) = 9 total power >= 8.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Surrak, the Hunt Caller", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findAllPermanents("Grizzly Bears").first()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                withClue("Formidable trigger requires a target creature") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("The targeted Grizzly Bears gains haste") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
            }

            test("with total power < 8, the Formidable trigger does not fire") {
                // Surrak alone = 5 total power < 8, so the intervening-if fails.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Surrak, the Hunt Caller", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                withClue("No target decision because the total-power gate is not met") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                }
            }
        }
    }
}
