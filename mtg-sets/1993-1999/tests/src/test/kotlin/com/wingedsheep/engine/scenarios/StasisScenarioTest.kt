package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.player.SkipNextUntapStepComponent
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StasisScenarioTest : FunSpec({
    val untap = card("Stasis Test Untap") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.Untap(t) } }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + untap); initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.myUpkeep() {
        passPriorityUntil(Step.UPKEEP); passPriorityUntil(Step.PRECOMBAT_MAIN)
        passPriorityUntil(Step.UPKEEP)
    }
    test("both players skip all their untaps but controller upkeep still triggers") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Stasis")
        val ids = listOf(d.putPermanentOnBattlefield(me, "Island"),
            d.putPermanentOnBattlefield(other, "Forest"), d.putCreatureOnBattlefield(other, "Grizzly Bears"))
        ids.forEach(d::tapPermanent)
        d.putPermanentOnBattlefield(me, "Island")
        d.myUpkeep(); d.activePlayer shouldBe me
        ids.forEach { d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
        d.bothPass(); (d.pendingDecision as YesNoDecision).playerId shouldBe me
    }
    test("declining upkeep payment sacrifices Stasis and later untaps resume") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        val stasis = d.putPermanentOnBattlefield(me, "Stasis")
        d.putPermanentOnBattlefield(me, "Island")
        val land = d.putPermanentOnBattlefield(other, "Forest").also(d::tapPermanent)
        d.myUpkeep(); d.bothPass(); d.submitYesNo(me, false).error shouldBe null
        d.getGraveyard(me).contains(stasis) shouldBe true
        d.passPriorityUntil(Step.PRECOMBAT_MAIN); d.passPriorityUntil(Step.UPKEEP)
        d.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
    }
    test("paying with an untapped blue source keeps Stasis in play") {
        val d = driver(); val me = d.activePlayer!!
        val stasis = d.putPermanentOnBattlefield(me, "Stasis")
        val land = d.putPermanentOnBattlefield(me, "Island")
        d.myUpkeep(); d.bothPass(); d.submitYesNo(me, true).error shouldBe null
        d.submitManaAutoPayOrDecline(me, true).error shouldBe null
        d.state.getBattlefield().contains(stasis) shouldBe true
        d.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
    }
    test("tapped Stasis continues to skip untaps and phased-out creatures stay out") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Stasis").also(d::tapPermanent)
        val bear = d.putCreatureOnBattlefield(other, "Grizzly Bears").also(d::tapPermanent)
        d.replaceState(d.state.updateEntity(bear) { it.with(PhasedOutComponent(other)) })
        d.passPriorityUntil(Step.UPKEEP)
        d.state.getEntity(bear)!!.has<PhasedOutComponent>() shouldBe true
        d.state.getEntity(bear)!!.has<TappedComponent>() shouldBe true
    }
    test("untapping by an effect outside the step still works") {
        val d = driver(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Stasis")
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears").also(d::tapPermanent)
        d.castSpell(me, d.putCardInHand(me, untap.name), listOf(bear)).error shouldBe null
        d.bothPass(); d.state.getEntity(bear)!!.has<TappedComponent>() shouldBe false
    }
    test("unable to pay from tapped blue sources automatically sacrifices Stasis") {
        val d = driver(); val me = d.activePlayer!!
        val stasis = d.putPermanentOnBattlefield(me, "Stasis")
        val land = d.putPermanentOnBattlefield(me, "Island").also(d::tapPermanent)
        d.myUpkeep(); d.bothPass()
        d.pendingDecision shouldBe null
        d.getGraveyard(me).contains(stasis) shouldBe true
        d.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
    }

    test("a player can spend a pending next-untap skip instead of the standing Stasis skip") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Stasis")
        d.replaceState(d.state.updateEntity(other) { it.with(SkipNextUntapStepComponent()) })
        d.passPriorityUntil(Step.UNTAP)
        val question = d.pendingDecision as ChooseOptionDecision
        question.playerId shouldBe other
        d.submitDecision(other, OptionChosenResponse(question.id, 0)).error shouldBe null
        d.state.step shouldBe Step.UPKEEP
        d.state.getEntity(other)!!.has<SkipNextUntapStepComponent>() shouldBe false
    }

})
