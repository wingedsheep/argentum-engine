package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Zurgo's Vanguard. */
class ZurgosVanguardScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Zurgo's Vanguard") {

            test("power equals the number of creatures you control; toughness stays 3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Zurgo's Vanguard")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vanguard = game.findPermanent("Zurgo's Vanguard")!!
                val projected = stateProjector.project(game.state)
                withClue("Power = 3 creatures you control (Vanguard + Bears + Giant); opponent's creature excluded") {
                    projected.getPower(vanguard) shouldBe 3
                }
                withClue("Toughness is the fixed printed 3") {
                    projected.getToughness(vanguard) shouldBe 3
                }
            }
        }
    }
}
