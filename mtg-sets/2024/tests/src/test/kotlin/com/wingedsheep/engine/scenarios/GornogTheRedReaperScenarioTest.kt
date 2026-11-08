package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gornog, the Red Reaper (J25 #49):
 *   "Haste. Cowards can't block Warriors. Whenever one or more Warriors you control attack a player,
 *    target creature that player controls becomes a Coward. Attacking Warriors you control get
 *    +X/+0, where X is the number of Cowards your opponents control."
 */
class GornogTheRedReaperScenarioTest : ScenarioTestBase() {

    private fun TestGame.attackWithGornogAndCowardBears() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Gornog, the Red Reaper" to 2)).error shouldBe null

        val decision = state.pendingDecision as ChooseTargetsDecision
        withClue("only creatures the attacked player controls are legal targets") {
            decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(
                findPermanent("Grizzly Bears")!!,
                findPermanent("Raging Goblin")!!
            )
        }
        selectTargets(listOf(findPermanent("Grizzly Bears")!!)).error shouldBe null
        resolveStack()
    }

    init {
        context("Gornog, the Red Reaper") {

            test("the attack trigger turns the target into only a Coward, which pumps Gornog and can't block it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Gornog, the Red Reaper", summoningSickness = true)
                    .withCardOnBattlefield(1, "Goblin Piker", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Raging Goblin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attackWithGornogAndCowardBears()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("becoming a Coward replaces the Bears' other creature types") {
                    game.state.projectedState.getSubtypes(bears) shouldBe setOf("Coward")
                }
                withClue("the attacking Warrior gets +1/+0 for the one Coward Bob controls") {
                    game.state.projectedState.getPower(game.findPermanent("Gornog, the Red Reaper")!!) shouldBe 3
                }
                withClue("a Warrior you control that isn't attacking gets no bonus") {
                    game.state.projectedState.getPower(game.findPermanent("Goblin Piker")!!) shouldBe 2
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("a Coward can't block a Warrior") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Gornog, the Red Reaper"))).error shouldNotBe null
                }
                withClue("a non-Coward still can") {
                    game.declareBlockers(mapOf("Raging Goblin" to listOf("Gornog, the Red Reaper"))).error shouldBe null
                }
            }

            test("the Coward type lasts past the turn, while the bonus only applies to attackers") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Gornog, the Red Reaper", summoningSickness = true)
                    .withCardOnBattlefield(1, "Goblin Piker", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Raging Goblin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attackWithGornogAndCowardBears()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("out of combat Gornog is back to its printed power") {
                    game.state.projectedState.getPower(game.findPermanent("Gornog, the Red Reaper")!!) shouldBe 2
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the type change has no duration, so the Bears are still a Coward on Bob's turn") {
                    game.state.projectedState.getSubtypes(game.findPermanent("Grizzly Bears")!!) shouldBe setOf("Coward")
                }
            }
        }
    }
}
