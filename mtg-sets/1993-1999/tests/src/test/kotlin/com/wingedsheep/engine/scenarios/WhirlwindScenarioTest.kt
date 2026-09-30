package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class WhirlwindScenarioTest : ScenarioTestBase() {
    init {
        test("destroys flying creatures on both sides and leaves ground creatures") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Whirlwind")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(1, "Royal Falcon")
                .withCardOnBattlefield(2, "Wild Griffin")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hollow Dogs")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Whirlwind").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Royal Falcon") shouldBe true
            game.isInGraveyard(2, "Wild Griffin") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hollow Dogs") shouldBe true
            game.isInGraveyard(1, "Whirlwind") shouldBe true
        }

        test("resolves without flying creatures") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Whirlwind")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Whirlwind").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Whirlwind") shouldBe true
        }
    }
}
