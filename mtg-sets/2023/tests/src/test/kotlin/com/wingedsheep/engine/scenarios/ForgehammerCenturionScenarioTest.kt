package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.ForgehammerCenturion
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Forgehammer Centurion (ONE #130) — {2}{R} 3/2 Creature — Phyrexian Warrior.
 *
 * "Whenever another creature or artifact you control is put into a graveyard from the battlefield,
 *  put an oil counter on this creature. Whenever this creature attacks, you may remove two oil
 *  counters from it. When you do, target creature can't block this turn."
 */
class ForgehammerCenturionScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ForgehammerCenturion))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
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

    fun bolt(driver: GameTestDriver, target: EntityId) {
        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.giveMana(driver.player1, Color.RED, 1)
        driver.castSpellWithTargets(driver.player1, bolt, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        driver.bothPass()
        while (driver.pendingDecision != null) driver.autoResolveDecision()
        if (driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    test("another creature you control dying adds an oil counter; an opponent's does not") {
        val driver = newDriver()
        val centurion = driver.putCreatureOnBattlefield(driver.player1, "Forgehammer Centurion")
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Savannah Lions")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Savannah Lions")

        bolt(driver, mine)
        oil(driver, centurion) shouldBe 1

        bolt(driver, theirs)
        oil(driver, centurion) shouldBe 1
    }

    test("attacking with two oil counters: remove them and the target creature can't block") {
        val driver = newDriver()
        val p1 = driver.player1
        val p2 = driver.player2
        val centurion = driver.putCreatureOnBattlefield(p1, "Forgehammer Centurion")
        driver.removeSummoningSickness(centurion)
        val blocker = driver.putCreatureOnBattlefield(p2, "Centaur Courser")
        setOil(driver, centurion, 3)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(centurion), p2).error shouldBe null

        var guard = 0
        while (driver.pendingDecision == null && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(p1, true).error shouldBe null

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(p1, listOf(blocker)).error shouldBe null
        guard = 0
        while (driver.state.stack.isNotEmpty() && driver.pendingDecision == null && guard++ < 10) driver.bothPass()

        oil(driver, centurion) shouldBe 1
        driver.state.projectedState.cantBlock(blocker) shouldBe true
    }

    test("with only one oil counter the may-clause is never offered") {
        val driver = newDriver()
        val p1 = driver.player1
        val p2 = driver.player2
        val centurion = driver.putCreatureOnBattlefield(p1, "Forgehammer Centurion")
        driver.removeSummoningSickness(centurion)
        val blocker = driver.putCreatureOnBattlefield(p2, "Centaur Courser")
        setOil(driver, centurion, 1)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(centurion), p2).error shouldBe null

        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) {
            (driver.pendingDecision is YesNoDecision) shouldBe false
            driver.bothPass()
        }
        driver.pendingDecision shouldBe null
        oil(driver, centurion) shouldBe 1
        driver.state.projectedState.cantBlock(blocker) shouldBe false
    }

    test("declining keeps the counters and the creature can still block") {
        val driver = newDriver()
        val p1 = driver.player1
        val p2 = driver.player2
        val centurion = driver.putCreatureOnBattlefield(p1, "Forgehammer Centurion")
        driver.removeSummoningSickness(centurion)
        val blocker = driver.putCreatureOnBattlefield(p2, "Centaur Courser")
        setOil(driver, centurion, 2)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(centurion), p2).error shouldBe null

        var guard = 0
        while (driver.pendingDecision == null && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(p1, false).error shouldBe null
        guard = 0
        while (driver.state.stack.isNotEmpty() && driver.pendingDecision == null && guard++ < 10) driver.bothPass()

        oil(driver, centurion) shouldBe 2
        driver.state.projectedState.cantBlock(blocker) shouldBe false
    }
})
