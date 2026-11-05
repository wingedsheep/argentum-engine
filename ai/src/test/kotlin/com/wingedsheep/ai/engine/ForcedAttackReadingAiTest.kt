package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.ForcedAttackWindow
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Forced attacks on an opponent's creatures — Imaginary Threats' "creatures target opponent
 * controls attack this turn if able", Kardur's "each creature your opponents
 * control attacks … if able".
 *
 * The requirement moves no permanent, so the board simulation sees a card spent for nothing, and
 * nothing told the AI that "this turn" binds an attack only on the opponent's own turn, before they
 * attack. Unread, Imaginary Threats was never cast at all.
 */
class ForcedAttackReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val policy by lazy { HoldPolicy(intents, forcedAttacksOnTheirTurn = true) }
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }
    private val threats by lazy { cardRegistry.getCard("Imaginary Threats")!! }

    private fun tags(name: String) = CardIntentAnalyzer.analyze(cardRegistry.getCard(name)!!).tags

    /**
     * Imaginary Threats and four Islands, a Horned Turtle to block with; the opponent has a Grizzly
     * Bears ([bearsTapped] or not). In [theirMain] the opponent's precombat main phase, else ours.
     */
    private fun threats(theirMain: Boolean, bearsTapped: Boolean = false) = { s: ScenarioBuilder ->
        s.withPlayers()
            .withTurnNumber(5)
            .let { if (theirMain) it.withActivePlayer(2) else it }
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withLandsOnBattlefield(1, "Island", 4)
            .withCardOnBattlefield(1, "Horned Turtle")
            .withLandsOnBattlefield(2, "Forest", 3)
            .withCardOnBattlefield(2, "Grizzly Bears", tapped = bearsTapped)
            .withCardInHand(1, "Imaginary Threats")
            .let { b -> (1..10).fold(b) { acc, _ -> acc.withCardInLibrary(1, "Island").withCardInLibrary(2, "Forest") } }
            .build()
            .let { if (theirMain) it.advanceToPriority(1, Step.PRECOMBAT_MAIN) else it }
    }

    private fun TestGame.threatsVerdict(): TimingVerdict {
        val card = findCardsInHand(1, "Imaginary Threats").first()
        return policy.verdictFor(state, player1Id, "Imaginary Threats", cast = CastSpell(player1Id, card, targets = listOf(ChosenTarget.Player(player2Id))))
    }

    private val liveWithoutFlag = AiProfile.LIVE.copy(id = "live-without-forced-attacks", forcedAttacksOnTheirTurn = false)

    private fun decide(
        position: (ScenarioBuilder) -> TestGame,
        profile: AiProfile = AiProfile.LIVE,
        check: PuzzleMove.() -> Unit,
    ): PuzzleResult = runner.run(
        AiPuzzle(
            id = "forced-attack",
            category = PuzzleCategory.PRIORITY_TIMING,
            expectation = "",
            aiSeat = 1,
            position = position,
            check = check,
        ),
        profile,
    )

    private fun PuzzleResult.shouldSucceed() = withClue("chose $move — $failure") { passed shouldBe true }

    init {
        test("a forced attack on an opponent's creatures reads as one") {
            listOf("Imaginary Threats", "Kardur, Doomscourge").forEach { name ->
                withClue("$name forces an opponent's creatures to attack") { tags(name) shouldContain IntentTag.FORCED_ATTACK }
            }
        }

        test("Imaginary Threats is rewarded in the opponent's main phase and neutral in our own") {
            threats(theirMain = true)(scenario()).threatsVerdict()
                .shouldBeInstanceOf<TimingVerdict.Adjust>().reason shouldBe "forces their attack"
            withClue("on our own turn 'this turn' binds no attack of theirs") {
                (threats(theirMain = false)(scenario()).threatsVerdict() as? TimingVerdict.Adjust)?.reason shouldBe null
            }
        }

        test("no window when the opponent has no creature that could attack") {
            val game = threats(theirMain = true, bearsTapped = true)(scenario())
            ForcedAttackWindow.forcesAnAttack(game.state, game.player1Id, threats) shouldBe false
        }

        test("the AI casts Imaginary Threats in the opponent's main phase; without the flag it passes") {
            decide(threats(theirMain = true), liveWithoutFlag) { shouldPass() }.shouldSucceed()
            decide(threats(theirMain = true)) { shouldCast("Imaginary Threats") }.shouldSucceed()
        }

        test("the AI holds Imaginary Threats in its own main phase") {
            decide(threats(theirMain = false)) { shouldPass() }.shouldSucceed()
        }
    }
}
