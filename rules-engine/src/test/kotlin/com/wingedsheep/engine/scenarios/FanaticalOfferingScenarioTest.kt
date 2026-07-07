package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.FanaticalOffering
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fanatical Offering (LCI #105) — {1}{B} Instant
 *
 * "As an additional cost to cast this spell, sacrifice an artifact or creature.
 *  Draw two cards and create a Map token."
 *
 * Tests:
 *  1. Sacrificing a creature as the additional cost draws two cards and creates a Map token.
 *  2. Sacrificing an artifact as the additional cost draws two cards and creates a Map token.
 */
class FanaticalOfferingScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(PredefinedTokens.allTokens)
        driver.registerCard(FanaticalOffering)
        // 20 Swamps to pay {1}{B} and supply the library for drawing
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("sacrificing a creature draws two cards and creates a Map token") {
        val driver = newDriver()
        val me = driver.activePlayer!!

        // Put a creature on the battlefield to sacrifice
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        // Record hand size before casting (spell will leave hand, then 2 drawn)
        val spell = driver.putCardInHand(me, "Fanatical Offering")
        val handBefore = driver.getHandSize(me)

        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.BLACK, 1)

        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // The sacrificed creature is gone from the battlefield
        driver.findPermanent(me, "Grizzly Bears") shouldBe null

        // Drew two cards: hand went from handBefore (includes Fanatical Offering) to
        // handBefore - 1 (spell cast) + 2 (draw) = handBefore + 1
        driver.getHandSize(me) shouldBe handBefore + 1

        // A Map token was created on the caster's battlefield
        driver.findPermanent(me, "Map") shouldNotBe null
    }

    test("sacrificing an artifact draws two cards and creates a Map token") {
        val driver = newDriver()
        val me = driver.activePlayer!!

        // Put an artifact creature on the battlefield to sacrifice (proves the artifact branch)
        val artifact = driver.putCreatureOnBattlefield(me, "Artifact Creature")

        val spell = driver.putCardInHand(me, "Fanatical Offering")
        val handBefore = driver.getHandSize(me)

        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.BLACK, 1)

        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(artifact)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // The sacrificed artifact creature is gone from the battlefield
        driver.findPermanent(me, "Artifact Creature") shouldBe null

        // Drew two cards
        driver.getHandSize(me) shouldBe handBefore + 1

        // A Map token was created
        driver.findPermanent(me, "Map") shouldNotBe null
    }
})
