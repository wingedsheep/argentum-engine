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
 * A trigger only a fight fires, granted until end of turn — Warriors' Lesson's "up to two target
 * creatures you control each gain 'whenever this creature deals combat damage to a player, draw a
 * card'", `ForEachTarget(GrantTriggeredAbility(…))`. Read with the keyword grants, it is spent only
 * in a fight still to come this turn; and since it pays only on a hit to a player, only in our own
 * attack — on the opponent's turn our creatures can only block, which never connects.
 */
class CombatDamageDrawGrantTimingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "combat-damage-draw-grant-timing",
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
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, LESSON)
        .withLandsOnBattlefield(2, "Swamp", 3)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .apply { repeat(4) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Swamp") } }
        .build()

    init {
        test("Warriors' Lesson's per-target draw trigger is read as a spell-wide fight grant") {
            val lesson = cardRegistry.requireCard(LESSON)
            ExpiringGrantWindow.readsSpellKeywordGrant(lesson.script.spellEffect!!) shouldBe true
            val reading = AiReadingAudit.read(lesson)
            withClue(reading.toString()) { reading.readFully shouldBe true }
        }

        test("the floor stays off before our attack, and closes where no attacker of ours can connect") {
            val spell = cardRegistry.requireCard(LESSON).script.spellEffect!!
            fun buysNothing(game: TestGame) = ExpiringGrantWindow.castBuysNothing(
                game.state, game.player1Id, listOf(spell),
                listOf("Hill Giant", "Grizzly Bears").map { ChosenTarget.Permanent(game.findPermanent(it)!!) },
                IntentCatalog.of(cardRegistry),
                readKeywordGrants = true,
            )
            val precombat = board(scenario(), 1).also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN) }
            withClue("our creatures can still attack and connect") { buysNothing(precombat) shouldBe false }
            val theirMain = board(scenario(), 2).also { it.advanceToPriority(1, Step.BEGIN_COMBAT) }
            withClue("their turn: a blocker never deals combat damage to a player (${theirMain.state.step}, active ${theirMain.state.activePlayerId == theirMain.player1Id})") { buysNothing(theirMain) shouldBe true }
            val ourSecondMain = board(scenario(), 1).also { it.advanceToPriority(1, Step.POSTCOMBAT_MAIN) }
            withClue("after our combat: no hit is left this turn") { buysNothing(ourSecondMain) shouldBe true }
        }

        test("Warriors' Lesson is not cast at the opponent's end step") {
            val result = decide({ s -> board(s, 2).also { it.advanceToPriority(1, Step.END) } }) { shouldNotCast(LESSON) }
            withClue("a combat-damage draw at their end step can never fire: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }

    private companion object {
        const val LESSON = "Warriors' Lesson"
    }
}
