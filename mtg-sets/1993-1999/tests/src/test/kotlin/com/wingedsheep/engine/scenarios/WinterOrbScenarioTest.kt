package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WinterOrbScenarioTest : FunSpec({
    val untap = card("Orb Test Untap") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val land = target(TargetFilter.Land); effect = Effects.Untap(land) }
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + untap)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("opponent chooses one land to untap while creatures untap normally") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Winter Orb")
        val lands = List(3) { d.putPermanentOnBattlefield(opponent, "Forest").also(d::tapPermanent) }
        val bear = d.putCreatureOnBattlefield(opponent, "Grizzly Bears").also(d::tapPermanent)
        d.passPriorityUntil(Step.UNTAP)
        val decision = d.pendingDecision as SelectCardsDecision
        decision.playerId shouldBe opponent
        decision.minSelections shouldBe 2
        decision.options.toSet() shouldBe lands.toSet()
        d.submitCardSelection(opponent, lands.take(2)).error shouldBe null
        lands.take(2).forEach { d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
        d.state.getEntity(lands.last())!!.has<TappedComponent>() shouldBe false
        d.state.getEntity(bear)!!.has<TappedComponent>() shouldBe false
    }

    test("a tapped Orb untaps with its controller's lands without restricting that untap") {
        val d = driver()
        val me = d.activePlayer!!
        val orb = d.putPermanentOnBattlefield(me, "Winter Orb").also(d::tapPermanent)
        val lands = List(3) { d.putPermanentOnBattlefield(me, "Forest").also(d::tapPermanent) }
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe me
        d.pendingDecision shouldBe null
        (lands + orb).forEach { d.state.getEntity(it)!!.has<TappedComponent>() shouldBe false }
    }

    test("untapped Orb restricts its controller and permits keeping every land tapped") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Winter Orb")
        val lands = List(2) { d.putPermanentOnBattlefield(me, "Forest").also(d::tapPermanent) }
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UNTAP)
        d.submitCardSelection(me, lands).error shouldBe null
        lands.forEach { d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
    }

    test("two Orbs do not reduce the one-land allowance") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        repeat(2) { d.putPermanentOnBattlefield(me, "Winter Orb") }
        val lands = List(3) { d.putPermanentOnBattlefield(opponent, "Forest").also(d::tapPermanent) }
        d.passPriorityUntil(Step.UNTAP)
        (d.pendingDecision as SelectCardsDecision).minSelections shouldBe 2
        d.submitCardSelection(opponent, lands.take(2)).error shouldBe null
        d.state.getEntity(lands.last())!!.has<TappedComponent>() shouldBe false
    }

    test("a single tapped land does not require a choice") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Winter Orb")
        val land = d.putPermanentOnBattlefield(d.getOpponent(me), "Forest").also(d::tapPermanent)
        d.passPriorityUntil(Step.UPKEEP)
        d.pendingDecision shouldBe null
        d.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
    }

    test("an effect untaps a land outside the untap step despite Winter Orb") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Winter Orb")
        val land = d.putPermanentOnBattlefield(me, "Forest").also(d::tapPermanent)
        d.putPermanentOnBattlefield(me, "Island")
        val twiddle = d.putCardInHand(me, untap.name)
        d.castSpell(me, twiddle, listOf(land)).error shouldBe null
        d.bothPass()
        d.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
    }
})
