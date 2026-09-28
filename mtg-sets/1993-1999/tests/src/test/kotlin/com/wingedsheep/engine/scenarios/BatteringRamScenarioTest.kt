package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Battering Ram (ATQ #41).
 *
 * {2} Artifact Creature — Construct 1/1
 * "At the beginning of combat on your turn, this creature gains banding until end of combat.
 *  Whenever this creature becomes blocked by a Wall, destroy that Wall at end of combat."
 *
 * Exercises the blocker-filtered becomes-blocked-by trigger: `Triggers.self.becomesBlocked(by = Wall)`
 * fires (with the Wall as the triggering entity) only when a Wall blocks Battering
 * Ram, and not for a non-Wall blocker.
 */
class BatteringRamScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    test("gains banding at the beginning of combat on its controller's turn") {
        val driver = createDriver()
        val attacker = driver.player1
        val ram = driver.putCreatureOnBattlefield(attacker, "Battering Ram")
        driver.removeSummoningSickness(ram)

        // Before combat: no banding.
        projector.project(driver.state).hasKeyword(ram, Keyword.BANDING) shouldBe false

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.bothPass() // resolve the begin-combat banding trigger

        projector.project(driver.state).hasKeyword(ram, Keyword.BANDING) shouldBe true
    }

    test("a Wall blocking Battering Ram is destroyed at end of combat") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2

        val ram = driver.putCreatureOnBattlefield(attacker, "Battering Ram")
        driver.removeSummoningSickness(ram)
        val wall = driver.putCreatureOnBattlefield(defender, "Wall of Spears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(ram), defender)
        driver.bothPass()

        driver.declareBlockers(defender, mapOf(wall to listOf(ram)))
        // The becomes-blocked-by-a-Wall trigger now fires; resolve everything through end of combat.
        driver.passPriorityUntil(Step.END_COMBAT)
        driver.bothPass()
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.getGraveyardCardNames(defender) shouldContain "Wall of Spears"
    }

    test("a NON-Wall blocker is NOT destroyed by Battering Ram") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2

        val ram = driver.putCreatureOnBattlefield(attacker, "Battering Ram")
        driver.removeSummoningSickness(ram)
        // Grizzly Bears (2/2) is not a Wall; it survives combat against the 1/1 Ram and is not
        // destroyed by the becomes-blocked-by-a-Wall trigger.
        val bears = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(ram), defender)
        driver.bothPass()

        driver.declareBlockers(defender, mapOf(bears to listOf(ram)))
        driver.passPriorityUntil(Step.END_COMBAT)
        driver.bothPass()
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.getGraveyardCardNames(defender) shouldNotContain "Grizzly Bears"
    }
})
