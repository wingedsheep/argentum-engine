package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Locks in that `Exists(player = You, zone = Battlefield, ...)` reads the **projected**
 * controller, not the base [ControllerComponent]. A Layer 2 control change (Threaten,
 * Mind Control, Annex) does not move the entity between owner-keyed battlefield zones —
 * it adds a floating [SerializableModification.ChangeController] that the projector
 * applies. Reading the base controller would mask the change and silently break every
 * `Exists(player=You, …)` site.
 */
class EvaluateExistsControllerProjectionTest : FunSpec({

    val evaluator = ConditionEvaluator()

    val player1 = EntityId.generate()
    val player2 = EntityId.generate()
    val bear = EntityId.generate()

    fun bearOwnedAndControlledBy(playerId: EntityId): ComponentContainer =
        ComponentContainer()
            .with(
                CardComponent(
                    cardDefinitionId = "Grizzly Bears",
                    name = "Grizzly Bears",
                    manaCost = ManaCost(emptyList()),
                    typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
                    ownerId = playerId,
                )
            )
            .with(OwnerComponent(playerId))
            .with(ControllerComponent(playerId))

    fun baseStateWithBearControlledBy(playerId: EntityId): GameState =
        GameState(turnOrder = listOf(player1, player2))
            .withEntity(player1, ComponentContainer())
            .withEntity(player2, ComponentContainer())
            .withEntity(bear, bearOwnedAndControlledBy(playerId))
            .addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), bear)

    fun contextFor(controllerId: EntityId, opponentId: EntityId) = EffectContext(
        sourceId = null,
        controllerId = controllerId,
    )

    val youControlACreature = Exists(
        player = Player.You,
        zone = Zone.BATTLEFIELD,
        filter = GameObjectFilter.Creature,
    )

    test("baseline: Exists(You, Creature) is true for the base controller") {
        val state = baseStateWithBearControlledBy(player1)

        evaluator.evaluate(state, youControlACreature, contextFor(player1, player2)) shouldBe true
        evaluator.evaluate(state, youControlACreature, contextFor(player2, player1)) shouldBe false
    }

    test("Layer 2 control change flips Exists(You) to the projected controller") {
        val baseState = baseStateWithBearControlledBy(player1)
        val state = baseState.addFloatingEffect(
            layer = Layer.CONTROL,
            modification = SerializableModification.ChangeController(player2),
            affectedEntities = setOf(bear),
            duration = Duration.EndOfTurn,
            context = contextFor(player1, player2),
        )

        // Sanity: base ControllerComponent still says player1; only the projection flipped.
        state.getEntity(bear)?.get<ControllerComponent>()?.playerId shouldBe player1
        state.projectedState.getController(bear) shouldBe player2

        // Reading the base component would say "you (player1) control a creature" — wrong.
        // The fix in ConditionEvaluator.evaluateExists must consult the projection.
        evaluator.evaluate(state, youControlACreature, contextFor(player1, player2)) shouldBe false
        evaluator.evaluate(state, youControlACreature, contextFor(player2, player1)) shouldBe true
    }
})
