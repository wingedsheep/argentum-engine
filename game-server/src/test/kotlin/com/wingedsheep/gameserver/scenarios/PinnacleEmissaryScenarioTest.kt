package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Pinnacle Emissary.
 *
 * Card reference:
 * - Pinnacle Emissary ({1}{U}{R}): Artifact Creature — Robot, 3/3
 *   "Whenever you cast an artifact spell, create a 1/1 colorless Drone artifact creature token
 *    with flying and 'This token can block only creatures with flying.'"
 *   "Warp {U/R}"
 *
 * Also exercises the CreatePredefinedTokenExecutor summoning-sickness fix: the Drone token
 * created by Emissary's trigger must enter with SummoningSicknessComponent so it can't attack
 * on the turn it was created.
 */
class PinnacleEmissaryScenarioTest : ScenarioTestBase() {

    init {
        context("Pinnacle Emissary trigger — whenever you cast an artifact spell") {

            test("casting an artifact spell creates a Drone token that has summoning sickness") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Pinnacle Emissary")
                    .withCardInHand(1, "Cryogen Relic")            // {1}{U}, MV 2 artifact
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Cryogen Relic")
                withClue("Casting Cryogen Relic should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("A Drone token should be on the caster's battlefield") {
                    game.isOnBattlefield("Drone") shouldBe true
                }

                // Regression for CreatePredefinedTokenExecutor: the Drone must enter with
                // SummoningSicknessComponent. Without this, a Drone created mid-turn could
                // attack on the same turn it was minted, violating CR 302.1.
                val droneId = game.findPermanent("Drone")!!
                val drone = game.state.getEntity(droneId)
                withClue("Drone should have SummoningSicknessComponent on the turn it ETBs") {
                    drone?.has<SummoningSicknessComponent>() shouldBe true
                }
            }

            test("casting a non-artifact spell does not create a Drone token") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Pinnacle Emissary")
                    .withCardInHand(1, "Glory Seeker")             // {1}{W} creature, not an artifact
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Glory Seeker")
                withClue("Casting Glory Seeker should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("No Drone token should exist — Glory Seeker is not an artifact spell") {
                    game.isOnBattlefield("Drone") shouldBe false
                }
            }
        }
    }
}
