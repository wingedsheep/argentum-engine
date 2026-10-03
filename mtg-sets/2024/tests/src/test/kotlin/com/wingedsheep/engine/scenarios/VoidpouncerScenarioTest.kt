package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.Voidpouncer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Voidpouncer (MH3) — {1}{R} Creature — Eldrazi 3/1, Devoid, Kicker {2}{C}.
 * "If this creature was kicked, it enters with two +1/+1 counters and a trample counter on it
 * and with haste."
 */
class VoidpouncerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Voidpouncer)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.counters(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    test("kicked: enters as a 5/3 with a trample counter, trample and haste, and can attack") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        driver.giveMana(you, Color.RED, 4)
        driver.giveColorlessMana(you, 1)
        val card = driver.putCardInHand(you, "Voidpouncer")
        driver.submit(
            CastSpell(
                playerId = you,
                cardId = card,
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        driver.bothPass()

        driver.stackSize shouldBe 0
        val perm = driver.findPermanent(you, "Voidpouncer")!!
        driver.counters(perm, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        driver.counters(perm, CounterType.TRAMPLE) shouldBe 1
        val projected = driver.state.projectedState
        projected.getPower(perm) shouldBe 5
        projected.getToughness(perm) shouldBe 3
        projected.hasKeyword(perm, Keyword.TRAMPLE) shouldBe true
        projected.hasKeyword(perm, Keyword.HASTE) shouldBe true

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(perm), defendingPlayer = opponent).error shouldBe null
    }

    test("unkicked: enters as a plain 3/1 with no counters, trample or haste") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        driver.giveMana(you, Color.RED, 2)
        val card = driver.putCardInHand(you, "Voidpouncer")
        driver.castSpell(you, card).error shouldBe null
        driver.bothPass()

        val perm = driver.findPermanent(you, "Voidpouncer")!!
        driver.counters(perm, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        driver.counters(perm, CounterType.TRAMPLE) shouldBe 0
        val projected = driver.state.projectedState
        projected.getPower(perm) shouldBe 3
        projected.getToughness(perm) shouldBe 1
        projected.hasKeyword(perm, Keyword.TRAMPLE) shouldBe false
        projected.hasKeyword(perm, Keyword.HASTE) shouldBe false
    }
})
