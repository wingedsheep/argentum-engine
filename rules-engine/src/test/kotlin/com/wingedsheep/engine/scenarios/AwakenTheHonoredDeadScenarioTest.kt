package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Awaken the Honored Dead ({B}{G}{U} Enchantment — Saga) — Tarkir: Dragonstorm.
 *
 * "III — You may discard a card. When you do, return target creature or land card from your
 *  graveyard to your hand."
 *
 * Chapter III is a reflexive trigger (CR 603.12): the return targets nothing when the chapter
 * triggers, only when the *reflexive* ability goes on the stack after a card is actually discarded.
 * Chapter II has milled three cards by then, so the graveyard always holds legal targets — which is
 * what makes the empty-hand case worth pinning. Without the feasibility gate the discard pipeline
 * would auto-select nothing, report success, and hand out a free return.
 */
class AwakenTheHonoredDeadScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.emptyHand(playerId: EntityId) {
        val handZone = ZoneKey(playerId, Zone.HAND)
        var emptied = state
        getHand(playerId).toList().forEach { card -> emptied = emptied.removeFromZone(handZone, card) }
        replaceState(emptied)
    }

    /** Advance to the active player's *next* precombat main. */
    fun GameTestDriver.advanceToNextTurnMain() {
        passPriorityUntil(Step.END, maxPasses = 300)
        passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
    }

    /**
     * Drain pending decisions and stack objects. Answers any "you may discard" with [discard] and
     * records whether a reflexive target was ever chosen.
     */
    fun GameTestDriver.settle(you: EntityId, discard: Boolean = false): Boolean {
        var targeted = false
        var guard = 0
        while (guard++ < 120) {
            when (val dec = pendingDecision) {
                is YesNoDecision -> submitYesNo(you, discard)
                is SelectCardsDecision ->
                    submitCardSelection(you, dec.options.take(dec.minSelections.coerceAtLeast(1)))
                is ChooseTargetsDecision -> {
                    submitTargetSelection(you, dec.legalTargets[0].orEmpty().take(1))
                    targeted = true
                }
                else -> if (state.stack.isNotEmpty()) bothPass() else return targeted
            }
        }
        return targeted
    }

    /**
     * Cast the Saga and settle chapter I (which destroys a target nonland permanent — an opponent
     * creature is provided so the chapter has something legal to point at).
     */
    fun GameTestDriver.castSaga(you: EntityId, opponent: EntityId): EntityId {
        putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val spell = putCardInHand(you, "Awaken the Honored Dead")
        giveMana(you, Color.BLACK, 1)
        giveMana(you, Color.GREEN, 1)
        giveMana(you, Color.BLUE, 1)
        castSpell(you, spell)
        settle(you)
        return findPermanent(you, "Awaken the Honored Dead")!!
    }

    test("chapter III on an empty hand: no discard prompt and nothing returns from the graveyard") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)

        val saga = d.castSaga(you, opponent)
        saga shouldNotBe null

        d.advanceToNextTurnMain() // opponent's turn
        d.advanceToNextTurnMain() // chapter II — mills three
        d.settle(you)
        d.advanceToNextTurnMain() // opponent's turn
        d.advanceToNextTurnMain() // chapter III triggers

        // Empty the hand while the chapter III trigger is still waiting to resolve.
        d.emptyHand(you)
        val graveyardBefore = d.getGraveyard(you).toSet()
        graveyardBefore.isEmpty() shouldBe false // chapter II milled, so a legal return target exists

        // Answering "yes" is impossible — the question must never be asked, and the reflexive
        // return must never go on the stack.
        d.settle(you, discard = true) shouldBe false
        d.getHandSize(you) shouldBe 0
        // Nothing left the graveyard. It *grows* by the Saga itself, sacrificed after its final
        // chapter (CR 714.4), so assert on the contents rather than the count.
        d.getGraveyard(you).toSet().containsAll(graveyardBefore) shouldBe true
    }
})
