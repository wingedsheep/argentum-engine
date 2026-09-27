package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.SeizanPerverterOfTruth
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Seizan, Perverter of Truth (CHK) — {3}{B}{B} Legendary Creature — Demon Spirit 6/5.
 *
 * "At the beginning of each player's upkeep, that player loses 2 life and draws two cards."
 *
 * Pins that "that player" is the upkeep player on both sides of the table: the opponent pays and
 * draws on their upkeep, the controller on theirs, and the non-active player is untouched.
 */
class SeizanPerverterOfTruthScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SeizanPerverterOfTruth))
        return driver
    }

    fun advanceToUpkeepOf(driver: GameTestDriver, player: EntityId) {
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        if (driver.activePlayer != player) {
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
            driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        }
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe player
    }

    fun assertUpkeepPlayerPays(driver: GameTestDriver, upkeepPlayer: EntityId) {
        val other = driver.getOpponent(upkeepPlayer)
        advanceToUpkeepOf(driver, upkeepPlayer)
        driver.stackSize shouldBe 1

        val lifeBefore = driver.getLifeTotal(upkeepPlayer)
        val handBefore = driver.getHandSize(upkeepPlayer)
        val otherLifeBefore = driver.getLifeTotal(other)
        val otherHandBefore = driver.getHandSize(other)

        driver.bothPass() // resolve Seizan's trigger

        driver.stackSize shouldBe 0
        driver.getLifeTotal(upkeepPlayer) shouldBe lifeBefore - 2
        driver.getHandSize(upkeepPlayer) shouldBe handBefore + 2
        driver.getLifeTotal(other) shouldBe otherLifeBefore
        driver.getHandSize(other) shouldBe otherHandBefore
    }

    test("the opponent loses 2 life and draws two cards on their upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))

        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(controller, "Seizan, Perverter of Truth")

        assertUpkeepPlayerPays(driver, opponent)
    }

    test("its controller loses 2 life and draws two cards on their own upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))

        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(controller, "Seizan, Perverter of Truth")

        assertUpkeepPlayerPays(driver, controller)
    }
})
