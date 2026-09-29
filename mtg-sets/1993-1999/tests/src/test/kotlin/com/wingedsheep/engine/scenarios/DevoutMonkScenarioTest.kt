package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class DevoutMonkScenarioTest : ScenarioTestBase() {
    init {
        test("entering triggers one life for its controller only when the trigger resolves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Devout Monk")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Devout Monk").error shouldBe null
            game.getLifeTotal(1) shouldBe 20
            game.passPriority().error shouldBe null
            game.passPriority().error shouldBe null

            game.isOnBattlefield("Devout Monk") shouldBe true
            game.getLifeTotal(1) shouldBe 20
            game.state.stack.size shouldBe 1

            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21
            game.getLifeTotal(2) shouldBe 20
        }

        test("another creature entering does not trigger the monk") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Devout Monk")
                .withCardInHand(1, "Eager Cadet")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Eager Cadet").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Eager Cadet") shouldBe true
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
            game.state.stack.size shouldBe 0
        }
    }
}
