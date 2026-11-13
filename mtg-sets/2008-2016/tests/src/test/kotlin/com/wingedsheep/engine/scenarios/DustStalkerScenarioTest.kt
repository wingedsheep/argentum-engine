package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dust Stalker (BFZ #202).
 *
 * "At the beginning of each end step, if you control no other colorless creatures, return this
 *  creature to its owner's hand."
 *
 * The intervening "if" reads projected colours over the controller's *other* creatures: a devoid
 * creature (colorless only through Devoid) keeps the Stalker in play, a coloured creature doesn't,
 * and the Stalker's own colorlessness never satisfies the check. "Each end step" includes the
 * opponent's.
 */
class DustStalkerScenarioTest : ScenarioTestBase() {

    init {
        context("Dust Stalker") {

            fun game(activePlayer: Int, vararg others: String) = run {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dust Stalker", summoningSickness = false)
                    .withActivePlayer(activePlayer)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                others.forEach { builder = builder.withCardOnBattlefield(1, it, summoningSickness = false) }
                repeat(5) { builder = builder.withCardInLibrary(1, "Grizzly Bears") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Grizzly Bears") }
                builder.build()
            }

            test("with no other colorless creature it returns to hand at an opponent's end step") {
                val game = game(activePlayer = 2, "Grizzly Bears")

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("a coloured creature doesn't count, and the Stalker doesn't count itself") {
                    game.isInHand(1, "Dust Stalker") shouldBe true
                }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("another devoid creature keeps it on the battlefield through its controller's end step") {
                val game = game(activePlayer = 1, "Culling Drone")

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Culling Drone is colorless through Devoid, so the ability doesn't trigger") {
                    game.isOnBattlefield("Dust Stalker") shouldBe true
                    game.isInHand(1, "Dust Stalker") shouldBe false
                }
            }
        }
    }
}
