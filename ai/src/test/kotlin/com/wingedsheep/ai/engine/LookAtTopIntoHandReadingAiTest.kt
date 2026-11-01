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
 * "Look at the top N cards of your library and put one of them into your hand" — a gather from the
 * library top moved into hand — read as card draw: an instant-speed activation that does only that is
 * cashed at the opponent's end step, with mana that empties at cleanup and nothing else to spend it on,
 * like a draw activation.
 */
class LookAtTopIntoHandReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "look-at-top-into-hand",
                category = PuzzleCategory.PRIORITY_TIMING,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    /** Duskwatch Recruiter ({2}{G}: look at the top three …) and three untapped Forests, at the opponent's end step. */
    private val recruiterAtTheirEndStep = { s: ScenarioBuilder ->
        s.withPlayers()
            .withActivePlayer(2)
            .withLandsOnBattlefield(1, "Forest", 3)
            .withCardOnBattlefield(1, "Duskwatch Recruiter")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(1, "Forest")
            .build()
            .advanceToPriority(1, Step.END)
    }

    /**
     * Duskwatch Recruiter, two Forests and two Islands, with a Counterspell in hand — our precombat
     * main. Activating taps three of the four lands and leaves the counter uncastable until our next turn.
     */
    private val recruiterWithACounterInHand = { s: ScenarioBuilder ->
        s.withPlayers()
            .withLandsOnBattlefield(1, "Forest", 2)
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardOnBattlefield(1, "Duskwatch Recruiter")
            .withCardInHand(1, "Counterspell")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(1, "Forest")
            .build()
    }

    init {
        test("an idle look-at-the-top-into-hand activation is used at the opponent's end step") {
            val result = decide(recruiterAtTheirEndStep) { shouldActivate("Duskwatch Recruiter") }
            withClue("look-at-top-into-hand reads as card draw, cashed at their end step like a draw activation: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("a look-at-the-top-into-hand activation is held in our main phase while a counterspell wants the mana") {
            val result = decide(recruiterWithACounterInHand) { shouldNotActivate("Duskwatch Recruiter") }
            withClue("look-at-top-into-hand reads as card draw, held for the counter like a draw activation: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
