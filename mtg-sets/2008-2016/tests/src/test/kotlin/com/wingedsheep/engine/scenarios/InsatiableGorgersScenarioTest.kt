package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.Outcome
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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Insatiable Gorgers (Eldritch Moon #134) — {2}{R}{R} Creature — Vampire Berserker 5/3.
 *
 * "This creature attacks each combat if able."
 * "Madness {3}{R}"
 *
 * Pins the forced attack, and that discarding it exiles it and lets it be cast for {3}{R}.
 */
class InsatiableGorgersScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
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

    test("it must attack each combat if able") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gorgers = driver.putCreatureOnBattlefield(you, "Insatiable Gorgers")
        driver.removeSummoningSickness(gorgers)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val noAttack = driver.submit(DeclareAttackers(playerId = you, attackers = emptyMap()))
        noAttack.outcome shouldNotBe Outcome.Done
        noAttack.error shouldContain "must attack"

        driver.submit(DeclareAttackers(playerId = you, attackers = mapOf(gorgers to opponent)))
            .outcome shouldBe Outcome.Done
    }

    test("discarded, it is exiled and can be cast for its madness cost of {3}{R}") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gorgers = driver.putCardInHand(player, "Insatiable Gorgers")
        discardAsCost(driver, player, gorgers)

        driver.getExile(player) shouldContain gorgers
        driver.getGraveyard(player).shouldNotContain(gorgers)

        driver.giveMana(player, Color.RED, 4)
        settle(driver)
        driver.submitYesNo(player, true)
        settle(driver)

        driver.findPermanent(player, "Insatiable Gorgers").shouldNotBeNull()
    }
})
