package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.MysteriousStranger
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.*
import io.kotest.matchers.shouldBe

class MysteriousStrangerScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + MysteriousStranger)
        initMultiplayer(List(4) { Deck.of("Mountain" to 40) })
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun enter(d: GameTestDriver): EntityId {
        val id = d.putCardInHand(d.player1, MysteriousStranger.name)
        d.giveMana(d.player1, Color.RED, 4)
        d.castSpell(d.player1, id).error.shouldBeNull()
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 40)
            d.passPriority(d.state.priorityPlayerId!!).error.shouldBeNull()
        return id
    }
    fun resolve(d: GameTestDriver) {
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 40)
            d.passPriority(d.state.priorityPlayerId!!).error.shouldBeNull()
    }
    fun choose(d: GameTestDriver, ids: List<EntityId>): ExecutionResult {
        val dec = d.pendingDecision as ChooseTargetsDecision
        return d.submitDecision(dec.playerId, TargetsResponse(dec.id, mapOf(0 to ids)))
    }
    test("one card from each eligible graveyard is mandatory and duplicate-owner choices fail") {
        val d = setup()
        val a = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val b = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val c = d.putCardInGraveyard(d.player2, "Lightning Bolt")
        d.putCardInGraveyard(d.state.turnOrder[2], "Grizzly Bears")
        enter(d)
        val dec = d.pendingDecision as ChooseTargetsDecision
        dec.targetRequirements.single().minTargets shouldBe 2
        dec.targetRequirements.single().exclusiveGroups shouldBe listOf(listOf(a, b), listOf(c))
        choose(d, listOf(a)).error.shouldNotBeNull()
        choose(d, listOf(a, b)).error.shouldNotBeNull()
        choose(d, listOf(a, c)).error.shouldBeNull()
        resolve(d)
        (a in d.state.getZone(d.player1, Zone.EXILE)) shouldBe true
        (c in d.state.getZone(d.player2, Zone.EXILE)) shouldBe true
        (b in d.state.getZone(d.player1, Zone.GRAVEYARD)) shouldBe true
        (d.pendingDecision is YesNoDecision) shouldBe true
        d.submitYesNo(d.player1, false).error.shouldBeNull()
        d.state.stack.size shouldBe 0
    }
    test("one eligible graveyard exiles one card without a copy offer") {
        val d = setup()
        val a = d.putCardInGraveyard(d.player2, "Lightning Bolt")
        enter(d)
        choose(d, listOf(a)).error.shouldBeNull()
        resolve(d)
        (a in d.state.getZone(d.player2, Zone.EXILE)) shouldBe true
        d.pendingDecision.shouldBeNull()
        d.state.stack.size shouldBe 0
    }
    test("no eligible graveyard resolves without a target prompt") {
        val d = setup()
        d.putCardInGraveyard(d.player2, "Grizzly Bears")
        enter(d)
        d.pendingDecision.shouldBeNull()
        d.state.stack.size shouldBe 0
    }
    test("a target that leaves before resolution does not count towards the copy threshold") {
        val d = setup()
        val a = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val b = d.putCardInGraveyard(d.player2, "Lightning Bolt")
        enter(d)
        choose(d, listOf(a, b)).error.shouldBeNull()
        d.replaceState(d.state.removeEntity(a))
        resolve(d)
        (b in d.state.getZone(d.player2, Zone.EXILE)) shouldBe true
        d.pendingDecision.shouldBeNull()
        d.state.stack.size shouldBe 0
    }
    test("all targets leaving prevents the ability from resolving") {
        val d = setup()
        val a = d.putCardInGraveyard(d.player2, "Lightning Bolt")
        enter(d)
        choose(d, listOf(a)).error.shouldBeNull()
        d.replaceState(d.state.removeEntity(a))
        resolve(d)
        d.pendingDecision.shouldBeNull()
        d.state.stack.size shouldBe 0
    }
    test("four owners including the controller each supply a target") {
        val d = setup()
        val cards = d.state.turnOrder.map { d.putCardInGraveyard(it, "Lightning Bolt") }
        val stranger = enter(d)
        val decision = d.pendingDecision as ChooseTargetsDecision
        decision.targetRequirements.single().minTargets shouldBe 4
        choose(d, cards).error.shouldBeNull()
        d.replaceState(d.state.removeEntity(stranger))
        // A fifth eligible card entering after targeting cannot change the locked target count.
        val later = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        resolve(d)
        cards.zip(d.state.turnOrder).forEach { (id, owner) -> (id in d.state.getZone(owner, Zone.EXILE)) shouldBe true }
        (later in d.state.getZone(d.player1, Zone.GRAVEYARD)) shouldBe true
        d.submitYesNo(d.player1, false).error.shouldBeNull()
    }
    test("the optional random copy can be cast with no mana and uses ordinary spell targeting") {
        val d = setup()
        val a = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val b = d.putCardInGraveyard(d.player2, "Lightning Bolt")
        enter(d)
        choose(d, listOf(a, b)).error.shouldBeNull()
        resolve(d)
        d.submitYesNo(d.player1, true).error.shouldBeNull()
        val targetChoice = d.pendingDecision as ChooseTargetsDecision
        d.submitDecision(targetChoice.playerId, TargetsResponse(targetChoice.id, mapOf(0 to listOf(d.player2)))).error.shouldBeNull()
        resolve(d)
        d.getLifeTotal(d.player2) shouldBe 17
        (a in d.state.getZone(d.player1, Zone.EXILE)) shouldBe true
        (b in d.state.getZone(d.player2, Zone.EXILE)) shouldBe true
    }

})
