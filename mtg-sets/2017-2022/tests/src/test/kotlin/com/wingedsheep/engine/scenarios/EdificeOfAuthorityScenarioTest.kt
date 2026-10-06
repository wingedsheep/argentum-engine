package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.EdificeOfAuthority
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class EdificeOfAuthorityScenarioTest : FunSpec({
    val creature = card("Edifice Test Creature") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.GainLife(1)
        }
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(1)
            manaAbility = true
        }
    }

    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(EdificeOfAuthority, creature))
        initMirrorMatch(Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.bricks(id: EntityId, count: Int) {
        replaceState(state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.BRICK to count))) })
    }
    fun GameTestDriver.count(id: EntityId) = state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.BRICK) ?: 0
    fun activation(player: EntityId, source: EntityId, target: EntityId, index: Int) = ActivateAbility(
        playerId = player, sourceId = source,
        abilityId = EdificeOfAuthority.activatedAbilities[index].id,
        targets = listOf(ChosenTarget.Permanent(target))
    )

    test("first ability prevents attacking only and adds one brick on resolution") {
        val d = setup()
        val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, "Edifice of Authority")
        val victim = d.putCreatureOnBattlefield(me, creature.name)
        d.giveColorlessMana(me, 1)
        d.submitSuccess(activation(me, source, victim, 0))
        d.count(source) shouldBe 0
        d.bothPass()
        d.count(source) shouldBe 1
        d.state.projectedState.cantAttack(victim) shouldBe true
        d.state.projectedState.cantBlock(victim) shouldBe false
        d.legalActions(me).any { (it.action as? ActivateAbility)?.sourceId == victim } shouldBe true
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.cantAttack(victim) shouldBe false
        d.count(source) shouldBe 1
    }

    test("an illegal target fizzles the whole first ability including its brick") {
        val d = setup()
        val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, "Edifice of Authority")
        val victim = d.putCreatureOnBattlefield(me, creature.name)
        d.giveColorlessMana(me, 1)
        d.submitSuccess(activation(me, source, victim, 0))
        d.moveToGraveyard(victim)
        d.bothPass()
        d.count(source) shouldBe 0
    }

    test("second ability is unavailable below three bricks and does not spend them") {
        val d = setup()
        val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, "Edifice of Authority")
        val victim = d.putCreatureOnBattlefield(me, creature.name)
        d.giveColorlessMana(me, 2)
        d.bricks(source, 2)
        val action = activation(me, source, victim, 1)
        d.legalActions(me).any { (it.action as? ActivateAbility)?.abilityId == action.abilityId } shouldBe false
        d.submit(action).error.shouldNotBeNull()
        d.bricks(source, 3)
        d.legalActions(me).any { (it.action as? ActivateAbility)?.abilityId == action.abilityId } shouldBe true
        d.submitSuccess(action)
        d.bothPass()
        d.count(source) shouldBe 3
    }

    test("second ability locks only its target including mana abilities until your next turn after source leaves") {
        val d = setup()
        val me = d.activePlayer!!
        val other = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(me, "Edifice of Authority")
        val victim = d.putCreatureOnBattlefield(other, creature.name)
        val untouched = d.putCreatureOnBattlefield(other, creature.name)
        d.removeSummoningSickness(victim)
        d.removeSummoningSickness(untouched)
        d.bricks(source, 3)
        d.giveColorlessMana(me, 1)
        d.submitSuccess(activation(me, source, victim, 1))
        d.bothPass()
        d.moveToGraveyard(source)
        d.state.projectedState.cantAttack(victim) shouldBe true
        d.state.projectedState.cantBlock(victim) shouldBe true
        d.state.projectedState.cantAttack(untouched) shouldBe false
        d.passPriority(me)
        d.legalActions(other).any { (it.action as? ActivateAbility)?.sourceId == victim } shouldBe false
        d.legalActions(other).any { (it.action as? ActivateAbility)?.sourceId == untouched } shouldBe true
        for (ability in creature.activatedAbilities) {
            d.submit(ActivateAbility(other, victim, ability.id)).error.shouldNotBeNull()
        }
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe other
        d.state.projectedState.cantAttack(victim) shouldBe true
        d.state.projectedState.cantBlock(victim) shouldBe true
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe me
        d.state.projectedState.cantAttack(victim) shouldBe false
        d.state.projectedState.cantBlock(victim) shouldBe false
        d.passPriority(me)
        d.legalActions(other).count { (it.action as? ActivateAbility)?.sourceId == victim } shouldBe 2
    }

    test("a creature can respond to the lock with its activated ability") {
        val d = setup()
        val me = d.activePlayer!!
        val other = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(me, "Edifice of Authority")
        val victim = d.putCreatureOnBattlefield(other, creature.name)
        d.bricks(source, 3)
        d.giveColorlessMana(me, 1)
        d.submitSuccess(activation(me, source, victim, 1))
        d.passPriority(me)
        d.submitSuccess(ActivateAbility(other, victim, creature.activatedAbilities[0].id))
        d.bothPass()
        d.getLifeTotal(other) shouldBe 21
        d.bothPass()
        d.state.projectedState.cantAttack(victim) shouldBe true
        d.getLifeTotal(other) shouldBe 21
    }

    test("brick threshold is checked on activation rather than resolution") {
        val d = setup()
        val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, "Edifice of Authority")
        val victim = d.putCreatureOnBattlefield(me, creature.name)
        d.bricks(source, 3)
        d.giveColorlessMana(me, 1)
        d.submitSuccess(activation(me, source, victim, 1))
        d.bricks(source, 0)
        d.bothPass()
        d.state.projectedState.cantAttack(victim) shouldBe true
        d.state.projectedState.cantBlock(victim) shouldBe true
    }
})
