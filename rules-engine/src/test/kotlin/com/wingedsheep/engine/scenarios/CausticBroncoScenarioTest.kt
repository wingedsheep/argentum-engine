package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Caustic Bronco (OTJ).
 *
 * Oracle: "Whenever this creature attacks, reveal the top card of your library and put it into
 * your hand. You lose life equal to that card's mana value if this creature isn't saddled.
 * Otherwise, each opponent loses that much life. Saddle 3"
 *
 * The reveal-to-hand happens unconditionally; the saddled state at resolution decides who loses
 * the life. Grizzly Bears (MV 2) is stacked on top of the library so the mana value is known.
 */
class CausticBroncoScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("attacking while not saddled: controller loses life equal to revealed card's mana value") {
        val driver = newDriver()
        val bronco = driver.putCreatureOnBattlefield(driver.player1, "Caustic Bronco")
        driver.removeSummoningSickness(bronco)
        driver.putCardOnTopOfLibrary(driver.player1, "Grizzly Bears") // MV 2

        val myLifeBefore = driver.getLifeTotal(driver.player1)
        val oppLifeBefore = driver.getLifeTotal(driver.player2)
        val handBefore = driver.getHandSize(driver.player1)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(bronco), driver.player2)
        driver.bothPass()

        // Revealed card went to hand.
        driver.getHandSize(driver.player1) shouldBe handBefore + 1
        // Not saddled: controller loses 2, opponent untouched.
        driver.getLifeTotal(driver.player1) shouldBe myLifeBefore - 2
        driver.getLifeTotal(driver.player2) shouldBe oppLifeBefore
    }

    test("attacking while saddled: each opponent loses life equal to the revealed card's mana value") {
        val driver = newDriver()
        val bronco = driver.putCreatureOnBattlefield(driver.player1, "Caustic Bronco")
        // Saddle 3 needs total power >= 3; two Grizzly Bears (power 2 each) suffice.
        val saddlerA = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val saddlerB = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(bronco)
        driver.putCardOnTopOfLibrary(driver.player1, "Grizzly Bears") // MV 2

        val myLifeBefore = driver.getLifeTotal(driver.player1)
        val oppLifeBefore = driver.getLifeTotal(driver.player2)

        driver.submitSuccess(SaddleMount(driver.player1, bronco, listOf(saddlerA, saddlerB)))
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(bronco), driver.player2)
        driver.bothPass()

        // Saddled: opponent loses 2, controller untouched.
        driver.getLifeTotal(driver.player2) shouldBe oppLifeBefore - 2
        driver.getLifeTotal(driver.player1) shouldBe myLifeBefore
    }
})
