package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.ThrivingSkyclaw
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Thriving Skyclaw (MH3) — flying; ETB {E}{E}{E}; on attack may pay {E}{E}{E} to put a +1/+1
 * counter on it.
 */
class ThrivingSkyclawScenarioTest : FunSpec({

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(ThrivingSkyclaw))
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.seedEnergy(amount: Int) {
        replaceState(
            state.updateEntity(player1) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.ENERGY, amount))
            }
        )
    }

    fun GameTestDriver.attackWith(skyclaw: EntityId) {
        removeSummoningSickness(skyclaw)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(skyclaw), player2).error shouldBe null
    }

    test("entering gives three energy") {
        val d = driver()
        val spell = d.putCardInHand(d.player1, ThrivingSkyclaw.name)
        d.giveMana(d.player1, Color.RED, 2)
        d.giveColorlessMana(d.player1, 2)
        d.castSpell(d.player1, spell).error shouldBe null
        d.bothPass() // resolve the creature spell
        d.bothPass() // resolve the enters trigger
        d.energy() shouldBe 3
    }

    test("paying three energy on attack puts a +1/+1 counter on it") {
        val d = driver()
        val skyclaw = d.putCreatureOnBattlefield(d.player1, ThrivingSkyclaw.name)
        d.seedEnergy(4)
        d.attackWith(skyclaw)
        d.bothPass() // resolve the attack trigger
        d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.energy() shouldBe 1
        d.plusOnes(skyclaw) shouldBe 1

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(d.player2) shouldBe 16
    }

    test("declining the payment keeps the energy and adds no counter") {
        val d = driver()
        val skyclaw = d.putCreatureOnBattlefield(d.player1, ThrivingSkyclaw.name)
        d.seedEnergy(3)
        d.attackWith(skyclaw)
        d.bothPass()
        d.submitYesNo(d.player1, false).error shouldBe null
        d.energy() shouldBe 3
        d.plusOnes(skyclaw) shouldBe 0
    }

    test("with fewer than three energy no payment is offered and no counter is added") {
        val d = driver()
        val skyclaw = d.putCreatureOnBattlefield(d.player1, ThrivingSkyclaw.name)
        d.seedEnergy(2)
        d.attackWith(skyclaw)
        d.bothPass()
        (d.state.pendingDecision is YesNoDecision) shouldBe false
        d.energy() shouldBe 2
        d.plusOnes(skyclaw) shouldBe 0
    }
})
