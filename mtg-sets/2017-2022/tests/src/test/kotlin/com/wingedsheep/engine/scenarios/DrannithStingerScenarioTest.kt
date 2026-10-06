package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Drannith Stinger (IKO) — "Whenever you cycle another card, this creature
 * deals 1 damage to each opponent." Cycling {1}.
 *
 * The regression this guards: the cycled-card trigger pass used to fire every cycle trigger on
 * the cycled card, so cycling a Stinger from hand pinged the opponent off its own observer ability.
 */
class DrannithStingerScenarioTest : ScenarioTestBase() {

    private fun setup(stingerOnBattlefield: Boolean, inHand: String): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, inHand)
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (stingerOnBattlefield) builder = builder.withCardOnBattlefield(1, "Drannith Stinger")
        repeat(3) { builder = builder.withCardInLibrary(1, "Mountain") }
        repeat(3) { builder = builder.withCardInLibrary(2, "Forest") }
        return builder.build()
    }

    init {
        context("Drannith Stinger") {
            test("cycling another card deals 1 damage to each opponent") {
                val game = setup(stingerOnBattlefield = true, inHand = "Lava Serpent")

                game.cycleCard(1, "Lava Serpent").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
                game.getLifeTotal(1) shouldBe 20
            }

            test("cycling the Stinger itself from hand does not trigger it") {
                val game = setup(stingerOnBattlefield = false, inHand = "Drannith Stinger")
                val handBefore = game.handSize(1)

                game.cycleCard(1, "Drannith Stinger").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Drannith Stinger") shouldBe true
                game.handSize(1) shouldBe handBefore
                withClue("the cycled Stinger's observer ability isn't functioning") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("cycling a second Stinger triggers only the one on the battlefield") {
                val game = setup(stingerOnBattlefield = true, inHand = "Drannith Stinger")

                game.cycleCard(1, "Drannith Stinger").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
            }
        }
    }
}
