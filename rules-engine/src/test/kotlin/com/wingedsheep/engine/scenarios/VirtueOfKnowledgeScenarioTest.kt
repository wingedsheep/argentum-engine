package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class VirtueOfKnowledgeScenarioTest : ScenarioTestBase() {

    init {
        context("Virtue of Knowledge") {
            test("a permanent entering causes an ETB ability of your permanent to trigger twice") {
                val game = scenario()
                    .withPlayers("Virtue", "Opponent")
                    .withCardOnBattlefield(1, "Virtue of Knowledge")
                    .withCardInHand(1, "Vampire Spawn")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Vampire Spawn").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 24
                game.getLifeTotal(2) shouldBe 16
            }

            test("an opponent's permanent entering can also cause your ability to trigger twice") {
                val game = scenario()
                    .withPlayers("Virtue", "Opponent")
                    .withCardOnBattlefield(1, "Virtue of Knowledge")
                    .withCardOnBattlefield(1, "Soul Warden")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 22
            }
        }
    }
}
