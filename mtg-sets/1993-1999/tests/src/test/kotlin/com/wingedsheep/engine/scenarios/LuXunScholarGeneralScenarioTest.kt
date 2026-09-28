package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Lu Xun, Scholar General (PTK): horsemanship; may draw when dealing damage to an opponent. */
class LuXunScholarGeneralScenarioTest : ScenarioTestBase() {
    init {
        test("damage to opponent lets controller draw") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lu Xun, Scholar General", summoningSickness = false)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val before = game.state.getZone(game.player1Id, com.wingedsheep.sdk.core.Zone.HAND).size
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Lu Xun, Scholar General" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()
            if (game.hasPendingDecision()) game.answerYesNo(true)
            game.resolveStack()

            game.state.getZone(game.player1Id, com.wingedsheep.sdk.core.Zone.HAND).size shouldBe before + 1
        }
    }
}
