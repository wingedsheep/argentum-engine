package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c15.cards.Gigantoplasm
import com.wingedsheep.mtg.sets.definitions.lea.cards.Clone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gigantoplasm (C15 #11) — "You may have this creature enter as a copy of any creature on the
 * battlefield, except it has '{X}: This creature has base power and toughness X/X.'"
 */
class GigantoplasmScenarioTest : FunSpec({
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(Gigantoplasm, Clone))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.copy(target: EntityId?, name: String = "Gigantoplasm"): EntityId {
        val id = putCardInHand(player1, name)
        giveMana(player1, Color.BLUE, 4)
        castSpell(player1, id).error shouldBe null
        bothPass()
        state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitCardSelection(player1, listOfNotNull(target)).error shouldBe null
        return id
    }

    fun GameTestDriver.xAbility(id: EntityId): AbilityId =
        state.getEntity(id)!!.get<CardComponent>()!!.copyActivatedAbilities.single().id

    fun GameTestDriver.activateX(id: EntityId, x: Int) {
        giveMana(player1, Color.BLUE, x)
        submit(ActivateAbility(playerId = player1, sourceId = id, abilityId = xAbility(id), xValue = x)).error shouldBe null
        bothPass()
    }

    test("copies a creature and gains an offered {X} ability that sets base P/T to X/X") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.copy(bear)
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe "Grizzly Bears"
        d.state.projectedState.getPower(id) shouldBe 2

        val ability = d.xAbility(id)
        d.legalActions(d.player1).count { (it.action as? ActivateAbility)?.abilityId == ability } shouldBe 1

        d.activateX(id, 5)
        d.state.projectedState.getPower(id) shouldBe 5
        d.state.projectedState.getToughness(id) shouldBe 5
    }

    test("a later activation overwrites the earlier one, and counters still apply on top") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.copy(bear)
        d.activateX(id, 6)
        d.replaceState(d.state.updateEntity(id) {
            it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        })
        d.activateX(id, 1)
        d.state.projectedState.getPower(id) shouldBe 2
        d.state.projectedState.getToughness(id) shouldBe 2
    }

    test("a Clone of Gigantoplasm copies the ability but not the X/X it set") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.copy(bear)
        d.activateX(id, 4)
        val clone = d.copy(id, "Clone")

        d.state.getEntity(clone)!!.get<CardComponent>()!!.copyActivatedAbilities shouldHaveSize 1
        d.state.projectedState.getPower(clone) shouldBe 2
        d.activateX(clone, 3)
        d.state.projectedState.getPower(clone) shouldBe 3
        d.state.projectedState.getPower(id) shouldBe 4
    }

    test("declining the copy gives no ability and the 0/0 dies") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.copy(null)
        d.getGraveyardCardNames(d.player1).contains("Gigantoplasm") shouldBe true
        d.state.getEntity(id)!!.get<CardComponent>()!!.copyActivatedAbilities.shouldBeEmpty()
    }

    test("leaving the battlefield restores the printed card without the ability") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.copy(bear)
        d.replaceState(d.zones.moveToZone(d.state, id, Zone.HAND).state)
        val card = d.state.getEntity(id)!!.get<CardComponent>()!!
        card.name shouldBe "Gigantoplasm"
        card.copyActivatedAbilities.shouldBeEmpty()
    }
})
