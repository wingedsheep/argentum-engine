package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Fiery Temper (Torment #97) — {1}{R}{R} Instant.
 *
 * "Fiery Temper deals 3 damage to any target."
 * "Madness {R}"
 *
 * Two ways it gets cast, and both are worth pinning: the printed {1}{R}{R}, and the madness line
 * (CR 702.35) where discarding it exiles the card and a single {R} buys the same 3 damage.
 * Tormenting Voice supplies the discard — its "as an additional cost, discard a card" is a
 * cost-payment discard, the route madness decks actually use.
 */
class FieryTemperScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    /** Let the stack drain until something needs an answer. */
    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
    }

    /** Discard [cardId] as Tormenting Voice's additional cost. */
    fun discardAsCost(driver: GameTestDriver, player: EntityId, cardId: EntityId) {
        val voice = driver.putCardInHand(player, "Tormenting Voice")
        driver.giveMana(player, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(cardId)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
    }

    test("cast for its printed cost, it deals 3 damage to any target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val temper = driver.putCardInHand(player, "Fiery Temper")
        driver.giveMana(player, Color.RED, 3)
        driver.castSpell(player, temper, targets = listOf(opponent)).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 17
        driver.getGraveyard(player) shouldContain temper
    }

    test("discarded, it is exiled and can be cast for its madness cost of {R}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val temper = driver.putCardInHand(player, "Fiery Temper")
        discardAsCost(driver, player, temper)

        // CR 702.35a — discarded into exile, never into the graveyard.
        driver.getExile(player) shouldContain temper
        driver.getGraveyard(player).shouldNotContain(temper)

        // A single red pays the madness cost, not the printed {1}{R}{R}.
        driver.giveMana(player, Color.RED, 1)
        settle(driver)
        driver.submitYesNo(player, true)
        driver.submitTargetSelection(player, listOf(opponent))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 17
        driver.getGraveyard(player) shouldContain temper
    }
})
