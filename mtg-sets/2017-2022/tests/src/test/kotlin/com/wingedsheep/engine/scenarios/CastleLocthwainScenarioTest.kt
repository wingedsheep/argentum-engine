package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.CastleLocthwain
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Castle Locthwain (ELD #241) — Land.
 *
 * "This land enters tapped unless you control a Swamp. {T}: Add {B}.
 *  {1}{B}{B}, {T}: Draw a card, then you lose life equal to the number of cards in your hand."
 */
class CastleLocthwainScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CastleLocthwain))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    test("draws a card, then loses life equal to the hand size including the drawn card") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val castle = driver.putLandOnBattlefield(me, "Castle Locthwain")
        val handBefore = driver.getHandSize(me)

        driver.giveMana(me, Color.BLACK, 3)
        val abilityId = driver.cardRegistry.getCard("Castle Locthwain")!!.script.activatedAbilities[1].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = castle,
                abilityId = abilityId,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.getHandSize(me) shouldBe handBefore + 1
        driver.getLifeTotal(me) shouldBe 20 - (handBefore + 1)
    }

    test("enters tapped without a Swamp, untapped with one") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val first = driver.putCardInHand(me, "Castle Locthwain")
        driver.playLand(me, first)
        driver.isTapped(driver.findPermanent(me, "Castle Locthwain")!!) shouldBe true

        val driver2 = createDriver()
        driver2.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me2 = driver2.activePlayer!!
        driver2.putLandOnBattlefield(me2, "Swamp")
        val second = driver2.putCardInHand(me2, "Castle Locthwain")
        driver2.playLand(me2, second)
        driver2.isTapped(driver2.findPermanent(me2, "Castle Locthwain")!!) shouldBe false
    }
})
