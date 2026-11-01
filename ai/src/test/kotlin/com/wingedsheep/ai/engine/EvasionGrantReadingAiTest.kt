package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow
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
 * An instant that only grants an evasion keyword until end of turn (flying, can't be blocked),
 * read for its timing: it buys something only in a fight still to come this turn. Cast at the
 * opponent's end step, or in our second main phase after combat, it is a card thrown away.
 */
class EvasionGrantReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(category: PuzzleCategory, position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "evasion-grant-timing",
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
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardInHand(1, card)
        .withLandsOnBattlefield(2, "Swamp", 3)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Swamp") } }
        .build()

    init {
        test("only a spell whose whole payoff is an end-of-turn keyword grant is read") {
            val reads = { card: String ->
                ExpiringGrantWindow.readsSpellKeywordGrant(cardRegistry.requireCard(card).script.spellEffect!!)
            }
            withClue("flying until end of turn is the whole spell") { reads("Jump") shouldBe true }
            withClue("haste is spent lifting summoning sickness, which the fight check counts against it") {
                reads("Unnatural Speed") shouldBe false
            }
            withClue("a card drawn beside the trample lasts") { reads("Charge Through") shouldBe false }
        }

        for (card in listOf("Jump", "Infiltrate")) {
            test("$card is not cast at the opponent's end step") {
                val result = decide(PuzzleCategory.PRIORITY_TIMING, { s ->
                    board(s, 2, card).also { it.advanceToPriority(1, Step.END) }
                }) { shouldNotCast(card) }
                withClue("an until-end-of-turn evasion grant at their end step has no fight left: chose ${result.move} — ${result.failure}") {
                    result.passed shouldBe true
                }
            }

            test("$card is not cast in our second main phase") {
                val result = decide(PuzzleCategory.PRIORITY_TIMING, { s ->
                    board(s, 1, card).also { it.advanceToPriority(1, Step.POSTCOMBAT_MAIN) }
                }) { shouldNotCast(card) }
                withClue("an until-end-of-turn evasion grant after combat has no fight left: chose ${result.move} — ${result.failure}") {
                    result.passed shouldBe true
                }
            }
        }
    }
}
