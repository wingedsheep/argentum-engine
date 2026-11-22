package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Fortress Cyclops (GTC) — gets +3/+0 when it attacks and +0/+3 when it blocks.
 */
class FortressCyclopsScenarioTest : ScenarioTestBase() {
    init {
        test("attacking makes it 6/3") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Fortress Cyclops")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val cyclops = game.findPermanent("Fortress Cyclops")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Fortress Cyclops" to 2)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(cyclops) shouldBe 6
            game.state.projectedState.getToughness(cyclops) shouldBe 3
        }

        test("blocking makes it 3/6") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Fortress Cyclops")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val cyclops = game.findPermanent("Fortress Cyclops")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Craw Wurm" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Fortress Cyclops" to listOf("Craw Wurm"))).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(cyclops) shouldBe 3
            game.state.projectedState.getToughness(cyclops) shouldBe 6
        }
    }
}
