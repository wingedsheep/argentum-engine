package com.wingedsheep.engine.state

import com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExileLookup
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.EmblemSourceComponent
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly

/**
 * An emblem whose text names the object that created it ("cards exiled with Tibalt, Cosmic
 * Impostor") reads the pile of that object's battlefield visit — live while it lasts, retained
 * once it has left — and never a later visit of the same card (CR 400.7: a new object).
 */
class EmblemLinkedExileTest : FunSpec({

    val creator = EntityId("tibalt")
    val emblem = EntityId("emblem")
    val first = EntityId("first-exiled")
    val later = EntityId("later-exiled")
    val visit = 10L

    fun board(): GameState = GameState()
        .withEntity(
            creator,
            ComponentContainer.EMPTY
                .with(BattlefieldEntryTimestampComponent(visit))
                .with(LinkedExileComponent(listOf(first))),
        )
        .withEntity(
            emblem,
            ComponentContainer.EMPTY.with(
                EmblemSourceComponent("Tibalt, Cosmic Impostor", "text", creatorId = creator, creatorEntryTimestamp = visit)
            ),
        )

    test("an emblem reads its creator's live pile") {
        val state = board()
        LinkedExileLookup.exiledWith(state, emblem) shouldContainExactly listOf(first)
        val grown = state.updateEntity(creator) { it.with(LinkedExileComponent(listOf(first, later))) }
        LinkedExileLookup.exiledWith(grown, emblem) shouldContainExactly listOf(first, later)
    }

    test("after the creator leaves, the emblem reads the departed visit's pile, not the next visit's") {
        val departed = board().copy(departedLinkedExile = mapOf(visit to listOf(first)))
            .updateEntity(creator) {
                it.with(BattlefieldEntryTimestampComponent(visit + 1)).with(LinkedExileComponent(listOf(later)))
            }
        LinkedExileLookup.exiledWith(departed, emblem) shouldContainExactly listOf(first)
    }

    test("an emblem with no creator link has no pile") {
        val state = board().withEntity(emblem, ComponentContainer.EMPTY.with(EmblemSourceComponent("Oko", "text")))
        LinkedExileLookup.exiledWith(state, emblem).shouldBeEmpty()
    }
})
