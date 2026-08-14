package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for The Speed Demon (DFT #105).
 *
 * The Speed Demon {3}{B}{B} — Legendary Creature — Demon 5/5
 * Flying, trample
 * Start your engines!
 * At the beginning of your end step, you draw X cards and lose X life, where X is your speed.
 *
 * The load-bearing claim is that X reads the *controller's* speed at resolution, so the draw and
 * the life loss scale together and move in lockstep with the speed track — 1 card / 1 life fresh
 * off start your engines!, 4 / 4 at max speed.
 */
class TheSpeedDemonScenarioTest : ScenarioTestBase() {

    init {
        context("The Speed Demon") {

            test("draws and loses one at the starting speed its own keyword confers") {
                val game = demonGame()
                withClue("start your engines! put the controller at speed 1") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }

                val handBefore = game.handSize(1)
                val lifeBefore = game.getLifeTotal(1)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 1
                game.getLifeTotal(1) shouldBe lifeBefore - 1
            }

            test("scales to four cards and four life at max speed") {
                val game = demonGame()
                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                val handBefore = game.handSize(1)
                val lifeBefore = game.getLifeTotal(1)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("X is your speed, so both halves read 4") {
                    game.handSize(1) shouldBe handBefore + 4
                    game.getLifeTotal(1) shouldBe lifeBefore - 4
                }
            }

            test("the opponent's end step is not yours") {
                val game = demonGame()
                val handBefore = game.handSize(1)
                val lifeBefore = game.getLifeTotal(1)

                // Run out player 1's turn, then stop in player 2's end step.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                val handAfterOwnTurn = game.handSize(1)
                val lifeAfterOwnTurn = game.getLifeTotal(1)
                withClue("the controller's own end step did fire") {
                    handAfterOwnTurn shouldBe handBefore + 1
                    lifeAfterOwnTurn shouldBe lifeBefore - 1
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("\"your end step\" must not fire on the opponent's turn") {
                    game.handSize(1) shouldBe handAfterOwnTurn
                    game.getLifeTotal(1) shouldBe lifeAfterOwnTurn
                }
            }
        }
    }

    /** The Speed Demon on the battlefield in the controller's precombat main phase. */
    private fun demonGame(): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "The Speed Demon", summoningSickness = false)
        repeat(12) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        // Built in the upkeep and advanced, so the CR 704.5z start-your-engines state-based action
        // has run before any test reads a speed.
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }
}
