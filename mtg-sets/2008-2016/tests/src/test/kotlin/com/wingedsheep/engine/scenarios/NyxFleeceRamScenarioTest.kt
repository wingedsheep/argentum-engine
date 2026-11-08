package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Nyx-Fleece Ram (JOU #18) — {1}{W} Enchantment Creature — Sheep 0/5.
 * "At the beginning of your upkeep, you gain 1 life."
 */
class NyxFleeceRamScenarioTest : ScenarioTestBase() {
    init {
        test("its controller gains 1 life at the beginning of their upkeep") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nyx-Fleece Ram")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 21
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
