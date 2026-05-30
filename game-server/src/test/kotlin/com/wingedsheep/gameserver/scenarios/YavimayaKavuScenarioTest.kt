package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Yavimaya Kavu's characteristic-defining ability:
 * - power = number of red creatures on the battlefield
 * - toughness = number of green creatures on the battlefield
 *
 * Yavimaya Kavu is itself both red and green, so it counts toward both.
 */
class YavimayaKavuScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Yavimaya Kavu CDA") {

            test("counts itself when alone (1 red, 1 green)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Yavimaya Kavu")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kavuId = game.findPermanent("Yavimaya Kavu")!!
                val projected = stateProjector.project(game.state)

                withClue("Power = number of red creatures (just itself)") {
                    projected.getPower(kavuId) shouldBe 1
                }
                withClue("Toughness = number of green creatures (just itself)") {
                    projected.getToughness(kavuId) shouldBe 1
                }
            }

            test("counts red and green creatures from all players") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Yavimaya Kavu")
                    .withCardOnBattlefield(1, "Raging Goblin")    // red, controlled by P1
                    .withCardOnBattlefield(2, "Grizzly Bears")    // green, controlled by P2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kavuId = game.findPermanent("Yavimaya Kavu")!!
                val projected = stateProjector.project(game.state)

                withClue("Power = red creatures: Yavimaya Kavu + Raging Goblin = 2") {
                    projected.getPower(kavuId) shouldBe 2
                }
                withClue("Toughness = green creatures: Yavimaya Kavu + Grizzly Bears = 2") {
                    projected.getToughness(kavuId) shouldBe 2
                }
            }
        }
    }
}
