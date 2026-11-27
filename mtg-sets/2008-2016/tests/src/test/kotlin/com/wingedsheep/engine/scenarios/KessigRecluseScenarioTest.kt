package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class KessigRecluseScenarioTest : FunSpec({

    fun newDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("reach: it can block a creature with flying") {
        val driver = newDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val birds = driver.putCreatureOnBattlefield(attacker, "Birds of Paradise")
        driver.removeSummoningSickness(birds)
        val recluse = driver.putCreatureOnBattlefield(defender, "Kessig Recluse")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(birds), defender).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(recluse to listOf(birds))).error shouldBe null
    }

    test("deathtouch: blocking a 5/5 destroys it") {
        val driver = newDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val force = driver.putCreatureOnBattlefield(attacker, "Force of Nature")
        driver.removeSummoningSickness(force)
        val recluse = driver.putCreatureOnBattlefield(defender, "Kessig Recluse")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(force), defender).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(recluse to listOf(force))).error shouldBe null
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.getGraveyard(attacker).contains(force) shouldBe true
    }
})
