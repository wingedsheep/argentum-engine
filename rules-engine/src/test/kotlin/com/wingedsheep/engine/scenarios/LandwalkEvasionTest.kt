package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Tests for landwalk evasion abilities.
 *
 * Landwalk rules (CR 702.14):
 * - A creature with landwalk can't be blocked as long as the defending player
 *   controls at least one land of the specified type.
 * - Landwalk abilities include: forestwalk, islandwalk, mountainwalk, plainswalk, swampwalk.
 */
class LandwalkEvasionTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 10,
                "Island" to 10,
                "Grizzly Bears" to 10,
                "Forest Walker" to 5,
                "Island Walker" to 5
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

    test("creature with forestwalk cannot be blocked when defender controls a Forest") {
        val driver = createDriver()

        // Put a Forest Walker on player 1's battlefield (attacker)
        val forestWalker = driver.putCreatureOnBattlefield(driver.player1, "Forest Walker")
        driver.removeSummoningSickness(forestWalker)

        // Put a Grizzly Bears on player 2's battlefield (potential blocker)
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        // Put a Forest on player 2's battlefield (defending player)
        driver.putLandOnBattlefield(driver.player2, "Forest")

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Forest Walker as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(forestWalker), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Try to block with Grizzly Bears - should fail due to forestwalk
        val result = driver.submitExpectFailure(
            com.wingedsheep.engine.core.DeclareBlockers(
                driver.player2,
                mapOf(blocker to listOf(forestWalker))
            )
        )

        result.isSuccess shouldBe false
        result.error shouldContainIgnoringCase "forestwalk"
        result.error shouldContainIgnoringCase "cannot be blocked"
    }

    test("creature with forestwalk CAN be blocked when defender does NOT control a Forest") {
        val driver = createDriver()

        // Put a Forest Walker on player 1's battlefield (attacker)
        val forestWalker = driver.putCreatureOnBattlefield(driver.player1, "Forest Walker")
        driver.removeSummoningSickness(forestWalker)

        // Put a Grizzly Bears on player 2's battlefield (potential blocker)
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        // Put an Island (NOT a Forest) on player 2's battlefield
        driver.putLandOnBattlefield(driver.player2, "Island")

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Forest Walker as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(forestWalker), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Block with Grizzly Bears - should succeed since defender has no Forest
        val result = driver.declareBlockers(
            driver.player2,
            mapOf(blocker to listOf(forestWalker))
        )

        result.isSuccess shouldBe true
    }

    test("creature with islandwalk cannot be blocked when defender controls an Island") {
        val driver = createDriver()

        // Put an Island Walker on player 1's battlefield (attacker)
        val islandWalker = driver.putCreatureOnBattlefield(driver.player1, "Island Walker")
        driver.removeSummoningSickness(islandWalker)

        // Put a Grizzly Bears on player 2's battlefield (potential blocker)
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        // Put an Island on player 2's battlefield (defending player)
        driver.putLandOnBattlefield(driver.player2, "Island")

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Island Walker as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(islandWalker), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Try to block with Grizzly Bears - should fail due to islandwalk
        val result = driver.submitExpectFailure(
            com.wingedsheep.engine.core.DeclareBlockers(
                driver.player2,
                mapOf(blocker to listOf(islandWalker))
            )
        )

        result.isSuccess shouldBe false
        result.error shouldContainIgnoringCase "islandwalk"
        result.error shouldContainIgnoringCase "cannot be blocked"
    }

    test("creature with islandwalk CAN be blocked when defender controls Forest but not Island") {
        val driver = createDriver()

        // Put an Island Walker on player 1's battlefield (attacker)
        val islandWalker = driver.putCreatureOnBattlefield(driver.player1, "Island Walker")
        driver.removeSummoningSickness(islandWalker)

        // Put a Grizzly Bears on player 2's battlefield (potential blocker)
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        // Put a Forest (NOT an Island) on player 2's battlefield
        driver.putLandOnBattlefield(driver.player2, "Forest")

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Island Walker as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(islandWalker), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Block with Grizzly Bears - should succeed since defender has no Island
        val result = driver.declareBlockers(
            driver.player2,
            mapOf(blocker to listOf(islandWalker))
        )

        result.isSuccess shouldBe true
    }

    test("landwalk only considers defending player's lands, not attacking player's") {
        val driver = createDriver()

        // Put a Forest Walker on player 1's battlefield (attacker)
        val forestWalker = driver.putCreatureOnBattlefield(driver.player1, "Forest Walker")
        driver.removeSummoningSickness(forestWalker)

        // Put a Grizzly Bears on player 2's battlefield (potential blocker)
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        // Put a Forest on PLAYER 1's battlefield (attacking player, not defender)
        driver.putLandOnBattlefield(driver.player1, "Forest")

        // Player 2 (defender) has no lands

        // Advance to player 1's declare attackers step
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.activePlayer shouldBe driver.player1

        // Declare Forest Walker as attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(forestWalker), driver.player2)
        attackResult.isSuccess shouldBe true

        // Both players pass to move to declare blockers
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Block with Grizzly Bears - should succeed because defender has no Forest
        val result = driver.declareBlockers(
            driver.player2,
            mapOf(blocker to listOf(forestWalker))
        )

        result.isSuccess shouldBe true
    }
})
