package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.CompleteTheCircuit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Complete the Circuit: the next instant or sorcery cast this turn is copied twice (three
 * resolutions total), and the trigger is one-shot.
 */
class CompleteTheCircuitScenarioTest : FunSpec({

    val cantrip = CardDefinition.sorcery(
        name = "Test Cantrip Sorcery",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Draw a card.",
        script = CardScript.spell(effect = DrawCardsEffect(1))
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CompleteTheCircuit, cantrip))
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("next spell is copied twice, and only the next one") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val circuit = driver.putCardInHand(p1, "Complete the Circuit")
        driver.giveMana(p1, Color.BLUE, 6)
        driver.submitSuccess(CastSpell(playerId = p1, cardId = circuit, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass()

        val first = driver.putCardInHand(p1, "Test Cantrip Sorcery")
        val second = driver.putCardInHand(p1, "Test Cantrip Sorcery")
        val before = driver.getHandSize(p1)
        driver.giveMana(p1, Color.RED, 2)
        driver.submitSuccess(CastSpell(playerId = p1, cardId = first, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass() // delayed trigger: two copies
        driver.bothPass()
        driver.bothPass()
        driver.bothPass()
        // one card left hand (cast), three draws
        driver.getHandSize(p1) shouldBe before - 1 + 3

        val mid = driver.getHandSize(p1)
        driver.submitSuccess(CastSpell(playerId = p1, cardId = second, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass()
        driver.getHandSize(p1) shouldBe mid - 1 + 1
    }
})
