package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Eldrazi Linebreaker (MH3 #117) — {1}{C}{R} Creature — Eldrazi 3/3
 *
 *   Devoid
 *   Trample
 *   At the beginning of combat on your turn, target creature you control gains haste and gets
 *   +X/+0 until end of turn, where X is the number of Eldrazi you control.
 */
class EldraziLinebreakerScenarioTest : ScenarioTestBase() {

    init {
        context("Eldrazi Linebreaker") {

            test("beginning of combat gives the target haste and +X/+0 counting only your Eldrazi") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Eldrazi Linebreaker", summoningSickness = false)
                    .withCardOnBattlefield(1, "Eldrazi Ravager", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withCardOnBattlefield(2, "Nulldrifter", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                withClue("the trigger asks for a target creature you control") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("Bears gains haste") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
                withClue("X = 2 (Linebreaker + Ravager; the opponent's Nulldrifter doesn't count)") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
            }

            test("does not trigger on the opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Eldrazi Linebreaker", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                game.state.projectedState.getPower(game.findPermanent("Eldrazi Linebreaker")!!) shouldBe 3
            }
        }
    }
}
