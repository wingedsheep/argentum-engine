package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.GustcloakSavior
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Gustcloak Savior.
 *
 * Gustcloak Savior: {4}{W}
 * Creature — Bird Soldier
 * 3/4
 * Flying
 * Whenever a creature you control becomes blocked, you may untap that creature and remove it from combat.
 */
class GustcloakSaviorTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Gustcloak Savior allows removing a blocked creature you control from combat") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put Savior on battlefield (it won't attack - just provides the trigger)
        val savior = driver.putCreatureOnBattlefield(attacker, "Gustcloak Savior")
        driver.removeSummoningSickness(savior)

        // Put a different creature to attack with
        val attackingCreature = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(attackingCreature)

        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(attackingCreature), defender)

        // Grizzly Bears should be tapped from attacking
        driver.state.getEntity(attackingCreature)?.has<TappedComponent>() shouldBe true

        driver.bothPass()

        // Block with Centaur Courser
        driver.declareBlockers(defender, mapOf(blocker to listOf(attackingCreature)))

        // Savior's trigger fires. Both pass to resolve.
        driver.bothPass()

        // Choose yes - untap and remove from combat
        driver.submitYesNo(attacker, true)

        // Grizzly Bears should be untapped
        driver.state.getEntity(attackingCreature)?.has<TappedComponent>() shouldBe false

        // Grizzly Bears should no longer be attacking
        driver.state.getEntity(attackingCreature)?.has<AttackingComponent>() shouldBe false

        // Grizzly Bears should not be marked as blocked
        driver.state.getEntity(attackingCreature)?.has<BlockedComponent>() shouldBe false

        // The blocker should no longer be blocking
        driver.state.getEntity(blocker)?.has<BlockingComponent>() shouldBe false

        // Advance through combat - no damage
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Both creatures should still be alive
        driver.findPermanent(attacker, "Grizzly Bears") shouldNotBe null
        driver.findPermanent(defender, "Centaur Courser") shouldNotBe null

        // Savior should still be alive
        driver.findPermanent(attacker, "Gustcloak Savior") shouldNotBe null

        // No damage to either player
        driver.assertLifeTotal(attacker, 20)
        driver.assertLifeTotal(defender, 20)
    }

    test("Gustcloak Savior - declining the trigger leaves the creature in combat") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val savior = driver.putCreatureOnBattlefield(attacker, "Gustcloak Savior")
        driver.removeSummoningSickness(savior)

        val attackingCreature = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(attackingCreature)

        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(attackingCreature), defender)
        driver.bothPass()

        // Block
        driver.declareBlockers(defender, mapOf(blocker to listOf(attackingCreature)))

        // Trigger fires. Both pass to resolve.
        driver.bothPass()

        // Choose no - stay in combat
        driver.submitYesNo(attacker, false)

        // Grizzly Bears should still be tapped and attacking
        driver.state.getEntity(attackingCreature)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(attackingCreature)?.has<AttackingComponent>() shouldBe true

        // 3/3 kills 2/2 in combat
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Grizzly Bears should be dead, blocker alive
        driver.findPermanent(attacker, "Grizzly Bears") shouldBe null
        driver.findPermanent(defender, "Centaur Courser") shouldNotBe null
    }
})
