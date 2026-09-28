package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Alaborn Zealot (P02).
 *
 * {W} Creature — Human Soldier 1/1
 * "When this creature blocks a creature, destroy both creatures."
 */
class AlabornZealotScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    test("blocking destroys both the Zealot and the blocked creature") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2
        val giant = driver.putCreatureOnBattlefield(attacker, "Hill Giant")
        driver.removeSummoningSickness(giant)
        val zealot = driver.putCreatureOnBattlefield(defender, "Alaborn Zealot")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(giant), defender)
        driver.bothPass()
        driver.declareBlockers(defender, mapOf(zealot to listOf(giant)))

        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.getGraveyardCardNames(attacker) shouldContain "Hill Giant"
        driver.getGraveyardCardNames(defender) shouldContain "Alaborn Zealot"
    }
})
