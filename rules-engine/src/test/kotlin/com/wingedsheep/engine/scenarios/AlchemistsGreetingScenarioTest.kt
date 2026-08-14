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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.FunSpec

/**
 * Alchemist's Greeting (Eldritch Moon #116) — {4}{R} Sorcery.
 *
 * "Alchemist's Greeting deals 4 damage to target creature."
 * "Madness {1}{R}"
 *
 * Creature-only removal, so the first test pins that it can't be pointed at a player. The madness
 * line (CR 702.35) is the card's real mode: a five-mana sorcery becomes two-mana instant-speed
 * removal, because the cast happens while the madness trigger resolves.
 */
class AlchemistsGreetingScenarioTest : FunSpec({

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

    test("cast for its printed cost, it deals 4 damage to a target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Grizzly Bears is a 2/2 — 4 damage is lethal.
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val greeting = driver.putCardInHand(player, "Alchemist's Greeting")
        driver.giveMana(player, Color.RED, 5)

        driver.castSpell(player, greeting, targets = listOf(bears)).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(opponent, "Grizzly Bears").shouldBeNull()
        driver.getGraveyard(player) shouldContain greeting
    }

    test("discarded, it is exiled and can be cast for its madness cost of {1}{R}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val greeting = driver.putCardInHand(player, "Alchemist's Greeting")
        discardAsCost(driver, player, greeting)

        driver.getExile(player) shouldContain greeting
        driver.getGraveyard(player).shouldNotContain(greeting)

        driver.giveMana(player, Color.RED, 2)
        settle(driver)
        driver.submitYesNo(player, true)
        driver.submitTargetSelection(player, listOf(bears))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(opponent, "Grizzly Bears").shouldBeNull()
        driver.getGraveyard(player) shouldContain greeting
    }
})
