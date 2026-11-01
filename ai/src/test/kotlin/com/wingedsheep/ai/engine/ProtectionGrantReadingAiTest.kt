package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.ai.puzzles.advanceToStackResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * An instant that only grants a protective keyword until end of turn (indestructible, hexproof),
 * read for its timing: it buys something only against removal on the stack or in a fight. Cast in
 * our own main phase, or at the opponent's end step, it protects nothing and is a card thrown away.
 */
class ProtectionGrantReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(category: PuzzleCategory, position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "protection-grant-timing",
                category = category,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    private fun board(s: ScenarioBuilder, active: Int, card: String) = s.withPlayers()
        .withActivePlayer(active)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardOnBattlefield(1, "Serra Angel")
        .withCardInHand(1, card)
        .withLandsOnBattlefield(2, "Swamp", 3)
        .withCardInHand(2, "Murder")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Swamp") } }
        .build()

    init {
        test("an indestructible grant answers burn on our creature, though it adds no toughness") {
            val result = decide(PuzzleCategory.STACK_RESPONSE, { s ->
                s.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Withstand Death")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .apply { repeat(4) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Mountain") } }
                    .build()
                    .also { g ->
                        g.castSpell(2, "Lightning Bolt", g.findPermanent("Grizzly Bears"))
                        g.advanceToStackResponse(1)
                    }
            }) {
                shouldCast("Withstand Death")
                shouldTarget("Grizzly Bears")
            }
            withClue("a protection grant is not priced by the toughness it adds: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        for (card in listOf("Divine Resilience", "Withstand Death")) {
            test("$card is not cast in our own main phase with nothing to protect against") {
                val result = decide(PuzzleCategory.PRIORITY_TIMING, { s -> board(s, 1, card) }) { shouldNotCast(card) }
                withClue("an until-end-of-turn protection grant reads as a response, not a main-phase play: chose ${result.move} — ${result.failure}") {
                    result.passed shouldBe true
                }
            }

            test("$card is not cast at the opponent's end step") {
                val result = decide(PuzzleCategory.PRIORITY_TIMING, { s ->
                    board(s, 2, card).also { it.advanceToPriority(1, Step.END) }
                }) { shouldNotCast(card) }
                withClue("an until-end-of-turn protection grant at their end step protects nothing: chose ${result.move} — ${result.failure}") {
                    result.passed shouldBe true
                }
            }

            test("$card still answers removal on our creature") {
                val result = decide(PuzzleCategory.STACK_RESPONSE, { s ->
                    board(s, 2, card).also { g ->
                        g.castSpell(2, "Murder", g.findPermanent("Serra Angel"))
                        g.advanceToStackResponse(1)
                    }
                }) {
                    shouldCast(card)
                    shouldTarget("Serra Angel")
                }
                withClue("a protection grant is spent in response to removal: chose ${result.move} — ${result.failure}") {
                    result.passed shouldBe true
                }
            }
        }
    }
}
