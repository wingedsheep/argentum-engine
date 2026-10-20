package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Arcades Sabboth (LEG #218).
 *
 * "Each untapped creature you control gets +0/+2 as long as it's not attacking."
 *
 * Tapped creatures never get the bonus; a vigilant attacker stays untapped but loses it while
 * it's attacking, and the opponent's creatures never get it.
 */
class ArcadesSabbothScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Arcades", "Opponent")
        .withCardOnBattlefield(1, "Arcades Sabboth")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Savannah Lions", tapped = true)
        .withCardOnBattlefield(1, "Serra Angel")
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Arcades Sabboth") {

            test("untapped creatures you control get +0/+2; tapped ones and opponents' don't") {
                val game = board()
                val projected = game.state.projectedState

                withClue("Arcades Sabboth buffs itself") {
                    projected.getToughness(game.findPermanent("Arcades Sabboth")!!) shouldBe 9
                }
                withClue("untapped Grizzly Bears") {
                    projected.getToughness(game.findPermanent("Grizzly Bears")!!) shouldBe 4
                }
                withClue("tapped Savannah Lions") {
                    projected.getToughness(game.findPermanent("Savannah Lions")!!) shouldBe 1
                }
                withClue("the opponent's Hill Giant") {
                    projected.getToughness(game.findPermanent("Hill Giant")!!) shouldBe 3
                }
            }

            test("a vigilant attacker stays untapped but loses the bonus while attacking") {
                val game = board()
                val angel = game.findPermanent("Serra Angel")!!

                game.state.projectedState.getToughness(angel) shouldBe 6

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Serra Angel" to 2)).error shouldBe null

                withClue("attacking Serra Angel is back to its printed toughness") {
                    game.state.projectedState.getToughness(angel) shouldBe 4
                }
                withClue("the non-attacking Grizzly Bears keep the bonus") {
                    game.state.projectedState.getToughness(game.findPermanent("Grizzly Bears")!!) shouldBe 4
                }
            }
        }
    }
}
