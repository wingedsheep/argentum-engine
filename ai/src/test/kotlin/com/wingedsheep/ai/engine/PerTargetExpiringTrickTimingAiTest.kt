package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
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
 * A pump or copy until end of turn run once per chosen target — "any number of target creatures
 * each get +1/+1 … until end of turn" (`ForEachTarget(ModifyStats(…, ContextTarget(0)))`, Setessan
 * Tactics, Rabid Attack), or a loop over the gathered chosen targets that turns each into a copy
 * (Polymorphous Rush). Read through the loop as the single-target trick it repeats, an instant
 * carrying one is a combat trick: cast at the opponent's end step or after our combat, the stats
 * are gone at cleanup with no fight to spend them in.
 */
class PerTargetExpiringTrickTimingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "per-target-expiring-trick-timing",
                category = PuzzleCategory.PRIORITY_TIMING,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    private fun board(s: ScenarioBuilder, active: Int) = s.withPlayers()
        .withActivePlayer(active)
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, RABID_ATTACK)
        .withLandsOnBattlefield(2, "Forest", 3)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .apply { repeat(4) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Forest") } }
        .build()

    init {
        test("a per-target pump or copy until end of turn is a combat trick, and reads fully") {
            for (card in listOf(RABID_ATTACK, "Setessan Tactics", "Polymorphous Rush")) {
                val definition = cardRegistry.requireCard(card)
                withClue(card) { CardIntentAnalyzer.analyze(definition).tags shouldContain IntentTag.COMBAT_TRICK }
                val reading = AiReadingAudit.read(definition)
                withClue("$card: $reading") { reading.readFully shouldBe true }
            }
        }

        test("Rabid Attack is not cast at the opponent's end step") {
            val result = decide({ s -> board(s, 2).also { it.advanceToPriority(1, Step.END) } }) { shouldNotCast(RABID_ATTACK) }
            withClue("+1/+0 until end of turn at their end step has no fight left: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("Rabid Attack is not cast in our second main phase") {
            val result = decide({ s -> board(s, 1).also { it.advanceToPriority(1, Step.POSTCOMBAT_MAIN) } }) { shouldNotCast(RABID_ATTACK) }
            withClue("+1/+0 until end of turn after combat has no fight left: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }

    private companion object {
        const val RABID_ATTACK = "Rabid Attack"
    }
}
