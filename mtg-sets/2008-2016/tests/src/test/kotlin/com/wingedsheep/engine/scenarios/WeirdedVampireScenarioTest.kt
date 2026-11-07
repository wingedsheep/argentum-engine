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
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Weirded Vampire (EMN #113) — {3}{B} 3/3 Vampire Horror with madness {2}{B}.
 *
 * Discarded, it goes to exile and can be cast for {2}{B}; declining puts it into the graveyard.
 * Uses [GameTestDriver] because the madness discard-as-cost path is driven there (see
 * BloodmadVampireScenarioTest).
 */
class WeirdedVampireScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

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

    test("discarded, it is exiled and can be cast for its madness cost of {2}{B}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vampire = driver.putCardInHand(player, "Weirded Vampire")
        discardAsCost(driver, player, vampire)

        driver.getExile(player) shouldContain vampire
        driver.getGraveyard(player).shouldNotContain(vampire)

        driver.giveMana(player, Color.BLACK, 3)
        settle(driver)
        driver.submitYesNo(player, true)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(player, "Weirded Vampire").shouldNotBeNull()
        driver.getExile(player).shouldNotContain(vampire)
    }

    test("declining the madness cast puts it into the graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vampire = driver.putCardInHand(player, "Weirded Vampire")
        discardAsCost(driver, player, vampire)

        driver.giveMana(player, Color.BLACK, 3)
        settle(driver)
        driver.submitYesNo(player, false)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getGraveyard(player) shouldContain vampire
        driver.getExile(player).shouldNotContain(vampire)
    }
})
