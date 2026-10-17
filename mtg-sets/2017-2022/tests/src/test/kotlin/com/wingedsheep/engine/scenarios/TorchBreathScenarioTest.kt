package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Torch Breath (SNC #127).
 *
 * "This spell costs {2} less to cast if it targets a blue permanent." The reduction applies to the
 * total cost with the announced X in it (CR 601.2f), so against a blue target X = 2 costs only {R}
 * even though the printed cost has no generic mana of its own.
 */
class TorchBreathScenarioTest : ScenarioTestBase() {

    private fun game(mountains: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Torch Breath")
        .withLandsOnBattlefield(1, "Mountain", mountains)
        .withCardOnBattlefield(2, "Wind Drake")
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.torchOffer() =
        getLegalActions(1).firstOrNull { it.actionType == "CastSpell" && it.description.contains("Torch Breath") }

    init {
        context("Torch Breath") {

            test("against a blue permanent the discount pays the X: X = 2 for just {R}") {
                val game = game(mountains = 1)
                val drake = game.findPermanent("Wind Drake").shouldNotBeNull()
                withClue("cast should succeed") { game.castXSpell(1, "Torch Breath", 2, drake).error shouldBe null }
                game.resolveStack()
                game.isOnBattlefield("Wind Drake") shouldBe false
            }

            test("the discount needs a blue target: X = 2 at a non-blue creature is unaffordable on one land") {
                val game = game(mountains = 1)
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                game.castXSpell(1, "Torch Breath", 2, giant).error shouldNotBe null
                game.isOnBattlefield("Hill Giant") shouldBe true
            }

            test("past the discount X is paid in mana: X = 3 at a blue target costs {1}{R}") {
                val game = game(mountains = 2)
                val drake = game.findPermanent("Wind Drake").shouldNotBeNull()
                game.castXSpell(1, "Torch Breath", 3, drake).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Wind Drake") shouldBe false
            }

            test("the offer's X ceiling counts the discount when a blue target exists") {
                val game = game(mountains = 1)
                game.torchOffer().shouldNotBeNull().maxAffordableX shouldBe 2
            }
        }
    }
}
