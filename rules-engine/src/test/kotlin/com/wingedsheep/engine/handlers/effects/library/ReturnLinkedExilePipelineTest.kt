package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ReturnLinkedExilePipelineTest : FunSpec({

    val playerId = EntityId.generate()
    val opponentId = EntityId.generate()
    val sourceId = EntityId.generate()
    val exiledCard1 = EntityId.generate()
    val exiledCard2 = EntityId.generate()

    fun cardComponent(name: String, ownerId: EntityId, isCreature: Boolean = true) = CardComponent(
        cardDefinitionId = name,
        name = name,
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = if (isCreature) setOf(CardType.CREATURE) else setOf(CardType.ENCHANTMENT)),
        ownerId = ownerId
    )

    fun baseState(): GameState {
        var state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        // Source permanent (e.g., Day of the Dragons) with linked exile
        val sourceContainer = ComponentContainer()
            .with(cardComponent("Source", playerId))
            .with(LinkedExileComponent(listOf(exiledCard1, exiledCard2)))
        state = state.withEntity(sourceId, sourceContainer)

        return state
    }

    fun context() = EffectContext(
        sourceId = sourceId,
        controllerId = playerId,
        opponentId = opponentId
    )

    fun createRegistry(): EffectExecutorRegistry {
        val registry = EffectExecutorRegistry()
        registry.registerModule(LibraryExecutors())
        return registry
    }

    test("returnLinkedExile moves cards from exile to controller's battlefield") {
        var state = baseState()

        // Both cards owned by player, in player's exile
        state = state.withEntity(exiledCard1, ComponentContainer()
            .with(cardComponent("Bear", playerId))
            .with(OwnerComponent(playerId)))
        state = state.addToZone(ZoneKey(playerId, Zone.EXILE), exiledCard1)

        state = state.withEntity(exiledCard2, ComponentContainer()
            .with(cardComponent("Wolf", playerId))
            .with(OwnerComponent(playerId)))
        state = state.addToZone(ZoneKey(playerId, Zone.EXILE), exiledCard2)

        val registry = createRegistry()
        val effect = EffectPatterns.returnLinkedExile()
        val result = registry.execute(state, effect, context())

        result.isSuccess shouldBe true

        // Cards should be on controller's battlefield
        val battlefield = result.state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD))
        battlefield.shouldContainExactlyInAnyOrder(exiledCard1, exiledCard2)

        // Cards should no longer be in exile
        result.state.getZone(ZoneKey(playerId, Zone.EXILE)).shouldBeEmpty()

        // Cards should have controller set to the effect controller
        result.state.getEntity(exiledCard1)?.get<ControllerComponent>()?.playerId shouldBe playerId
        result.state.getEntity(exiledCard2)?.get<ControllerComponent>()?.playerId shouldBe playerId

        // Creatures should have summoning sickness
        result.state.getEntity(exiledCard1)?.has<SummoningSicknessComponent>() shouldBe true

        // Should emit ZoneChangeEvents
        val zoneEvents = result.events.filterIsInstance<ZoneChangeEvent>()
        zoneEvents.size shouldBe 2
    }

    test("returnLinkedExile with underOwnersControl returns cards to owners' battlefields") {
        var state = baseState()

        // Card 1 owned by player
        state = state.withEntity(exiledCard1, ComponentContainer()
            .with(cardComponent("Bear", playerId))
            .with(OwnerComponent(playerId)))
        state = state.addToZone(ZoneKey(playerId, Zone.EXILE), exiledCard1)

        // Card 2 owned by opponent
        state = state.withEntity(exiledCard2, ComponentContainer()
            .with(cardComponent("Wolf", opponentId))
            .with(OwnerComponent(opponentId)))
        state = state.addToZone(ZoneKey(opponentId, Zone.EXILE), exiledCard2)

        val registry = createRegistry()
        val effect = EffectPatterns.returnLinkedExile(underOwnersControl = true)
        val result = registry.execute(state, effect, context())

        result.isSuccess shouldBe true

        // Card 1 on player's battlefield (owned by player)
        result.state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)).shouldContainExactlyInAnyOrder(exiledCard1)

        // Card 2 on opponent's battlefield (owned by opponent)
        result.state.getZone(ZoneKey(opponentId, Zone.BATTLEFIELD)).shouldContainExactlyInAnyOrder(exiledCard2)

        // Controllers match owners
        result.state.getEntity(exiledCard1)?.get<ControllerComponent>()?.playerId shouldBe playerId
        result.state.getEntity(exiledCard2)?.get<ControllerComponent>()?.playerId shouldBe opponentId
    }

    test("returnLinkedExile without underOwnersControl puts all cards on controller's battlefield") {
        var state = baseState()

        // Card 1 owned by player
        state = state.withEntity(exiledCard1, ComponentContainer()
            .with(cardComponent("Bear", playerId))
            .with(OwnerComponent(playerId)))
        state = state.addToZone(ZoneKey(playerId, Zone.EXILE), exiledCard1)

        // Card 2 owned by opponent but returns under controller's control
        state = state.withEntity(exiledCard2, ComponentContainer()
            .with(cardComponent("Wolf", opponentId))
            .with(OwnerComponent(opponentId)))
        state = state.addToZone(ZoneKey(opponentId, Zone.EXILE), exiledCard2)

        val registry = createRegistry()
        val effect = EffectPatterns.returnLinkedExile(underOwnersControl = false)
        val result = registry.execute(state, effect, context())

        result.isSuccess shouldBe true

        // Both cards on controller's (player's) battlefield
        val battlefield = result.state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD))
        battlefield.shouldContainExactlyInAnyOrder(exiledCard1, exiledCard2)

        // Both controlled by the effect controller
        result.state.getEntity(exiledCard1)?.get<ControllerComponent>()?.playerId shouldBe playerId
        result.state.getEntity(exiledCard2)?.get<ControllerComponent>()?.playerId shouldBe playerId
    }

    test("returnLinkedExile with empty linked exile is no-op") {
        var state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        // Source with empty linked exile
        val sourceContainer = ComponentContainer()
            .with(cardComponent("Source", playerId))
            .with(LinkedExileComponent(emptyList()))
        state = state.withEntity(sourceId, sourceContainer)

        val registry = createRegistry()
        val effect = EffectPatterns.returnLinkedExile()
        val result = registry.execute(state, effect, context())

        result.isSuccess shouldBe true
        result.events.filterIsInstance<ZoneChangeEvent>().shouldBeEmpty()
    }

    test("returnLinkedExile skips cards no longer in exile") {
        var state = baseState()

        // Card 1 still in exile
        state = state.withEntity(exiledCard1, ComponentContainer()
            .with(cardComponent("Bear", playerId))
            .with(OwnerComponent(playerId)))
        state = state.addToZone(ZoneKey(playerId, Zone.EXILE), exiledCard1)

        // Card 2 moved to graveyard (no longer in exile)
        state = state.withEntity(exiledCard2, ComponentContainer()
            .with(cardComponent("Wolf", playerId))
            .with(OwnerComponent(playerId)))
        state = state.addToZone(ZoneKey(playerId, Zone.GRAVEYARD), exiledCard2)

        val registry = createRegistry()
        val effect = EffectPatterns.returnLinkedExile()
        val result = registry.execute(state, effect, context())

        result.isSuccess shouldBe true

        // Only card 1 should be on battlefield
        result.state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)).shouldContainExactlyInAnyOrder(exiledCard1)

        // Card 2 should still be in graveyard
        result.state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)).shouldContainExactlyInAnyOrder(exiledCard2)
    }
})
