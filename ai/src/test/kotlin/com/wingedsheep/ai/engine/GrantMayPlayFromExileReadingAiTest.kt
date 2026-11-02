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
 * Impulse draw read as card draw: "exile the top two cards of your library; until the end of your
 * next turn, you may play those cards" is two cards, only held in exile instead of in hand. Before,
 * the evaluator counted only the hand, so the spell read as a card spent for nothing and the AI
 * sat on it with its mana idle.
 */
class GrantMayPlayFromExileReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "impulse-draw",
                category = PuzzleCategory.PRIORITY_TIMING,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    /**
     * Burning Curiosity ({2}{R} sorcery: exile the top two, play them through your next turn) as
     * the only spell in hand, three Mountains up, the library six-drops it can't cast this turn —
     * so the only thing the cast can buy is the two cards themselves.
     */
    private val curiosityWithIdleMana = { s: ScenarioBuilder ->
        s.withPlayers()
            .withActivePlayer(1)
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withCardInHand(1, "Burning Curiosity")
            .apply { repeat(12) { withCardInLibrary(1, "Kulrath Zealot") } }
            .apply { repeat(12) { withCardInLibrary(2, "Island") } }
            .build()
            .advanceToPriority(1, Step.POSTCOMBAT_MAIN)
    }

    init {
        test("an impulse draw spell is cast with idle mana") {
            val result = decide(curiosityWithIdleMana) { shouldCast("Burning Curiosity") }
            withClue("cards playable from exile read as held cards, so impulse draw is card draw: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
