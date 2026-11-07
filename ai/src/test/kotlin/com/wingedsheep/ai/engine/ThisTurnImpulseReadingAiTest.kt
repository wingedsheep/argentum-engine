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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * A "this turn" impulse we time ourselves — "exile the top three cards of your library. Until end
 * of turn, you may play those cards" (Act on Impulse), "{R}, sacrifice two artifacts: exile the top
 * card of your library. You may play that card this turn" (Scion of Opulence) — reads as
 * [IntentTag.IMPULSE]: cards to spend before cleanup, not cards banked. It is not [IntentTag.DRAW]
 * (that earns the end-step window, where these cards would evaporate unplayed); it earns its own
 * window instead — a main phase of ours with mana left over to play what it exiles.
 */
class ThisTurnImpulseReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("'$name' is not in the corpus")

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "this-turn-impulse",
                category = PuzzleCategory.PRIORITY_TIMING,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    /**
     * Act on Impulse as the only card in hand, six Mountains up in our last main phase, the library
     * one-mana creatures: three Mountains pay for the impulse and three are left to play what it
     * exiles. Passing empties all six at cleanup.
     */
    private val impulseWithManaToSpare = { s: ScenarioBuilder ->
        s.withPlayers()
            .withActivePlayer(1)
            .withLandsOnBattlefield(1, "Mountain", 6)
            .withCardInHand(1, "Act on Impulse")
            .apply { repeat(12) { withCardInLibrary(1, "Raging Goblin") } }
            .apply { repeat(12) { withCardInLibrary(2, "Island") } }
            .build()
            .advanceToPriority(1, Step.POSTCOMBAT_MAIN)
    }

    /** The same, with only the three Mountains Act on Impulse itself costs: nothing left to play what it exiles. */
    private val impulseWithNoManaToSpare = { s: ScenarioBuilder ->
        s.withPlayers()
            .withActivePlayer(1)
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withCardInHand(1, "Act on Impulse")
            .apply { repeat(12) { withCardInLibrary(1, "Raging Goblin") } }
            .apply { repeat(12) { withCardInLibrary(2, "Island") } }
            .build()
            .advanceToPriority(1, Step.POSTCOMBAT_MAIN)
    }

    init {
        test("a this-turn impulse we time ourselves reads as an impulse, not as card draw") {
            val tags = CardIntentAnalyzer.analyze(card("Act on Impulse")).tags
            tags shouldContain IntentTag.IMPULSE
            tags shouldNotContain IntentTag.DRAW
            CardIntentAnalyzer.analyzeSelf(card("Harnesser of Storms")).tags shouldNotContain IntentTag.DRAW
        }

        test("an impulse activation reads fully, and the audit finds no intent gap") {
            withClue("Scion of Opulence's sacrifice-two-artifacts impulse knows what it is for") {
                AiReadingAudit.audit(card("Scion of Opulence")).shouldBeEmpty()
                AiReadingAudit.audit(card("Act on Impulse")).shouldBeEmpty()
            }
        }

        test("a lasting impulse still reads as card draw, not as a this-turn impulse") {
            val tags = CardIntentAnalyzer.analyze(card("Burning Curiosity")).tags
            tags shouldContain IntentTag.DRAW
            tags shouldNotContain IntentTag.IMPULSE
        }

        test("the draft heuristic prices a this-turn impulse above nothing, below a lasting one") {
            fun spell(name: String) = LimitedCardRater.explain(card(name)).heuristic.firstOrNull { it.part == "Spell" }?.value ?: 0.0
            val impulse = spell("Act on Impulse")
            val lasting = spell("Burning Curiosity")
            withClue("Act on Impulse's spell term $impulse, Burning Curiosity's $lasting") {
                (impulse > 0.0) shouldBe true
                (impulse < lasting) shouldBe true
            }
        }

        test("an impulse is cast in our main phase with mana left to play what it exiles") {
            val result = decide(impulseWithManaToSpare) { shouldCast("Act on Impulse") }
            withClue("a this-turn impulse with three mana to spare after it is three goblins, not three idle Mountains: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("an impulse with no mana left to play what it exiles is not cast") {
            val result = decide(impulseWithNoManaToSpare) { shouldPass() }
            withClue("three Mountains pay for the impulse and leave nothing to play the exiled cards with: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
