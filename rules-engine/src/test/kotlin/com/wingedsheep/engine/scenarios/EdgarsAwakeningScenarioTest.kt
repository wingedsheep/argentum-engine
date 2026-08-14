package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Edgar's Awakening (Innistrad: Crimson Vow #110) — {3}{B}{B} Sorcery.
 *
 * "Return target creature card from your graveyard to the battlefield.
 *  When you discard this card, you may pay {B}. When you do, return target creature card from
 *  your graveyard to your hand."
 *
 * The discard half is the only user of the "when you discard this card" self-bound trigger, so
 * this file also covers that trigger shape: that it fires from the discarded card (already in the
 * graveyard by then) rather than needing a permanent to watch for it, and that declining the
 * optional {B} leaves the graveyard untouched.
 */
class EdgarsAwakeningScenarioTest : FunSpec({

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

    test("cast normally it reanimates a creature card from your graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCardInGraveyard(player, "Grizzly Bears")
        val awakening = driver.putCardInHand(player, "Edgar's Awakening")
        driver.giveMana(player, Color.BLACK, 5)
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = awakening,
                targets = listOf(ChosenTarget.Card(bears, player, Zone.GRAVEYARD)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(player, "Grizzly Bears").shouldNotBeNull()
        driver.getGraveyard(player).shouldNotContain(bears)
    }

    test("discarding it offers {B} to return a creature card from your graveyard to your hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCardInGraveyard(player, "Grizzly Bears")
        val awakening = driver.putCardInHand(player, "Edgar's Awakening")
        discardAsCost(driver, player, awakening)

        driver.getGraveyard(player) shouldContain awakening

        // After the discard, so Tormenting Voice's own {1}{R} can't eat the black.
        driver.giveMana(player, Color.BLACK, 1)
        settle(driver)
        driver.submitYesNo(player, true)
        // The reflexive trigger picks its target only after the {B} is paid.
        driver.submitTargetSelection(player, listOf(bears))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getHand(player) shouldContain bears
        driver.getGraveyard(player).shouldNotContain(bears)
    }

    test("declining the {B} leaves the graveyard alone") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCardInGraveyard(player, "Grizzly Bears")
        val awakening = driver.putCardInHand(player, "Edgar's Awakening")
        discardAsCost(driver, player, awakening)

        driver.giveMana(player, Color.BLACK, 1)
        settle(driver)
        driver.submitYesNo(player, false)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getGraveyard(player) shouldContain bears
        driver.getHand(player).shouldNotContain(bears)
    }
})
