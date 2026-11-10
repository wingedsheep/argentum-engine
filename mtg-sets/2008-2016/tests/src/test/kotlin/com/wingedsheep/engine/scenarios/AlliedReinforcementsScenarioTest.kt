package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

/**
 * Allied Reinforcements — {3}{W} Sorcery. "Create two 2/2 white Knight Ally creature tokens."
 */
class AlliedReinforcementsScenarioTest : ScenarioTestBase() {
    init {
        test("creates two 2/2 white Knight Ally tokens") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Allied Reinforcements")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Allied Reinforcements").error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            val tokens = game.state.getZone(game.player1Id, Zone.BATTLEFIELD)
                .filter { projected.hasSubtype(it, "Knight") }
            tokens.size shouldBe 2
            tokens.forEach { token ->
                projected.hasSubtype(token, "Ally") shouldBe true
                projected.getPower(token) shouldBe 2
                projected.getToughness(token) shouldBe 2
                projected.getColors(token) shouldBe setOf(Color.WHITE.name)
            }
            game.isInGraveyard(1, "Allied Reinforcements") shouldBe true
        }
    }
}
