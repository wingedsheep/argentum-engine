package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.MasterOfCruelties
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Master of Cruelties: can only attack alone; unblocked against a player it sets that player's
 * life to 1 and then assigns no combat damage this combat.
 */
class MasterOfCrueltiesScenarioTest : FunSpec({

    val walker = card("Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 5
        oracleText = ""
    }

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all)
            registerCard(MasterOfCruelties)
            registerCard(walker)
        }
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val master = driver.putCreatureOnBattlefield(driver.player1, "Master of Cruelties")
        driver.removeSummoningSickness(master)
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(bears)
        return Triple(driver, master, bears)
    }

    test("it can't attack alongside another creature") {
        val (driver, master, bears) = setup()
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, mapOf(master to driver.player2, bears to driver.player2))
            .error shouldNotBe null
    }

    test("unblocked, the defending player's life becomes 1 and it deals no combat damage") {
        val (driver, master, _) = setup()
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, mapOf(master to driver.player2)).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(driver.player2)
        driver.passPriorityUntil(Step.END_COMBAT)

        driver.getLifeTotal(driver.player2) shouldBe 1
        driver.getLifeTotal(driver.player1) shouldBe 20
    }

    test("blocked, nothing happens to the defending player's life") {
        val (driver, master, _) = setup()
        val wall = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, mapOf(master to driver.player2)).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(driver.player2, mapOf(wall to listOf(master))).error shouldBe null
        driver.passPriorityUntil(Step.END_COMBAT)

        driver.getLifeTotal(driver.player2) shouldBe 20
    }

    test("attacking a planeswalker unblocked doesn't trigger it — the walker takes the damage (ruling)") {
        val (driver, master, _) = setup()
        val pw = driver.putPermanentOnBattlefield(driver.player2, "Test Walker")
        driver.replaceState(
            driver.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 5)) }
        )
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, mapOf(master to pw)).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(driver.player2)
        driver.passPriorityUntil(Step.END_COMBAT)

        driver.getLifeTotal(driver.player2) shouldBe 20
        driver.state.getEntity(pw)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 4
    }
})
