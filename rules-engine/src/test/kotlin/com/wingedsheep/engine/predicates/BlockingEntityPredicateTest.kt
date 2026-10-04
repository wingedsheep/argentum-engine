package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.IsBlockingEntity` — "each creature blocking **it**" where "it" is a role the
 * ability names (CR 509): matches blockers of the referenced creature only, read live off the
 * blocker's `BlockingComponent`, and nothing when the reference resolves to nothing.
 */
class BlockingEntityPredicateTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val attackerPlayer = EntityId.generate()
    val defenderPlayer = EntityId.generate()

    fun creature(controller: EntityId, blocking: List<EntityId>? = null): ComponentContainer {
        val base = ComponentContainer()
            .with(CardComponent(
                cardDefinitionId = "Creature",
                name = "Creature",
                manaCost = ManaCost(emptyList()),
                typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
                ownerId = controller
            ))
            .with(OwnerComponent(controller))
            .with(ControllerComponent(controller))
        return if (blocking != null) base.with(BlockingComponent(blocking)) else base
    }

    val goblin = EntityId.generate()
    val otherAttacker = EntityId.generate()
    val blockerOfGoblin = EntityId.generate()
    val doubleBlocker = EntityId.generate()      // blocks both attackers
    val blockerOfOther = EntityId.generate()
    val bystander = EntityId.generate()
    val atk = ZoneKey(attackerPlayer, Zone.BATTLEFIELD)
    val def = ZoneKey(defenderPlayer, Zone.BATTLEFIELD)
    val state = GameState()
        .withEntity(attackerPlayer, ComponentContainer())
        .withEntity(defenderPlayer, ComponentContainer())
        .withEntity(goblin, creature(attackerPlayer)).addToZone(atk, goblin)
        .withEntity(otherAttacker, creature(attackerPlayer)).addToZone(atk, otherAttacker)
        .withEntity(blockerOfGoblin, creature(defenderPlayer, listOf(goblin))).addToZone(def, blockerOfGoblin)
        .withEntity(doubleBlocker, creature(defenderPlayer, listOf(otherAttacker, goblin))).addToZone(def, doubleBlocker)
        .withEntity(blockerOfOther, creature(defenderPlayer, listOf(otherAttacker))).addToZone(def, blockerOfOther)
        .withEntity(bystander, creature(defenderPlayer)).addToZone(def, bystander)

    val filter = GameObjectFilter.Creature.blockingEntity(EffectTarget.TriggeringEntity)

    fun matches(id: EntityId, ctx: PredicateContext) =
        evaluator.matches(state, state.projectedState, id, filter, ctx)

    test("matches exactly the creatures blocking the referenced creature") {
        val ctx = PredicateContext(controllerId = attackerPlayer, triggeringEntityId = goblin)
        matches(blockerOfGoblin, ctx) shouldBe true
        matches(doubleBlocker, ctx) shouldBe true
        matches(blockerOfOther, ctx) shouldBe false
        matches(bystander, ctx) shouldBe false
        matches(goblin, ctx) shouldBe false
    }

    test("follows the reference, not the source") {
        val ctx = PredicateContext(controllerId = attackerPlayer, sourceId = goblin, triggeringEntityId = otherAttacker)
        matches(blockerOfGoblin, ctx) shouldBe false
        matches(doubleBlocker, ctx) shouldBe true
        matches(blockerOfOther, ctx) shouldBe true
    }

    test("a reference that resolves to nothing matches nothing") {
        val ctx = PredicateContext(controllerId = attackerPlayer)
        listOf(blockerOfGoblin, doubleBlocker, blockerOfOther, bystander).forEach { matches(it, ctx) shouldBe false }
    }
})
