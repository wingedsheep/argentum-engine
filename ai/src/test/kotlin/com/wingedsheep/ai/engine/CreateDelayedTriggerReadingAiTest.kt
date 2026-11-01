package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A delayed trigger read as the effect it fires: "draw a card at the beginning of the next turn's
 * upkeep" is a draw, only a later one. The draw lands past the search horizon, so the board alone
 * prices cracking Mishra's Bauble as a lost artifact; read as card flow, the opponent's end step is
 * the window to cash it, as for any draw activation.
 */
class CreateDelayedTriggerReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "delayed-draw-activation",
                category = PuzzleCategory.PRIORITY_TIMING,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    /** Mishra's Bauble ({T}, sacrifice: look at the top card; draw at the next upkeep), at the opponent's end step. */
    private val baubleAtTheirEndStep = { s: ScenarioBuilder ->
        s.withPlayers()
            .withActivePlayer(2)
            .withLandsOnBattlefield(1, "Island", 1)
            .withCardOnBattlefield(1, "Mishra's Bauble")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(2, "Island")
            .build()
            .advanceToPriority(1, Step.END)
    }

    init {
        test("a delayed draw activation is cashed at the opponent's end step") {
            val result = decide(baubleAtTheirEndStep) { shouldActivate("Mishra's Bauble") }
            withClue("a delayed trigger reads as the draw it fires, cashed at their end step: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
