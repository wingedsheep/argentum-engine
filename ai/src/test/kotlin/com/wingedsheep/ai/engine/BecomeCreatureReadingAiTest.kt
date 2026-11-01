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
 * An instant-speed "until end of turn, this land becomes a creature" activation, read as an
 * end-of-turn creature payoff: the body is worth paying for only while it can still attack or
 * block this turn. Once combat is over, animating a manland buys a creature cleanup erases.
 */
class BecomeCreatureReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "become-creature-activation",
                category = PuzzleCategory.PRIORITY_TIMING,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    /** Restless Spire ({U}{R}: becomes a 2/1 until end of turn) with its mana open. */
    private val spireBoard = { s: ScenarioBuilder -> spireBoardOn(s, activePlayer = 1) }

    private fun spireBoardOn(s: ScenarioBuilder, activePlayer: Int): TestGame =
        s.withPlayers()
            .withActivePlayer(activePlayer)
            .withLandsOnBattlefield(1, "Island", 1)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardOnBattlefield(1, "Restless Spire")
            .withCardOnBattlefield(2, "Hill Giant")
            .build()

    init {
        test("a manland is not animated in our postcombat main, with no fight left this turn") {
            val result = decide({ spireBoard(it).advanceToPriority(1, Step.POSTCOMBAT_MAIN) }) {
                shouldNotActivate("Restless Spire")
            }
            withClue("an end-of-turn animation reads as a creature payoff spent only in a fight this turn: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("a manland is not animated in the opponent's end step") {
            val result = decide({ spireBoardOn(it, activePlayer = 2).advanceToPriority(1, Step.END) }) {
                shouldNotActivate("Restless Spire")
            }
            withClue("an end-of-turn animation after their combat buys a body cleanup erases: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("a manland is still animated in our precombat main to attack for lethal") {
            val result = decide({ s ->
                s.withPlayers()
                    .withLifeTotal(2, 2)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Restless Spire")
                    .build()
            }) {
                shouldActivate("Restless Spire")
            }
            withClue("an end-of-turn animation with our attack still ahead is not refused: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
