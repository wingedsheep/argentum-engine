package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khans.cards.TormentingVoice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Tormenting Voice.
 *
 * Tormenting Voice: {1}{R}
 * Sorcery
 * As an additional cost to cast this spell, discard a card.
 * Draw two cards.
 */
class TormentingVoiceTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TormentingVoice))
        return driver
    }

    test("Tormenting Voice requires discarding a card and draws two cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Tormenting Voice and another card in hand
        val voice = driver.putCardInHand(activePlayer, "Tormenting Voice")
        val discard = driver.putCardInHand(activePlayer, "Mountain")
        driver.giveMana(activePlayer, Color.RED, 2)

        val handSizeBefore = driver.getHand(activePlayer).size

        // Cast Tormenting Voice, discarding a Mountain
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(discard)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        // The discarded card should be in graveyard
        driver.getGraveyardCardNames(activePlayer) shouldContain "Mountain"

        // Resolve the spell
        driver.bothPass()

        // Drew two cards, cast one, discarded one: net change = +2 - 1 - 1 = 0
        // But actually hand was: voice + discard + whatever was there before
        // After cast: voice leaves hand (to stack), discard goes to graveyard, then draws 2
        // So hand size should be handSizeBefore - 2 (voice + discard) + 2 (draw) = handSizeBefore
        driver.getHand(activePlayer).size shouldBe handSizeBefore
    }

    test("Tormenting Voice fails without discarding a card") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val voice = driver.putCardInHand(activePlayer, "Tormenting Voice")
        driver.giveMana(activePlayer, Color.RED, 2)

        // Try to cast without discarding
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = voice,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }

    test("Cannot discard the spell being cast as its own additional cost") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val voice = driver.putCardInHand(activePlayer, "Tormenting Voice")
        driver.giveMana(activePlayer, Color.RED, 2)

        // Try to discard the spell itself
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(voice)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }
})
