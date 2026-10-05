package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PhantasmalForcesScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.myUpkeep() {
        passPriorityUntil(Step.UPKEEP)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        passPriorityUntil(Step.UPKEEP)
    }
    test("opponent upkeep does not demand payment and controller can pay to keep it") {
        val d = driver(); val me = d.activePlayer!!
        val forces = d.putCreatureOnBattlefield(me, "Phantasmal Forces")
        d.putPermanentOnBattlefield(me, "Island")
        d.passPriorityUntil(Step.UPKEEP)
        d.state.stack.size shouldBe 0
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.bothPass()
        d.submitYesNo(me, true).error shouldBe null
        d.submitManaAutoPayOrDecline(me, true).error shouldBe null
        d.state.getBattlefield().contains(forces) shouldBe true
    }
    test("declining the upkeep payment sacrifices it") {
        val d = driver(); val me = d.activePlayer!!
        val forces = d.putCreatureOnBattlefield(me, "Phantasmal Forces")
        d.putPermanentOnBattlefield(me, "Island")
        d.myUpkeep(); d.bothPass()
        d.submitYesNo(me, false).error shouldBe null
        d.getGraveyard(me).contains(forces) shouldBe true
    }
    test("without blue mana the upkeep automatically sacrifices it") {
        val d = driver(); val me = d.activePlayer!!
        val forces = d.putCreatureOnBattlefield(me, "Phantasmal Forces")
        d.myUpkeep(); d.bothPass()
        d.pendingDecision shouldBe null
        d.getGraveyard(me).contains(forces) shouldBe true
    }
})
