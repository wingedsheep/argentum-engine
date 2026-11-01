package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * An instant-speed activation that only prevents damage until end of turn, read as a shield: it
 * buys something only when damage can still come this turn — a fight its recipient is in, or a
 * spell or ability on the stack it answers. In our own precombat main, with nothing on the stack,
 * tapping Samite Healer shields nothing and leaves it tapped through the opponent's attack.
 */
class PreventDamageReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "prevent-damage-activation",
                category = PuzzleCategory.PRIORITY_TIMING,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    /** Samite Healer ({T}: prevent the next 1 damage to any target this turn) in our precombat main. */
    private val healerInOurMain = { s: ScenarioBuilder ->
        s.withPlayers()
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardOnBattlefield(1, "Samite Healer")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .build()
    }

    init {
        test("a damage shield is not bought in our own main phase with nothing to prevent") {
            val result = decide(healerInOurMain) { shouldNotActivate("Samite Healer") }
            withClue("an end-of-turn prevention shield reads as a payoff spent only on damage this turn: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
