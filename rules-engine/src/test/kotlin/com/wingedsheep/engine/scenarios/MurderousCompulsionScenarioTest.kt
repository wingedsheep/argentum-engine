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
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Murderous Compulsion (Shadows over Innistrad #126) — {1}{B} Sorcery.
 *
 * "Destroy target tapped creature."
 * "Madness {1}{B}"
 *
 * "Tapped" is part of the *target requirement*, so an untapped creature is never a legal target —
 * that's the first test. The madness line (CR 702.35) matters more here than on most madness
 * cards: the cast happens while the trigger resolves, so this sorcery can answer a creature that
 * tapped to attack on the opponent's turn.
 */
class MurderousCompulsionScenarioTest : FunSpec({

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

    test("it destroys a tapped creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.tapPermanent(bears)
        val compulsion = driver.putCardInHand(player, "Murderous Compulsion")
        driver.giveMana(player, Color.BLACK, 2)

        driver.castSpell(player, compulsion, targets = listOf(bears)).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(opponent, "Grizzly Bears").shouldBeNull()
    }

    test("an untapped creature is not a legal target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val compulsion = driver.putCardInHand(player, "Murderous Compulsion")
        driver.giveMana(player, Color.BLACK, 2)

        driver.castSpell(player, compulsion, targets = listOf(bears)).isSuccess shouldBe false
        driver.findPermanent(opponent, "Grizzly Bears").shouldNotBeNull()
    }

    test("discarded, it is exiled and can be cast for its madness cost of {1}{B}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.tapPermanent(bears)
        val compulsion = driver.putCardInHand(player, "Murderous Compulsion")
        discardAsCost(driver, player, compulsion)

        driver.getExile(player) shouldContain compulsion
        driver.getGraveyard(player).shouldNotContain(compulsion)

        driver.giveMana(player, Color.BLACK, 2)
        settle(driver)
        driver.submitYesNo(player, true)
        driver.submitTargetSelection(player, listOf(bears))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(opponent, "Grizzly Bears").shouldBeNull()
        driver.getGraveyard(player) shouldContain compulsion
    }
})
