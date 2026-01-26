package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Tests for power-based blocking restrictions.
 *
 * Cards like Fleet-Footed Monk have the ability:
 * "This creature can't be blocked by creatures with power 2 or greater."
 *
 * The blocking restriction uses PROJECTED power, which means:
 * - A 1/1 creature CAN block Fleet-Footed Monk
 * - A 2/2 creature CANNOT block Fleet-Footed Monk
 * - A 1/1 creature that has been given +2/+2 by Giant Growth CANNOT block Fleet-Footed Monk
 */
class PowerBlockingRestrictionTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 10,
                "Plains" to 10,
                "Grizzly Bears" to 5,
                "Savannah Lions" to 5,
                "Fleet-Footed Monk" to 5,
                "Giant Growth" to 5
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

    test("Fleet-Footed Monk cannot be blocked by creature with power 2") {
        val driver = createDriver()

        // Put Fleet-Footed Monk on player 1's battlefield (attacker)
        val monk = driver.putCreatureOnBattlefield(driver.player1, "Fleet-Footed Monk")
        driver.removeSummoningSickness(monk)

        // Put Grizzly Bears (2/2) on player 2's battlefield (potential blocker)
        val bears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Fleet-Footed Monk as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(monk), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Try to block with Grizzly Bears - should fail due to power restriction
        val result = driver.submitExpectFailure(
            com.wingedsheep.engine.core.DeclareBlockers(
                driver.player2,
                mapOf(bears to listOf(monk))
            )
        )

        result.isSuccess shouldBe false
        result.error shouldContainIgnoringCase "cannot block"
        result.error shouldContainIgnoringCase "power"
    }

    test("Fleet-Footed Monk CAN be blocked by creature with power 1") {
        val driver = createDriver()

        // Put Fleet-Footed Monk on player 1's battlefield (attacker)
        val monk = driver.putCreatureOnBattlefield(driver.player1, "Fleet-Footed Monk")
        driver.removeSummoningSickness(monk)

        // Put Savannah Lions (1/1) on player 2's battlefield (potential blocker)
        val lions = driver.putCreatureOnBattlefield(driver.player2, "Savannah Lions")
        driver.removeSummoningSickness(lions)

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Fleet-Footed Monk as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(monk), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Block with Savannah Lions - should succeed (power 1 < 2)
        val result = driver.declareBlockers(
            driver.player2,
            mapOf(lions to listOf(monk))
        )

        result.isSuccess shouldBe true
    }

    test("Fleet-Footed Monk cannot be blocked by creature whose power was increased by a spell") {
        val driver = createDriver()

        // Put Fleet-Footed Monk on player 1's battlefield (attacker)
        val monk = driver.putCreatureOnBattlefield(driver.player1, "Fleet-Footed Monk")
        driver.removeSummoningSickness(monk)

        // Put Savannah Lions (1/1) on player 2's battlefield (potential blocker)
        val lions = driver.putCreatureOnBattlefield(driver.player2, "Savannah Lions")
        driver.removeSummoningSickness(lions)

        // Give player 2 a Giant Growth in hand and mana to cast it
        val giantGrowth = driver.putCardInHand(driver.player2, "Giant Growth")
        val forest = driver.putLandOnBattlefield(driver.player2, "Forest")

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Fleet-Footed Monk as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(monk), driver.player2)
        attackResult.isSuccess shouldBe true

        // Player 1 passes priority, player 2 gets priority
        driver.passPriority(driver.player1)

        // Player 2 casts Giant Growth on Savannah Lions (+3/+3 makes it 4/4)
        val castResult = driver.castSpell(driver.player2, giantGrowth, listOf(lions))
        castResult.isSuccess shouldBe true

        // Both pass to resolve Giant Growth
        driver.bothPass()

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Try to block with Savannah Lions (now 4/4 due to Giant Growth) - should FAIL
        // The projected power is 4, which is >= 2
        val result = driver.submitExpectFailure(
            com.wingedsheep.engine.core.DeclareBlockers(
                driver.player2,
                mapOf(lions to listOf(monk))
            )
        )

        result.isSuccess shouldBe false
        result.error shouldContainIgnoringCase "cannot block"
        result.error shouldContainIgnoringCase "power"
    }

    test("Fleet-Footed Monk cannot be blocked by creature with exactly power 2") {
        val driver = createDriver()

        // Put Fleet-Footed Monk on player 1's battlefield (attacker)
        val monk = driver.putCreatureOnBattlefield(driver.player1, "Fleet-Footed Monk")
        driver.removeSummoningSickness(monk)

        // Put a creature with exactly power 2 (Goblin Guide is 2/1)
        val goblin = driver.putCreatureOnBattlefield(driver.player2, "Goblin Guide")
        driver.removeSummoningSickness(goblin)

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Fleet-Footed Monk as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(monk), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Try to block with Goblin Guide (power 2) - should fail
        val result = driver.submitExpectFailure(
            com.wingedsheep.engine.core.DeclareBlockers(
                driver.player2,
                mapOf(goblin to listOf(monk))
            )
        )

        result.isSuccess shouldBe false
        result.error shouldContainIgnoringCase "cannot block"
    }

    test("normal creature can be blocked by creatures with any power") {
        val driver = createDriver()

        // Put Savannah Lions on player 1's battlefield (attacker without restriction)
        val lions = driver.putCreatureOnBattlefield(driver.player1, "Savannah Lions")
        driver.removeSummoningSickness(lions)

        // Put Grizzly Bears (2/2) on player 2's battlefield (blocker)
        val bears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Savannah Lions as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(lions), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Block with Grizzly Bears - should succeed (normal creature has no restriction)
        val result = driver.declareBlockers(
            driver.player2,
            mapOf(bears to listOf(lions))
        )

        result.isSuccess shouldBe true
    }
})
