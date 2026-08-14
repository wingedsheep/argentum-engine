package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyDrawAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A draw instruction whose announcement-level replacement (CR 121.2a) required a player
 * choice must still be performed once the choice is made.
 *
 * `ModifyDrawAmount` fires against
 * [com.wingedsheep.engine.replacement.PendingGameEvent.DrawAmountPending] at the announcement
 * site. When two of them compete, `ReplacementEffectProcessor` pauses with a
 * `ChooseOptionDecision` (CR 616.1) — and nothing used to resume the draw afterwards:
 * `DrawAmountPending` pushed no frame to carry the instruction forward, and
 * `ReplacementContinuationResumer.handleReplacedOutcome` gated execution on
 * `ReplacementOutcome.Replaced`, so a `Modified` outcome — all `ModifyDrawAmount` ever
 * produces — fell through and the modified event was discarded. The spell resolved,
 * prompted, and then drew nothing, with no error.
 *
 * The fix is [com.wingedsheep.engine.replacement.PendingGameEvent.performContinuation]: a
 * `Modified` outcome leaves the event still to happen, so the modified event supplies the
 * frame that performs it on resume.
 *
 * This is invisible to `ReplacementEffectProcessorTest`'s two-effect test, which asserts only
 * the `Paused` structure and then *simulates* the rest by hand-stamping
 * `GameState.activeReplacementChain` — it never submits the decision, which is the code that
 * actually runs in a game.
 *
 * The competing effect is a test-only card because a multiplier and a modifier are what make
 * the ordering observable, and Quantum Riddler is the only shipped announcement-level
 * `ModifyDrawAmount`. Two Riddlers no longer reach this path at all — they produce the same
 * total either way, so the processor skips the prompt (see `ReplacementChoiceTest`).
 */
class DrawAmountReplacementChoiceTest : ScenarioTestBase() {

    /** "If you would draw one or more cards, you draw twice that many instead." */
    private val doubler = card("Test Draw Doubler") {
        manaCost = "{2}{U}"
        typeLine = "Enchantment"
        replacementEffect(
            ModifyDrawAmount(
                multiplier = 2,
                appliesTo = EventPattern.DrawCardsEvent(),
            )
        )
    }

    init {
        cardRegistry.register(doubler)

        test("a single Quantum Riddler modifies the announced draw without prompting") {
            // The control. One announcement-level ModifyDrawAmount has nothing to compete with,
            // so the processor auto-applies it and never reaches the choice path.
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Quantum Riddler")
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Divination")
            withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("Only one replacement applies, so there is no choice to present") {
                (game.state.pendingDecision is ChooseOptionDecision) shouldBe false
            }
            withClue(
                "Casting Divination empties the hand, so Quantum Riddler's CardsInHandAtMost(1) " +
                    "holds and the announced draw of 2 becomes 3 (CR 121.2a)."
            ) {
                game.handSize(1) shouldBe 3
            }
        }

        test("a +1 and a doubler: the draw still happens after the player resolves the choice") {
            // (2+1)*2 = 6 vs (2*2)+1 = 5, so CR 616.1 genuinely hands the order to the player.
            // Whichever they pick, both apply exactly once (CR 614.5 + 616.1f) and — the point
            // of this test — the instruction must still draw.
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Quantum Riddler")
                .withCardOnBattlefield(1, "Test Draw Doubler")
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val libraryBefore = game.librarySize(1)

            val cast = game.castSpell(1, "Divination")
            withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val decision = game.state.pendingDecision
            withClue(
                "A modifier and a multiplier give different totals depending on order, so the " +
                    "affected player chooses which applies first (CR 616.1). Got: $decision"
            ) {
                (decision is ChooseOptionDecision) shouldBe true
            }
            decision as ChooseOptionDecision
            withClue("Both replacements should be offered") {
                decision.options.size shouldBe 2
            }
            withClue("Options a player cannot tell apart are options they cannot answer") {
                decision.options.toSet().size shouldBe 2
            }

            val chosenIsRiddler = decision.options[0].startsWith("Quantum Riddler")
            val expectedHand = if (chosenIsRiddler) 6 else 5

            val answer = game.submitDecision(OptionChosenResponse(decision.id, 0))
            withClue("Answering the replacement choice should not error: ${answer.error}") {
                answer.error shouldBe null
            }
            game.resolveStack()

            withClue("The choice is resolved, so nothing should still be pending") {
                game.state.pendingDecision shouldBe null
            }
            withClue(
                "Divination announces 2; applying \"${decision.options[0]}\" first gives " +
                    "$expectedHand. Holding 0 means the modified draw instruction was dropped."
            ) {
                game.handSize(1) shouldBe expectedHand
            }
            withClue("Every card in hand came off the library") {
                game.librarySize(1) shouldBe libraryBefore - expectedHand
            }
        }
    }
}
