package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.MindstormCrown
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mindstorm Crown (MRD #207) — "At the beginning of your upkeep, draw a card if you had no cards
 * in hand at the beginning of this turn. If you had a card in hand, this artifact deals 1 damage
 * to you."
 *
 * The card is a test of *when* the hand is measured, not of what the two branches do. Every test
 * here runs the Crown through player 1's upkeep with the same board and reads the same two
 * numbers (life, hand size); only the hand's history differs. The last test is the one that
 * matters: it makes the live hand and the turn-start hand disagree, so the lookalike
 * implementation — `Conditions.EmptyHand`, a hand read at resolution — fails there and passes
 * everywhere else.
 *
 * Setup runs on player 2's turn so that player 1's *next* untap step is one transition away: the
 * snapshot has to be taken by the engine at that turn boundary, not planted by the test.
 */
class MindstormCrownScenarioTest : FunSpec({

    /** Player 2's precombat main, with a Mindstorm Crown already on player 1's battlefield. */
    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + MindstormCrown)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(d.player1, "Mindstorm Crown")
        return d
    }

    fun GameTestDriver.emptyHand(playerId: EntityId) {
        val hand = ZoneKey(playerId, Zone.HAND)
        replaceState(state.copy(zones = state.zones + (hand to emptyList())))
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && d.state.stack.isNotEmpty() && !d.isPaused) d.bothPass()
    }

    test("an empty hand at the turn's start draws a card and costs no life") {
        val d = driver()
        d.emptyHand(d.player1)

        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe d.player1
        resolveStack(d)

        withClue("the draw branch ran") {
            d.getHandSize(d.player1) shouldBe 1
        }
        withClue("and only that branch — the two halves are exclusive") {
            d.getLifeTotal(d.player1) shouldBe 20
        }
    }

    test("holding a card at the turn's start costs 1 life and draws nothing") {
        val d = driver()
        d.emptyHand(d.player1)
        d.putCardInHand(d.player1, "Dark Ritual")

        d.passPriorityUntil(Step.UPKEEP)
        resolveStack(d)

        d.getLifeTotal(d.player1) shouldBe 19
        withClue("no card was drawn — the hand is untouched") {
            d.getHandSize(d.player1) shouldBe 1
        }
    }

    test("emptying your hand after the turn began does not buy you the draw") {
        // The discriminating case. One card in hand when the turn starts, then it is cast in
        // response to the Crown's own trigger, so by the time the ability resolves the hand is
        // empty. A live "you have no cards in hand" read draws here; the printed card bills you.
        val d = driver()
        d.emptyHand(d.player1)
        val ritual = d.putCardInHand(d.player1, "Dark Ritual")

        d.passPriorityUntil(Step.UPKEEP)
        withClue("the Crown's trigger is waiting on the stack") {
            d.state.stack.isNotEmpty() shouldBe true
        }

        // Respond to the trigger rather than letting it resolve, so make sure player 1 is the one
        // holding priority first.
        if (d.priorityPlayer != d.player1) d.passPriority(d.priorityPlayer!!)
        d.giveMana(d.player1, Color.BLACK, 1)
        d.castSpell(d.player1, ritual).isSuccess shouldBe true
        withClue("the hand is empty before the trigger resolves") {
            d.getHandSize(d.player1) shouldBe 0
        }
        resolveStack(d)

        withClue("the answer was fixed at the turn's beginning, when a card was held") {
            d.getLifeTotal(d.player1) shouldBe 19
            d.getHandSize(d.player1) shouldBe 0
        }
    }
})
