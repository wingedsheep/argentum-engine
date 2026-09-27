package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ProtectorComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.IsProtectedBy` — the battle's protector, not its controller, answers the reused
 * [ControllerPredicate]. A Siege is controlled by its caster and protected by an opponent, so the two
 * readings disagree on exactly the battles these cards care about.
 */
class BattleProtectedByPredicateTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val playerA = EntityId.generate()
    val playerB = EntityId.generate()

    fun battle(controller: EntityId, protector: EntityId?): ComponentContainer {
        val base = ComponentContainer()
            .with(CardComponent(
                cardDefinitionId = "Battle",
                name = "Battle",
                manaCost = ManaCost(emptyList()),
                typeLine = TypeLine(cardTypes = setOf(CardType.BATTLE)),
                ownerId = controller
            ))
            .with(OwnerComponent(controller))
            .with(ControllerComponent(controller))
        return if (protector != null) base.with(ProtectorComponent(protector)) else base
    }

    // A's Siege (protected by B), B's Siege (protected by A), and an A battle with no protector yet.
    val aSiege = EntityId.generate()
    val bSiege = EntityId.generate()
    val unassigned = EntityId.generate()
    val state = GameState()
        .withEntity(playerA, ComponentContainer())
        .withEntity(playerB, ComponentContainer())
        .withEntity(aSiege, battle(playerA, playerB)).addToZone(ZoneKey(playerA, Zone.BATTLEFIELD), aSiege)
        .withEntity(bSiege, battle(playerB, playerA)).addToZone(ZoneKey(playerB, Zone.BATTLEFIELD), bSiege)
        .withEntity(unassigned, battle(playerA, null)).addToZone(ZoneKey(playerA, Zone.BATTLEFIELD), unassigned)
    val asA = PredicateContext(controllerId = playerA)

    fun matches(id: EntityId, filter: GameObjectFilter, ctx: PredicateContext = asA) =
        evaluator.matches(state, state.projectedState, id, filter, ctx)

    test("an opponent protects: reads the protector, not the controller") {
        val filter = GameObjectFilter.Battle.protectedBy()
        matches(aSiege, filter) shouldBe true
        matches(bSiege, filter) shouldBe false
    }

    test("you protect") {
        val filter = GameObjectFilter.Battle.protectedBy(ControllerPredicate.ControlledByYou)
        matches(aSiege, filter) shouldBe false
        matches(bSiege, filter) shouldBe true
    }

    test("that player protects: resolves against the triggering player") {
        val filter = GameObjectFilter.Battle.protectedBy(ControllerPredicate.ControlledByTriggeringPlayer)
        val damagedB = PredicateContext(controllerId = playerA, triggeringEntityId = playerB)
        matches(aSiege, filter, damagedB) shouldBe true
        matches(bSiege, filter, damagedB) shouldBe false
    }

    test("a battle with no protector never matches") {
        matches(unassigned, GameObjectFilter.Battle.protectedBy()) shouldBe false
        matches(unassigned, GameObjectFilter.Battle.protectedBy(ControllerPredicate.ControlledByAny)) shouldBe false
    }

    test("owner-based leaves never match a protector") {
        matches(aSiege, GameObjectFilter.Battle.protectedBy(ControllerPredicate.OwnedByOpponent)) shouldBe false
    }
})
