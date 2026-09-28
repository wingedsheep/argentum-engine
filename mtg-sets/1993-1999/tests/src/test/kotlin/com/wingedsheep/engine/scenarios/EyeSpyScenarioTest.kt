package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Eye Spy (Portal Second Age).
 *
 * Oracle: "Look at the top card of target player's library. You may put that card into
 * their graveyard."
 */
class EyeSpyScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Eye Spy")
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun cast(game: TestGame) {
        val spell = game.state.getHand(game.player1Id).first()
        val result = game.execute(CastSpell(game.player1Id, spell, listOf(ChosenTarget.Player(game.player2Id))))
        withClue("cast: ${result.error}") { result.error shouldBe null }
        game.resolveStack()
    }

    init {
        context("Eye Spy") {
            test("putting the top card into the graveyard") {
                val game = setup()
                val libBefore = game.librarySize(2)
                cast(game)
                val top = game.state.getLibrary(game.player2Id).first()
                game.selectCards(listOf(top)).error shouldBe null
                game.resolveStack()
                game.librarySize(2) shouldBe libBefore - 1
                game.graveyardSize(2) shouldBe 1
            }

            test("declining leaves the top card in place") {
                val game = setup()
                val libBefore = game.librarySize(2)
                cast(game)
                val top = game.state.getLibrary(game.player2Id).first()
                game.skipSelection().error shouldBe null
                game.resolveStack()
                game.librarySize(2) shouldBe libBefore
                game.graveyardSize(2) shouldBe 0
                game.state.getLibrary(game.player2Id).first() shouldBe top
            }
        }
    }
}
