package com.wingedsheep.engine.state

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * A "you may play this card" permission names one object. When its card changes zones it becomes
 * a new object (CR 400.7) and the permission no longer applies to it — the move drops the card
 * from every grant. The stack is the one exception on the way in: a cast still reads the
 * permission that authorised it, and the grant is dropped wherever the spell goes next.
 */
class MayPlayPermissionZoneChangeTest : FunSpec({
    val owner = EntityId.generate()
    val other = EntityId.generate()
    val cardId = EntityId.generate()
    val secondId = EntityId.generate()
    val permId = EntityId.generate()

    fun card(name: String) = ComponentContainer.of(
        CardComponent(name, name, ManaCost(emptyList()),
            TypeLine(cardTypes = setOf(CardType.SORCERY)), ownerId = owner),
        OwnerComponent(owner), ControllerComponent(owner)
    )

    val graveyard = ZoneKey(owner, Zone.GRAVEYARD)

    /** Both cards in [zone], with one permission covering both. */
    fun granted(zone: Zone = Zone.GRAVEYARD): GameState = GameState(turnOrder = listOf(owner, other))
        .withEntity(owner, ComponentContainer.EMPTY)
        .withEntity(other, ComponentContainer.EMPTY)
        .withEntity(cardId, card("First"))
        .withEntity(secondId, card("Second"))
        .addToZone(ZoneKey(owner, zone), cardId)
        .addToZone(ZoneKey(owner, zone), secondId)
        .addMayPlayPermission(
            MayPlayPermission(id = permId, cardIds = setOf(cardId, secondId), controllerId = owner, timestamp = 1)
        )

    fun GameState.covered(): Set<EntityId> = mayPlayPermissions.flatMap { it.cardIds }.toSet()

    test("a card that leaves the graveyard and comes back no longer has the permission") {
        val exiled = granted().removeFromZone(graveyard, cardId).addToZone(ZoneKey(owner, Zone.EXILE), cardId)
        exiled.covered() shouldBe setOf(secondId)

        val back = exiled.removeFromZone(ZoneKey(owner, Zone.EXILE), cardId).addToZone(graveyard, cardId)
        back.covered() shouldBe setOf(secondId)
    }

    test("the other cards of a shared grant keep it") {
        val moved = granted().removeFromZone(graveyard, cardId).addToZone(ZoneKey(owner, Zone.HAND), cardId)
        moved.mayPlayPermissions.single().id shouldBe permId
        moved.mayPlayPermissions.single().cardIds shouldBe setOf(secondId)
    }

    test("a grant whose last card moves is removed outright") {
        val moved = granted()
            .removeFromZone(graveyard, cardId).addToZone(ZoneKey(owner, Zone.LIBRARY), cardId)
            .removeFromZone(graveyard, secondId).addToZone(ZoneKey(owner, Zone.BATTLEFIELD), secondId)
        moved.mayPlayPermissions.shouldBeEmpty()
    }

    test("a card leaving exile and returning to exile is a new object too") {
        val exile = ZoneKey(owner, Zone.EXILE)
        val moved = granted(Zone.EXILE).removeFromZone(exile, cardId).addToZone(exile, cardId)
        moved.covered() shouldBe setOf(secondId)
    }

    test("the permission survives the move onto the stack, and goes when the spell lands anywhere") {
        val cast = granted().removeFromZone(graveyard, cardId).pushToStack(cardId)
        cast.covered() shouldBe setOf(cardId, secondId)

        // Countered or fizzled: back to the graveyard as a new object.
        val (_, popped) = cast.popFromStack()
        popped.addToZone(graveyard, cardId).covered() shouldBe setOf(secondId)
    }

    test("reordering a zone or initialising identities is not a zone change") {
        val state = granted()
        state.reorderZone(graveyard, listOf(secondId, cardId)).covered() shouldBe setOf(cardId, secondId)
        state.initializeObjectIdentities().covered() shouldBe setOf(cardId, secondId)
    }
})
