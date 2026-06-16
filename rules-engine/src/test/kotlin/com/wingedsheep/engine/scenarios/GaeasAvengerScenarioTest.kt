package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Gaea's Avenger (ATQ #33).
 *
 * "Gaea's Avenger's power and toughness are each equal to 1 plus the number of artifacts
 *  your opponents control."
 *
 * Regression: a recent fix changed this CDA to count OPPONENTS' artifacts, not the
 * controller's own. These tests pin that behaviour.
 */
class GaeasAvengerScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Gaea's Avenger") {

            test("P/T = 1 + opponent's artifacts; controller's own artifacts do not count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gaea's Avenger", summoningSickness = false)
                    // Opponent (player 2) controls 2 artifacts -> P/T should be 3/3.
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Ornithopter")
                    // Player 1's own artifacts must NOT increase the stats.
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val avengerId = game.findPermanent("Gaea's Avenger")!!
                val projected = stateProjector.project(game.state)

                withClue("1 + 2 opponent artifacts = 3 power (own 3 artifacts ignored)") {
                    projected.getPower(avengerId) shouldBe 3
                }
                withClue("1 + 2 opponent artifacts = 3 toughness (own 3 artifacts ignored)") {
                    projected.getToughness(avengerId) shouldBe 3
                }
            }

            test("with zero opponent artifacts it is 1/1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gaea's Avenger", summoningSickness = false)
                    // Only the controller has an artifact; opponent has none.
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val avengerId = game.findPermanent("Gaea's Avenger")!!
                val projected = stateProjector.project(game.state)

                withClue("1 + 0 opponent artifacts = 1 power") {
                    projected.getPower(avengerId) shouldBe 1
                }
                withClue("1 + 0 opponent artifacts = 1 toughness") {
                    projected.getToughness(avengerId) shouldBe 1
                }
            }
        }
    }
}
