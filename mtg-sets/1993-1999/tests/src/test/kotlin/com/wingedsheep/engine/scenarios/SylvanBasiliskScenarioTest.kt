package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Sylvan Basilisk (P02 #146).
 *
 * {3}{G}{G} Creature — Basilisk 2/4
 * "Whenever this creature becomes blocked by a creature, destroy that creature."
 *
 * `Triggers.self.becomesBlocked(by = Creature)` fires once per blocker, each with its own blocker
 * as the triggering entity — so every creature in a gang block is destroyed before combat damage.
 */
class SylvanBasiliskScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    test("a single blocker is destroyed before combat damage") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2
        val basilisk = driver.putCreatureOnBattlefield(attacker, "Sylvan Basilisk")
        driver.removeSummoningSickness(basilisk)
        val bears = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(basilisk), defender)
        driver.bothPass()
        driver.declareBlockers(defender, mapOf(bears to listOf(basilisk)))

        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.getGraveyardCardNames(defender) shouldContain "Grizzly Bears"
        // Blocked, so no damage reaches the defending player even with the blocker gone.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.getLifeTotal(defender) shouldBe 20
        driver.getGraveyardCardNames(attacker) shouldNotContain "Sylvan Basilisk"
    }

    test("each creature in a double block gets its own trigger and is destroyed") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2
        val basilisk = driver.putCreatureOnBattlefield(attacker, "Sylvan Basilisk")
        driver.removeSummoningSickness(basilisk)
        val bears = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        val giant = driver.putCreatureOnBattlefield(defender, "Hill Giant")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(basilisk), defender)
        driver.bothPass()
        driver.declareBlockers(defender, mapOf(bears to listOf(basilisk), giant to listOf(basilisk)))

        driver.stackSize shouldBe 2
        driver.bothPass()
        driver.bothPass()

        val graveyard = driver.getGraveyardCardNames(defender)
        graveyard shouldContain "Grizzly Bears"
        graveyard shouldContain "Hill Giant"
    }

    test("an unblocked Basilisk triggers nothing") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2
        val basilisk = driver.putCreatureOnBattlefield(attacker, "Sylvan Basilisk")
        driver.removeSummoningSickness(basilisk)
        driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(basilisk), defender)
        driver.bothPass()
        driver.declareBlockers(defender, emptyMap())

        driver.stackSize shouldBe 0
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.getGraveyardCardNames(defender) shouldNotContain "Grizzly Bears"
        driver.getLifeTotal(defender) shouldBe 18
    }
})
