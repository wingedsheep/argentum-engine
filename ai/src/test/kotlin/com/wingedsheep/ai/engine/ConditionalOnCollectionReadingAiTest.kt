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
 * A pipeline's "if those cards include … / otherwise …" branch (`ConditionalOnCollectionEffect`)
 * is read through to what each side does: Planar Genesis puts a card into hand on its `orElse`
 * side, so it reads as card draw, and an idle instant-speed draw is cashed at the opponent's end
 * step instead of rotting in hand through cleanup.
 */
class ConditionalOnCollectionReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "conditional-on-collection",
                category = PuzzleCategory.PRIORITY_TIMING,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    /** Planar Genesis with a Forest and an Island untapped, at the opponent's end step; no land on top. */
    private val genesisAtTheirEndStep = { s: ScenarioBuilder ->
        s.withPlayers()
            .withActivePlayer(2)
            .withLandsOnBattlefield(1, "Forest", 1)
            .withLandsOnBattlefield(1, "Island", 1)
            .withCardInHand(1, "Planar Genesis")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(1, "Grizzly Bears")
            .build()
            .advanceToPriority(1, Step.END)
    }

    init {
        test("an idle instant whose payoff sits in an if-those-cards-include branch is cast at the opponent's end step") {
            val result = decide(genesisAtTheirEndStep) { shouldCast("Planar Genesis") }
            withClue("the otherwise branch puts a card into hand — card draw, cashed at their end step: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
