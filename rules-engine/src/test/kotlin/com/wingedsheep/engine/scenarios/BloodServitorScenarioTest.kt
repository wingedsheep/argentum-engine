package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Blood Servitor (VOW #252) — {3} Artifact Creature — Construct, 2/2.
 *
 *   When this creature enters, create a Blood token.
 *
 * Exercises the ETB Blood token creation, the only thing this vanilla-bodied artifact creature
 * does.
 */
class BloodServitorScenarioTest : ScenarioTestBase() {

    init {
        context("Blood Servitor") {

            test("entering the battlefield creates a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Blood Servitor")
                    .withLandsOnBattlefield(1, "Wastes", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Blood Servitor").error shouldBe null
                game.resolveStack()

                withClue("Blood Servitor is on the battlefield") {
                    game.isOnBattlefield("Blood Servitor") shouldBe true
                }
                withClue("a Blood token is created on entering the battlefield") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }
        }
    }
}
