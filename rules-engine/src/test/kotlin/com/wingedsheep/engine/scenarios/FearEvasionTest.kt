package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Tests for fear evasion ability.
 *
 * Fear rules (CR 702.36):
 * - A creature with fear can't be blocked except by artifact creatures and/or black creatures.
 */
class FearEvasionTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 10,
                "Forest" to 10,
                "Grizzly Bears" to 10,
                "Fear Creature" to 5,
                "Black Creature" to 5,
                "Artifact Creature" to 5
            ),
            skipMulligans = true
        )
        return driver
    }

    /**
     * Helper to advance to player 1's declare attackers step.
     * Handles cases where game state might start on a different player's turn.
     */
    fun GameTestDriver.advanceToPlayer1DeclareAttackers() {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        // If we reached declare attackers but it's not player 1's turn, advance to their turn
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }
    }

    test("non-black, non-artifact creature cannot block creature with fear") {
        val driver = createDriver()

        // Put a Fear Creature on player 1's battlefield (attacker)
        val fearCreature = driver.putCreatureOnBattlefield(driver.player1, "Fear Creature")
        driver.removeSummoningSickness(fearCreature)

        // Put a Grizzly Bears (green, non-artifact) on player 2's battlefield (potential blocker)
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Fear Creature as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(fearCreature), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Try to block with Grizzly Bears - should fail due to fear
        val result = driver.submitExpectFailure(
            com.wingedsheep.engine.core.DeclareBlockers(
                driver.player2,
                mapOf(blocker to listOf(fearCreature))
            )
        )

        result.isSuccess shouldBe false
        result.error shouldContainIgnoringCase "fear"
        result.error shouldContainIgnoringCase "cannot block"
    }

    test("black creature CAN block creature with fear") {
        val driver = createDriver()

        // Put a Fear Creature on player 1's battlefield (attacker)
        val fearCreature = driver.putCreatureOnBattlefield(driver.player1, "Fear Creature")
        driver.removeSummoningSickness(fearCreature)

        // Put a Black Creature on player 2's battlefield (potential blocker)
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Black Creature")
        driver.removeSummoningSickness(blocker)

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Fear Creature as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(fearCreature), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Block with Black Creature - should succeed since it's black
        val result = driver.declareBlockers(
            driver.player2,
            mapOf(blocker to listOf(fearCreature))
        )

        result.isSuccess shouldBe true
    }

    test("artifact creature CAN block creature with fear") {
        val driver = createDriver()

        // Put a Fear Creature on player 1's battlefield (attacker)
        val fearCreature = driver.putCreatureOnBattlefield(driver.player1, "Fear Creature")
        driver.removeSummoningSickness(fearCreature)

        // Put an Artifact Creature on player 2's battlefield (potential blocker)
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Artifact Creature")
        driver.removeSummoningSickness(blocker)

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Fear Creature as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(fearCreature), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Block with Artifact Creature - should succeed since it's an artifact creature
        val result = driver.declareBlockers(
            driver.player2,
            mapOf(blocker to listOf(fearCreature))
        )

        result.isSuccess shouldBe true
    }

    test("fear creature attacks unblocked when defender has only non-black non-artifact blockers") {
        val driver = createDriver()

        // Put a Fear Creature on player 1's battlefield (attacker)
        val fearCreature = driver.putCreatureOnBattlefield(driver.player1, "Fear Creature")
        driver.removeSummoningSickness(fearCreature)

        // Put a Grizzly Bears on player 2's battlefield (can't block fear)
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        // Record player 2's starting life
        val startingLife = driver.getLifeTotal(driver.player2)

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Fear Creature as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(fearCreature), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Declare no blockers (can't block anyway)
        val blockResult = driver.declareBlockers(driver.player2, emptyMap())
        blockResult.isSuccess shouldBe true

        // Both players pass through combat damage
        driver.bothPass() // End of declare blockers
        driver.bothPass() // Combat damage step

        // Player 2 should have taken 2 damage from the Fear Creature
        driver.getLifeTotal(driver.player2) shouldBe startingLife - 2
    }
})
