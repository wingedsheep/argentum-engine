package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Faithbearer Paladin (EMN #25) — {4}{W} Creature — Human Knight, 3/4.
 *
 *   Lifelink
 *
 * Its unblocked combat damage gains its controller that much life.
 */
class FaithbearerPaladinScenarioTest : ScenarioTestBase() {

    init {
        test("unblocked combat damage gains its controller 3 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Faithbearer Paladin")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Faithbearer Paladin" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 23
        }
    }
}
