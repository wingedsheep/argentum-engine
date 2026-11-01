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
 * An instant-speed activation that only scries, read as card selection: the opponent's end step,
 * with mana that empties at cleanup and nothing else to spend it on, is the window to look at the
 * top of our library — the same window a draw activation is cashed in.
 */
class ScryReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "scry-activation",
                category = PuzzleCategory.PRIORITY_TIMING,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    /** Crystal Ball ({1}, {T}: Scry 2) and two untapped Islands, at the opponent's end step. */
    private val crystalBallAtTheirEndStep = { s: ScenarioBuilder ->
        s.withPlayers()
            .withActivePlayer(2)
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardOnBattlefield(1, "Crystal Ball")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Grizzly Bears")
            .build()
            .advanceToPriority(1, Step.END)
    }

    /** Crystal Ball, one Forest and a Giant Growth for the Bears facing a Hill Giant — our precombat main. */
    private val crystalBallWithATrickInHand = { s: ScenarioBuilder ->
        s.withPlayers()
            .withLandsOnBattlefield(1, "Forest", 1)
            .withCardOnBattlefield(1, "Crystal Ball")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInHand(1, "Giant Growth")
            .build()
    }

    init {
        test("an idle scry activation is used at the opponent's end step") {
            val result = decide(crystalBallAtTheirEndStep) { shouldActivate("Crystal Ball") }
            withClue("scry reads as card selection, cashed at their end step like a draw activation: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("a scry activation is held in our main phase while an instant wants the mana") {
            val result = decide(crystalBallWithATrickInHand) { shouldNotActivate("Crystal Ball") }
            withClue("scry reads as card selection, held for the trick like a draw activation: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
