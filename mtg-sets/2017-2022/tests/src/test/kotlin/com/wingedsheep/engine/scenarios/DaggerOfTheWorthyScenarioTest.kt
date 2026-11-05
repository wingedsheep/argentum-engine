package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Dagger of the Worthy (HOU #160).
 *
 * Focus: the granted afflict 1 (a projected `AFFLICT_1` on a creature with no printed afflict)
 * makes the equipped creature drain the defender by 1 when it becomes blocked, and the
 * equipped creature gets +2/+0.
 */
class DaggerOfTheWorthyScenarioTest : ScenarioTestBase() {

    init {
        context("Dagger of the Worthy") {

            fun board(blocker: Boolean): TestGame {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Dagger of the Worthy", "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                if (blocker) builder.withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                return builder.build()
            }

            test("equipped creature gets +2/+0") {
                val game = board(blocker = false)
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 2
            }

            test("blocked: afflict 1 makes the defending player lose 1 life") {
                val game = board(blocker = true)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
            }

            test("unblocked: no afflict drain, just +2/+0 combat damage") {
                val game = board(blocker = false)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(emptyMap()).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                game.getLifeTotal(2) shouldBe 16
            }
        }
    }
}
