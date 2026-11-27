package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class HeadlessSkaabScenarioTest : FunSpec({

    fun newDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("casting it exiles a creature card from your graveyard, and it enters tapped") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val bears = driver.putCardInGraveyard(you, "Grizzly Bears")
        val skaab = driver.putCardInHand(you, "Headless Skaab")
        driver.giveMana(you, Color.BLUE, 3)

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = skaab,
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(bears)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        val permanent = driver.findPermanent(you, "Headless Skaab")
        permanent shouldNotBe null
        driver.isTapped(permanent!!) shouldBe true
        driver.getExile(you).contains(bears) shouldBe true
    }

    test("with no creature card in the graveyard it can't be cast") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        driver.putCardInGraveyard(you, "Lightning Bolt")
        val skaab = driver.putCardInHand(you, "Headless Skaab")
        driver.giveMana(you, Color.BLUE, 3)

        driver.castSpell(you, skaab).outcome shouldNotBe Outcome.Done
        driver.findPermanent(you, "Headless Skaab") shouldBe null
    }
})
