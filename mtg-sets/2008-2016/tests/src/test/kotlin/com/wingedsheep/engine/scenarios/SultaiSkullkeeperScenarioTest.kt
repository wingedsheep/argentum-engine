package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Sultai Skullkeeper (Fate Reforged #53) — {1}{U} Creature — Snake Shaman 2/1.
 *
 * "When this creature enters, mill two cards."
 */
class SultaiSkullkeeperScenarioTest : ScenarioTestBase() {
    init {
        test("entering mills its controller's top two cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Sultai Skullkeeper")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Sultai Skullkeeper").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Sultai Skullkeeper") shouldBe true
            game.graveyardSize(1) shouldBe 2
            game.librarySize(1) shouldBe 1
            game.graveyardSize(2) shouldBe 0
        }
    }
}
