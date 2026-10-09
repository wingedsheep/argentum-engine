package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.dmu.cards.ValiantVeteran
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ValiantVeteranScenarioTest : FunSpec({
    val soldier = card("Veteran Test Soldier") {
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
    }
    val bear = card("Veteran Test Bear") {
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    fun driver() = GameTestDriver().apply {
        registerCards(listOf(ValiantVeteran, DominariaUnitedForest274, soldier, bear))
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(player1, Color.WHITE, 5)
    }
    fun activate(d: GameTestDriver, source: EntityId) = d.submit(
        ActivateAbility(
            d.player1, source, ValiantVeteran.activatedAbilities.single().id,
            paymentStrategy = PaymentStrategy.FromPool,
        )
    )
    fun counters(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("lord excludes itself, opponents and non-Soldiers; multiple Veterans stack") {
        val d = driver()
        val first = d.putCreatureOnBattlefield(d.player1, ValiantVeteran.name)
        val ally = d.putCreatureOnBattlefield(d.player1, soldier.name)
        val enemy = d.putCreatureOnBattlefield(d.player2, soldier.name)
        val other = d.putCreatureOnBattlefield(d.player1, bear.name)
        d.state.projectedState.getPower(first) shouldBe 2
        d.state.projectedState.getPower(ally) shouldBe 2
        d.state.projectedState.getToughness(ally) shouldBe 2
        d.state.projectedState.getPower(enemy) shouldBe 1
        d.state.projectedState.getPower(other) shouldBe 2
        val second = d.putCreatureOnBattlefield(d.player1, ValiantVeteran.name)
        d.state.projectedState.getPower(first) shouldBe 3
        d.state.projectedState.getPower(second) shouldBe 3
        d.state.projectedState.getPower(ally) shouldBe 3
        d.moveToGraveyard(first)
        d.state.projectedState.getPower(second) shouldBe 2
        d.state.projectedState.getPower(ally) shouldBe 2
    }

    test("graveyard activation exiles as cost and counts Soldiers at resolution") {
        val d = driver()
        val source = d.putCardInGraveyard(d.player1, ValiantVeteran.name)
        val departed = d.putCreatureOnBattlefield(d.player1, soldier.name)
        val veteran = d.putCreatureOnBattlefield(d.player1, ValiantVeteran.name)
        val ally = d.putCreatureOnBattlefield(d.player1, soldier.name)
        val enemy = d.putCreatureOnBattlefield(d.player2, soldier.name)
        val other = d.putCreatureOnBattlefield(d.player1, bear.name)
        activate(d, source).outcome shouldBe Outcome.Done
        (source in d.state.getZone(ZoneKey(d.player1, Zone.EXILE))) shouldBe true
        (source in d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe false
        d.stackSize shouldBe 1
        counters(d, ally) shouldBe 0
        d.moveToGraveyard(departed)
        val arrival = d.putCreatureOnBattlefield(d.player1, soldier.name)
        d.bothPass()
        d.stackSize shouldBe 0
        for (id in listOf(veteran, ally, arrival)) counters(d, id) shouldBe 1
        for (id in listOf(source, departed, enemy, other)) counters(d, id) shouldBe 0
        d.state.projectedState.getPower(veteran) shouldBe 3
        d.state.projectedState.getPower(ally) shouldBe 3
    }

    test("graveyard ability can resolve with no Soldiers and cannot activate from battlefield") {
        val d = driver()
        val battlefield = d.putCreatureOnBattlefield(d.player1, ValiantVeteran.name)
        activate(d, battlefield).error shouldNotBe null
        d.moveToGraveyard(battlefield)
        activate(d, battlefield).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 0
        (battlefield in d.state.getZone(ZoneKey(d.player1, Zone.EXILE))) shouldBe true
        d.giveMana(d.player1, Color.WHITE, 5)
        activate(d, battlefield).error shouldNotBe null
    }
})
