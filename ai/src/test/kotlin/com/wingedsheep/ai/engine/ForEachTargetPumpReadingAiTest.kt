package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToStackResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "One or two target creatures each get +2/+1 until end of turn" (Appeal to Eirdu) runs its pump once
 * per target, so the stat change sits inside a `ForEach` over the targets. Read through that wrapper it
 * is the combat trick a single-target pump is: it buys something only in a fight, so cast at the
 * opponent's end step it is a card thrown away.
 */
class ForEachTargetPumpReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(category: PuzzleCategory, position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "for-each-target-pump-timing",
                category = category,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    init {
        test("a pump run once per target reads as a combat trick") {
            for (card in listOf("Appeal to Eirdu", "Coordinated Assault")) {
                withClue("$card: each target's +N/+N ends at cleanup, so it is a trick to hold") {
                    CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags shouldContain IntentTag.COMBAT_TRICK
                }
            }
        }

        test("Appeal to Eirdu is not cast at the opponent's end step") {
            val result = decide(PuzzleCategory.PRIORITY_TIMING, { s ->
                s.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInHand(1, "Appeal to Eirdu")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Swamp") } }
                    .build()
                    .also { it.advanceToPriority(1, Step.END) }
            }) { shouldNotCast("Appeal to Eirdu") }
            withClue("a per-target until-end-of-turn pump at their end step pumps nothing: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("Appeal to Eirdu is not cast precombat when nothing can attack into the blockers") {
            val result = decide(PuzzleCategory.PRIORITY_TIMING, { s ->
                s.withPlayers()
                    .withActivePlayer(1)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardInHand(1, "Appeal to Eirdu")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Swamp") } }
                    .build()
                    .also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN) }
            }) { shouldNotCast("Appeal to Eirdu") }
            withClue("chose ${result.move} — ${result.failure}") { result.passed shouldBe true }
        }
        test("Appeal to Eirdu is not cast in our postcombat main phase") {
            val result = decide(PuzzleCategory.PRIORITY_TIMING, { s ->
                s.withPlayers()
                    .withActivePlayer(1)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardInHand(1, "Appeal to Eirdu")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Swamp") } }
                    .build()
                    .also { it.advanceToPriority(1, Step.POSTCOMBAT_MAIN) }
            }) { shouldNotCast("Appeal to Eirdu") }
            withClue("chose ${result.move} — ${result.failure}") { result.passed shouldBe true }
        }
        test("Appeal to Eirdu is not cast in the opponent's upkeep") {
            val result = decide(PuzzleCategory.PRIORITY_TIMING, { s ->
                s.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardInHand(1, "Appeal to Eirdu")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Swamp") } }
                    .build()
                    .also { it.advanceToPriority(1, Step.UPKEEP) }
            }) { shouldNotCast("Appeal to Eirdu") }
            withClue("chose ${result.move} — ${result.failure}") { result.passed shouldBe true }
        }

        test("Appeal to Eirdu still saves our creature from burn") {
            val result = decide(PuzzleCategory.STACK_RESPONSE, { s ->
                s.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Appeal to Eirdu")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInHand(2, "Shock")
                    .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }
                    .build()
                    .also { g ->
                        g.castSpell(2, "Shock", g.findPermanent("Grizzly Bears"))
                        g.advanceToStackResponse(1)
                    }
            }) { shouldCast("Appeal to Eirdu"); shouldTarget("Grizzly Bears") }
            withClue("chose ${result.move} — ${result.failure}") { result.passed shouldBe true }
        }

        test("Appeal to Eirdu still wins a block") {
            val result = decide(PuzzleCategory.PRIORITY_TIMING, { s ->
                s.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Appeal to Eirdu")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }
                    .build()
                    .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Hill Giant" to 1)) }
                    .advanceToDeclaration(1, Step.DECLARE_BLOCKERS)
                    .also { it.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))) }
                    .advanceToPriority(1, Step.DECLARE_BLOCKERS)
            }) { shouldCast("Appeal to Eirdu"); shouldTarget("Grizzly Bears") }
            withClue("chose ${result.move} — ${result.failure}") { result.passed shouldBe true }
        }
    }
}
