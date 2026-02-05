package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.ZonePlacement
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MoveToZoneEffectExecutorTest : FunSpec({

    val executor = MoveToZoneEffectExecutor()

    val playerId = EntityId.generate()
    val cardId = EntityId.generate()

    fun creatureCard(ownerId: EntityId, keywords: Set<Keyword> = emptySet()) = CardComponent(
        cardDefinitionId = "Test Creature",
        name = "Test Creature",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
        ownerId = ownerId,
        baseKeywords = keywords
    )

    fun battlefieldState(
        playerId: EntityId,
        cardId: EntityId,
        cardComponent: CardComponent
    ): GameState {
        val container = ComponentContainer()
            .with(cardComponent)
            .with(OwnerComponent(playerId))
            .with(ControllerComponent(playerId))
        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        return GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(cardId, container)
            .addToZone(battlefieldZone, cardId)
    }

    fun graveyardState(
        playerId: EntityId,
        cardId: EntityId,
        cardComponent: CardComponent
    ): GameState {
        val container = ComponentContainer()
            .with(cardComponent)
            .with(OwnerComponent(playerId))
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        return GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(cardId, container)
            .addToZone(graveyardZone, cardId)
    }

    fun context(targetId: EntityId, controllerId: EntityId) = EffectContext(
        sourceId = null,
        controllerId = controllerId,
        opponentId = null,
        targets = listOf(ChosenTarget.Permanent(targetId))
    )

    test("move from battlefield to graveyard (default)") {
        val card = creatureCard(playerId)
        val state = battlefieldState(playerId, cardId, card)

        val effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.GRAVEYARD
        )

        val result = executor.execute(state, effect, context(cardId, playerId))
        result.isSuccess shouldBe true

        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        result.state.getZone(graveyardZone) shouldContain cardId

        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        result.state.getZone(battlefieldZone) shouldNotContain cardId

        val zoneEvent = result.events.filterIsInstance<ZoneChangeEvent>().first()
        zoneEvent.fromZone shouldBe Zone.BATTLEFIELD
        zoneEvent.toZone shouldBe Zone.GRAVEYARD
    }

    test("move from battlefield to hand (bounce)") {
        val card = creatureCard(playerId)
        val state = battlefieldState(playerId, cardId, card)

        val effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.HAND
        )

        val result = executor.execute(state, effect, context(cardId, playerId))
        result.isSuccess shouldBe true

        val handZone = ZoneKey(playerId, Zone.HAND)
        result.state.getZone(handZone) shouldContain cardId

        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        result.state.getZone(battlefieldZone) shouldNotContain cardId
    }

    test("move from battlefield to exile") {
        val card = creatureCard(playerId)
        val state = battlefieldState(playerId, cardId, card)

        val effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.EXILE
        )

        val result = executor.execute(state, effect, context(cardId, playerId))
        result.isSuccess shouldBe true

        val exileZone = ZoneKey(playerId, Zone.EXILE)
        result.state.getZone(exileZone) shouldContain cardId
    }

    test("move to library top") {
        val card = creatureCard(playerId)
        val state = battlefieldState(playerId, cardId, card)

        val effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Top
        )

        val result = executor.execute(state, effect, context(cardId, playerId))
        result.isSuccess shouldBe true

        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val library = result.state.getZone(libraryZone)
        library.first() shouldBe cardId
    }

    test("move to library bottom") {
        val card = creatureCard(playerId)
        // Add an existing card in the library first
        val existingLibraryCard = EntityId.generate()
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val state = battlefieldState(playerId, cardId, card)
            .withEntity(existingLibraryCard, ComponentContainer().with(creatureCard(playerId)))
            .addToZone(libraryZone, existingLibraryCard)

        val effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Bottom
        )

        val result = executor.execute(state, effect, context(cardId, playerId))
        result.isSuccess shouldBe true

        val library = result.state.getZone(libraryZone)
        library.last() shouldBe cardId
        library.first() shouldBe existingLibraryCard
    }

    test("move to library shuffled emits LibraryShuffledEvent") {
        val card = creatureCard(playerId)
        val state = battlefieldState(playerId, cardId, card)

        val effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Shuffled
        )

        val result = executor.execute(state, effect, context(cardId, playerId))
        result.isSuccess shouldBe true

        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        result.state.getZone(libraryZone) shouldContain cardId

        result.events.filterIsInstance<ZoneChangeEvent>().first().toZone shouldBe Zone.LIBRARY
        result.events.filterIsInstance<LibraryShuffledEvent>().first().playerId shouldBe playerId
    }

    test("move to battlefield tapped") {
        val card = creatureCard(playerId)
        val state = graveyardState(playerId, cardId, card)
        val ctx = EffectContext(
            sourceId = null,
            controllerId = playerId,
            opponentId = null,
            targets = listOf(ChosenTarget.Permanent(cardId))
        )

        val effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.BATTLEFIELD,
            placement = ZonePlacement.Tapped
        )

        val result = executor.execute(state, effect, ctx)
        result.isSuccess shouldBe true

        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        result.state.getZone(battlefieldZone) shouldContain cardId

        val entity = result.state.getEntity(cardId)!!
        entity.get<TappedComponent>() shouldBe TappedComponent
        entity.get<ControllerComponent>()!!.playerId shouldBe playerId
    }

    test("byDestruction = true destroys the permanent") {
        val card = creatureCard(playerId)
        val state = battlefieldState(playerId, cardId, card)

        val effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.GRAVEYARD,
            byDestruction = true
        )

        val result = executor.execute(state, effect, context(cardId, playerId))
        result.isSuccess shouldBe true

        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        result.state.getZone(graveyardZone) shouldContain cardId

        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        result.state.getZone(battlefieldZone) shouldNotContain cardId
    }

    test("byDestruction = true on indestructible creature is a no-op") {
        val card = creatureCard(playerId, keywords = setOf(Keyword.INDESTRUCTIBLE))
        val state = battlefieldState(playerId, cardId, card)

        val effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.GRAVEYARD,
            byDestruction = true
        )

        val result = executor.execute(state, effect, context(cardId, playerId))
        result.isSuccess shouldBe true

        // Card stays on battlefield
        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        result.state.getZone(battlefieldZone) shouldContain cardId

        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        result.state.getZone(graveyardZone) shouldNotContain cardId
    }

    test("battlefield components are cleaned up when leaving battlefield") {
        val card = creatureCard(playerId)
        val state = battlefieldState(playerId, cardId, card)

        val effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.HAND
        )

        val result = executor.execute(state, effect, context(cardId, playerId))
        result.isSuccess shouldBe true

        val entity = result.state.getEntity(cardId)!!
        entity.get<ControllerComponent>() shouldBe null
        entity.get<TappedComponent>() shouldBe null
    }
})
