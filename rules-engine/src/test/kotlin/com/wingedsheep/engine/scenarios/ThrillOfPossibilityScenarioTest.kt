package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.ThrillOfPossibility
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Thrill of Possibility {1}{R} Instant (ELD canonical; reprinted in FDN).
 *
 * As an additional cost to cast this spell, discard a card.
 * Draw two cards.
 */
class ThrillOfPossibilityScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThrillOfPossibility))
        return driver
    }

    test("discards a card as an additional cost, then draws two") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val thrill = driver.putCardInHand(me, "Thrill of Possibility")
        val fodder = driver.putCardInHand(me, "Mountain")
        driver.giveMana(me, Color.RED, 2)

        val handBefore = driver.getHand(me).size

        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = thrill,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(fodder)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        // The discarded card is in the graveyard immediately (paid as a cost).
        driver.getGraveyardCardNames(me) shouldContain "Mountain"

        driver.bothPass()

        // Net hand: spell to stack (-1), fodder discarded (-1), then draw two (+2) => unchanged.
        driver.getHand(me).size shouldBe handBefore
    }

    test("cannot be cast without paying the discard cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val thrill = driver.putCardInHand(me, "Thrill of Possibility")
        driver.giveMana(me, Color.RED, 2)

        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = thrill,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }
})
