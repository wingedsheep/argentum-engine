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
import io.kotest.matchers.shouldBe

/**
 * "Target creature becomes prepared" read as card advantage: a prepared creature's controller may
 * cast a copy of its prepare spell, a spell on top of the cards in hand — the same reading as a
 * copy-and-cast. An instant-speed activation that only readies a creature is then cashed at the
 * opponent's end step with mana that empties at cleanup, like a draw activation.
 */
class BecomePreparedReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "become-prepared",
                category = PuzzleCategory.PRIORITY_TIMING,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    /**
     * Hexhaven Dueling Arena ({4}, {T}: target creature becomes prepared), four untapped Forests and an
     * unprepared Woodwork Prodigy, at the opponent's end step: nothing else to spend the mana on.
     */
    private val arenaAtTheirEndStep = { s: ScenarioBuilder ->
        s.withPlayers()
            .withActivePlayer(2)
            .withLandsOnBattlefield(1, "Forest", 4)
            .withCardOnBattlefield(1, "Hexhaven Dueling Arena")
            .withCardOnBattlefield(1, "Woodwork Prodigy")
            .withLandsOnBattlefield(2, "Swamp", 3)
            .apply { repeat(4) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Swamp") } }
            .build()
            .advanceToPriority(1, Step.END)
    }

    init {
        test("an idle prepare activation is used at the opponent's end step") {
            val result = decide(arenaAtTheirEndStep) { shouldActivate("Hexhaven Dueling Arena") }
            withClue("becoming prepared reads as card advantage, cashed at their end step like a draw activation: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("becoming prepared reads as card advantage, on itself or on a target") {
            for (card in listOf("Hexhaven Dueling Arena", "Woodwork Prodigy", "Bloodline Recollector")) {
                withClue(card) {
                    CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags shouldContain IntentTag.DRAW
                }
            }
        }
    }
}
