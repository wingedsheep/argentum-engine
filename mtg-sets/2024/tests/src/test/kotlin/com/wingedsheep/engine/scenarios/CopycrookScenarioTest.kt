package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.Copycrook
import com.wingedsheep.mtg.sets.definitions.lea.cards.Clone
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CopycrookScenarioTest : FunSpec({
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(Copycrook, Clone))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.copy(target: EntityId?, name: String = "Copycrook"): EntityId {
        val id = putCardInHand(player1, name)
        giveMana(player1, Color.BLUE, 4)
        castSpell(player1, id).error shouldBe null
        bothPass()
        state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitCardSelection(player1, listOfNotNull(target)).error shouldBe null
        return id
    }
    fun GameTestDriver.attack(id: EntityId) {
        removeSummoningSickness(id)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(id), player2).error shouldBe null
    }
    test("copies an opposing creature and connives on attacking with a nonland discard") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val copy = d.copy(bear)
        d.state.getEntity(copy)!!.get<CardComponent>()!!.name shouldBe "Grizzly Bears"
        val discard = d.putCardInHand(d.player1, "Hill Giant")
        val before = d.state.getHand(d.player1).size
        d.attack(copy)
        d.bothPass()
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.state.getHand(d.player1).size shouldBe before + 1
        d.submitCardSelection(d.player1, listOf(discard)).error shouldBe null
        d.state.getEntity(copy)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        d.state.getHand(d.player1).size shouldBe before
    }
    test("Clone inherits the connive trigger as a copiable value") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val copy = d.copy(bear)
        val clone = d.copy(copy, "Clone")
        d.state.getEntity(clone)!!.get<CardComponent>()!!.copyTriggeredAbilities.size shouldBe 1
        d.attack(clone)
        d.bothPass()
        val decision = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, listOf(decision.options.first())).error shouldBe null
        (d.state.getEntity(clone)!!.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }
    test("copying Copycrook adds a second independent connive instance") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val first = d.copy(bear)
        val second = d.copy(first)
        d.state.getEntity(second)!!.get<CardComponent>()!!.copyTriggeredAbilities.size shouldBe 2
        d.attack(second)
        // The trigger order may need a choice; both instances must reach the stack.
        if (d.state.pendingDecision != null) d.autoResolveDecision()
        d.state.stack.size shouldBe 2
    }
    test("declining the copy grants no trigger and the zero toughness creature dies") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.copy(null)
        d.getGraveyardCardNames(d.player1).contains("Copycrook") shouldBe true
        d.state.getEntity(id)!!.get<CardComponent>()!!.copyTriggeredAbilities shouldBe emptyList()
    }
    test("leaving the battlefield restores the printed identity and removes the exception") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.copy(bear)
        d.replaceState(d.zones.moveToZone(d.state, id, Zone.HAND).state)
        val card = d.state.getEntity(id)!!.get<CardComponent>()!!
        card.name shouldBe "Copycrook"
        card.copyTriggeredAbilities shouldBe emptyList()
    }
    test("the attack trigger still connives if the copy leaves before resolution") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.copy(bear)
        val discard = d.putCardInHand(d.player1, "Hill Giant")
        d.attack(id)
        d.replaceState(d.zones.moveToZone(d.state, id, Zone.GRAVEYARD).state)
        val before = d.state.getHand(d.player1).size
        d.bothPass()
        d.state.getHand(d.player1).size shouldBe before + 1
        d.submitCardSelection(d.player1, listOf(discard)).error shouldBe null
        d.state.getHand(d.player1).size shouldBe before
        d.state.getEntity(id)!!.get<CardComponent>()!!.copyTriggeredAbilities shouldBe emptyList()
        (d.state.getEntity(id)!!.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }

})
