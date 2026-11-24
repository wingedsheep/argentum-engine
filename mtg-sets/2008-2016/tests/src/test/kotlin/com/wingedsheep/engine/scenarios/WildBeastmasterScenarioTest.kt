package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Wild Beastmaster (RTR) — whenever it attacks, each other creature you control gets +X/+X until
 * end of turn, where X is its power. X is read as the trigger resolves and then locked in.
 */
class WildBeastmasterScenarioTest : ScenarioTestBase() {
    init {
        test("other creatures get +X/+X equal to the Beastmaster's power, and X stays locked") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Wild Beastmaster")
                .withCardOnBattlefield(1, "Craw Wurm", tapped = true)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val beastmaster = game.findPermanent("Wild Beastmaster")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            val theirBears = game.findPermanent("Grizzly Bears")!!

            // Beastmaster becomes 4/4 before combat.
            game.castSpell(1, "Giant Growth", beastmaster).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Wild Beastmaster" to 2)).error shouldBe null
            game.resolveStack()

            // X = 4: the tapped, non-attacking Wurm gets +4/+4; the Beastmaster and the opponent's Bears don't.
            game.state.projectedState.getPower(wurm) shouldBe 10
            game.state.projectedState.getToughness(wurm) shouldBe 8
            game.state.projectedState.getPower(beastmaster) shouldBe 4
            game.state.projectedState.getPower(theirBears) shouldBe 2

            // Pumping the Beastmaster after the trigger resolved doesn't change X.
            game.castSpell(1, "Giant Growth", beastmaster).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(beastmaster) shouldBe 7
            game.state.projectedState.getPower(wurm) shouldBe 10
        }
    }
}
