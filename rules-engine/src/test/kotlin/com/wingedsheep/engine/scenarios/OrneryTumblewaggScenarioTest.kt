package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ornery Tumblewagg (OTJ).
 *
 * Oracle: "At the beginning of combat on your turn, put a +1/+1 counter on target creature.
 * Whenever this creature attacks while saddled, double the number of +1/+1 counters on target
 * creature. Saddle 2"
 */
class OrneryTumblewaggScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("beginning of combat puts a +1/+1 counter on the target creature") {
        val driver = newDriver()
        val wagg = driver.putCreatureOnBattlefield(driver.player1, "Ornery Tumblewagg")
        val ally = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(wagg)

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.submitTargetSelection(driver.player1, listOf(ally))
        driver.bothPass()

        driver.plusOneCounters(ally) shouldBe 1
    }

    test("attacking while saddled doubles +1/+1 counters on the target creature") {
        val driver = newDriver()
        val wagg = driver.putCreatureOnBattlefield(driver.player1, "Ornery Tumblewagg")
        // Saddle 2 needs total power >= 2; one Grizzly Bears (power 2) suffices.
        val saddler = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        // A creature already bearing counters whose total we will double.
        val target = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(wagg)

        driver.submitSuccess(SaddleMount(driver.player1, wagg, listOf(saddler)))
        driver.bothPass()

        // Begin-combat trigger: drop the +1/+1 counter on `target` so it has 1 to double.
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.submitTargetSelection(driver.player1, listOf(target))
        driver.bothPass()
        driver.plusOneCounters(target) shouldBe 1

        // Declare attackers: attacks-while-saddled doubles the counters on the chosen creature.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(wagg), driver.player2)
        driver.submitTargetSelection(driver.player1, listOf(target))
        driver.bothPass()

        driver.plusOneCounters(target) shouldBe 2
    }
})
