package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Fog of War (BRO #180) — gain 1 life per creature on the battlefield (both sides), and prevent
 * combat damage this turn from creatures with power 3 or less only.
 */
class FogOfWarScenarioTest : ScenarioTestBase() {

    init {
        test("gains life for every creature and only stops combat damage from power 3 or less") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Fog of War")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Fog of War").error shouldBe null
            game.resolveStack()

            // Grizzly Bears, Craw Wurm, and the opponent's Hill Giant.
            game.getLifeTotal(1) shouldBe 23

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Craw Wurm" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            // The 2-power Bears is prevented; the 6-power Wurm is not.
            game.getLifeTotal(2) shouldBe 14
        }
    }
}
