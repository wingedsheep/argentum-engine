package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.legions.cards.LavabornMuse
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Lavaborn Muse:
 * {3}{R} Creature — Spirit 3/3
 * At the beginning of each opponent's upkeep, if that player has two or fewer
 * cards in hand, Lavaborn Muse deals 3 damage to that player.
 */
class LavabornMuseTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LavabornMuse))
        return driver
    }

    /**
     * From PRECOMBAT_MAIN on player 1's turn, advance to the next UPKEEP step,
     * which will be the opponent's (player 2) upkeep.
     */
    fun advanceToOpponentUpkeep(driver: GameTestDriver, opponent: EntityId) {
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe opponent
    }

    /**
     * From PRECOMBAT_MAIN on player 1's turn, advance to the controller's next upkeep
     * by going through the opponent's full turn.
     */
    fun advanceToControllerUpkeep(driver: GameTestDriver, controller: EntityId) {
        // Go through the opponent's turn to reach controller's upkeep
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        // Now at opponent's upkeep — continue to the next upkeep (controller's)
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe controller
    }

    /**
     * Reduce a player's hand to exactly [count] cards by moving excess cards to their library.
     */
    fun reduceHandTo(driver: GameTestDriver, playerId: EntityId, count: Int) {
        val hand = driver.getHand(playerId)
        if (hand.size <= count) return
        val keep = hand.take(count)
        val move = hand.drop(count)
        val handKey = ZoneKey(playerId, Zone.HAND)
        val libraryKey = ZoneKey(playerId, Zone.LIBRARY)
        val newZones = driver.state.zones.toMutableMap()
        newZones[handKey] = keep
        newZones[libraryKey] = move + (newZones[libraryKey] ?: emptyList())
        driver.replaceState(driver.state.copy(zones = newZones))
    }

    test("deals 3 damage when opponent has 2 cards in hand at upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(controller, "Lavaborn Muse")
        reduceHandTo(driver, opponent, 2)

        val lifeBefore = driver.getLifeTotal(opponent)

        advanceToOpponentUpkeep(driver, opponent)

        // Trigger should be on the stack
        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe lifeBefore - 3
    }

    test("deals 3 damage when opponent has 0 cards in hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(controller, "Lavaborn Muse")
        reduceHandTo(driver, opponent, 0)

        val lifeBefore = driver.getLifeTotal(opponent)

        advanceToOpponentUpkeep(driver, opponent)
        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe lifeBefore - 3
    }

    test("does not trigger when opponent has 3 or more cards in hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(controller, "Lavaborn Muse")
        reduceHandTo(driver, opponent, 3)

        val lifeBefore = driver.getLifeTotal(opponent)

        advanceToOpponentUpkeep(driver, opponent)

        // Trigger should NOT fire
        driver.stackSize shouldBe 0
        driver.getLifeTotal(opponent) shouldBe lifeBefore
    }

    test("does not trigger during controller's own upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(controller, "Lavaborn Muse")
        reduceHandTo(driver, controller, 0)

        val lifeBefore = driver.getLifeTotal(controller)

        // Advance to controller's own upkeep (next turn)
        advanceToControllerUpkeep(driver, controller)

        // Trigger should NOT fire on controller's upkeep
        driver.stackSize shouldBe 0
        driver.getLifeTotal(controller) shouldBe lifeBefore
    }
})
