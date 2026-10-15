package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Witty Roastmaster (SNC #131) — {2}{R} 3/2 Creature — Devil Citizen.
 *
 * "Alliance — Whenever another creature you control enters, this creature deals 1 damage to each
 *  opponent."
 */
class WittyRoastmasterScenarioTest : ScenarioTestBase() {

    init {
        context("Witty Roastmaster") {
            test("another creature entering under your control deals 1 damage to each opponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Witty Roastmaster")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("the opponent takes 1; the controller is untouched") {
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
