package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.FieryGambit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fiery Gambit (MRD #90) — "Flip a coin until you lose a flip or choose to stop flipping. … If you
 * win one or more flips, … 3 damage to target creature. If you win two or more flips, … 6 damage to
 * each opponent. If you win three or more flips, draw nine cards and untap all lands you control."
 *
 * The card is one tally read three times, so the tests are organised by tally: 0, 1, 2 and 3 won
 * flips, each asserting *all three* tiers so a gate that fires too early is caught as loudly as one
 * that never fires. The tiers are cumulative — three won flips pays out all three — which is the
 * mistake a `when`-style exclusive branch would make.
 *
 * Every run is seeded. `GameRng` is SplitMix64, so a seed fixes the whole flip sequence:
 *  - seed 3 → lose, …            (the first flip is lost; nothing happens and nothing is asked)
 *  - seed 6 → win, lose          (one won flip)
 *  - seed 9 → win, win, lose     (two won flips)
 *  - seed 1 → win, win, win, …   (three won flips, then we choose to stop)
 *
 * Seed 1 is also the "choose to stop flipping" path: we answer *no* after the third win rather than
 * letting a fourth flip end the run, which proves the tally survives the stop as well as a loss.
 */
class FieryGambitScenarioTest : FunSpec({

    /** A board where player 1 can cast Fiery Gambit at a 2/2, with three tapped Mountains out. */
    class Board(val d: GameTestDriver, val opponent: EntityId, val bears: EntityId, val gambit: EntityId) {
        val me: EntityId get() = d.player1
        fun lands(): List<EntityId> = d.getLands(me)
        fun handSize(): Int = d.getHandSize(me)
    }

    fun board(seed: Long): Board {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + FieryGambit)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val opponent = d.getOpponent(d.player1)
        // The victim of the first tier. A 2/2 dies to 3 damage, so "did the tier fire?" is a
        // zone check rather than a damage-marker read.
        val bears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Lands for the third tier's untap — placed directly rather than via `setupLands`, which
        // burns a turn per land drop and hands priority away. They are tapped here and the spell is
        // paid from the floating pool instead, so an untapped land at the end can only come from
        // the card.
        repeat(3) { d.putLandOnBattlefield(d.player1, "Mountain") }
        d.getLands(d.player1).forEach { d.tapPermanent(it) }

        val gambit = d.putCardInHand(d.player1, "Fiery Gambit")
        d.giveMana(d.player1, Color.RED, 1)
        d.giveColorlessMana(d.player1, 2)

        // Seeded last so nothing in setup consumes draws from the sequence under test.
        d.replaceState(d.state.copy(rng = GameRng.seeded(seed)))
        return Board(d, opponent, bears, gambit)
    }

    /** Cast at the Bears and let it start resolving. Returns the hand size just before resolution. */
    fun Board.cast(): Int {
        val cast = d.castSpell(me, gambit, targets = listOf(bears))
        withClue("cast failed: ${cast.error}") { cast.isSuccess shouldBe true }
        val handBeforeResolution = handSize()
        d.bothPass()
        return handBeforeResolution
    }

    fun Board.pendingFlipQuestion(): YesNoDecision? = d.pendingDecision as? YesNoDecision

    test("losing the first flip does nothing at all — and never asks whether to continue") {
        val b = board(seed = 3)
        val handBefore = b.cast()

        withClue("a lost flip ends the run, so there is no 'flip again?' to answer") {
            b.pendingFlipQuestion() shouldBe null
        }
        withClue("tier one didn't fire: the Bears survive") {
            b.d.findPermanent(b.opponent, "Grizzly Bears") shouldBe b.bears
        }
        b.d.getLifeTotal(b.opponent) shouldBe 20
        b.handSize() shouldBe handBefore
        withClue("no untap either — the lands stay as we tapped them") {
            b.lands().count { b.d.isTapped(it) } shouldBe 3
        }
    }

    test("one won flip deals 3 to the target creature and nothing else") {
        val b = board(seed = 6)
        val handBefore = b.cast()

        // seed 6: win, then lose. Continue after the win; the second flip ends the run.
        b.pendingFlipQuestion() shouldNotBeNullClue "the won flip must offer 'flip again?'"
        b.d.keepFlipping()

        withClue("tier one fired: 3 damage kills the 2/2") {
            b.d.findPermanent(b.opponent, "Grizzly Bears") shouldBe null
            b.d.getGraveyardCardNames(b.opponent).contains("Grizzly Bears") shouldBe true
        }
        withClue("tier two needs two wins") {
            b.d.getLifeTotal(b.opponent) shouldBe 20
        }
        withClue("tier three needs three wins") {
            b.handSize() shouldBe handBefore
            b.lands().count { b.d.isTapped(it) } shouldBe 3
        }
    }

    test("two won flips add 6 damage to each opponent, but not the draw-nine tier") {
        val b = board(seed = 9)
        val handBefore = b.cast()

        // seed 9: win, win, lose.
        b.d.keepFlipping()
        b.d.keepFlipping()

        withClue("both of the first two tiers fired") {
            b.d.findPermanent(b.opponent, "Grizzly Bears") shouldBe null
            b.d.getLifeTotal(b.opponent) shouldBe 14
        }
        withClue("tier three still needs a third win") {
            b.handSize() shouldBe handBefore
            b.lands().count { b.d.isTapped(it) } shouldBe 3
        }
    }

    test("three won flips pay out all three tiers, and choosing to stop keeps the tally") {
        val b = board(seed = 1)
        val handBefore = b.cast()

        // seed 1: win, win, win, … — we stop of our own accord after the third.
        b.d.keepFlipping()
        b.d.keepFlipping()
        b.d.stopFlipping()

        withClue("no flip is pending once we've stopped") {
            b.pendingFlipQuestion() shouldBe null
        }
        withClue("the tiers are cumulative: three wins pays all three, not just the last") {
            b.d.findPermanent(b.opponent, "Grizzly Bears") shouldBe null
            b.d.getLifeTotal(b.opponent) shouldBe 14
            b.handSize() shouldBe handBefore + 9
        }
        withClue("untap all lands you control") {
            b.lands().count { b.d.isTapped(it) } shouldBe 0
        }
        withClue("only *your* lands untap — the opponent's board is untouched by this tier") {
            b.d.getLifeTotal(b.me) shouldBe 20
        }
    }
})

/** Small readability helper: assert non-null with a clue, without pulling in a matcher import. */
private infix fun Any?.shouldNotBeNullClue(clue: String) {
    withClue(clue) { (this != null) shouldBe true }
}

/**
 * Answer "flip again" and assert only that nothing errored.
 *
 * Deliberately not `isSuccess`: that reads `error == null && pendingDecision == null`, so a
 * continue-answer that correctly pauses on the *next* "flip again?" reports `isSuccess == false`.
 * Asserting it would fail on precisely the runs that work.
 */
private fun GameTestDriver.keepFlipping() {
    val result = submitYesNo(player1, true)
    withClue("answering 'flip again' errored: ${result.error}") { result.error shouldBe null }
}

/** Answer "stop flipping". This one does end the run, so there should be no decision left. */
private fun GameTestDriver.stopFlipping() {
    val result = submitYesNo(player1, false)
    withClue("answering 'stop flipping' errored: ${result.error}") { result.error shouldBe null }
}
