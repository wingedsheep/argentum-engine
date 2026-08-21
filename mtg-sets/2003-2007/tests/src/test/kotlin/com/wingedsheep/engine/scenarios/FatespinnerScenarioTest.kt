package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.state.components.player.QueuedPhase
import com.wingedsheep.engine.state.components.player.SkippedTurnPartsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Fatespinner
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fatespinner (MRD #36) — "At the beginning of each opponent's upkeep, that player chooses draw
 * step, main phase, or combat phase. The player skips each instance of the chosen step or phase
 * this turn."
 *
 * Two claims worth pinning, because both are invisible if you only assert the end state:
 *
 *  - **The opponent chooses, and the opponent is skipped.** The trigger belongs to Fatespinner's
 *    controller but the decision is routed to the player whose upkeep it is, and the skip lands on
 *    that same player. Asserting only "a step got skipped" can't tell a correct routing from one
 *    that asked the wrong player.
 *  - **A skipped step is proceeded past as though it didn't exist** (CR 500.11 / 614.10) — the
 *    engine must not merely no-op inside it. These tests assert on the *step sequence the game
 *    actually walks*, so a draw step that happens but draws nothing would fail.
 *
 * The "main phase" case additionally proves the duration: "each instance ... this turn" means both
 * main phases (CR 505.1), not just the next one.
 */
class FatespinnerScenarioTest : FunSpec({

    // Hoisted out of the test bodies: `TestCards.all` forces a ClassGraph scan of the whole corpus,
    // and paying that inside a test puts it under the per-test timeout.
    val cards = TestCards.all + Fatespinner

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /**
     * Pass priority until a decision is raised, without `passPriorityUntil`'s auto-resolve — which
     * would answer Fatespinner's own prompt before the test could inspect who it went to.
     */
    fun GameTestDriver.passUntilDecision(maxPasses: Int = 60) {
        repeat(maxPasses) {
            if (state.pendingDecision != null) return
            state.priorityPlayerId?.let { passPriority(it) }
        }
        error("no decision was raised within $maxPasses passes (step ${state.step})")
    }

    /** Pass priority until the game leaves the current step, and report the step it landed on. */
    fun GameTestDriver.passUntilStepChanges(maxPasses: Int = 40): Step {
        val from = state.step
        repeat(maxPasses) {
            if (state.step != from) return state.step
            val holder = state.priorityPlayerId ?: error("nobody holds priority in ${state.step}")
            passPriority(holder)
        }
        error("the game never advanced past $from")
    }

    /** Every distinct step the game walks through on the way to [target], inclusive. */
    fun GameTestDriver.stepsUntil(target: Step, maxPasses: Int = 80): List<Step> {
        val seen = mutableListOf(state.step)
        repeat(maxPasses) {
            if (state.step == target) return seen
            val next = passUntilStepChanges()
            if (seen.lastOrNull() != next) seen.add(next)
        }
        error("never reached $target; walked $seen")
    }

    /**
     * Roll into the opponent's upkeep and answer Fatespinner's prompt with the named option,
     * asserting on the way that the prompt was put to the opponent rather than to its controller.
     */
    fun GameTestDriver.chooseAtOpponentUpkeep(option: String) {
        passUntilDecision()
        val decision = state.pendingDecision
        decision shouldNotBe null
        withClue("\"that player chooses\" — the prompt goes to the player whose upkeep it is, not to Fatespinner's controller") {
            decision!!.playerId shouldBe player2
        }
        val choice = decision as ChooseOptionDecision
        withClue("all three printed options are offered") {
            choice.options shouldBe listOf("Draw step", "Main phase", "Combat phase")
        }
        // Answering completes the ability's resolution — the prompt was raised mid-resolution,
        // so the chosen skip is applied as the continuation resumes.
        submitDecision(player2, OptionChosenResponse(choice.id, choice.options.indexOf(option)))
            .error shouldBe null
        state.pendingDecision shouldBe null
    }

    test("choosing the draw step skips it outright — the step never happens and no card is drawn") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Fatespinner")

        d.chooseAtOpponentUpkeep("Draw step")
        val handBefore = d.getHandSize(d.player2)

        withClue("the upkeep hands off straight to the precombat main phase — the draw step is proceeded past as though it didn't exist (CR 500.11)") {
            d.passUntilStepChanges() shouldBe Step.PRECOMBAT_MAIN
        }
        withClue("and so the active player drew nothing") {
            d.getHandSize(d.player2) shouldBe handBefore
        }
    }

    test("choosing the main phase skips BOTH main phases — 'each instance ... this turn'") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Fatespinner")

        d.chooseAtOpponentUpkeep("Main phase")

        withClue("the draw step is untouched — only the chosen part is skipped") {
            d.passUntilStepChanges() shouldBe Step.DRAW
        }
        withClue("the precombat main phase is skipped: the draw step hands off to the combat phase") {
            d.passUntilStepChanges() shouldBe Step.BEGIN_COMBAT
        }

        val rest = d.stepsUntil(Step.END)
        withClue("and the postcombat main phase is skipped too — CR 505.1 makes both of them 'the main phase', so 'each instance this turn' covers the second one. Walked: $rest") {
            rest shouldNotContain Step.POSTCOMBAT_MAIN
        }
    }

    test("choosing the combat phase skips every step of it") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Fatespinner")

        d.chooseAtOpponentUpkeep("Combat phase")

        d.passUntilStepChanges() shouldBe Step.DRAW
        d.passUntilStepChanges() shouldBe Step.PRECOMBAT_MAIN
        withClue("all five combat steps are skipped in one go — the precombat main phase hands off to the postcombat main phase") {
            d.passUntilStepChanges() shouldBe Step.POSTCOMBAT_MAIN
        }
    }

    test("an ADDITIONAL combat phase created later in the turn is skipped too") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Fatespinner")

        d.chooseAtOpponentUpkeep("Combat phase")
        // Aggravated Assault / Aurelia queue an extra combat phase during the turn; the queue is
        // what the TurnManager drains after the postcombat main phase. Seeding it directly keeps
        // this test about the skip rather than about paying for the card that creates the phase.
        d.addComponent(
            d.player2,
            AdditionalPhasesComponent(listOf(QueuedPhase(ExtraPhaseKind.COMBAT)))
        )

        d.passUntilStepChanges() shouldBe Step.DRAW
        d.passUntilStepChanges() shouldBe Step.PRECOMBAT_MAIN
        withClue("the natural combat phase is skipped") {
            d.passUntilStepChanges() shouldBe Step.POSTCOMBAT_MAIN
        }
        withClue("\"each instance ... this turn\" reaches the queued extra combat phase as well — it is drained and discarded, not entered") {
            d.passUntilStepChanges() shouldBe Step.END
        }
    }

    test("the skip is turn-scoped — the marker is gone once the turn ends") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Fatespinner")

        d.chooseAtOpponentUpkeep("Combat phase")
        withClue("the choice is recorded on the choosing player, not on Fatespinner's controller") {
            d.state.getEntity(d.player2)?.get<SkippedTurnPartsComponent>() shouldNotBe null
            d.state.getEntity(d.player1)?.get<SkippedTurnPartsComponent>() shouldBe null
        }

        // Walk out of player 2's turn and into player 1's upkeep; cleanup drops the marker on the
        // way. Fatespinner does not trigger on its own controller's upkeep, so nothing re-adds it.
        d.stepsUntil(Step.END)
        d.passPriorityUntil(Step.UPKEEP)
        d.state.activePlayerId shouldBe d.player1

        withClue("\"this turn\" — the marker does not survive into the next turn") {
            d.state.getEntity(d.player2)?.get<SkippedTurnPartsComponent>() shouldBe null
        }
    }
})
