package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.TrawlerDrake
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
 * Trawler Drake (ONE #74) — {2}{U} 0/0 Creature — Phyrexian Drake.
 *
 * "Flying. This creature enters with an oil counter on it. This creature gets +1/+1 for each oil
 *  counter on it. Whenever you cast a noncreature spell, put an oil counter on this creature."
 */
class TrawlerDrakeScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TrawlerDrake))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
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

    fun castDrake(driver: GameTestDriver): EntityId {
        val p1 = driver.player1
        val card = driver.putCardInHand(p1, "Trawler Drake")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 2)
        driver.castSpell(p1, card).outcome shouldBe Outcome.Done
        resolveAll(driver)
        val drake = driver.findPermanent(p1, "Trawler Drake")
        drake shouldNotBe null
        return drake!!
    }

    test("enters with an oil counter as a 1/1 flier") {
        val driver = newDriver()
        val drake = castDrake(driver)
        oil(driver, drake) shouldBe 1
        driver.state.projectedState.getPower(drake) shouldBe 1
        driver.state.projectedState.getToughness(drake) shouldBe 1
        driver.state.projectedState.hasKeyword(drake, Keyword.FLYING) shouldBe true
    }

    test("casting a noncreature spell adds an oil counter, growing the drake") {
        val driver = newDriver()
        val drake = castDrake(driver)
        val p1 = driver.player1
        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpell(p1, bolt, listOf(driver.player2)).outcome shouldBe Outcome.Done
        resolveAll(driver)
        oil(driver, drake) shouldBe 2
        driver.state.projectedState.getPower(drake) shouldBe 2
        driver.state.projectedState.getToughness(drake) shouldBe 2
    }

    test("casting a creature spell does not add a counter") {
        val driver = newDriver()
        val drake = castDrake(driver)
        val p1 = driver.player1
        val lions = driver.putCardInHand(p1, "Savannah Lions")
        driver.giveMana(p1, Color.WHITE, 1)
        driver.castSpell(p1, lions).outcome shouldBe Outcome.Done
        resolveAll(driver)
        oil(driver, drake) shouldBe 1
    }

    test("an opponent's noncreature spell does not add a counter") {
        val driver = newDriver()
        val drake = castDrake(driver)
        val p2 = driver.player2
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.state.activePlayerId shouldBe p2
        val bolt = driver.putCardInHand(p2, "Lightning Bolt")
        driver.giveMana(p2, Color.RED, 1)
        driver.castSpell(p2, bolt, listOf(driver.player1)).outcome shouldBe Outcome.Done
        resolveAll(driver)
        oil(driver, drake) shouldBe 1
    }
})
