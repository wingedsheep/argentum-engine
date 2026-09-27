package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.IsTransformed` — CR 701.27g's "transformed permanent": a double-faced permanent
 * on the battlefield with its back face up. The card being double-faced is not enough (a front-face
 * werewolf isn't transformed), a back-face-up *spell* isn't a permanent, and an object with no
 * transforming faces (a modal double-faced card keeps its faces in `cardFaces`, never in a
 * `DoubleFacedComponent`) never matches.
 */
class TransformedPermanentPredicateTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val player = EntityId.generate()

    fun permanent(name: String, face: DoubleFacedComponent.Face?): ComponentContainer {
        val base = ComponentContainer()
            .with(CardComponent(
                cardDefinitionId = name,
                name = name,
                manaCost = ManaCost(emptyList()),
                typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
                ownerId = player
            ))
            .with(OwnerComponent(player))
            .with(ControllerComponent(player))
        return if (face == null) base
        else base.with(DoubleFacedComponent(frontCardDefinitionId = "Front", backCardDefinitionId = "Back", currentFace = face))
    }

    val backUp = EntityId.generate()
    val frontUp = EntityId.generate()
    val singleFaced = EntityId.generate()
    val backUpSpell = EntityId.generate()
    val battlefield = ZoneKey(player, Zone.BATTLEFIELD)
    val state = GameState()
        .withEntity(player, ComponentContainer())
        .withEntity(backUp, permanent("Back", DoubleFacedComponent.Face.BACK)).addToZone(battlefield, backUp)
        .withEntity(frontUp, permanent("Front", DoubleFacedComponent.Face.FRONT)).addToZone(battlefield, frontUp)
        .withEntity(singleFaced, permanent("Bear", null)).addToZone(battlefield, singleFaced)
        // Cast transformed (a Siege's defeat trigger): back face up, but on the stack.
        .withEntity(backUpSpell, permanent("Back", DoubleFacedComponent.Face.BACK))
        .let { it.copy(stack = it.stack + backUpSpell) }

    fun matches(id: EntityId, filter: GameObjectFilter = GameObjectFilter.Permanent.transformed()) =
        evaluator.matches(state, state.projectedState, id, filter, PredicateContext(controllerId = player))

    test("a double-faced permanent with its back face up is transformed") {
        matches(backUp) shouldBe true
    }

    test("front face up is never transformed, even for a transforming double-faced permanent") {
        matches(frontUp) shouldBe false
    }

    test("a permanent with no transforming faces is never transformed") {
        matches(singleFaced) shouldBe false
    }

    test("a back-face-up spell is not a transformed permanent") {
        matches(backUpSpell, GameObjectFilter.Any.transformed()) shouldBe false
    }
})
