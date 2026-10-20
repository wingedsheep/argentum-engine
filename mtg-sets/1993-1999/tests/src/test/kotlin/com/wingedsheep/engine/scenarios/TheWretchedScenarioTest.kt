package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for The Wretched (LEG #121).
 *
 * {3}{B}{B} Creature — Demon 2/5
 * "At end of combat, gain control of all creatures blocking this creature for as long as you
 *  control this creature."
 *
 * Covers: the end-of-combat trigger takes exactly the creatures blocking The Wretched (not other
 * creatures the defender controls), and the stolen blockers go home once The Wretched leaves the
 * battlefield.
 */
class TheWretchedScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver
    }

    test("gains control of its blocker at end of combat, but not of a non-blocking creature") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2

        val wretched = driver.putCreatureOnBattlefield(attacker, "The Wretched")
        driver.removeSummoningSickness(wretched)
        // Hill Giant (3/3) survives the Wretched's 2 damage, and the Wretched (2/5) survives its 3.
        val blocker = driver.putCreatureOnBattlefield(defender, "Hill Giant")
        val bystander = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(wretched), defender)
        driver.bothPass()
        driver.declareBlockers(defender, mapOf(blocker to listOf(wretched)))

        driver.passPriorityUntil(Step.END_COMBAT)
        driver.bothPass() // resolve the end-of-combat trigger

        val projected = projector.project(driver.state)
        projected.getController(blocker) shouldBe attacker
        projected.getController(bystander) shouldBe defender
    }

    test("the stolen blocker returns once The Wretched leaves the battlefield") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2

        val wretched = driver.putCreatureOnBattlefield(attacker, "The Wretched")
        driver.removeSummoningSickness(wretched)
        val blocker = driver.putCreatureOnBattlefield(defender, "Hill Giant")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(wretched), defender)
        driver.bothPass()
        driver.declareBlockers(defender, mapOf(blocker to listOf(wretched)))

        driver.passPriorityUntil(Step.END_COMBAT)
        driver.bothPass()
        projector.project(driver.state).getController(blocker) shouldBe attacker

        driver.moveToGraveyard(wretched)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        projector.project(driver.state).getController(blocker) shouldBe defender
    }
})
