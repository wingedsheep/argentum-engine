package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Prized Griffin (ELD #24) — {4}{W} Creature — Griffin 3/4. "Flying"
 *
 * A ground creature can't block it, so it connects for 3.
 */
class PrizedGriffinScenarioTest : ScenarioTestBase() {

    init {
        context("Prized Griffin") {

            test("has flying, can't be blocked by a ground creature, and deals 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Prized Griffin")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val griffin = game.findPermanent("Prized Griffin")!!
                game.state.projectedState.hasKeyword(griffin, Keyword.FLYING) shouldBe true
                game.state.projectedState.getPower(griffin) shouldBe 3
                game.state.projectedState.getToughness(griffin) shouldBe 4

                game.declareAttackers(mapOf("Prized Griffin" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Prized Griffin"))).error shouldNotBe null
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                game.getLifeTotal(2) shouldBe 17
            }
        }
    }
}
