package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Smoldering Efreet ({1}{R}, 2/2): "When this creature dies, it deals 2 damage to you."
 */
class SmolderingEfreetScenarioTest : ScenarioTestBase() {

    init {
        context("Smoldering Efreet") {

            test("when it dies it deals 2 damage to its controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Smoldering Efreet")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val efreet = game.findPermanent("Smoldering Efreet")!!
                game.castSpell(2, "Lightning Bolt", targetId = efreet).error shouldBe null
                game.resolveStack()

                withClue("the Efreet died") {
                    game.isInGraveyard(1, "Smoldering Efreet") shouldBe true
                }
                withClue("its controller takes 2; the Bolt's caster takes nothing") {
                    game.getLifeTotal(1) shouldBe 18
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
