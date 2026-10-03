package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.HexgoldSlith
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
 * Hexgold Slith (MH3) — ETB {E}{E}; on attack may pay {E}{E} for first strike until end of turn;
 * combat damage to a player puts a +1/+1 counter on it.
 */
class HexgoldSlithScenarioTest : FunSpec({

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(HexgoldSlith))
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
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

    fun GameTestDriver.attackWith(slith: EntityId) {
        removeSummoningSickness(slith)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(slith), player2).error shouldBe null
    }

    test("entering gives two energy") {
        val d = driver()
        val spell = d.putCardInHand(d.player1, HexgoldSlith.name)
        d.giveMana(d.player1, Color.WHITE, 1)
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, spell).error shouldBe null
        d.bothPass() // resolve the creature spell
        d.bothPass() // resolve the enters trigger
        d.energy() shouldBe 2
    }

    test("paying two energy on attack grants first strike; combat damage to the player adds a +1/+1 counter") {
        val d = driver()
        val slith = d.putCreatureOnBattlefield(d.player1, HexgoldSlith.name)
        d.seedEnergy(3)
        d.attackWith(slith)
        d.bothPass() // resolve the attack trigger
        d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.energy() shouldBe 1
        d.state.projectedState.hasKeyword(slith, Keyword.FIRST_STRIKE) shouldBe true

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(d.player2) shouldBe 18
        d.plusOnes(slith) shouldBe 1
    }

    test("declining the payment keeps the energy and grants no first strike") {
        val d = driver()
        val slith = d.putCreatureOnBattlefield(d.player1, HexgoldSlith.name)
        d.seedEnergy(2)
        d.attackWith(slith)
        d.bothPass()
        d.submitYesNo(d.player1, false).error shouldBe null
        d.energy() shouldBe 2
        d.state.projectedState.hasKeyword(slith, Keyword.FIRST_STRIKE) shouldBe false
    }

    test("with fewer than two energy there is no payment offered and no first strike") {
        val d = driver()
        val slith = d.putCreatureOnBattlefield(d.player1, HexgoldSlith.name)
        d.seedEnergy(1)
        d.attackWith(slith)
        d.bothPass()
        (d.state.pendingDecision is YesNoDecision) shouldBe false
        d.energy() shouldBe 1
        d.state.projectedState.hasKeyword(slith, Keyword.FIRST_STRIKE) shouldBe false
    }

    test("combat damage dealt to a blocking creature puts no counter on it") {
        val d = driver()
        val slith = d.putCreatureOnBattlefield(d.player1, HexgoldSlith.name)
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.seedEnergy(2)
        d.attackWith(slith)
        d.bothPass()
        // First strike lets the 2/1 kill the 2/2 blocker and survive to be inspected.
        d.submitYesNo(d.player1, true).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player2, mapOf(bears to listOf(slith))).error shouldBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(d.player2) shouldBe 20
        d.state.getBattlefield().contains(bears) shouldBe false
        d.state.getBattlefield().contains(slith) shouldBe true
        d.plusOnes(slith) shouldBe 0
    }
})
