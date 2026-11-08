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
 * Distemper of the Blood (Eldritch Moon #126) — {1}{R} Sorcery.
 *
 * "Target creature gets +2/+2 and gains trample until end of turn."
 * "Madness {R}"
 *
 * The first test pins the pump-and-trample; the second the madness line (CR 702.35): discarded, the
 * sorcery is exiled and cast for {R} while the madness trigger resolves.
 */
class DistemperOfTheBloodScenarioTest : FunSpec({

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
        driver.giveMana(driver.activePlayer!!, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(cardId)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
    }

    test("cast for {1}{R}, target creature gets +2/+2 and gains trample until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
        val distemper = driver.putCardInHand(player, "Distemper of the Blood")
        driver.giveMana(player, Color.RED, 2)

        driver.castSpell(player, distemper, targets = listOf(bears)).outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.projectedState.getPower(bears) shouldBe 4
        driver.state.projectedState.getToughness(bears) shouldBe 4
        driver.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
        driver.getGraveyard(player) shouldContain distemper
    }

    test("discarded, it is exiled and can be cast for its madness cost of {R}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val distemper = driver.putCardInHand(player, "Distemper of the Blood")
        discardAsCost(driver, player, distemper)

        driver.getExile(player) shouldContain distemper
        driver.getGraveyard(player).shouldNotContain(distemper)

        driver.giveMana(player, Color.RED, 1)
        settle(driver)
        driver.submitYesNo(player, true)
        driver.submitTargetSelection(player, listOf(bears))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.projectedState.getPower(bears) shouldBe 4
        driver.state.projectedState.getToughness(bears) shouldBe 4
        driver.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
        driver.getGraveyard(player) shouldContain distemper
    }
})
