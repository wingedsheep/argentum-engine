package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class GatherCardsFromLinkedExileTest : FunSpec({

    val executor = GatherCardsExecutor()

    val playerId = EntityId.generate()
    val opponentId = EntityId.generate()
    val sourceId = EntityId.generate()
    val exiledCard1 = EntityId.generate()
    val exiledCard2 = EntityId.generate()
    val exiledCard3 = EntityId.generate()

    fun cardComponent(name: String, ownerId: EntityId) = CardComponent(
        cardDefinitionId = name,
        name = name,
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
        ownerId = ownerId
    )

    fun context() = EffectContext(
        sourceId = sourceId,
        controllerId = playerId,
    )

    fun gatherEffect() = GatherCardsEffect(
        source = CardSource.FromLinkedExile(),
        storeAs = "linked"
    )

    test("gathers all linked cards that are in exile") {
        var state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        // Source permanent with LinkedExileComponent
        val sourceContainer = ComponentContainer()
            .with(cardComponent("Day of the Dragons", playerId))
            .with(LinkedExileComponent(listOf(exiledCard1, exiledCard2)))
        state = state.withEntity(sourceId, sourceContainer)
        state = state.addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), sourceId)

        // Two cards in exile
        state = state.withEntity(exiledCard1, ComponentContainer()
            .with(cardComponent("Bear", playerId))
            .with(OwnerComponent(playerId)))
        state = state.addToZone(ZoneKey(playerId, Zone.EXILE), exiledCard1)

        state = state.withEntity(exiledCard2, ComponentContainer()
            .with(cardComponent("Wolf", playerId))
            .with(OwnerComponent(playerId)))
        state = state.addToZone(ZoneKey(playerId, Zone.EXILE), exiledCard2)

        val result = executor.execute(state, gatherEffect(), context())

        result.isSuccess shouldBe true
        result.updatedCollections["linked"]!!.shouldContainExactlyInAnyOrder(exiledCard1, exiledCard2)
    }

    test("filters out cards no longer in exile") {
        var state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        // Source with 3 linked cards, but only 1 is still in exile
        val sourceContainer = ComponentContainer()
            .with(cardComponent("Day of the Dragons", playerId))
            .with(LinkedExileComponent(listOf(exiledCard1, exiledCard2, exiledCard3)))
        state = state.withEntity(sourceId, sourceContainer)
        state = state.addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), sourceId)

        // Only exiledCard1 is in exile
        state = state.withEntity(exiledCard1, ComponentContainer()
            .with(cardComponent("Bear", playerId))
            .with(OwnerComponent(playerId)))
        state = state.addToZone(ZoneKey(playerId, Zone.EXILE), exiledCard1)

        // exiledCard2 moved to graveyard
        state = state.withEntity(exiledCard2, ComponentContainer()
            .with(cardComponent("Wolf", playerId))
            .with(OwnerComponent(playerId)))
        state = state.addToZone(ZoneKey(playerId, Zone.GRAVEYARD), exiledCard2)

        // exiledCard3 moved to battlefield
        state = state.withEntity(exiledCard3, ComponentContainer()
            .with(cardComponent("Elk", playerId))
            .with(OwnerComponent(playerId)))
        state = state.addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), exiledCard3)

        val result = executor.execute(state, gatherEffect(), context())

        result.isSuccess shouldBe true
        result.updatedCollections["linked"]!!.shouldContainExactlyInAnyOrder(exiledCard1)
    }

    test("returns empty collection when no LinkedExileComponent exists") {
        var state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        // Source without LinkedExileComponent
        val sourceContainer = ComponentContainer()
            .with(cardComponent("Day of the Dragons", playerId))
        state = state.withEntity(sourceId, sourceContainer)
        state = state.addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), sourceId)

        val result = executor.execute(state, gatherEffect(), context())

        result.isSuccess shouldBe true
        result.updatedCollections["linked"]!!.shouldBeEmpty()
    }

    test("returns empty collection when linked exile list is empty") {
        var state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        val sourceContainer = ComponentContainer()
            .with(cardComponent("Day of the Dragons", playerId))
            .with(LinkedExileComponent(emptyList()))
        state = state.withEntity(sourceId, sourceContainer)
        state = state.addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), sourceId)

        val result = executor.execute(state, gatherEffect(), context())

        result.isSuccess shouldBe true
        result.updatedCollections["linked"]!!.shouldBeEmpty()
    }

    test("handles cards owned by different players") {
        var state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        val sourceContainer = ComponentContainer()
            .with(cardComponent("Planar Guide", playerId))
            .with(LinkedExileComponent(listOf(exiledCard1, exiledCard2)))
        state = state.withEntity(sourceId, sourceContainer)

        // Card owned by player in player's exile
        state = state.withEntity(exiledCard1, ComponentContainer()
            .with(cardComponent("Bear", playerId))
            .with(OwnerComponent(playerId)))
        state = state.addToZone(ZoneKey(playerId, Zone.EXILE), exiledCard1)

        // Card owned by opponent in opponent's exile
        state = state.withEntity(exiledCard2, ComponentContainer()
            .with(cardComponent("Wolf", opponentId))
            .with(OwnerComponent(opponentId)))
        state = state.addToZone(ZoneKey(opponentId, Zone.EXILE), exiledCard2)

        val result = executor.execute(state, gatherEffect(), context())

        result.isSuccess shouldBe true
        result.updatedCollections["linked"]!!.shouldContainExactlyInAnyOrder(exiledCard1, exiledCard2)
    }

    test("error when no source entity") {
        val state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        val noSourceContext = EffectContext(
            sourceId = null,
            controllerId = playerId,
        )

        val result = executor.execute(state, gatherEffect(), noSourceContext)

        result.isSuccess shouldBe false
    }
})
