package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.cards.ElbrusTheBindingBlade
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Elbrus, the Binding Blade // Withengar Unbound (DKA #147)
 *
 * Proves the Equipment-to-creature transform: the ATTACHED-bound combat-damage trigger unattaches
 * Elbrus and then transforms it into a 13/13 creature on the battlefield, and Withengar's
 * "whenever a player loses the game" fires on a concession in a three-player pod.
 */
class ElbrusTheBindingBladeScenarioTest : FunSpec({

    fun GameTestDriver.attach(equipment: EntityId, creature: EntityId) {
        replaceState(
            state
                .updateEntity(equipment) { it.with(AttachedToComponent(creature)) }
                .updateEntity(creature) { it.with(AttachmentsComponent(listOf(equipment))) }
        )
    }

    fun GameTestDriver.faceName(id: EntityId): String = state.getEntity(id)!!.get<CardComponent>()!!.name

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (guard++ < 30 && stackSize > 0) passPriority(priorityPlayer!!)
    }

    fun setup(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ElbrusTheBindingBlade)
        val players = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        return d to players
    }

    /** Equip Elbrus to a Centaur Courser, attack p2 unblocked, and stop in p1's postcombat main. */
    fun GameTestDriver.connectWithElbrus(p1: EntityId, p2: EntityId): Pair<EntityId, EntityId> {
        passPriorityUntil(Step.PRECOMBAT_MAIN, p1)
        val elbrus = putPermanentOnBattlefield(p1, "Elbrus, the Binding Blade")
        val courser = putCreatureOnBattlefield(p1, "Centaur Courser").also { removeSummoningSickness(it) }
        attach(elbrus, courser)
        withClue("equipped creature gets +1/+0") {
            state.projectedState.getPower(courser) shouldBe 4
            state.projectedState.getToughness(courser) shouldBe 3
        }

        passPriorityUntil(Step.DECLARE_ATTACKERS, p1)
        declareAttackers(p1, listOf(courser), p2).error shouldBe null
        passPriorityUntil(Step.POSTCOMBAT_MAIN, p1)
        return elbrus to courser
    }

    test("combat damage to a player unattaches Elbrus and transforms it into Withengar Unbound") {
        val (d, p) = setup()
        val (p1, p2, _) = p
        val (elbrus, courser) = d.connectWithElbrus(p1, p2)

        d.getLifeTotal(p2) shouldBe 16
        withClue("transformed to the back face") {
            d.faceName(elbrus) shouldBe "Withengar Unbound"
            d.state.projectedState.isCreature(elbrus) shouldBe true
            d.state.projectedState.getPower(elbrus) shouldBe 13
            d.state.projectedState.getToughness(elbrus) shouldBe 13
            d.state.projectedState.hasKeyword(elbrus, Keyword.FLYING) shouldBe true
            d.state.projectedState.hasKeyword(elbrus, Keyword.INTIMIDATE) shouldBe true
            d.state.projectedState.hasKeyword(elbrus, Keyword.TRAMPLE) shouldBe true
        }
        withClue("Elbrus was unattached first") {
            d.state.getEntity(elbrus)!!.has<AttachedToComponent>() shouldBe false
            (d.state.getEntity(courser)!!.get<AttachmentsComponent>()?.attachedIds ?: emptyList()).contains(elbrus) shouldBe false
            d.state.projectedState.getPower(courser) shouldBe 3
        }
    }

    test("Withengar gets thirteen +1/+1 counters when a player loses the game") {
        val (d, p) = setup()
        val (p1, p2, p3) = p
        val (withengar, _) = d.connectWithElbrus(p1, p2)
        d.faceName(withengar) shouldBe "Withengar Unbound"

        d.concede(p3)
        d.resolveStack()

        d.state.gameOver shouldBe false
        d.state.getEntity(withengar)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 13
        d.state.projectedState.getPower(withengar) shouldBe 26
        d.state.projectedState.getToughness(withengar) shouldBe 26
    }
})
