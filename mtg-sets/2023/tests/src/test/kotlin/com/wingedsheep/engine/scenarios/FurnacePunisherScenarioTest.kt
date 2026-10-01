package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.FurnacePunisher
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Furnace Punisher (ONE) — "At the beginning of each player's upkeep, this creature deals 2 damage
 * to that player unless they control two or more basic lands."
 *
 * Pins that the basic-land count is read off the player whose upkeep it is, not the controller.
 */
class FurnacePunisherScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FurnacePunisher))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
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

    test("opponent with one basic land takes 2 on their upkeep, even though the controller has two") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(controller, "Furnace Punisher")
        driver.putPermanentOnBattlefield(controller, "Mountain")
        driver.putPermanentOnBattlefield(controller, "Mountain")
        driver.putPermanentOnBattlefield(opponent, "Mountain")

        advanceToUpkeepOf(driver, opponent)
        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18
        driver.getLifeTotal(controller) shouldBe 20
    }

    test("opponent with two basic lands takes no damage") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(controller, "Furnace Punisher")
        driver.putPermanentOnBattlefield(opponent, "Mountain")
        driver.putPermanentOnBattlefield(opponent, "Mountain")

        advanceToUpkeepOf(driver, opponent)
        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("controller with fewer than two basic lands takes 2 on their own upkeep") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(controller, "Furnace Punisher")
        driver.putPermanentOnBattlefield(opponent, "Mountain")
        driver.putPermanentOnBattlefield(opponent, "Mountain")

        advanceToUpkeepOf(driver, opponent)
        driver.bothPass()
        driver.getLifeTotal(opponent) shouldBe 20

        advanceToUpkeepOf(driver, controller)
        driver.stackSize shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(controller) shouldBe 18
    }
})
