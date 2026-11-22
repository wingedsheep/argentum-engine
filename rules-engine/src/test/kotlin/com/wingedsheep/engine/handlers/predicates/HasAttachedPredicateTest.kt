package com.wingedsheep.engine.handlers.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.HasAttached` — "with <filter> attached": the candidate is the host and the nested
 * filter runs against each attachment. Chained with `attachedToBySource()` it names the host of the
 * source's own host (Artificer's Hex: Aura → Equipment → creature).
 */
class HasAttachedPredicateTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val you = EntityId.generate()
    val opponent = EntityId.generate()

    fun card(owner: EntityId, types: Set<CardType>, subtypes: Set<Subtype> = emptySet()) = CardComponent(
        cardDefinitionId = "Test",
        name = "Test",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = types, subtypes = subtypes),
        ownerId = owner,
        baseStats = if (CardType.CREATURE in types) CreatureStats(2, 2) else null,
    )

    fun container(controller: EntityId, card: CardComponent, vararg extras: Component): ComponentContainer {
        var c = ComponentContainer().with(card).with(OwnerComponent(controller)).with(ControllerComponent(controller))
        extras.forEach { c = c.copy(components = c.components + (it::class.java to it)) }
        return c
    }

    val creature = EntityId.generate()
    val bareCreature = EntityId.generate()
    val equipment = EntityId.generate()
    val hex = EntityId.generate()

    fun state(): GameState {
        var s = GameState().withEntity(you, ComponentContainer()).withEntity(opponent, ComponentContainer())
        listOf(
            creature to container(opponent, card(opponent, setOf(CardType.CREATURE)), AttachmentsComponent(listOf(equipment))),
            bareCreature to container(opponent, card(opponent, setOf(CardType.CREATURE))),
            equipment to container(
                opponent, card(opponent, setOf(CardType.ARTIFACT), setOf(Subtype.EQUIPMENT)),
                AttachedToComponent(creature), AttachmentsComponent(listOf(hex)),
            ),
            hex to container(you, card(you, setOf(CardType.ENCHANTMENT), setOf(Subtype.AURA)), AttachedToComponent(equipment)),
        ).forEach { (id, c) ->
            s = s.withEntity(id, c).addToZone(ZoneKey(c.get<ControllerComponent>()!!.playerId, Zone.BATTLEFIELD), id)
        }
        return s
    }

    test("matches a host whose attachment satisfies the nested filter") {
        val s = state()
        val ctx = PredicateContext(controllerId = you)
        val equipped = GameObjectFilter.Creature.hasAttached(GameObjectFilter.Artifact.withSubtype("Equipment"))
        evaluator.matches(s, s.projectedState, creature, equipped, ctx) shouldBe true
        evaluator.matches(s, s.projectedState, bareCreature, equipped, ctx) shouldBe false
    }

    test("the nested filter is evaluated per attachment, controller relative to the context") {
        val s = state()
        val ctx = PredicateContext(controllerId = you)
        evaluator.matches(s, s.projectedState, creature, GameObjectFilter.Creature.hasAttached(GameObjectFilter.Any.youControl()), ctx) shouldBe false
        evaluator.matches(s, s.projectedState, creature, GameObjectFilter.Creature.hasAttached(GameObjectFilter.Any.opponentControls()), ctx) shouldBe true
    }

    test("chains through two attachments from the source: Aura -> Equipment -> creature") {
        val s = state()
        val chained = GameObjectFilter.Creature.hasAttached(GameObjectFilter.Any.attachedToBySource())
        val fromHex = PredicateContext(controllerId = you, sourceId = hex)
        evaluator.matches(s, s.projectedState, creature, chained, fromHex) shouldBe true
        evaluator.matches(s, s.projectedState, bareCreature, chained, fromHex) shouldBe false
        // From the Equipment's point of view its own host carries no attachment attached to *it*.
        evaluator.matches(s, s.projectedState, creature, chained, PredicateContext(controllerId = you, sourceId = equipment)) shouldBe false
    }
})
