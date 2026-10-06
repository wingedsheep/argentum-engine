package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Demonic Hordes (LEA #103).
 *
 * "At the beginning of your upkeep, unless you pay {B}{B}{B}, tap this creature and sacrifice a
 *  land of an opponent's choice."
 *
 * The part worth pinning is who picks: declining hands the choice to the *opponent*, who picks
 * among the Demon controller's lands, and only that land is sacrificed.
 */
class DemonicHordesScenarioTest : ScenarioTestBase() {

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    private fun atPlayer1Upkeep(): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Demonic Hordes", summoningSickness = false)
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardOnBattlefield(1, "Forest")
            .withLandsOnBattlefield(2, "Mountain", 1)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
        repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
        val game = builder.build()
        game.passUntilPhase(Phase.ENDING, Step.END)
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        context("Demonic Hordes upkeep tax") {

            test("declining taps the Demon and the opponent picks which of my lands I sacrifice") {
                val game = atPlayer1Upkeep()
                val demon = game.findPermanent("Demonic Hordes")!!
                val forest = game.findPermanent("Forest")!!

                game.answerYesNo(false)

                val decision = game.getPendingDecision()
                withClue("the opponent, not the controller, makes the land choice") {
                    (decision != null) shouldBe true
                    decision!!.playerId shouldBe game.player2Id
                }
                game.selectCards(listOf(forest))
                game.resolveStack()

                withClue("Demonic Hordes is tapped") {
                    isTapped(game, demon) shouldBe true
                }
                withClue("only the chosen land is sacrificed") {
                    game.isOnBattlefield("Forest") shouldBe false
                    game.isInGraveyard(1, "Forest") shouldBe true
                    game.findPermanents("Swamp").size shouldBe 3
                }
                withClue("the opponent's own land is never a candidate") {
                    game.isOnBattlefield("Mountain") shouldBe true
                }
            }

            test("paying {B}{B}{B} spares both the Demon and the lands") {
                val game = atPlayer1Upkeep()
                val demon = game.findPermanent("Demonic Hordes")!!

                game.answerYesNo(true)
                game.resolveStack()

                withClue("Demonic Hordes stays untapped") {
                    isTapped(game, demon) shouldBe false
                }
                withClue("no land is sacrificed") {
                    game.isOnBattlefield("Forest") shouldBe true
                    game.findPermanents("Swamp").size shouldBe 3
                }
            }
        }
    }
}
