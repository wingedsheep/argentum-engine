package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.AssaultronDominator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Assaultron Dominator (PIP) — ETB {E}{E}; whenever an artifact creature you control attacks, you
 * may pay {E} and, if you do, choose a +1/+1, first strike, or trample counter for that creature.
 * Pins the may-pay gate feeding a resolution-time counter choice onto the triggering attacker.
 */
class AssaultronDominatorScenarioTest : FunSpec({

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(AssaultronDominator))
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun GameTestDriver.counters(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun GameTestDriver.seedEnergy(amount: Int) {
        replaceState(
            state.updateEntity(player1) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.ENERGY, amount))
            }
        )
    }

    test("entering gives two energy") {
        val d = driver()
        val spell = d.putCardInHand(d.player1, AssaultronDominator.name)
        d.giveMana(d.player1, Color.RED, 1)
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, spell).error shouldBe null
        d.bothPass() // resolve the creature spell
        d.bothPass() // resolve the enters trigger
        d.energy() shouldBe 2
    }

    test("paying {E} lets you choose a first strike counter for the attacking artifact creature") {
        val d = driver()
        val dominator = d.putCreatureOnBattlefield(d.player1, AssaultronDominator.name)
        d.removeSummoningSickness(dominator)
        d.seedEnergy(2)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(dominator), d.player2).error shouldBe null
        d.bothPass() // resolve the attack trigger

        d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.energy() shouldBe 1

        val choose = d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choose.options.size shouldBe 3
        d.submitDecision(d.player1, OptionChosenResponse(choose.id, 1)).error shouldBe null

        d.counters(dominator, CounterType.FIRST_STRIKE) shouldBe 1
        d.counters(dominator, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        d.state.projectedState.hasKeyword(dominator, Keyword.FIRST_STRIKE) shouldBe true
    }

    test("declining keeps the energy; a non-artifact attacker doesn't trigger") {
        val d = driver()
        val dominator = d.putCreatureOnBattlefield(d.player1, AssaultronDominator.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(dominator)
        d.removeSummoningSickness(bears)
        d.seedEnergy(2)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(dominator, bears), d.player2).error shouldBe null
        d.bothPass() // only the Dominator's own attack triggers

        d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, false).error shouldBe null
        d.energy() shouldBe 2
        (d.state.pendingDecision == null) shouldBe true
        d.counters(dominator, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        d.counters(bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
    }

    test("with no energy the payment isn't offered") {
        val d = driver()
        val dominator = d.putCreatureOnBattlefield(d.player1, AssaultronDominator.name)
        d.removeSummoningSickness(dominator)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(dominator), d.player2).error shouldBe null
        d.bothPass()
        (d.state.pendingDecision is YesNoDecision) shouldBe false
        d.counters(dominator, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
    }
})
