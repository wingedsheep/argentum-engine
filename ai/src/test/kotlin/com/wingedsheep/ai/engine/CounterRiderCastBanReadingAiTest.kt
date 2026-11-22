package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToStackResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "Counter target spell. Its controller can't cast spells this turn" (Render Silent): the cast ban
 * binds the countered spell's controller ([com.wingedsheep.sdk.scripting.targets.EffectTarget.TargetController])
 * and rides on the counter, so its timing is the counter's — a spell on the stack — and the AI
 * spends it as the counterspell it is.
 */
class CounterRiderCastBanReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "counter-rider-cast-ban",
                category = PuzzleCategory.STACK_RESPONSE,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    init {
        test("the cast ban beside a counter is timed by the counter") {
            val reading = AiReadingAudit.read(cardRegistry.requireCard("Render Silent"))
            withClue("Render Silent: $reading") { reading.readFully shouldBe true }
        }

        test("Render Silent counters the opponent's Serra Angel") {
            val result = decide(
                position = { s ->
                    s.withPlayers()
                        .withActivePlayer(2)
                        .withLandsOnBattlefield(2, "Plains", 5)
                        .withCardInHand(2, "Serra Angel")
                        .withLandsOnBattlefield(1, "Plains", 1)
                        .withLandsOnBattlefield(1, "Island", 2)
                        .withCardInHand(1, "Render Silent")
                        .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Plains") } }
                        .build()
                        .also { it.castSpell(2, "Serra Angel") }
                        .advanceToStackResponse(1)
                },
                check = {
                    shouldCast("Render Silent")
                    shouldTarget("Serra Angel")
                },
            )
            withClue(result.toString()) { result.passed shouldBe true }
        }
    }
}
