package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A pump or copy until end of turn run once per chosen target — "any number of target creatures
 * each get +1/+0 … until end of turn" (`ForEachTarget(ModifyStats(…, ContextTarget(0)))`, Rabid
 * Attack), or a loop over the gathered chosen targets that turns each into a copy
 * (Polymorphous Rush). Read through the loop as the single-target trick it repeats, an instant
 * carrying one is a combat trick: held for blocks, where it wins a fight, and given no window at
 * the opponent's end step, where the stats are gone at cleanup with no fight to spend them in.
 * (Setessan Tactics' fights make it removal instead: GrantedFightRemovalReadingAiTest.)
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

    /** Our Grizzly Bears attacking, blocked by their Centaur Courser: +1/+0 turns a trade-down into a trade. */
    private fun bearsBlocked(s: ScenarioBuilder) = s.withPlayers()
        .withActivePlayer(1)
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, RABID_ATTACK)
        .withCardOnBattlefield(2, "Centaur Courser")
        .apply { repeat(4) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Forest") } }
        .build()
        .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
        .also { it.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null }
        .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
        .also { it.declareBlockers(mapOf("Centaur Courser" to listOf("Grizzly Bears"))).error shouldBe null }
        .advanceToPriority(1, Step.DECLARE_BLOCKERS)

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
            for (card in listOf(RABID_ATTACK, "Polymorphous Rush")) {
                val definition = cardRegistry.requireCard(card)
                withClue(card) { CardIntentAnalyzer.analyze(definition).tags shouldContain IntentTag.COMBAT_TRICK }
                val reading = AiReadingAudit.read(definition)
                withClue("$card: $reading") { reading.readFully shouldBe true }
            }
        }

        test("Rabid Attack is cast once blocks are in, where +1/+0 kills the blocker") {
            val result = decide(::bearsBlocked) { shouldCast(RABID_ATTACK) }
            withClue("the blocked Bears kill the Courser with +1/+0: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("the hold has no window for Rabid Attack at the opponent's end step, with no fight left") {
            val game = board(scenario(), 2).advanceToPriority(1, Step.END)
            val card = game.findCardsInHand(1, RABID_ATTACK).first()
            val cast = CastSpell(game.player1Id, card, listOf("Hill Giant", "Grizzly Bears").map { ChosenTarget.Permanent(game.findPermanent(it)!!) })
            val policy = HoldPolicy(
                IntentCatalog.of(cardRegistry),
                holdExpiringGrantsForCombat = true, refuseUnspendableGrants = true, holdUnusablePumps = true,
                cashCantripsInTheEndStep = true,
            )
            policy.verdictFor(game.state, game.player1Id, RABID_ATTACK, cast = cast) shouldBe TimingVerdict.NoWindow
        }
    }

    private companion object {
        const val RABID_ATTACK = "Rabid Attack"
    }
}
