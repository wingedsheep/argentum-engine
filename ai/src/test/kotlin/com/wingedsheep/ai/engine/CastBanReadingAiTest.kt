package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cast bans aimed at opponents — Silence's "your opponents can't cast spells this turn", Sphinx's
 * Decree's and Azor's "during that player's next turn", Orim's Chant's "target player".
 *
 * The ban moves no permanent, so the board simulation sees a card spent for nothing. Unread, the
 * AI didn't know the card was disruption at all, could aim Orim's Chant at itself, and had no reason
 * to cast an instant-speed ban in the one window it covers a whole turn: the opponent's upkeep.
 */
class CastBanReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val policy by lazy { HoldPolicy(intents, castBansDenyTheirTurn = true) }
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun tags(name: String) = CardIntentAnalyzer.analyze(cardRegistry.getCard(name)!!).tags

    /** Silence and a Plains, the opponent holding two cards and the Forests to cast them; [theirUpkeep] or our own precombat main. */
    private fun silence(theirUpkeep: Boolean) = { s: ScenarioBuilder ->
        s.withPlayers()
            .withTurnNumber(4)
            .let { if (theirUpkeep) it.withActivePlayer(2).inPhase(Phase.BEGINNING, Step.UPKEEP) else it }
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(2, "Forest", 3)
            .withCardInHand(1, "Silence")
            .withCardInHand(2, "Grizzly Bears")
            .withCardInHand(2, "Giant Growth")
            .let { b -> (1..10).fold(b) { acc, _ -> acc.withCardInLibrary(1, "Plains").withCardInLibrary(2, "Forest") } }
            .build()
            .let { if (theirUpkeep) it.advanceToPriority(1, Step.UPKEEP) else it }
    }

    /** Sphinx's Decree and two Plains in our precombat main; the opponent holds [theirHand]. */
    private fun decree(vararg theirHand: String) = { s: ScenarioBuilder ->
        s.withPlayers()
            .withTurnNumber(4)
            .withLandsOnBattlefield(1, "Plains", 2)
            .withLandsOnBattlefield(2, "Mountain", 3)
            .withCardInHand(1, "Sphinx's Decree")
            .let { theirHand.fold(it) { b, card -> b.withCardInHand(2, card) } }
            .let { b -> (1..10).fold(b) { acc, _ -> acc.withCardInLibrary(1, "Plains").withCardInLibrary(2, "Mountain") } }
            .build()
    }

    private fun TestGame.silenceVerdict(): TimingVerdict =
        policy.verdictFor(state, player1Id, "Silence", cast = CastSpell(player1Id, findCardsInHand(1, "Silence").first()))

    private val liveWithoutFlag = AiProfile.LIVE.copy(id = "live-without-cast-bans", castBansDenyTheirTurn = false)

    private fun decide(
        position: (ScenarioBuilder) -> TestGame,
        profile: AiProfile = AiProfile.LIVE,
        check: PuzzleMove.() -> Unit,
    ): PuzzleResult = runner.run(
        AiPuzzle(
            id = "cast-ban",
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
        test("a ban on opponents reads as a cast ban; one that binds its controller too is a price, not a payoff") {
            listOf("Silence", "Sphinx's Decree", "Azor, the Lawbringer", "Xantid Swarm", "Orim's Chant").forEach { name ->
                withClue("$name stops an opponent casting spells") { tags(name) shouldContain IntentTag.CAST_BAN }
            }
            listOf("Conduit of Worlds", "Calamity's Wake").forEach { name ->
                withClue("$name bans its own controller too") { tags(name) shouldNotContain IntentTag.CAST_BAN }
            }
        }

        test("Orim's Chant's target player is aimed at an opponent") {
            intents.spellTargetPolarities("Orim's Chant") shouldBe listOf(TargetPolarity.HARMFUL)
        }

        test("Silence is rewarded in the opponent's upkeep and neutral in our own main phase") {
            val upkeep = silence(theirUpkeep = true)(scenario())
            upkeep.silenceVerdict().shouldBeInstanceOf<TimingVerdict.Adjust>().reason shouldBe "ban denies their turn"
            silence(theirUpkeep = false)(scenario()).silenceVerdict() shouldBe TimingVerdict.Neutral
        }

        test("the AI stops in the opponent's upkeep and casts Silence there; without the flag it passes") {
            decide(silence(theirUpkeep = true), liveWithoutFlag) { shouldPass() }.shouldSucceed()
            decide(silence(theirUpkeep = true)) { shouldCast("Silence") }.shouldSucceed()
        }

        test("the engine AI controller asks the server for the opponent's upkeep only while it holds a live ban") {
            val upkeep = silence(theirUpkeep = true)(scenario())
            withClue("the server's smart auto-pass would skip this window; the seat asks to keep it") {
                EngineAiPlayerController(cardRegistry, upkeep.player1Id, { upkeep.state }).wantsPriority(upkeep.state) shouldBe true
            }
            val ownMain = silence(theirUpkeep = false)(scenario())
            EngineAiPlayerController(cardRegistry, ownMain.player1Id, { ownMain.state }).wantsPriority(ownMain.state) shouldBe false
        }

        test("the AI holds Silence in its own main phase, where it denies only their responses") {
            decide(silence(theirUpkeep = false)) { shouldPass() }.shouldSucceed()
        }

        test("the AI casts Sphinx's Decree with idle mana while the opponent holds cards; without the flag it never does") {
            decide(decree("Lightning Bolt", "Shock"), liveWithoutFlag) { shouldPass() }.shouldSucceed()
            decide(decree("Lightning Bolt", "Shock")) { shouldCast("Sphinx's Decree") }.shouldSucceed()
        }

        test("the AI keeps Sphinx's Decree when the opponent's hand is empty — there is nothing to deny") {
            decide(decree()) { shouldPass() }.shouldSucceed()
        }
    }
}
