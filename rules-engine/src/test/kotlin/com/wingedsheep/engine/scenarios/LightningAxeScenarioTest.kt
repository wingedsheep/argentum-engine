package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tsp.TimeSpiralSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Lightning Axe — {R} Instant
 * As an additional cost to cast this spell, discard a card or pay {5}.
 * Lightning Axe deals 5 damage to target creature.
 *
 * The binary additional cost is a cast-time [ChooseOptionDecision] between the two modes; both
 * resolve the same 5-damage effect and differ only in the additional cost paid.
 */
class LightningAxeScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + TimeSpiralSet.cards)
        return d
    }

    /** Answer the cast-time decisions: pick [modeIndex], target [victim], discard [discardFodder]. */
    fun GameTestDriver.finishCast(p1: EntityId, modeIndex: Int, victim: EntityId, discardFodder: EntityId?) {
        var guard = 0
        while (pendingDecision != null && guard++ < 12) {
            when (val dec = pendingDecision) {
                is ChooseOptionDecision -> submitDecision(p1, OptionChosenResponse(dec.id, modeIndex))
                is ChooseTargetsDecision -> submitTargetSelection(p1, listOf(victim))
                is SelectCardsDecision -> submitCardSelection(p1, listOf(discardFodder!!))
                else -> error("unexpected decision ${dec!!::class.simpleName}")
            }
        }
    }

    test("discard mode: discard a card and deal 5 damage to the target creature") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = d.putCreatureOnBattlefield(p2, "Centaur Courser") // 3/3
        val axe = d.putCardInHand(p1, "Lightning Axe")
        val fodder = d.putCardInHand(p1, "Centaur Courser")
        d.giveMana(p1, Color.RED, 1)

        d.submit(CastSpell(playerId = p1, cardId = axe, paymentStrategy = PaymentStrategy.FromPool))
            .isPaused shouldBe true
        d.finishCast(p1, modeIndex = 0, victim = victim, discardFodder = fodder)
        d.bothPass()

        // 5 damage kills the 3/3; the discarded card and the spent spell are in the graveyard.
        d.getGraveyard(p2) shouldContain victim
        d.getGraveyard(p1) shouldContain fodder
        d.getHand(p1) shouldNotContain fodder
    }

    test("pay mode: pay {5} and deal 5 damage without discarding") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = d.putCreatureOnBattlefield(p2, "Centaur Courser") // 3/3
        val axe = d.putCardInHand(p1, "Lightning Axe")
        val keeper = d.putCardInHand(p1, "Centaur Courser")
        d.giveMana(p1, Color.RED, 1)
        d.giveColorlessMana(p1, 5)

        d.submit(CastSpell(playerId = p1, cardId = axe, paymentStrategy = PaymentStrategy.FromPool))
            .isPaused shouldBe true
        d.finishCast(p1, modeIndex = 1, victim = victim, discardFodder = null)
        d.bothPass()

        // Creature dies; no card was discarded (pay mode).
        d.getGraveyard(p2) shouldContain victim
        d.getHand(p1) shouldContain keeper
    }
})
