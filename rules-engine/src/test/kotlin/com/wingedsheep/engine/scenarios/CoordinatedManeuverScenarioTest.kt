package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Coordinated Maneuver. */
class CoordinatedManeuverScenarioTest : ScenarioTestBase() {

    init {
        context("Coordinated Maneuver") {

            test("damage mode deals damage equal to the number of creatures you control") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Coordinated Maneuver")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 — two creatures you control
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 enemy target (will take 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val enemy = game.findPermanents("Glory Seeker")
                    .first { game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player2Id }

                // Mode 0 = damage to creature/planeswalker. 2 creatures controlled → 2 damage → kills the 2/2.
                game.castSpellWithMode(1, "Coordinated Maneuver", modeIndex = 0, targetId = enemy)
                game.resolveStack()

                withClue("Enemy 2/2 should be dead after taking 2 damage (2 creatures you control)") {
                    game.findPermanents("Glory Seeker")
                        .none { game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player2Id } shouldBe true
                }
            }

            test("destroy-enchantment mode destroys target enchantment") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Coordinated Maneuver")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Grand Melee") // enchantment
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val melee = game.findPermanent("Grand Melee")!!
                game.castSpellWithMode(1, "Coordinated Maneuver", modeIndex = 1, targetId = melee)
                game.resolveStack()

                withClue("Grand Melee should be destroyed") {
                    game.findPermanent("Grand Melee") shouldBe null
                }
            }
        }
    }
}
