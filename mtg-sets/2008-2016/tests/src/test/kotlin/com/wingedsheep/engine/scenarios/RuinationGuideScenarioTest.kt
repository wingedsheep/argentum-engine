package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ruination Guide (BFZ #64): "Other colorless creatures you control get +1/+0."
 *
 * The lord filters on projected colour: a devoid creature is colorless only through its devoid
 * effect, so it must get the bonus; a coloured creature, an opponent's colorless creature, and
 * the Guide itself must not.
 */
class RuinationGuideScenarioTest : ScenarioTestBase() {

    init {
        context("Ruination Guide") {

            test("pumps other colorless creatures you control, not coloured ones or itself") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Ruination Guide")
                    .withCardOnBattlefield(1, "Culling Drone") // {1}{B} devoid 2/2: colorless
                    .withCardOnBattlefield(1, "Ornithopter") // colorless artifact 0/2
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Dominator Drone") // devoid 3/2, opponent's
                    .build()

                val projected = game.state.projectedState
                val guide = game.findPermanent("Ruination Guide")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val ornithopter = game.findPermanent("Ornithopter")!!
                val myDrone = game.findPermanent("Culling Drone")!!
                val theirDrone = game.findPermanent("Dominator Drone")!!

                withClue("devoid creature you control gets +1/+0") {
                    projected.getPower(myDrone) shouldBe 3
                    projected.getToughness(myDrone) shouldBe 2
                }
                withClue("colorless artifact creature gets +1/+0") {
                    projected.getPower(ornithopter) shouldBe 1
                    projected.getToughness(ornithopter) shouldBe 2
                }
                withClue("coloured creature is unaffected") {
                    projected.getPower(bears) shouldBe 2
                }
                withClue("the Guide doesn't pump itself") {
                    projected.getPower(guide) shouldBe 3
                }
                withClue("an opponent's colorless creature is unaffected") {
                    projected.getPower(theirDrone) shouldBe 3
                }
            }
        }
    }
}
