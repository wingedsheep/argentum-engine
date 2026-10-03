package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.SmeltedChargebug
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SmeltedChargebugScenarioTest : FunSpec({

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(SmeltedChargebug))
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.energy() =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun GameTestDriver.drainEnergy() {
        replaceState(state.updateEntity(player1) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withRemoved(CounterType.ENERGY, energy()))
        })
    }

    /** Cast Smelted Chargebug, resolve it and its enters trigger, and make it able to attack. */
    fun GameTestDriver.castChargebug(): EntityId {
        val spell = putCardInHand(player1, SmeltedChargebug.name)
        giveMana(player1, Color.RED, 2)
        castSpell(player1, spell).error shouldBe null
        bothPass() // resolve the creature spell
        bothPass() // resolve the enters trigger
        val bug = findPermanent(player1, SmeltedChargebug.name)!!
        removeSummoningSickness(bug)
        return bug
    }

    /** Attack with [attackers]; returns the attack trigger's target decision. */
    fun GameTestDriver.attackWith(attackers: List<EntityId>): ChooseTargetsDecision {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, attackers, player2).error shouldBe null
        return pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
    }

    test("has menace and enters giving two energy") {
        val d = driver()
        val bug = d.castChargebug()
        d.energy() shouldBe 2
        d.state.projectedState.hasKeyword(bug, Keyword.MENACE) shouldBe true
    }

    test("paying {E} gives another attacking creature +1/+0 and menace until end of turn") {
        val d = driver()
        val bug = d.castChargebug()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(bear)
        val decision = d.attackWith(listOf(bug, bear))
        (bug in decision.legalTargets.getValue(0)) shouldBe false
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.energy() shouldBe 1
        val projected = d.state.projectedState
        projected.getPower(bear) shouldBe 3
        projected.getToughness(bear) shouldBe 2
        projected.hasKeyword(bear, Keyword.MENACE) shouldBe true
        projected.getPower(bug) shouldBe 1
    }

    test("a non-attacking creature can't be targeted") {
        val d = driver()
        val bug = d.castChargebug()
        val attacker = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(attacker)
        val stayHome = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val decision = d.attackWith(listOf(bug, attacker))
        decision.legalTargets.getValue(0).toSet() shouldBe setOf(attacker)
        (stayHome in decision.legalTargets.getValue(0)) shouldBe false
    }

    test("declining the payment keeps the energy and gives nothing") {
        val d = driver()
        val bug = d.castChargebug()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(bear)
        d.attackWith(listOf(bug, bear))
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null
        d.bothPass()
        d.submitYesNo(d.player1, false).error shouldBe null
        d.energy() shouldBe 2
        d.state.projectedState.getPower(bear) shouldBe 2
        d.state.projectedState.hasKeyword(bear, Keyword.MENACE) shouldBe false
    }

    test("with no energy there is no payment prompt and no pump") {
        val d = driver()
        val bug = d.castChargebug()
        d.drainEnergy()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(bear)
        d.attackWith(listOf(bug, bear))
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null
        d.bothPass()
        (d.pendingDecision is YesNoDecision) shouldBe false
        d.state.projectedState.getPower(bear) shouldBe 2
        d.state.projectedState.hasKeyword(bear, Keyword.MENACE) shouldBe false
    }
})
