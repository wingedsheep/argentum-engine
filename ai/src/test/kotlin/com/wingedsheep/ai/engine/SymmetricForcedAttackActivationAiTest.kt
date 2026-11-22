package com.wingedsheep.ai.engine

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
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A symmetric forced attack on an activated ability — Goblin Diplomats' "{T}: Each creature attacks
 * this turn if able".
 *
 * The requirement binds every creature, so it was read as a price on our own and never tagged. But
 * on the opponent's turn before they attack, our creatures can't attack anyway: it binds only theirs,
 * the same forced attack Imaginary Threats is. On our own turn it binds only ours, which is a price.
 */
class SymmetricForcedAttackActivationAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val policy by lazy { HoldPolicy(intents, forcedAttacksOnTheirTurn = true) }
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    /**
     * Goblin Diplomats and a Horned Turtle to block with; the opponent has a Grizzly Bears. On the
     * opponent's turn ([theirTurn]) the AI holds priority in their precombat main phase — the
     * window before their attack that auto-pass leaves it — else in its own.
     */
    private fun diplomats(theirTurn: Boolean, blocker: Boolean = true) = { s: ScenarioBuilder ->
        s.withPlayers()
            .withTurnNumber(5)
            .let { if (theirTurn) it.withActivePlayer(2) else it }
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardOnBattlefield(1, "Goblin Diplomats")
            .let { if (blocker) it.withCardOnBattlefield(1, "Horned Turtle") else it }
            .withLandsOnBattlefield(2, "Forest", 2)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .let { b -> (1..10).fold(b) { acc, _ -> acc.withCardInLibrary(1, "Mountain").withCardInLibrary(2, "Forest") } }
            .build()
            .let { if (theirTurn) it.advanceToPriority(1, Step.PRECOMBAT_MAIN) else it }
    }

    private fun TestGame.diplomatsVerdict(): TimingVerdict {
        val source = findPermanent("Goblin Diplomats")!!
        val ability = cardRegistry.getCard("Goblin Diplomats")!!.script.activatedAbilities.single()
        return policy.verdictFor(
            state, player1Id, "Goblin Diplomats",
            activation = ActivateAbility(player1Id, source, ability.id),
        )
    }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult = runner.run(
        AiPuzzle(
            id = "symmetric-forced-attack",
            category = PuzzleCategory.PRIORITY_TIMING,
            expectation = "",
            aiSeat = 1,
            position = position,
            check = check,
        ),
        AiProfile.LIVE,
    )

    private fun PuzzleResult.shouldSucceed() = withClue("chose $move — $failure") { passed shouldBe true }

    init {
        test("each creature attacking this turn reads as a forced attack") {
            CardIntentAnalyzer.analyze(cardRegistry.getCard("Goblin Diplomats")!!).tags shouldContain IntentTag.FORCED_ATTACK
        }

        test("rewarded before the opponent attacks, refused in our own turn") {
            diplomats(theirTurn = true)(scenario()).diplomatsVerdict()
                .shouldBeInstanceOf<TimingVerdict.Adjust>().reason shouldBe "forces their attack"
            diplomats(theirTurn = false)(scenario()).diplomatsVerdict() shouldBe TimingVerdict.NoWindow
        }

        test("not rewarded when tapping it leaves us no blocker") {
            diplomats(theirTurn = true, blocker = false)(scenario()).diplomatsVerdict()
                .let { (it as? TimingVerdict.Adjust)?.reason } shouldNotBe "forces their attack"
        }

        test("the AI activates Goblin Diplomats in the opponent's main phase, before they attack") {
            decide(diplomats(theirTurn = true)) { shouldActivate("Goblin Diplomats") }.shouldSucceed()
        }

        test("the AI does not activate Goblin Diplomats in its own main phase") {
            decide(diplomats(theirTurn = false)) { shouldNotActivate("Goblin Diplomats") }.shouldSucceed()
        }
    }
}
