package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.TranscendentMessage
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Transcendent Message (MOM #83) — {X}{U}{U}{U}{U} Instant, convoke. "Draw X cards."
 *
 * The announced X is generic mana in the total cost, so creatures convoked for {1} pay it.
 */
class TranscendentMessageScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TranscendentMessage)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(card: EntityId, x: Int, convokers: List<EntityId>) = submit(
        CastSpell(
            playerId = player1,
            cardId = card,
            xValue = x,
            paymentStrategy = PaymentStrategy.AutoPay,
            alternativePayment = AlternativePaymentChoice(
                convokedCreatures = convokers.associateWith { ConvokePayment(color = null) }
            ),
        )
    )

    test("X=3 paid entirely by three convoked creatures; four Islands pay {U}{U}{U}{U}") {
        val driver = newDriver()
        repeat(4) { driver.putLandOnBattlefield(driver.player1, "Island") }
        val bears = (1..3).map { driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        val card = driver.putCardInHand(driver.player1, "Transcendent Message")
        val handBefore = driver.getHand(driver.player1).size

        driver.cast(card, x = 3, convokers = bears).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getHand(driver.player1).size shouldBe handBefore - 1 + 3
        bears.forEach { driver.isTapped(it) shouldBe true }
        driver.getUntappedLands(driver.player1).size shouldBe 0
    }

    test("X split between lands and convoked creatures") {
        val driver = newDriver()
        repeat(6) { driver.putLandOnBattlefield(driver.player1, "Island") }
        val bears = (1..2).map { driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        val card = driver.putCardInHand(driver.player1, "Transcendent Message")
        val handBefore = driver.getHand(driver.player1).size

        driver.cast(card, x = 4, convokers = bears).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getHand(driver.player1).size shouldBe handBefore - 1 + 4
        driver.getUntappedLands(driver.player1).size shouldBe 0
    }

    test("without convoke the full X is charged as mana") {
        val driver = newDriver()
        repeat(4) { driver.putLandOnBattlefield(driver.player1, "Island") }
        repeat(3) { driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        val card = driver.putCardInHand(driver.player1, "Transcendent Message")

        driver.cast(card, x = 3, convokers = emptyList()).outcome shouldNotBe Outcome.Done

        repeat(3) { driver.putLandOnBattlefield(driver.player1, "Island") }
        val handBefore = driver.getHand(driver.player1).size
        driver.cast(card, x = 3, convokers = emptyList()).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getHand(driver.player1).size shouldBe handBefore - 1 + 3
        driver.getUntappedLands(driver.player1).size shouldBe 0
    }
})
