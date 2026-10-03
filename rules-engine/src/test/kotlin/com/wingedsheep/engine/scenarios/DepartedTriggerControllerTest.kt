package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Authored departure abilities belong to the controller immediately before their source left. */
class DepartedTriggerControllerTest : FunSpec({
    val dies = card("Test Departed Self Dies") {
        typeLine = "Creature"
        power = 2
        toughness = 2
        triggeredAbility { trigger = Triggers.self.dies(); effect = Effects.GainLife(3) }
    }
    val leaves = card("Test Departed Self Leaves") {
        typeLine = "Creature"
        power = 2
        toughness = 2
        triggeredAbility { trigger = Triggers.self.leaves(); effect = Effects.GainLife(3) }
    }
    val observer = card("Test Departed Other Dies") {
        typeLine = "Creature"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
            effect = Effects.GainLife(3)
        }
    }
    val aura = card("Test Departed Aura") {
        typeLine = "Enchantment — Aura"
        triggeredAbility { trigger = Triggers.attached.dies(); effect = Effects.GainLife(3) }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(dies, leaves, observer, aura))
        it.initMirrorMatch(Deck.of("Mountain" to 40))
    }
    fun steal(d: GameTestDriver, state: GameState, id: EntityId) =
        d.services.effectExecutorRegistry.execute(state, Effects.GainControl(EffectTarget.SpecificEntity(id)),
            EffectContext(sourceId = id, controllerId = d.player2)).state
    fun event(state: GameState, id: EntityId, name: String, owner: EntityId, destination: Zone = Zone.GRAVEYARD) =
        ZoneChangeEvent(entityId = id, entityName = name, fromZone = Zone.BATTLEFIELD,
            toZone = destination, ownerId = owner, lastKnown = EntitySnapshot.fromProjection(id, state).copy(
                attachedTo = state.getEntity(id)?.get<AttachedToComponent>()?.targetId,
                cardDefinitionId = state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.cardDefinitionId,
            ))
    fun depart(state: GameState, id: EntityId, owner: EntityId, destination: Zone = Zone.GRAVEYARD) =
        state.moveToZone(id, state.logicalZone(id)!!, ZoneKey(owner, destination))
            .updateEntity(id) { it.without<AttachedToComponent>() }
    fun verify(d: GameTestDriver, state: GameState, events: List<ZoneChangeEvent>, source: EntityId) {
        val trigger = d.services.triggerDetector.detectTriggers(state, events).single { it.sourceId == source }
        trigger.controllerId shouldBe d.player2
        val resolved = d.services.effectExecutorRegistry.execute(state, trigger.ability.effect,
            EffectContext(sourceId = source, controllerId = trigger.controllerId, triggerContext = trigger.triggerContext,
                objectReferences = trigger.objectReferences))
        resolved.state.getEntity(d.player2)!!.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()
            ?.life shouldBe 23
        resolved.state.getEntity(d.player1)!!.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()
            ?.life shouldBe 20
    }
    test("stolen self-dies ability belongs to projected controller rather than owner") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, dies.name)
        val stolen = steal(d, d.state, id)
        val death = event(stolen, id, dies.name, d.player1)
        death.lastKnown!!.controllerId shouldBe d.player2
        verify(d, depart(stolen, id, d.player1), listOf(death), id)
    }
    test("legacy departure snapshots without controller retain owner fallback") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, dies.name)
        val stolen = steal(d, d.state, id)
        val death = event(stolen, id, dies.name, d.player1)
        val legacy = death.copy(lastKnown = death.lastKnown!!.copy(controllerId = null))
        val trigger = d.services.triggerDetector.detectTriggers(depart(stolen, id, d.player1), listOf(legacy)).single()
        trigger.controllerId shouldBe d.player1
    }
    test("stolen leaves-battlefield ability belongs to departed controller when exiled") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, leaves.name)
        val stolen = steal(d, d.state, id)
        val leave = event(stolen, id, leaves.name, d.player1, Zone.EXILE)
        verify(d, depart(stolen, id, d.player1, Zone.EXILE), listOf(leave), id)
    }
    test("simultaneously dead stolen observer matches creatures controlled by its last controller") {
        val d = driver()
        val watcher = d.putCreatureOnBattlefield(d.player1, observer.name)
        val victim = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val stolen = steal(d, d.state, watcher)
        val events = listOf(event(stolen, watcher, observer.name, d.player1), event(stolen, victim, "Centaur Courser", d.player2))
        val dead = depart(depart(stolen, watcher, d.player1), victim, d.player2)
        verify(d, dead, events, watcher)
    }
    test("dead attached stolen Aura pays its last controller when host and Aura leave together") {
        val d = driver()
        val host = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val enchanted = d.putPermanentOnBattlefield(d.player1, aura.name)
        d.addComponent(enchanted, AttachedToComponent(host))
        val stolen = steal(d, d.state, enchanted)
        val events = listOf(event(stolen, host, "Centaur Courser", d.player1), event(stolen, enchanted, aura.name, d.player1))
        events.last().lastKnown!!.attachedTo shouldBe host
        events.last().lastKnown!!.controllerId shouldBe d.player2
        val dead = depart(depart(stolen, host, d.player1), enchanted, d.player1)
        verify(d, dead, events, enchanted)
    }
})
