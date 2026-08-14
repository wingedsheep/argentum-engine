package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

/**
 * The one feature, measured on its own rather than through an evaluator.
 *
 * `PuzzleSuiteTest` measures the outcome (`instants-09` and `-10`, with the rest of the category as
 * the control); this pins the *shape* of the floor. Four claims:
 *
 *  1. It is **scoped** to a permanent with flash — an instant and an ordinary creature are declined
 *     before any window reasoning runs.
 *  2. The window is **our whole turn, and theirs until attackers are known**. That is the entire
 *     behaviour: hold on ours, hold through their upkeep, release once they have committed.
 *  3. The **four payoffs** each decline it — haste, a stack object, an ETB that changes a combat,
 *     an ETB that hands us something to spend.
 *  4. It **ends**, by [Patience]'s releases rather than by any of its own.
 *
 * Where the claim is about the policy's shape the intent is synthesized, so the test says what it
 * means without depending on which cards a set happens to contain. Where the claim is about
 * *reading a real card* — the assumption the two puzzles rest on — the intent comes from the
 * analyzer.
 */
class AmbushWindowTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun intentFor(cardName: String) =
        intents.forName(cardName) ?: error("$cardName is not in the registry")

    /**
     * A quiet board: seat 1 holds the mana for the flash creature and a creature to defend, seat 2
     * holds the removal one test needs on the stack.
     *
     * [grip] is seat 1's filler only — [Patience]'s hand-size release reads our hand, never theirs.
     */
    private fun position(turn: Int = 3, grip: Int = 1, priorityTo: Int = 1) = scenario()
        .withPlayers()
        .withActivePlayer(priorityTo)
        .withTurnNumber(turn)
        .withLandsOnBattlefield(1, "Plains", 4)
        .withCardsInHand(1, "Craw Wurm", grip)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(2, "Swamp", 4)
        .withCardInHand(2, "Murder")
        .build()

    /**
     * Whether the floor applies to [intent] with seat 1 to act in [step] of [activeSeat]'s turn.
     *
     * The step and active player are set on the state directly rather than by advancing the game.
     * This is a test of one predicate over a position, not of how the position is reached — and
     * advancing would drag in untaps, draws and triggers that have nothing to do with the claim.
     */
    private fun TestGame.floors(
        intent: CardIntent,
        step: Step = Step.PRECOMBAT_MAIN,
        activeSeat: Int = 1,
    ): Boolean = AmbushWindow.holds(
        state.copy(
            step = step,
            phase = step.phase,
            activePlayerId = if (activeSeat == 1) player1Id else player2Id,
        ),
        player1Id,
        intent,
    )

    init {

        // ── 1. Scope ──

        test("Restoration Angel reads as a flash body that only ever touches our own board") {
            // The card that started this, read by the real analyzer. Three things are load-bearing
            // for the puzzles, and the third is the surprising one:
            //
            //  - `flashPermanent` is what gets it into the policy at all.
            //  - It carries none of the tags that pay off before the ambush.
            //  - It IS tagged REMOVAL/EXILE_REMOVAL, for blinking a creature *we control* — the
            //    `hitsAnotherPermanent` fail-open that [CardIntent.targetsOnlyOurPermanents]
            //    exists to work around. Pinned here so the workaround's premise is visible: if a
            //    later analyzer change makes the tag honest, this assertion fails and the field
            //    can be deleted rather than quietly kept forever.
            val angel = intentFor("Restoration Angel")
            angel.flashPermanent shouldBe true
            angel.hasHaste shouldBe false
            angel.speed shouldBe Speed.INSTANT
            angel.tags.filter { it in PAYS_OFF_BEFORE_THE_AMBUSH }.shouldBeEmpty()
            angel.tags.filter { it in ANSWERS_THEIR_BOARD }.shouldNotBeEmpty()
            angel.targetsOnlyOurPermanents shouldBe true
        }

        test("an instant and an ordinary creature are out of scope") {
            val game = position()
            // Speed.INSTANT, but spent when it resolves — its window is about what it *answers*,
            // which is a question [HoldPolicy]'s own branches ask and this one must not re-ask.
            game.floors(intentFor("Giant Growth")) shouldBe false
            // A permanent, but castable only in this window anyway: no later window to hold for.
            game.floors(intentFor("Grizzly Bears")) shouldBe false
        }

        // ── 2. The window ──

        test("every step of our own turn is floored") {
            val game = position()
            val angel = intentFor("Restoration Angel")
            for (step in Step.entries.filter { it.hasPriority }) {
                withClue("our own ${step.displayName}") {
                    game.floors(angel, step = step, activeSeat = 1) shouldBe true
                }
            }
        }

        test("their turn is floored until attackers are declared") {
            val game = position()
            val angel = intentFor("Restoration Angel")

            // Before the attack is known, holding still buys information.
            game.floors(angel, step = Step.UPKEEP, activeSeat = 2) shouldBe true
            game.floors(angel, step = Step.PRECOMBAT_MAIN, activeSeat = 2) shouldBe true
            game.floors(angel, step = Step.BEGIN_COMBAT, activeSeat = 2) shouldBe true

            // Attackers are in. This is the ambush, and the last window a blocker can still be
            // deployed in time — CR 509.1 declares blockers in the *next* step.
            game.floors(angel, step = Step.DECLARE_ATTACKERS, activeSeat = 2) shouldBe false

            // And the release that matters when they decline to attack at all: the engine skips the
            // declare-attackers step entirely then, so a policy releasing only there would hold the
            // card all the way to cleanup.
            game.floors(angel, step = Step.END, activeSeat = 2) shouldBe false
        }

        // ── 3. The four payoffs ──

        test("printed haste declines the floor") {
            // Raging Kavu — flash and haste on one card, which is the combination that exists
            // precisely so you can deploy it and attack with it.
            val kavu = intentFor("Raging Kavu")
            kavu.flashPermanent shouldBe true
            kavu.hasHaste shouldBe true
            position().floors(kavu) shouldBe false
        }

        test("something on the stack declines the floor") {
            // Seat 2 needs priority to put the spell there, which is why this position hands them
            // the turn. `floors` overwrites the active player on the state it tests either way, so
            // the assertions below are about the stack and nothing else.
            val game = position(priorityTo = 2)
            val angel = intentFor("Restoration Angel")
            game.floors(angel) shouldBe true

            game.castSpell(2, "Murder", game.findPermanent("Grizzly Bears")).error shouldBe null
            game.state.stack.shouldNotBeEmpty()
            game.floors(angel) shouldBe false
        }

        test("an ETB that clears a blocker declines the floor") {
            // Nebelgast Herald — flash, and a trigger that taps a creature an opponent controls.
            // Tapping a blocker is worth doing *before* we attack, and this policy has no way to
            // rank that window against the ambush, so it declines rather than guessing.
            //
            // The pair with the Restoration Angel case above, and the reason the side-of-the-table
            // question has to be asked: both cards carry a tag from [ANSWERS_THEIR_BOARD], and only
            // the Herald's targeting actually points across the table.
            val herald = intentFor("Nebelgast Herald")
            herald.flashPermanent shouldBe true
            herald.tags.filter { it in ANSWERS_THEIR_BOARD }.shouldNotBeEmpty()
            herald.targetsOnlyOurPermanents shouldBe false
            position().floors(herald) shouldBe false
        }

        test("an ETB that hands us a card declines the floor") {
            // Synthesized rather than taken from a set: the claim is about the tag, and pinning it
            // to whichever flash cantrip creature happens to be implemented is a worse test.
            val drawer = intentFor("Restoration Angel").copy(tags = setOf(IntentTag.DRAW))
            position().floors(drawer) shouldBe false
        }

        // ── 4. The releases, all inherited from Patience ──

        test("past Patience.SPENT_BY_TURN there is no floor") {
            val angel = intentFor("Restoration Angel")
            position(turn = Patience.SPENT_BY_TURN - 1).floors(angel) shouldBe true
            position(turn = Patience.SPENT_BY_TURN).floors(angel) shouldBe false
        }

        test("a hand at maximum size has no floor") {
            val angel = intentFor("Restoration Angel")
            // The Angel would itself be one of the cards in hand, so the filler is the maximum
            // minus it — six holds the floor, seven is already discarding something.
            position(grip = 6).floors(angel) shouldBe true
            position(grip = 7).floors(angel) shouldBe false
        }
    }

    private companion object {
        /**
         * The two tag sets [AmbushWindow] declines on. Duplicated here on purpose: the policy's own
         * copies are private, and a test that imported them could only ever agree with itself.
         */
        val PAYS_OFF_BEFORE_THE_AMBUSH = setOf(
            IntentTag.PUMP, IntentTag.ANTHEM, IntentTag.EVASION_GRANT, IntentTag.PROTECTION,
            IntentTag.DRAW, IntentTag.TUTOR, IntentTag.RAMP, IntentTag.RECURSION,
        )

        val ANSWERS_THEIR_BOARD = setOf(
            IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL, IntentTag.SWEEPER, IntentTag.NEUTRALIZE,
            IntentTag.TAPPER, IntentTag.FIGHT,
        )
    }
}
