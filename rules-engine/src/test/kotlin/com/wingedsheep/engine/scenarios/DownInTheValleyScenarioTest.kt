package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Down in the Valley — the Saga's three distinct chapter shapes.
 *
 * Chapter II is the one worth a test: "This Saga gains …" grants a triggered ability to a
 * *noncreature permanent* — the Saga itself — for the rest of its life, so the payoff has to keep
 * firing on later turns and has to die with the Saga at chapter IV.
 */
class DownInTheValleyScenarioTest : ScenarioTestBase() {

    init {
        context("Down in the Valley") {

            /** Advance to the start of the given player-turn's precombat main. */
            fun TestGame.advanceToTurn(turnNumber: Int) {
                var guard = 0
                while (state.turnNumber < turnNumber && guard < 20) {
                    passUntilPhase(Phase.ENDING, Step.END)
                    resolveStack()
                    passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    resolveStack()
                    guard++
                }
            }

            fun board() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Down in the Valley")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("chapter I searches a basic land into hand") {
                val game = board()

                game.castSpell(1, "Down in the Valley").error shouldBe null
                game.resolveStack()

                withClue("chapter I raised a library search") {
                    (game.getPendingDecision() is SelectCardsDecision) shouldBe true
                }
                val forest = game.findCardsInLibrary(1, "Forest").single()
                game.selectCards(listOf(forest))
                game.resolveStack()

                withClue("the chosen basic land went to hand, not the battlefield") {
                    game.isInHand(1, "Forest") shouldBe true
                    game.findCardsInLibrary(1, "Forest").size shouldBe 0
                }
            }

            test("chapter II grants the Saga a landfall trigger that mints Elf tokens") {
                val game = board()

                game.castSpell(1, "Down in the Valley").error shouldBe null
                game.resolveStack()
                game.findCardsInLibrary(1, "Forest").firstOrNull()
                    ?.let { game.selectCards(listOf(it)) }
                game.resolveStack()

                withClue("no landfall payoff before chapter II") {
                    game.findAllPermanents("Elf Token").size shouldBe 0
                }

                // Player 1's next turn: chapter II resolves after the draw step.
                game.advanceToTurn(3)

                val landInHand = game.findCardsInHand(1, "Forest").firstOrNull()
                withClue("a land to play so the granted landfall trigger can fire") {
                    (landInHand != null) shouldBe true
                }
                game.execute(PlayLand(game.player1Id, landInHand!!)).error shouldBe null
                game.resolveStack()

                withClue("the Saga's granted landfall trigger created a 1/1 Elf") {
                    game.findAllPermanents("Elf Token").size shouldBe 1
                }
            }

            test("chapter III pumps Elves and gives them vigilance") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Down in the Valley")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Down in the Valley").error shouldBe null
                game.resolveStack()
                game.findCardsInLibrary(1, "Forest").firstOrNull()
                    ?.let { game.selectCards(listOf(it)) }
                game.resolveStack()

                val elves = game.findPermanent("Llanowar Elves")!!
                withClue("no pump before chapter III") {
                    game.state.projectedState.getPower(elves) shouldBe 1
                }

                game.advanceToTurn(5) // chapter II on turn 3, chapter III on turn 5
                game.resolveStack()

                withClue("Elves you control get +1/+0 and gain vigilance") {
                    game.state.projectedState.getPower(elves) shouldBe 2
                    game.state.projectedState.getToughness(elves) shouldBe 1
                    game.state.projectedState.hasKeyword(elves, Keyword.VIGILANCE) shouldBe true
                }
            }
        }
    }
}
