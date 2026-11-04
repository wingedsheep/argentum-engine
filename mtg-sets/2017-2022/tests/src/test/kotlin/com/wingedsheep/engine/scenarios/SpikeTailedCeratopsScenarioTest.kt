package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.SpikeTailedCeratops
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SpikeTailedCeratopsScenarioTest : FunSpec({
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + SpikeTailedCeratops)
        it.initMirrorMatch(deck = Deck.of("Forest" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("Ceratops can block two attackers but not three") {
        val driver = driver()
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Spike-Tailed Ceratops")
        val attackers = List(3) { driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        attackers.forEach(driver::removeSummoningSickness)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, attackers, driver.player2).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        driver.declareBlockers(driver.player2, mapOf(blocker to attackers)).error shouldNotBe null
        driver.declareBlockers(driver.player2, mapOf(blocker to attackers.take(2))).error shouldBe null
    }

    test("two Ceratops do not stack extra blocks or grant extra blocks to other creatures") {
        val driver = driver()
        val first = driver.putCreatureOnBattlefield(driver.player2, "Spike-Tailed Ceratops")
        driver.putCreatureOnBattlefield(driver.player2, "Spike-Tailed Ceratops")
        val bear = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val attackers = List(3) { driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        attackers.forEach(driver::removeSummoningSickness)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, attackers, driver.player2).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        driver.declareBlockers(driver.player2, mapOf(first to attackers)).error shouldNotBe null
        driver.declareBlockers(driver.player2, mapOf(bear to attackers.take(2))).error shouldNotBe null
        driver.declareBlockers(
            driver.player2, mapOf(first to attackers.take(2), bear to listOf(attackers[2]))
        ).error shouldBe null
    }
})
