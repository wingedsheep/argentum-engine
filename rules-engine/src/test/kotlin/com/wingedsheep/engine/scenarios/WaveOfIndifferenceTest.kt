package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.WaveOfIndifference
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Tests for Wave of Indifference.
 *
 * {X}{R} Sorcery
 * X target creatures can't block this turn.
 */
class WaveOfIndifferenceTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Mountain" to 10,
                "Forest" to 10,
                "Grizzly Bears" to 10,
                "Wave of Indifference" to 5
            ),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.advanceToPlayer1PrecombatMain() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.PRECOMBAT_MAIN)
            safety++
        }
    }

    fun GameTestDriver.advanceToPlayer1DeclareAttackers() {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }
    }

    test("Wave of Indifference prevents targeted creature from blocking") {
        val driver = createDriver()

        // Set up creatures
        val attacker = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        // Put lands for casting
        repeat(5) { driver.putLandOnBattlefield(driver.player1, "Mountain") }

        // Advance to precombat main phase
        driver.advanceToPlayer1PrecombatMain()
        driver.currentStep shouldBe Step.PRECOMBAT_MAIN

        // Cast Wave of Indifference with X=1 targeting the blocker
        val waveCard = driver.findCardInHand(driver.player1, "Wave of Indifference")
            ?: driver.putCardInHand(driver.player1, "Wave of Indifference")
        val castResult = driver.castXSpell(driver.player1, waveCard, xValue = 1, targets = listOf(blocker))
        castResult.isSuccess shouldBe true

        // Advance to combat
        driver.advanceToPlayer1DeclareAttackers()
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS

        // Declare attacker
        val attackResult = driver.declareAttackers(driver.player1, listOf(attacker), driver.player2)
        attackResult.isSuccess shouldBe true

        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Try to block with the targeted creature - should fail
        val blockResult = driver.submitExpectFailure(
            DeclareBlockers(driver.player2, mapOf(blocker to listOf(attacker)))
        )
        blockResult.isSuccess shouldBe false
        blockResult.error shouldContainIgnoringCase "can't block"
    }

    test("Wave of Indifference does not prevent non-targeted creature from blocking") {
        val driver = createDriver()

        // Set up creatures
        val attacker = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        val targetedBlocker = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(targetedBlocker)

        val freeBlocker = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(freeBlocker)

        repeat(5) { driver.putLandOnBattlefield(driver.player1, "Mountain") }

        driver.advanceToPlayer1PrecombatMain()

        // Cast Wave targeting only one blocker
        val waveCard = driver.findCardInHand(driver.player1, "Wave of Indifference")
            ?: driver.putCardInHand(driver.player1, "Wave of Indifference")
        val castResult = driver.castXSpell(driver.player1, waveCard, xValue = 1, targets = listOf(targetedBlocker))
        castResult.isSuccess shouldBe true

        // Advance to combat
        driver.advanceToPlayer1DeclareAttackers()
        val attackResult = driver.declareAttackers(driver.player1, listOf(attacker), driver.player2)
        attackResult.isSuccess shouldBe true

        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Free blocker should be able to block
        val blockResult = driver.declareBlockers(driver.player2, mapOf(freeBlocker to listOf(attacker)))
        blockResult.isSuccess shouldBe true
    }

    test("Wave of Indifference with X=2 prevents two creatures from blocking") {
        val driver = createDriver()

        val attacker1 = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(attacker1)
        val attacker2 = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(attacker2)

        val blocker1 = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker1)
        val blocker2 = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker2)

        repeat(5) { driver.putLandOnBattlefield(driver.player1, "Mountain") }

        driver.advanceToPlayer1PrecombatMain()

        val waveCard = driver.findCardInHand(driver.player1, "Wave of Indifference")
            ?: driver.putCardInHand(driver.player1, "Wave of Indifference")
        val castResult = driver.castXSpell(
            driver.player1, waveCard, xValue = 2,
            targets = listOf(blocker1, blocker2)
        )
        castResult.isSuccess shouldBe true

        driver.advanceToPlayer1DeclareAttackers()
        val attackResult = driver.declareAttackers(
            driver.player1,
            listOf(attacker1, attacker2),
            driver.player2
        )
        attackResult.isSuccess shouldBe true

        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Neither blocker should be able to block
        val blockResult1 = driver.submitExpectFailure(
            DeclareBlockers(driver.player2, mapOf(blocker1 to listOf(attacker1)))
        )
        blockResult1.isSuccess shouldBe false
        blockResult1.error shouldContainIgnoringCase "can't block"
    }
})
