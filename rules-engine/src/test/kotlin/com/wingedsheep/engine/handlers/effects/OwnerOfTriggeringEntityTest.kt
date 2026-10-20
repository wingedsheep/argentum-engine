package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [Player.OwnerOfTriggeringEntity] — the owner of the object that triggered the ability, as opposed
 * to its controller ([Player.ControllerOfTriggeringEntity]), which follows a steal.
 */
class OwnerOfTriggeringEntityTest : FunSpec({
    val creature = EntityId.generate()
    val owner = EntityId.generate()
    val thief = EntityId.generate()
    val caster = EntityId.generate()
    val context = EffectContext(sourceId = null, controllerId = caster, triggeringEntityId = creature)

    fun card(ownerId: EntityId) = CardComponent(
        cardDefinitionId = "Grizzly Bears",
        name = "Grizzly Bears",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine.creature(),
        ownerId = ownerId,
    )

    fun resolve(state: GameState, ctx: EffectContext = context) =
        TargetResolutionUtils.resolvePlayerRef(Player.OwnerOfTriggeringEntity, ctx, state)

    test("names the triggering object's owner, not the player who stole it") {
        val state = GameState().withEntity(
            creature,
            ComponentContainer().with(card(owner)).with(ControllerComponent(thief))
        )
        resolve(state) shouldBe owner
        TargetResolutionUtils.resolvePlayerRef(Player.ControllerOfTriggeringEntity, context, state) shouldBe thief
    }

    test("a card already in the graveyard still answers with its owner") {
        val state = GameState().withEntity(creature, ComponentContainer().with(card(owner)))
        resolve(state) shouldBe owner
    }

    test("a token that has ceased to exist answers from its last-known snapshot") {
        val ctx = context.copy(
            triggerContext = TriggerContext(
                triggeringEntityId = creature,
                triggeringLastKnownSnapshot = EntitySnapshot(entityId = creature, controllerId = thief, ownerId = owner),
            )
        )
        resolve(GameState(), ctx) shouldBe owner
    }

    test("a snapshot of some other object is not read") {
        val ctx = context.copy(
            triggerContext = TriggerContext(
                triggeringLastKnownSnapshot = EntitySnapshot(entityId = EntityId.generate(), ownerId = owner),
            )
        )
        resolve(GameState(), ctx) shouldBe null
    }

    test("is null when nothing triggered the ability") {
        resolve(GameState(), context.copy(triggeringEntityId = null)) shouldBe null
    }
})
