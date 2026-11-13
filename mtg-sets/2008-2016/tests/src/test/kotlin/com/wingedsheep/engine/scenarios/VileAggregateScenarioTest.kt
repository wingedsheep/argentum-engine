package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vile Aggregate (BFZ #137): "Vile Aggregate's power is equal to the number of colorless creatures
 * you control."
 *
 * The characteristic-defining power reads projected colour: Vile Aggregate and other devoid
 * creatures are colorless only through their devoid effect, so they count; a coloured creature and
 * an opponent's colorless creature don't.
 */
class VileAggregateScenarioTest : ScenarioTestBase() {

    init {
        context("Vile Aggregate") {

            test("alone, it counts itself") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Vile Aggregate")
                    .build()

                val aggregate = game.findPermanent("Vile Aggregate")!!
                game.state.projectedState.getPower(aggregate) shouldBe 1
                game.state.projectedState.getToughness(aggregate) shouldBe 5
            }

            test("counts your devoid creatures, not coloured ones or the opponent's colorless ones") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Vile Aggregate")
                    .withCardOnBattlefield(1, "Culling Drone") // {1}{B} devoid: colorless
                    .withCardOnBattlefield(1, "Grizzly Bears") // green
                    .withCardOnBattlefield(2, "Dominator Drone") // devoid, opponent's
                    .build()

                val aggregate = game.findPermanent("Vile Aggregate")!!
                withClue("itself + Culling Drone; not Grizzly Bears or the opponent's Dominator Drone") {
                    game.state.projectedState.getPower(aggregate) shouldBe 2
                }
                game.state.projectedState.getToughness(aggregate) shouldBe 5
            }
        }
    }
}
