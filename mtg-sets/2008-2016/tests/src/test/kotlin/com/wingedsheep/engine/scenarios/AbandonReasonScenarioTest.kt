package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.AbandonReason
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Abandon Reason (Eldritch Moon #115) — {2}{R} Instant.
 *
 * "Up to two target creatures each get +1/+0 and gain first strike until end of turn."
 * "Madness {1}{R}"
 */
class AbandonReasonScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AbandonReason)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
    }

    fun GameTestDriver.assertPumped(id: EntityId) {
        state.projectedState.getPower(id) shouldBe 3
        state.projectedState.getToughness(id) shouldBe 2
        state.projectedState.hasKeyword(id, Keyword.FIRST_STRIKE) shouldBe true
    }

    test("both targets get +1/+0 and first strike") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bearA = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val bearB = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val spell = driver.putCardInHand(me, "Abandon Reason")
        driver.giveMana(me, Color.RED, 3)
        driver.castSpell(me, spell, targets = listOf(bearA, bearB)).outcome shouldBe Outcome.Done
        settle(driver)

        driver.assertPumped(bearA)
        driver.assertPumped(bearB)
    }

    test("discarded, it can be cast for its madness cost of {1}{R}") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val spell = driver.putCardInHand(me, "Abandon Reason")
        val voice = driver.putCardInHand(me, "Tormenting Voice")
        driver.giveMana(me, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(spell)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.getExile(me) shouldContain spell
        driver.getGraveyard(me).shouldNotContain(spell)

        driver.giveMana(me, Color.RED, 2)
        settle(driver)
        driver.submitYesNo(me, true)
        driver.submitTargetSelection(me, listOf(bear))
        settle(driver)

        driver.assertPumped(bear)
        driver.getGraveyard(me) shouldContain spell
    }
})
