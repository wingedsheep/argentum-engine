package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Moonfolk Puzzlemaker (NEO #68) — {2}{U} Artifact Creature — Moonfolk Wizard,
 * 1/4, Flying.
 *
 *   Whenever this creature becomes tapped, scry 1.
 *
 * Attacking taps it, which fires the scry; choosing the top card sends it to the bottom.
 */
class MoonfolkPuzzlemakerScenarioTest : ScenarioTestBase() {

    init {
        context("Moonfolk Puzzlemaker") {

            test("becoming tapped by attacking scries 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Moonfolk Puzzlemaker", summoningSickness = false)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("nothing triggers while it is untapped") {
                    game.getPendingDecision() shouldBe null
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Moonfolk Puzzlemaker" to 2)).error shouldBe null
                game.resolveStack()

                val scry = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a scry 1 selection; got ${game.getPendingDecision()}")
                val top = game.state.getLibrary(game.player1Id).first()
                withClue("scry 1 looks at exactly the top card") {
                    scry.options shouldBe listOf(top)
                }

                game.selectCards(listOf(top)).error shouldBe null
                game.resolveStack()

                withClue("the scried card went to the bottom of the library") {
                    game.state.getLibrary(game.player1Id).last() shouldBe top
                    game.librarySize(1) shouldBe 2
                }
            }
        }
    }
}
