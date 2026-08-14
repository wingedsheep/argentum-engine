package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Cool but Rude ({1}{R} Enchantment — Class) — Through the Mists.
 *
 * Level 1: "Whenever you attack, you may discard a card. If you do, draw a card."
 *
 * The draw is gated on the *discard actually happening* ([com.wingedsheep.sdk.dsl.Effects.IfYouDo]),
 * not on the yes/no answer — an empty hand can't discard, so it can't draw either. That case is also
 * declared infeasible up front, so this attack trigger stops asking an unanswerable question every
 * single combat. (Contrast Miasma Demon's "you may discard any number of cards", where discarding
 * zero cards is a legal way to perform the action and the payoff does happen.)
 */
class CoolButRudeScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
    }

    fun GameTestDriver.emptyHand(playerId: EntityId) {
        val handZone = ZoneKey(playerId, Zone.HAND)
        var emptied = state
        getHand(playerId).toList().forEach { card -> emptied = emptied.removeFromZone(handZone, card) }
        replaceState(emptied)
    }

    /** Drain the attack triggers, answering the "you may discard a card" yes/no with [discard]. */
    fun GameTestDriver.resolveAttackTriggers(you: EntityId, discard: Boolean): Boolean {
        var asked = false
        var guard = 0
        while (guard++ < 40) {
            when (val dec = pendingDecision) {
                is YesNoDecision -> { asked = true; submitYesNo(you, discard) }
                is SelectCardsDecision ->
                    submitCardSelection(you, dec.options.take(dec.minSelections.coerceAtLeast(1)))
                else -> if (state.stack.isNotEmpty()) bothPass() else return asked
            }
        }
        return asked
    }

    test("discarding on attack draws a replacement card") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)

        d.putPermanentOnBattlefield(you, "Cool but Rude")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.removeSummoningSickness(bear)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val handBefore = d.getHandSize(you)
        d.declareAttackers(you, listOf(bear), opponent)

        d.resolveAttackTriggers(you, discard = true) shouldBe true

        // One card discarded, one drawn — hand size unchanged, graveyard up by one.
        d.getHandSize(you) shouldBe handBefore
        d.getGraveyard(you).size shouldBe 1
    }

    test("declining the discard draws nothing") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)

        d.putPermanentOnBattlefield(you, "Cool but Rude")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.removeSummoningSickness(bear)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val handBefore = d.getHandSize(you)
        d.declareAttackers(you, listOf(bear), opponent)

        d.resolveAttackTriggers(you, discard = false) shouldBe true

        d.getHandSize(you) shouldBe handBefore
        d.getGraveyard(you).isEmpty() shouldBe true
    }

    test("an empty hand is never asked, and never draws") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)

        d.putPermanentOnBattlefield(you, "Cool but Rude")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.removeSummoningSickness(bear)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        // Empty the hand *after* the draw step, so nothing is left to discard.
        d.emptyHand(you)
        d.getHandSize(you) shouldBe 0

        d.declareAttackers(you, listOf(bear), opponent)

        d.resolveAttackTriggers(you, discard = true) shouldBe false

        // No discard means no draw: the payoff hangs off the action, not off answering "yes".
        d.getHandSize(you) shouldBe 0
        d.getGraveyard(you).isEmpty() shouldBe true
    }
})
