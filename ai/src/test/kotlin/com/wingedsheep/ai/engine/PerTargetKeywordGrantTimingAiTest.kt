package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A keyword grant until end of turn run once per target — Phalanx Formation's "any number of target
 * creatures each gain double strike until end of turn", `ForEachTarget(GrantKeyword(…))`. Read
 * through the loop as the single-target grant it repeats, it is spent only in a fight still to come
 * this turn: cast at the opponent's end step, or after our combat, it is a card thrown away.
 */
class PerTargetKeywordGrantTimingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "per-target-keyword-grant-timing",
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
        .withLandsOnBattlefield(1, "Plains", 5)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, PHALANX)
        .withLandsOnBattlefield(2, "Swamp", 3)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Swamp") } }
        .build()

    init {
        test("Phalanx Formation's per-target double strike is read as a spell-wide keyword grant") {
            val phalanx = cardRegistry.requireCard(PHALANX)
            ExpiringGrantWindow.readsSpellKeywordGrant(phalanx.script.spellEffect!!) shouldBe true
            val reading = AiReadingAudit.read(phalanx)
            withClue(reading.toString()) { reading.readFully shouldBe true }
        }

        test("the floor stays off while the grant still has a fight, and closes once there is none") {
            val spell = cardRegistry.requireCard(PHALANX).script.spellEffect!!
            fun buysNothing(game: TestGame) = ExpiringGrantWindow.castBuysNothing(
                game.state, game.player1Id, listOf(spell),
                listOf("Hill Giant", "Grizzly Bears").map { ChosenTarget.Permanent(game.findPermanent(it)!!) },
                IntentCatalog.of(cardRegistry),
                readKeywordGrants = true,
            )
            val precombat = board(scenario(), 1).also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN) }
            withClue("our creatures can still attack: double strike can still be spent") { buysNothing(precombat) shouldBe false }
            val theirEnd = board(scenario(), 2).also { it.advanceToPriority(1, Step.END) }
            withClue("their end step: no fight is left this turn") { buysNothing(theirEnd) shouldBe true }
        }

        test("Phalanx Formation is not cast at the opponent's end step") {
            val result = decide({ s -> board(s, 2).also { it.advanceToPriority(1, Step.END) } }) { shouldNotCast(PHALANX) }
            withClue("double strike until end of turn at their end step has no fight left: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("Phalanx Formation is not cast in our second main phase") {
            val result = decide({ s -> board(s, 1).also { it.advanceToPriority(1, Step.POSTCOMBAT_MAIN) } }) { shouldNotCast(PHALANX) }
            withClue("double strike until end of turn after combat has no fight left: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }

    private companion object {
        const val PHALANX = "Phalanx Formation"
    }
}
