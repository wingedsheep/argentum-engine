package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ProvokeEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Provoke must decide "is the target a creature?" from *projected* state: a land animated by a
 * continuous effect is a creature and can be provoked, and a creature whose card types were
 * overwritten so it is no longer a creature cannot. The printed type line sees neither change.
 */
class ProvokeProjectedCreatureTest : FunSpec({
    val attackerController = EntityId.generate()
    val defender = EntityId.generate()
    val provoker = EntityId.generate()
    val targetId = EntityId.generate()

    fun card(name: String, owner: EntityId, vararg types: CardType) = CardComponent(
        cardDefinitionId = name,
        name = name,
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = types.toSet()),
        ownerId = owner
    )

    fun context() = EffectContext(
        sourceId = provoker,
        controllerId = attackerController,
        targets = listOf(ChosenTarget.Permanent(targetId))
    )

    /** An attacking provoker and a tapped [targetCard] the defending player controls. */
    fun baseState(targetCard: CardComponent): GameState =
        GameState(turnOrder = listOf(attackerController, defender))
            .withEntity(attackerController, ComponentContainer())
            .withEntity(defender, ComponentContainer())
            .withEntity(
                provoker,
                ComponentContainer()
                    .with(card("Provoker", attackerController, CardType.CREATURE))
                    .with(OwnerComponent(attackerController))
                    .with(ControllerComponent(attackerController))
                    .with(AttackingComponent(defenderId = defender))
            )
            .withEntity(
                targetId,
                ComponentContainer()
                    .with(targetCard)
                    .with(OwnerComponent(defender))
                    .with(ControllerComponent(defender))
                    .with(TappedComponent)
            )
            .addToZone(ZoneKey(attackerController, Zone.BATTLEFIELD), provoker)
            .addToZone(ZoneKey(defender, Zone.BATTLEFIELD), targetId)

    fun forcedToBlock(state: GameState): Boolean = state.floatingEffects.any {
        val mod = it.effect.modification
        mod is SerializableModification.MustBlockSpecificAttacker &&
            mod.attackerId == provoker && targetId in it.effect.affectedEntities
    }

    test("a land animated into a creature can be provoked") {
        val state = baseState(card("Animated Land", defender, CardType.LAND)).addFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.AddType("CREATURE"),
            affectedEntities = setOf(targetId),
            duration = Duration.EndOfTurn,
            context = EffectContext(sourceId = null, controllerId = defender)
        )
        state.projectedState.isCreature(targetId) shouldBe true

        val result = ProvokeExecutor().execute(state, ProvokeEffect(), context())

        result.error.shouldBeNull()
        result.state.getEntity(targetId)!!.has<TappedComponent>() shouldBe false
        forcedToBlock(result.state) shouldBe true
    }

    test("a creature that stopped being a creature cannot be provoked") {
        val state = baseState(card("Former Creature", defender, CardType.CREATURE)).addFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.SetCardTypes(setOf("ARTIFACT")),
            affectedEntities = setOf(targetId),
            duration = Duration.EndOfTurn,
            context = EffectContext(sourceId = null, controllerId = defender)
        )
        state.projectedState.isCreature(targetId) shouldBe false

        val result = ProvokeExecutor().execute(state, ProvokeEffect(), context())

        result.error.shouldNotBeNull()
        result.state.getEntity(targetId)!!.has<TappedComponent>() shouldBe true
        forcedToBlock(result.state) shouldBe false
    }
})
