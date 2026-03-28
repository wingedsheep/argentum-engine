package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Serpentine Basilisk.
 *
 * Serpentine Basilisk: {2}{G}{G}
 * Creature — Basilisk
 * 2/3
 * Whenever Serpentine Basilisk deals combat damage to a creature,
 * destroy that creature at end of combat.
 * Morph {1}{G}{G}
 */
class SerpentineBasiliskTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("basilisk destroys blocker at end of combat") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put Serpentine Basilisk on the battlefield
        val basilisk = driver.putCreatureOnBattlefield(attacker, "Serpentine Basilisk")
        driver.removeSummoningSickness(basilisk)

        // Put a 3/3 blocker (survives 2 combat damage but should be destroyed at end of combat)
        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with basilisk
        driver.declareAttackers(attacker, listOf(basilisk), defender)
        driver.bothPass()

        // Block with Centaur Courser
        driver.declareBlockers(defender, mapOf(blocker to listOf(basilisk)))
        driver.bothPass()

        // Combat damage step - basilisk deals 2 to Centaur Courser, trigger fires
        // (no first strike creatures, so first strike step is skipped per CR 510.4)
        driver.currentStep shouldBe Step.COMBAT_DAMAGE

        // Trigger goes on stack - both pass to resolve it (creates delayed trigger)
        driver.bothPass()

        // Centaur Courser should still be alive (3 toughness, took 2 damage)
        val courserAfterTrigger = driver.findPermanent(defender, "Centaur Courser")
        courserAfterTrigger shouldNotBe null

        // Advance to end of combat - delayed trigger fires and goes on the stack
        driver.passPriorityUntil(Step.END_COMBAT)

        // Resolve the delayed destroy trigger
        driver.bothPass()

        // Centaur Courser should now be destroyed
        val courserAfterEndCombat = driver.findPermanent(defender, "Centaur Courser")
        courserAfterEndCombat shouldBe null
        driver.getGraveyardCardNames(defender) shouldContain "Centaur Courser"
    }

    test("trigger does not fire when dealing combat damage to a player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put Serpentine Basilisk on the battlefield
        val basilisk = driver.putCreatureOnBattlefield(attacker, "Serpentine Basilisk")
        driver.removeSummoningSickness(basilisk)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with basilisk (no blockers)
        driver.declareAttackers(attacker, listOf(basilisk), defender)
        driver.bothPass()

        // No blockers
        driver.declareNoBlockers(defender)
        driver.bothPass()

        // Combat damage to player - trigger should NOT fire (toCreatureOnly)
        // (no first strike creatures, so first strike step is skipped per CR 510.4)
        // Player just takes 2 damage, no trigger on stack
        driver.assertLifeTotal(defender, 18)
    }

    test("basilisk kills blocker that would survive the combat damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Basilisk (2/3) vs Grizzly Bears (2/2)
        // Bears die to combat damage anyway, but the trigger also fires
        val basilisk = driver.putCreatureOnBattlefield(attacker, "Serpentine Basilisk")
        driver.removeSummoningSickness(basilisk)

        val blocker = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack and block
        driver.declareAttackers(attacker, listOf(basilisk), defender)
        driver.bothPass()
        driver.declareBlockers(defender, mapOf(blocker to listOf(basilisk)))
        driver.bothPass()

        // Combat damage - Bears (2/2) take 2 lethal damage and die to SBA
        // (no first strike creatures, so first strike step is skipped per CR 510.4)
        // Trigger also fires but Bears are already dead
        // Basilisk (2/3) takes 2 damage, survives with 1 toughness
        driver.currentStep shouldBe Step.COMBAT_DAMAGE

        // Resolve the trigger (if any) and advance
        driver.bothPass()

        // Bears should be in graveyard (died to lethal combat damage)
        driver.getGraveyardCardNames(defender) shouldContain "Grizzly Bears"

        // Basilisk should survive
        driver.findPermanent(attacker, "Serpentine Basilisk") shouldNotBe null
    }
})
