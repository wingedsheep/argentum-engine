package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SomberwaldDryadScenarioTest : FunSpec({

    fun attackWithDryad(defenderControlsForest: Boolean): Pair<GameTestDriver, Pair<EntityId, EntityId>> {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val dryad = driver.putCreatureOnBattlefield(attacker, "Somberwald Dryad")
        driver.removeSummoningSickness(dryad)
        val bears = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        if (defenderControlsForest) driver.putLandOnBattlefield(defender, "Forest")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(dryad), defender).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        return driver to (dryad to bears)
    }

    test("forestwalk: it can't be blocked while the defending player controls a Forest") {
        val (driver, ids) = attackWithDryad(defenderControlsForest = true)
        val (dryad, bears) = ids
        val defender = driver.getOpponent(driver.activePlayer!!)
        driver.declareBlockers(defender, mapOf(bears to listOf(dryad))).error shouldNotBe null
    }

    test("without a Forest it can be blocked") {
        val (driver, ids) = attackWithDryad(defenderControlsForest = false)
        val (dryad, bears) = ids
        val defender = driver.getOpponent(driver.activePlayer!!)
        driver.declareBlockers(defender, mapOf(bears to listOf(dryad))).error shouldBe null
    }
})
