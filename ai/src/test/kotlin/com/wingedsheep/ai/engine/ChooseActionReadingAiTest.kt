package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Choose one of these actions" picked by the card's controller on resolution: every branch is
 * something the card can do, so it is read through to its branches the way a "you may …" gate is.
 * An instant whose branches all draw ("sacrifice an artifact or discard a card; if you do, draw two")
 * is card draw, cashed at the opponent's end step rather than spent in our main phase.
 */
class ChooseActionReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "choose-action",
                category = PuzzleCategory.PRIORITY_TIMING,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    /**
     * Vision of Love, an out-of-reach Craw Wurm to discard, and two untapped Mountains. The runner
     * stocks both libraries with inert filler, so a draw is worth what a card is worth.
     */
    private fun visionOfLove(active: Int, step: Step?) = { s: ScenarioBuilder ->
        val game = s.withPlayers()
            .withActivePlayer(active)
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardInHand(1, "Vision of Love")
            .withCardInHand(1, "Craw Wurm")
            .build()
        if (step != null) game.advanceToPriority(1, step) else game
    }

    init {
        test("a draw-only choose-an-action instant is not spent in our main phase") {
            val result = decide(visionOfLove(active = 1, step = null)) { shouldNotCast("Vision of Love") }
            withClue("controller-chosen ChooseAction reads its branches (draw), held for their end step: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("a draw-only choose-an-action instant is cast at the opponent's end step") {
            val result = decide(visionOfLove(active = 2, step = Step.END)) { shouldCast("Vision of Love") }
            withClue("controller-chosen ChooseAction reads its branches (draw), cashed at their end step: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("an action an opponent chooses stays unread — no branch is what the card does") {
            CardIntentAnalyzer.analyze(cardRegistry.requireCard("Vision of Love")).tags shouldContain IntentTag.DRAW
            withClue("Thornplate Intimidator: the opponent picks sacrifice, discard or life loss") {
                CardIntentAnalyzer.analyze(cardRegistry.requireCard("Thornplate Intimidator")).tags shouldNotContain IntentTag.DRAW
            }
        }
    }
}
