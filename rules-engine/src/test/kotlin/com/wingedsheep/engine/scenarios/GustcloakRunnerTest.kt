package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.GustcloakRunner
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Gustcloak Runner.
 *
 * Gustcloak Runner: {W}
 * Creature — Human Soldier
 * 1/1
 * Whenever Gustcloak Runner becomes blocked, you may untap it and remove it from combat.
 */
class GustcloakRunnerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Gustcloak Runner is untapped and removed from combat when blocked and player chooses yes") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val runner = driver.putCreatureOnBattlefield(attacker, "Gustcloak Runner")
        driver.removeSummoningSickness(runner)

        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(runner), defender)

        // Runner should be tapped from attacking
        driver.state.getEntity(runner)?.has<TappedComponent>() shouldBe true

        driver.bothPass()

        // Block with Centaur Courser (3/3)
        driver.declareBlockers(defender, mapOf(blocker to listOf(runner)))

        // Trigger fires. Both pass to resolve.
        driver.bothPass()

        // Choose yes - untap and remove from combat
        driver.submitYesNo(attacker, true)

        // Runner should be untapped
        driver.state.getEntity(runner)?.has<TappedComponent>() shouldBe false

        // Runner should no longer be attacking
        driver.state.getEntity(runner)?.has<AttackingComponent>() shouldBe false

        // Runner should not be marked as blocked
        driver.state.getEntity(runner)?.has<BlockedComponent>() shouldBe false

        // The blocker should no longer be blocking
        driver.state.getEntity(blocker)?.has<BlockingComponent>() shouldBe false

        // Advance through combat - no damage
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Both creatures should still be alive
        driver.findPermanent(attacker, "Gustcloak Runner") shouldNotBe null
        driver.findPermanent(defender, "Centaur Courser") shouldNotBe null

        // No damage to either player
        driver.assertLifeTotal(attacker, 20)
        driver.assertLifeTotal(defender, 20)
    }

    test("Gustcloak Runner stays in combat when player chooses no") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val runner = driver.putCreatureOnBattlefield(attacker, "Gustcloak Runner")
        driver.removeSummoningSickness(runner)

        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(runner), defender)
        driver.bothPass()

        // Block with Centaur Courser (3/3)
        driver.declareBlockers(defender, mapOf(blocker to listOf(runner)))

        // Trigger fires. Both pass to resolve.
        driver.bothPass()

        // Choose no - stay in combat
        driver.submitYesNo(attacker, false)

        // Runner should still be tapped and attacking
        driver.state.getEntity(runner)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(runner)?.has<AttackingComponent>() shouldBe true

        // 3/3 kills 1/1 in combat
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Runner should be dead, blocker alive
        driver.findPermanent(attacker, "Gustcloak Runner") shouldBe null
        driver.findPermanent(defender, "Centaur Courser") shouldNotBe null
    }

    test("Gustcloak Runner deals no combat damage when removed from combat") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val runner = driver.putCreatureOnBattlefield(attacker, "Gustcloak Runner")
        driver.removeSummoningSickness(runner)

        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(runner), defender)
        driver.bothPass()

        driver.declareBlockers(defender, mapOf(blocker to listOf(runner)))

        // Trigger fires. Both pass to resolve.
        driver.bothPass()

        // Choose yes - untap and remove from combat
        driver.submitYesNo(attacker, true)

        // Advance through combat
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Both creatures should still be alive - no damage dealt
        driver.findPermanent(attacker, "Gustcloak Runner") shouldNotBe null
        driver.findPermanent(defender, "Centaur Courser") shouldNotBe null
    }
})
