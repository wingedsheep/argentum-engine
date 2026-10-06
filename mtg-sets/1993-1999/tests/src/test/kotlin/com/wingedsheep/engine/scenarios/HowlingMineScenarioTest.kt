package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.HowlingMine
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Howling Mine (LEA) — {2} Artifact.
 *
 * "At the beginning of each player's draw step, if this artifact is untapped, that player draws an
 * additional card."
 *
 * Pins the each-player scope (the opponent draws too, not just the controller) and the intervening
 * "if": a tapped Mine doesn't trigger, and tapping it in response stops the draw on resolution.
 */
class HowlingMineScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HowlingMine))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        return driver
    }

    // Advance to the next draw step belonging to [player]; the turn-based draw has already happened.
    fun advanceToDrawOf(driver: GameTestDriver, player: EntityId) {
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        if (driver.activePlayer != player) {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        }
        driver.currentStep shouldBe Step.DRAW
        driver.activePlayer shouldBe player
    }

    test("untapped: each player draws an additional card on their own draw step") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(controller, "Howling Mine")

        advanceToDrawOf(driver, opponent)
        driver.stackSize shouldBe 1
        val oppHand = driver.getHandSize(opponent)
        driver.bothPass()
        driver.getHandSize(opponent) shouldBe oppHand + 1

        advanceToDrawOf(driver, controller)
        driver.stackSize shouldBe 1
        val myHand = driver.getHandSize(controller)
        driver.bothPass()
        driver.getHandSize(controller) shouldBe myHand + 1
    }

    test("tapped at the start of the draw step: it doesn't trigger") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val mine = driver.putPermanentOnBattlefield(controller, "Howling Mine")
        driver.tapPermanent(mine)

        // The controller's untap step doesn't come before the opponent's draw step.
        advanceToDrawOf(driver, opponent)
        driver.isTapped(mine) shouldBe true
        driver.stackSize shouldBe 0
    }

    test("tapped in response: the intervening if is rechecked and no card is drawn") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val mine = driver.putPermanentOnBattlefield(controller, "Howling Mine")

        advanceToDrawOf(driver, opponent)
        driver.stackSize shouldBe 1
        driver.tapPermanent(mine)
        val oppHand = driver.getHandSize(opponent)
        driver.bothPass()
        driver.stackSize shouldBe 0
        driver.getHandSize(opponent) shouldBe oppHand
    }
})
