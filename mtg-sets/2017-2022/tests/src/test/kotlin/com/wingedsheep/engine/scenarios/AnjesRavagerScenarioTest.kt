package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c19.cards.AnjesRavager
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Anje's Ravager (Commander 2019 #22) — {2}{R} Creature — Vampire Berserker 3/3.
 *
 * "This creature attacks each combat if able."
 * "Whenever this creature attacks, discard your hand, then draw three cards."
 * "Madness {1}{R}"
 */
class AnjesRavagerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AnjesRavager)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
    }

    test("attacking discards your whole hand, then draws three cards") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ravager = driver.putCreatureOnBattlefield(me, "Anje's Ravager")
        driver.removeSummoningSickness(ravager)
        val bearA = driver.putCardInHand(me, "Grizzly Bears")
        val bearB = driver.putCardInHand(me, "Grizzly Bears")
        val handBefore = driver.getHand(me)
        (handBefore.size > 3) shouldBe true

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(ravager), opponent)
        settle(driver)

        driver.getHandSize(me) shouldBe 3
        handBefore.forEach { driver.getHand(me) shouldNotContain it }
        driver.getGraveyard(me) shouldContain bearA
        driver.getGraveyard(me) shouldContain bearB
    }

    test("discarded, it is exiled and can be cast for its madness cost of {1}{R}") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ravager = driver.putCardInHand(me, "Anje's Ravager")
        val voice = driver.putCardInHand(me, "Tormenting Voice")
        driver.giveMana(me, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(ravager)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.getExile(me) shouldContain ravager
        driver.getGraveyard(me).shouldNotContain(ravager)

        driver.giveMana(me, Color.RED, 2)
        settle(driver)
        driver.submitYesNo(me, true)
        settle(driver)

        driver.findPermanent(me, "Anje's Ravager").shouldNotBeNull()
    }
})
