package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.DawningPurist
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Dawning Purist.
 *
 * Dawning Purist: {2}{W}
 * Creature — Human Cleric
 * 2/2
 * Whenever Dawning Purist deals combat damage to a player, you may destroy target
 * enchantment that player controls.
 * Morph {1}{W}
 */
class DawningPuristTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("deals combat damage and destroys opponent enchantment") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.player1
        val defender = driver.player2

        // Put Dawning Purist on battlefield and remove summoning sickness
        val purist = driver.putCreatureOnBattlefield(attacker, "Dawning Purist")
        driver.removeSummoningSickness(purist)

        // Put an enchantment on opponent's battlefield
        val enchantment = driver.putPermanentOnBattlefield(defender, "Test Enchantment")
        driver.findPermanent(defender, "Test Enchantment") shouldNotBe null

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with Dawning Purist
        driver.declareAttackers(attacker, listOf(purist), defender)
        driver.bothPass()

        // No blockers
        driver.declareNoBlockers(defender)
        driver.bothPass()

        // Skip first strike damage
        driver.currentStep shouldBe Step.FIRST_STRIKE_COMBAT_DAMAGE
        driver.bothPass()

        // Combat damage is dealt - trigger fires with MayEffect
        driver.currentStep shouldBe Step.COMBAT_DAMAGE

        // Step 1: "May destroy?" — answer yes
        val yesNoDecision = driver.pendingDecision as YesNoDecision
        driver.submitYesNo(yesNoDecision.playerId, true)

        // Step 2: Choose target enchantment
        val chooseTargets = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(attacker, listOf(enchantment))

        // Trigger goes on stack - resolve it
        driver.bothPass()

        // Enchantment should be destroyed
        driver.findPermanent(defender, "Test Enchantment") shouldBe null
        driver.getGraveyardCardNames(defender) shouldContain "Test Enchantment"

        // Defender took 2 combat damage
        driver.assertLifeTotal(defender, 18)
    }

    test("deals combat damage but declines to destroy enchantment") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.player1
        val defender = driver.player2

        // Put Dawning Purist on battlefield and remove summoning sickness
        val purist = driver.putCreatureOnBattlefield(attacker, "Dawning Purist")
        driver.removeSummoningSickness(purist)

        // Put an enchantment on opponent's battlefield
        driver.putPermanentOnBattlefield(defender, "Test Enchantment")

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with Dawning Purist
        driver.declareAttackers(attacker, listOf(purist), defender)
        driver.bothPass()

        // No blockers
        driver.declareNoBlockers(defender)
        driver.bothPass()

        // Skip first strike damage
        driver.bothPass()

        // Combat damage - trigger fires with MayEffect, decline
        val yesNoDecision = driver.pendingDecision as YesNoDecision
        driver.submitYesNo(yesNoDecision.playerId, false)

        // Enchantment should still be on the battlefield
        driver.findPermanent(defender, "Test Enchantment") shouldNotBe null

        // Defender still took 2 combat damage
        driver.assertLifeTotal(defender, 18)
    }

    test("no trigger when no enchantments to target") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.player1
        val defender = driver.player2

        // Put Dawning Purist on battlefield and remove summoning sickness
        val purist = driver.putCreatureOnBattlefield(attacker, "Dawning Purist")
        driver.removeSummoningSickness(purist)

        // No enchantments on opponent's battlefield

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with Dawning Purist
        driver.declareAttackers(attacker, listOf(purist), defender)
        driver.bothPass()

        // No blockers
        driver.declareNoBlockers(defender)
        driver.bothPass()

        // Skip first strike damage
        driver.bothPass()

        // Combat damage step - no enchantments so trigger doesn't go on stack
        // Game should proceed normally
        driver.assertLifeTotal(defender, 18)
    }

    test("trigger does not fire when blocked and no damage dealt to player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.player1
        val defender = driver.player2

        // Put Dawning Purist on battlefield and remove summoning sickness
        val purist = driver.putCreatureOnBattlefield(attacker, "Dawning Purist")
        driver.removeSummoningSickness(purist)

        // Put a 2/2 blocker and an enchantment on opponent's battlefield
        val blocker = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)
        driver.putPermanentOnBattlefield(defender, "Test Enchantment")

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with Dawning Purist
        driver.declareAttackers(attacker, listOf(purist), defender)
        driver.bothPass()

        // Block with Grizzly Bears
        driver.declareBlockers(defender, mapOf(blocker to listOf(purist)))
        driver.bothPass()

        // Skip first strike damage
        driver.bothPass()

        // Combat damage - both creatures die, no damage to player
        // Trigger should NOT fire since no combat damage was dealt to a player

        // Life total should be unchanged
        driver.assertLifeTotal(defender, 20)

        // Enchantment should still be on the battlefield
        driver.findPermanent(defender, "Test Enchantment") shouldNotBe null
    }
})
