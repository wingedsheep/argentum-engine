package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
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
 *
 * Both chosen targets get the pump and first strike; discarded, the card is exiled and cast for its
 * madness cost while the madness trigger resolves (CR 702.35).
 */
class AbandonReasonScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
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

    fun shouldBePumped(driver: GameTestDriver, creature: EntityId) {
        driver.state.projectedState.getPower(creature) shouldBe 3
        driver.state.projectedState.getToughness(creature) shouldBe 2
        driver.state.projectedState.hasKeyword(creature, Keyword.FIRST_STRIKE) shouldBe true
    }

    test("cast for its printed cost, both target creatures get +1/+0 and first strike") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bearsA = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val bearsB = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spell = driver.putCardInHand(player, "Abandon Reason")
        driver.giveMana(player, Color.RED, 3)

        driver.castSpell(player, spell, targets = listOf(bearsA, bearsB)).outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        shouldBePumped(driver, bearsA)
        shouldBePumped(driver, bearsB)
    }

    test("discarded, it is exiled and can be cast for its madness cost of {1}{R}") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spell = driver.putCardInHand(player, "Abandon Reason")

        // Discard it as Tormenting Voice's additional cost.
        val voice = driver.putCardInHand(player, "Tormenting Voice")
        driver.giveMana(player, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(spell)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.getExile(player) shouldContain spell
        driver.getGraveyard(player).shouldNotContain(spell)

        driver.giveMana(player, Color.RED, 2)
        settle(driver)
        driver.submitYesNo(player, true)
        driver.submitTargetSelection(player, listOf(bears))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        shouldBePumped(driver, bears)
        driver.getGraveyard(player) shouldContain spell
    }
})
