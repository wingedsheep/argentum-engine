package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Self-Destruct (FIN #157) — {1}{R} Instant.
 *
 * "Target creature you control deals X damage to any other target and X damage to itself, where X
 *  is its power."
 *
 * The controlled creature is the damage source for both prongs and X is read off its power. Here a
 * 3/3 deals 3 to a 2/2 (killing it) and 3 to itself (killing itself).
 */
class SelfDestructScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("3/3 deals 3 to the opposing 2/2 and 3 to itself — both die") {
        val driver = newDriver()
        val yours = driver.putCreatureOnBattlefield(driver.player1, "Hill Giant") // 3/3
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears") // 2/2

        val boom = driver.putCardInHand(driver.player1, "Self-Destruct")
        driver.giveMana(driver.player1, Color.RED, 1)
        driver.giveColorlessMana(driver.player1, 1)
        driver.castSpell(driver.player1, boom, listOf(yours, theirs)).isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(driver.player2, "Grizzly Bears") shouldBe null
        driver.findPermanent(driver.player1, "Hill Giant") shouldBe null
    }
})
