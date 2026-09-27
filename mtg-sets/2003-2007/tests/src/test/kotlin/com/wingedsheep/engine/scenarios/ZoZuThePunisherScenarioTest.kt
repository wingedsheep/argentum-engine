package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.ZoZuThePunisher
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Zo-Zu the Punisher — {1}{R}{R} Legendary Creature — Goblin Warrior 2/2.
 *
 * "Whenever a land enters, Zo-Zu deals 2 damage to that land's controller."
 */
class ZoZuThePunisherScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ZoZuThePunisher)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 30) { bothPass(); guard++ }
    }

    test("a land entering under the opponent's control deals 2 damage to the opponent") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.player2

        driver.putPermanentOnBattlefield(me, "Zo-Zu the Punisher")
        val myLife = driver.getLifeTotal(me)
        val oppLife = driver.getLifeTotal(opp)

        // Advance to the opponent's precombat main and have them play a land.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe opp
        val land = driver.putCardInHand(opp, "Mountain")
        driver.playLand(opp, land).error shouldBe null
        driver.resolveStack()

        driver.getLifeTotal(opp) shouldBe oppLife - 2
        driver.getLifeTotal(me) shouldBe myLife
    }

    test("symmetric: a land entering under Zo-Zu's controller damages that controller") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.player2

        driver.putPermanentOnBattlefield(me, "Zo-Zu the Punisher")
        val myLife = driver.getLifeTotal(me)
        val oppLife = driver.getLifeTotal(opp)

        val land = driver.putCardInHand(me, "Mountain")
        driver.playLand(me, land).error shouldBe null
        driver.resolveStack()

        driver.getLifeTotal(me) shouldBe myLife - 2
        driver.getLifeTotal(opp) shouldBe oppLife
    }

    test("a nonland permanent entering does not trigger") {
        val driver = createDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Zo-Zu the Punisher")
        val myLife = driver.getLifeTotal(me)

        val courser = driver.putCardInHand(me, "Centaur Courser")
        driver.giveMana(me, Color.GREEN, 3)
        driver.castSpell(me, courser).error shouldBe null
        driver.resolveStack()

        driver.findPermanent(me, "Centaur Courser") shouldNotBe null

        driver.getLifeTotal(me) shouldBe myLife
    }
})
