package com.wingedsheep.ai.engine

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
 * "Target creature gains 'When this creature dies, return it to the battlefield'", read as
 * protection: a trick carrying that grant answers a Murder aimed at our creature, so the combat-trick
 * hold must not keep it in hand while the creature dies.
 */
class GrantedReturnTriggerReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "granted-dies-return",
                category = PuzzleCategory.STACK_RESPONSE,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    /** The opponent's Murder on our Serra Angel, on the stack; [save] in our hand with the mana for it. */
    private fun murderOnOurAngel(save: String) = { s: ScenarioBuilder ->
        val game = s.withPlayers()
            .withActivePlayer(2)
            .withLandsOnBattlefield(2, "Swamp", 3)
            .withCardInHand(2, "Murder")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withLandsOnBattlefield(1, "Forest", 2)
            .withCardOnBattlefield(1, "Serra Angel")
            .withCardInHand(1, save)
            .apply { repeat(4) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Swamp") } }
            .build()
        game.castSpell(2, "Murder", game.findPermanent("Serra Angel"))
        game.advanceToStackResponse(1)
    }

    init {
        test("a pump that grants a dies-and-returns trigger is cast in response to removal on our creature") {
            val result = decide(murderOnOurAngel("Fake Your Own Death")) {
                shouldCast("Fake Your Own Death")
                shouldTarget("Serra Angel")
            }
            withClue("a granted 'when this dies, return it' reads as protection, which answers a Murder: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("a pump that grants hexproof is cast in response to removal on our creature") {
            val result = decide(murderOnOurAngel("Blossoming Defense")) {
                shouldCast("Blossoming Defense")
                shouldTarget("Serra Angel")
            }
            withClue("a protective trick may answer removal its toughness can't, so it is not held for combat: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
