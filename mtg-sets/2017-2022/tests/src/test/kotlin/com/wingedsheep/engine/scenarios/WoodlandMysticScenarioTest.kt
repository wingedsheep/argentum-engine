package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Woodland Mystic (M20 #344) — {1}{G} 1/1 Creature — Elf Druid.
 *
 *   {T}: Add {G}.
 */
class WoodlandMysticScenarioTest : ScenarioTestBase() {

    init {
        context("Woodland Mystic") {
            test("taps for green mana to help cast a spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Woodland Mystic")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
