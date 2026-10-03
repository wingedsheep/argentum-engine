package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CardPredicate.IsSnow` — the snow supertype (CR 205.4g), read from projected types on the
 * battlefield and from the card's own type line elsewhere.
 */
class SnowSupertypePredicateTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val player = EntityId.generate()

    fun card(name: String, typeLine: TypeLine) = ComponentContainer()
        .with(CardComponent(
            cardDefinitionId = name,
            name = name,
            manaCost = ManaCost(emptyList()),
            typeLine = typeLine,
            ownerId = player
        ))
        .with(OwnerComponent(player))
        .with(ControllerComponent(player))

    val snowLand = EntityId.generate()
    val plainLand = EntityId.generate()
    val snowCreatureInHand = EntityId.generate()
    val state = GameState()
        .withEntity(player, ComponentContainer())
        .withEntity(snowLand, card("Snow-Covered Island", TypeLine(
            supertypes = setOf(Supertype.BASIC, Supertype.SNOW), cardTypes = setOf(CardType.LAND)
        ))).addToZone(ZoneKey(player, Zone.BATTLEFIELD), snowLand)
        .withEntity(plainLand, card("Island", TypeLine(
            supertypes = setOf(Supertype.BASIC), cardTypes = setOf(CardType.LAND)
        ))).addToZone(ZoneKey(player, Zone.BATTLEFIELD), plainLand)
        .withEntity(snowCreatureInHand, card("Avalanche Caller", TypeLine(
            supertypes = setOf(Supertype.SNOW), cardTypes = setOf(CardType.CREATURE)
        ))).addToZone(ZoneKey(player, Zone.HAND), snowCreatureInHand)
    val ctx = PredicateContext(controllerId = player)

    fun matches(id: EntityId, filter: GameObjectFilter) =
        evaluator.matches(state, state.projectedState, id, filter, ctx)

    test("snow land on the battlefield matches; a plain basic doesn't") {
        matches(snowLand, GameObjectFilter.Land.snow()) shouldBe true
        matches(plainLand, GameObjectFilter.Land.snow()) shouldBe false
    }

    test("composes with the card type: a snow land is not a snow creature") {
        matches(snowLand, GameObjectFilter.Creature.snow()) shouldBe false
        matches(snowLand, GameObjectFilter.Permanent.snow().youControl()) shouldBe true
    }

    test("off the battlefield it reads the card's printed supertypes") {
        matches(snowCreatureInHand, GameObjectFilter.Creature.snow()) shouldBe true
    }
})
