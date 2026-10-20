package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rohgahh of Kher Keep (LEG #255).
 *
 * "At the beginning of your upkeep, you may pay {R}{R}{R}. If you don't, tap Rohgahh and all
 *  creatures named Kobolds of Kher Keep, then an opponent gains control of them.
 *  Creatures you control named Kobolds of Kher Keep get +2/+2."
 *
 * Worth pinning: declining reaches *every* Kobolds of Kher Keep, the opponent's included (it is
 * tapped too), and the whole group — Rohgahh with it — ends up under the opponent, where the
 * name-filtered anthem now pumps the opponent's Kobolds instead.
 */
class RohgahhOfKherKeepScenarioTest : ScenarioTestBase() {

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    private fun controllerOf(game: TestGame, id: EntityId): EntityId? =
        game.state.projectedState.getController(id)

    private fun atPlayer1Upkeep(): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Rohgahh of Kher Keep")
            .withCardOnBattlefield(1, "Kobolds of Kher Keep")
            .withCardOnBattlefield(2, "Kobolds of Kher Keep")
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { builder = builder.withCardInLibrary(1, "Mountain") }
        repeat(5) { builder = builder.withCardInLibrary(2, "Mountain") }
        val game = builder.build()
        game.passUntilPhase(Phase.ENDING, Step.END)
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    private fun koboldsControlledBy(game: TestGame, player: EntityId): List<EntityId> =
        game.findPermanents("Kobolds of Kher Keep").filter { controllerOf(game, it) == player }

    init {
        context("Rohgahh of Kher Keep") {

            test("the anthem pumps only the Kobolds its controller has") {
                val game = atPlayer1Upkeep()
                val mine = koboldsControlledBy(game, game.player1Id).single()
                val theirs = koboldsControlledBy(game, game.player2Id).single()

                game.state.projectedState.getPower(mine) shouldBe 2
                game.state.projectedState.getToughness(mine) shouldBe 3
                game.state.projectedState.getPower(theirs) shouldBe 0
                game.state.projectedState.getToughness(theirs) shouldBe 1
            }

            test("declining taps Rohgahh and every Kobolds of Kher Keep, then the opponent gains all of them") {
                val game = atPlayer1Upkeep()
                val rohgahh = game.findPermanent("Rohgahh of Kher Keep")!!
                val mine = koboldsControlledBy(game, game.player1Id).single()
                val theirs = koboldsControlledBy(game, game.player2Id).single()

                game.answerYesNo(false)
                game.resolveStack()

                withClue("Rohgahh and both Kobolds are tapped — the opponent's included") {
                    isTapped(game, rohgahh) shouldBe true
                    isTapped(game, mine) shouldBe true
                    isTapped(game, theirs) shouldBe true
                }
                withClue("the opponent controls Rohgahh and every Kobolds of Kher Keep") {
                    controllerOf(game, rohgahh) shouldBe game.player2Id
                    controllerOf(game, mine) shouldBe game.player2Id
                    controllerOf(game, theirs) shouldBe game.player2Id
                }
                withClue("the anthem follows Rohgahh to its new controller's Kobolds") {
                    game.state.projectedState.getPower(mine) shouldBe 2
                    game.state.projectedState.getPower(theirs) shouldBe 2
                }
            }

            test("paying {R}{R}{R} keeps everything where it was") {
                val game = atPlayer1Upkeep()
                val rohgahh = game.findPermanent("Rohgahh of Kher Keep")!!
                val mine = koboldsControlledBy(game, game.player1Id).single()
                val theirs = koboldsControlledBy(game, game.player2Id).single()

                game.answerYesNo(true)
                game.resolveStack()

                withClue("nothing is tapped") {
                    isTapped(game, rohgahh) shouldBe false
                    isTapped(game, mine) shouldBe false
                    isTapped(game, theirs) shouldBe false
                }
                withClue("control is unchanged") {
                    controllerOf(game, rohgahh) shouldBe game.player1Id
                    controllerOf(game, mine) shouldBe game.player1Id
                    controllerOf(game, theirs) shouldBe game.player2Id
                }
            }
        }
    }
}
