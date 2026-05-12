package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * BDD test for the 'modified creature you control' predicate.
 *
 * GIVEN Player A controls:
 *   C1 — creature with an Equipment (controlled by A) attached
 *   C2 — creature with an Aura (controlled by A) attached
 *   C3 — creature with a +1/+1 counter
 *   C4 — vanilla creature with no attachments and no counters
 * AND Player A also controls an Aura attached to Player B's creature C5
 * WHEN the engine evaluates StatePredicate.IsModified with ControlledByYou (playerA) via matchesWithProjection
 * THEN C1, C2, C3 match (count == 3)
 * AND  C4 does not match (vanilla, unmodified)
 * AND  C5 does not match (controlled by playerB, not playerA)
 */
class ModifiedCreatureFilterEquipmentAurasCountersTest : FunSpec({

    val evaluator = PredicateEvaluator()
    val playerA = EntityId.generate()
    val playerB = EntityId.generate()

    test("IsModified matches creatures with Equipment, Aura, or counters and excludes vanilla and non-controlled") {
        val c1Id = EntityId.generate()
        val c2Id = EntityId.generate()
        val c3Id = EntityId.generate()
        val c4Id = EntityId.generate()
        val c5Id = EntityId.generate()
        val equipment1Id = EntityId.generate()
        val aura1Id = EntityId.generate()
        val aura2Id = EntityId.generate()

        fun creatureContainer(owner: EntityId, controller: EntityId): ComponentContainer =
            ComponentContainer()
                .with(CardComponent(
                    cardDefinitionId = "Creature",
                    name = "Creature",
                    manaCost = ManaCost(emptyList()),
                    typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
                    ownerId = owner,
                    baseStats = CreatureStats(2, 2)
                ))
                .with(OwnerComponent(owner))
                .with(ControllerComponent(controller))

        val equipment1Container = ComponentContainer()
            .with(CardComponent(
                cardDefinitionId = "Equipment",
                name = "Equipment",
                manaCost = ManaCost(emptyList()),
                typeLine = TypeLine(cardTypes = setOf(CardType.ARTIFACT), subtypes = setOf(Subtype.EQUIPMENT)),
                ownerId = playerA
            ))
            .with(OwnerComponent(playerA))
            .with(ControllerComponent(playerA))
            .with(AttachedToComponent(c1Id))

        val aura1Container = ComponentContainer()
            .with(CardComponent(
                cardDefinitionId = "Aura1",
                name = "Aura1",
                manaCost = ManaCost(emptyList()),
                typeLine = TypeLine(cardTypes = setOf(CardType.ENCHANTMENT), subtypes = setOf(Subtype.AURA)),
                ownerId = playerA
            ))
            .with(OwnerComponent(playerA))
            .with(ControllerComponent(playerA))
            .with(AttachedToComponent(c2Id))

        // Aura controlled by playerA but attached to playerB's creature C5
        val aura2Container = ComponentContainer()
            .with(CardComponent(
                cardDefinitionId = "Aura2",
                name = "Aura2",
                manaCost = ManaCost(emptyList()),
                typeLine = TypeLine(cardTypes = setOf(CardType.ENCHANTMENT), subtypes = setOf(Subtype.AURA)),
                ownerId = playerA
            ))
            .with(OwnerComponent(playerA))
            .with(ControllerComponent(playerA))
            .with(AttachedToComponent(c5Id))

        val c1Container = creatureContainer(playerA, playerA).with(AttachmentsComponent(listOf(equipment1Id)))
        val c2Container = creatureContainer(playerA, playerA).with(AttachmentsComponent(listOf(aura1Id)))
        val c3Container = creatureContainer(playerA, playerA)
            .with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        val c4Container = creatureContainer(playerA, playerA)
        val c5Container = creatureContainer(playerB, playerB).with(AttachmentsComponent(listOf(aura2Id)))

        var state = GameState()
            .withEntity(playerA, ComponentContainer())
            .withEntity(playerB, ComponentContainer())

        for ((id, container) in listOf(
            c1Id to c1Container,
            c2Id to c2Container,
            c3Id to c3Container,
            c4Id to c4Container,
            equipment1Id to equipment1Container,
            aura1Id to aura1Container,
            aura2Id to aura2Container
        )) {
            state = state.withEntity(id, container).addToZone(ZoneKey(playerA, Zone.BATTLEFIELD), id)
        }
        for ((id, container) in listOf(c5Id to c5Container)) {
            state = state.withEntity(id, container).addToZone(ZoneKey(playerB, Zone.BATTLEFIELD), id)
        }

        val modifiedFilter = GameObjectFilter(
            statePredicates = listOf(StatePredicate.IsModified),
            controllerPredicate = ControllerPredicate.ControlledByYou
        )
        val context = PredicateContext(controllerId = playerA)
        val projected = state.projectedState

        // C1: equipment attached — should match
        evaluator.matchesWithProjection(state, projected, c1Id, modifiedFilter, context) shouldBe true

        // C2: aura attached — should match
        evaluator.matchesWithProjection(state, projected, c2Id, modifiedFilter, context) shouldBe true

        // C3: +1/+1 counter — should match
        evaluator.matchesWithProjection(state, projected, c3Id, modifiedFilter, context) shouldBe true

        // C4: vanilla, nothing attached, no counters — should NOT match
        evaluator.matchesWithProjection(state, projected, c4Id, modifiedFilter, context) shouldBe false

        // C5: not controlled by playerA — should NOT match even though playerA's aura is attached
        evaluator.matchesWithProjection(state, projected, c5Id, modifiedFilter, context) shouldBe false
    }
})
