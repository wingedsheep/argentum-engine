package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.ExuberantFuseling
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Exuberant Fuseling (ONE #129) — {R} 0/1 Creature — Phyrexian Goblin Warrior.
 *
 * "Trample. This creature gets +1/+0 for each oil counter on it. When this creature enters and
 *  whenever another creature or artifact you control is put into a graveyard from the
 *  battlefield, put an oil counter on this creature."
 */
class ExuberantFuselingScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ExuberantFuseling))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun resolveAll(driver: GameTestDriver) {
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && guard++ < 10) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    fun castFuseling(driver: GameTestDriver): EntityId {
        val p1 = driver.player1
        val card = driver.putCardInHand(p1, "Exuberant Fuseling")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpell(p1, card).outcome shouldBe Outcome.Done
        resolveAll(driver)
        val fuseling = driver.findPermanent(p1, "Exuberant Fuseling")
        fuseling shouldNotBe null
        return fuseling!!
    }

    fun bolt(driver: GameTestDriver, target: EntityId) {
        val p1 = driver.player1
        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpell(p1, bolt, listOf(target)).outcome shouldBe Outcome.Done
        resolveAll(driver)
    }

    test("enters, gets an oil counter, and is a 1/1 trampler") {
        val driver = newDriver()
        val fuseling = castFuseling(driver)
        oil(driver, fuseling) shouldBe 1
        driver.state.projectedState.getPower(fuseling) shouldBe 1
        driver.state.projectedState.getToughness(fuseling) shouldBe 1
        driver.state.projectedState.hasKeyword(fuseling, Keyword.TRAMPLE) shouldBe true
    }

    test("another creature you control dying adds an oil counter (+1/+0 only)") {
        val driver = newDriver()
        val fuseling = castFuseling(driver)
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        bolt(driver, bears)
        driver.findPermanent(driver.player1, "Grizzly Bears") shouldBe null
        oil(driver, fuseling) shouldBe 2
        driver.state.projectedState.getPower(fuseling) shouldBe 2
        driver.state.projectedState.getToughness(fuseling) shouldBe 1
    }

    test("an opponent's creature dying does not add a counter") {
        val driver = newDriver()
        val fuseling = castFuseling(driver)
        val bears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        bolt(driver, bears)
        driver.findPermanent(driver.player2, "Grizzly Bears") shouldBe null
        oil(driver, fuseling) shouldBe 1
    }
})
