package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class PierceStriderScenarioTest : ScenarioTestBase() {
    init {
        test("entering makes the targeted opponent lose three life without draining it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Pierce Strider")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Pierce Strider").error shouldBe null
            game.resolveStack()
            if (game.state.pendingDecision is ChooseTargetsDecision) {
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()
            }

            game.isOnBattlefield("Pierce Strider") shouldBe true
            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
