package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Skyspear Cavalry (JOU #26) — {3}{W}{W} Creature — Human Soldier 2/2.
 *
 *   Flying
 *   Double strike
 */
class SkyspearCavalryScenarioTest : ScenarioTestBase() {
    init {
        test("unblocked, it deals its 2 damage twice") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Skyspear Cavalry")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Skyspear Cavalry" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("first-strike and regular damage both landed: 2 + 2") {
                game.getLifeTotal(2) shouldBe 16
            }
        }
    }
}
