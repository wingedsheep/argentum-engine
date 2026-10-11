package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blc.cards.MrFoxglove
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mr. Foxglove (BLC).
 *
 * "Whenever Mr. Foxglove attacks, draw cards equal to the number of cards in defending player's
 * hand minus the number of cards in your hand. If you didn't draw cards this way, you may put a
 * creature card from your hand onto the battlefield."
 *
 * The "didn't draw" gate is read from the cards-drawn tally across the draw, so the test pins
 * both branches: a positive difference draws and offers no put; a non-positive one draws nothing
 * and offers the put.
 */
class MrFoxgloveScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MrFoxglove)
        driver.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("defending player has more cards: draws the difference and offers no put") {
        val driver = newDriver()
        val fox = driver.putCreatureOnBattlefield(driver.player1, "Mr. Foxglove")
        driver.removeSummoningSickness(fox)
        driver.putCardInHand(driver.player1, "Grizzly Bears")
        repeat(4) { driver.putCardInHand(driver.player2, "Mountain") }

        val myHand = driver.getHandSize(driver.player1)
        val theirHand = driver.getHandSize(driver.player2)
        (theirHand - myHand) shouldBe 3

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(fox), driver.player2)
        driver.bothPass()

        driver.pendingDecision shouldBe null
        driver.getHandSize(driver.player1) shouldBe myHand + 3
        driver.findPermanent(driver.player1, "Grizzly Bears") shouldBe null
    }

    test("no positive difference: draws nothing and may put a creature card onto the battlefield") {
        val driver = newDriver()
        val fox = driver.putCreatureOnBattlefield(driver.player1, "Mr. Foxglove")
        driver.removeSummoningSickness(fox)
        val bears = driver.putCardInHand(driver.player1, "Grizzly Bears")

        val myHand = driver.getHandSize(driver.player1)
        (driver.getHandSize(driver.player2) - myHand < 0) shouldBe true

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(fox), driver.player2)
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options.contains(bears) shouldBe true
        driver.submitCardSelection(driver.player1, listOf(bears))

        driver.findPermanent(driver.player1, "Grizzly Bears") shouldNotBe null
        driver.getHandSize(driver.player1) shouldBe myHand - 1
    }

    test("no positive difference: declining the put leaves the hand alone") {
        val driver = newDriver()
        val fox = driver.putCreatureOnBattlefield(driver.player1, "Mr. Foxglove")
        driver.removeSummoningSickness(fox)
        driver.putCardInHand(driver.player1, "Grizzly Bears")
        val myHand = driver.getHandSize(driver.player1)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(fox), driver.player2)
        driver.bothPass()

        driver.submitCardSelection(driver.player1, emptyList())
        driver.findPermanent(driver.player1, "Grizzly Bears") shouldBe null
        driver.getHandSize(driver.player1) shouldBe myHand
    }
})
