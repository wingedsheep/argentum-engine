package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Tests for unblockable creatures.
 *
 * Cards like Phantom Warrior have the ability:
 * "Phantom Warrior can't be blocked."
 *
 * This means no creature can legally declare blockers against it.
 */
class UnblockableTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 10,
                "Island" to 10,
                "Grizzly Bears" to 5,
                "Phantom Warrior" to 5,
                "Wind Drake" to 5
            ),
            skipMulligans = true
        )
        return driver
    }

    /**
     * Helper to advance to player 1's declare attackers step.
     */
    fun GameTestDriver.advanceToPlayer1DeclareAttackers() {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }
    }

    test("Phantom Warrior cannot be blocked by any creature") {
        val driver = createDriver()

        // Put Phantom Warrior on player 1's battlefield (attacker)
        val phantomWarrior = driver.putCreatureOnBattlefield(driver.player1, "Phantom Warrior")
        driver.removeSummoningSickness(phantomWarrior)

        // Put Grizzly Bears on player 2's battlefield (potential blocker)
        val bears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Phantom Warrior as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(phantomWarrior), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Try to block with Grizzly Bears - should fail because Phantom Warrior can't be blocked
        val result = driver.submitExpectFailure(
            com.wingedsheep.engine.core.DeclareBlockers(
                driver.player2,
                mapOf(bears to listOf(phantomWarrior))
            )
        )

        result.isSuccess shouldBe false
        result.error shouldContainIgnoringCase "can't be blocked"
    }

    test("Phantom Warrior cannot be blocked by flying creature") {
        val driver = createDriver()

        // Put Phantom Warrior on player 1's battlefield (attacker)
        val phantomWarrior = driver.putCreatureOnBattlefield(driver.player1, "Phantom Warrior")
        driver.removeSummoningSickness(phantomWarrior)

        // Put Wind Drake (flying) on player 2's battlefield (potential blocker)
        val windDrake = driver.putCreatureOnBattlefield(driver.player2, "Wind Drake")
        driver.removeSummoningSickness(windDrake)

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Phantom Warrior as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(phantomWarrior), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Try to block with Wind Drake - should fail because Phantom Warrior can't be blocked
        val result = driver.submitExpectFailure(
            com.wingedsheep.engine.core.DeclareBlockers(
                driver.player2,
                mapOf(windDrake to listOf(phantomWarrior))
            )
        )

        result.isSuccess shouldBe false
        result.error shouldContainIgnoringCase "can't be blocked"
    }

    test("normal creature can still be blocked") {
        val driver = createDriver()

        // Put Grizzly Bears on player 1's battlefield (attacker without unblockable)
        val attackingBears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(attackingBears)

        // Put another Grizzly Bears on player 2's battlefield (blocker)
        val blockingBears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(blockingBears)

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Grizzly Bears as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(attackingBears), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Block with Grizzly Bears - should succeed (normal creature can be blocked)
        val result = driver.declareBlockers(
            driver.player2,
            mapOf(blockingBears to listOf(attackingBears))
        )

        result.isSuccess shouldBe true
    }
})
