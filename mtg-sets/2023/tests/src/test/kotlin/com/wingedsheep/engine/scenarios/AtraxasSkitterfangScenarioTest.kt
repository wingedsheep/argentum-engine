package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.AtraxasSkitterfang
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Atraxa's Skitterfang (ONE #223) — {3} 2/2 Artifact Creature — Phyrexian Insect.
 *
 * "This creature enters with three oil counters on it.
 *  At the beginning of combat on your turn, you may remove an oil counter from this creature. When
 *  you do, target creature you control gains your choice of flying, vigilance, deathtouch, or
 *  lifelink until end of turn."
 */
class AtraxasSkitterfangScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AtraxasSkitterfang))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun setOil(driver: GameTestDriver, id: EntityId, n: Int) {
        driver.replaceState(
            driver.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.OIL to n))) }
        )
    }

    fun drainStack(driver: GameTestDriver) {
        var guard = 0
        while (driver.pendingDecision == null && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
    }

    test("enters with three oil counters when cast") {
        val driver = newDriver()
        val p1 = driver.player1
        val card = driver.putCardInHand(p1, "Atraxa's Skitterfang")
        driver.giveColorlessMana(p1, 3)
        driver.castSpell(p1, card).error shouldBe null
        driver.bothPass()
        val fang = driver.findPermanent(p1, "Atraxa's Skitterfang").shouldNotBeNull()
        oil(driver, fang) shouldBe 3
    }

    test("removing an oil counter lets the chosen keyword land on a target creature you control") {
        val driver = newDriver()
        val p1 = driver.player1
        val fang = driver.putCreatureOnBattlefield(p1, "Atraxa's Skitterfang")
        val bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        setOil(driver, fang, 3)

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        drainStack(driver)
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(p1, true).error shouldBe null
        oil(driver, fang) shouldBe 2

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(p1, listOf(bear)).error shouldBe null
        drainStack(driver)

        // The keyword is chosen as the reflexive ability resolves (ruling 2023-02-04).
        val mode = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        mode.options.size shouldBe 4
        driver.submitDecision(p1, OptionChosenResponse(mode.id, 2)).error shouldBe null // deathtouch
        drainStack(driver)

        val projected = driver.state.projectedState
        projected.hasKeyword(bear, Keyword.DEATHTOUCH) shouldBe true
        projected.hasKeyword(bear, Keyword.FLYING) shouldBe false
        projected.hasKeyword(bear, Keyword.LIFELINK) shouldBe false
        projected.hasKeyword(fang, Keyword.DEATHTOUCH) shouldBe false
    }

    test("declining keeps the oil counter and grants nothing") {
        val driver = newDriver()
        val p1 = driver.player1
        val fang = driver.putCreatureOnBattlefield(p1, "Atraxa's Skitterfang")
        val bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        setOil(driver, fang, 3)

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        drainStack(driver)
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(p1, false).error shouldBe null
        drainStack(driver)

        driver.pendingDecision shouldBe null
        oil(driver, fang) shouldBe 3
        val projected = driver.state.projectedState
        listOf(Keyword.FLYING, Keyword.VIGILANCE, Keyword.DEATHTOUCH, Keyword.LIFELINK).forEach {
            projected.hasKeyword(bear, it) shouldBe false
        }
    }

    test("with no oil counters left the may-clause is never offered") {
        val driver = newDriver()
        val p1 = driver.player1
        val fang = driver.putCreatureOnBattlefield(p1, "Atraxa's Skitterfang")
        val bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        setOil(driver, fang, 0)

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) {
            (driver.pendingDecision is YesNoDecision) shouldBe false
            driver.bothPass()
        }
        driver.pendingDecision shouldBe null
        driver.state.projectedState.hasKeyword(bear, Keyword.FLYING) shouldBe false
    }
})
