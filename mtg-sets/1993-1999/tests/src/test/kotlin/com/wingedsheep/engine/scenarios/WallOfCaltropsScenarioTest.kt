package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Wall of Caltrops (LEG #42).
 *
 * "Whenever this creature blocks a creature, if at least one other Wall creature is blocking that
 * creature and no non-Wall creatures are blocking that creature, this creature gains banding until
 * end of turn."
 */
class WallOfCaltropsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    test("blocking alongside only another Wall grants banding") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2
        val giant = driver.putCreatureOnBattlefield(attacker, "Hill Giant")
        driver.removeSummoningSickness(giant)
        val caltrops = driver.putCreatureOnBattlefield(defender, "Wall of Caltrops")
        val stoneWall = driver.putCreatureOnBattlefield(defender, "Wall of Stone")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(giant), defender)
        driver.bothPass()
        driver.declareBlockers(defender, mapOf(caltrops to listOf(giant), stoneWall to listOf(giant)))

        driver.stackSize shouldBe 1
        driver.state.projectedState.hasKeyword(caltrops, Keyword.BANDING) shouldBe false
        driver.bothPass()

        driver.stackSize shouldBe 0
        driver.state.projectedState.hasKeyword(caltrops, Keyword.BANDING) shouldBe true
    }

    test("a non-Wall blocking the same creature stops the trigger") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2
        val giant = driver.putCreatureOnBattlefield(attacker, "Hill Giant")
        driver.removeSummoningSickness(giant)
        val caltrops = driver.putCreatureOnBattlefield(defender, "Wall of Caltrops")
        val stoneWall = driver.putCreatureOnBattlefield(defender, "Wall of Stone")
        val bears = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(giant), defender)
        driver.bothPass()
        driver.declareBlockers(
            defender,
            mapOf(caltrops to listOf(giant), stoneWall to listOf(giant), bears to listOf(giant))
        )

        driver.stackSize shouldBe 0
        driver.state.projectedState.hasKeyword(caltrops, Keyword.BANDING) shouldBe false
    }

    test("blocking alone does not grant banding") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2
        val giant = driver.putCreatureOnBattlefield(attacker, "Hill Giant")
        driver.removeSummoningSickness(giant)
        val caltrops = driver.putCreatureOnBattlefield(defender, "Wall of Caltrops")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(giant), defender)
        driver.bothPass()
        driver.declareBlockers(defender, mapOf(caltrops to listOf(giant)))

        driver.stackSize shouldBe 0
        driver.state.projectedState.hasKeyword(caltrops, Keyword.BANDING) shouldBe false
    }
})
