package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class MoveCollectionDestroyTest : FunSpec({

    val cardRegistry = com.wingedsheep.engine.registry.CardRegistry()
    val executor = MoveCollectionExecutor(cardRegistry)

    val playerId = EntityId.generate()
    val opponentId = EntityId.generate()
    val cardId1 = EntityId.generate()
    val cardId2 = EntityId.generate()
    val cardId3 = EntityId.generate()

    fun creatureCard(name: String, ownerId: EntityId, keywords: Set<Keyword> = emptySet()) = CardComponent(
        cardDefinitionId = name,
        name = name,
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
        ownerId = ownerId,
        baseKeywords = keywords
    )

    fun battlefieldState(
        vararg creatures: Triple<EntityId, EntityId, CardComponent>
    ): GameState {
        var state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        for ((cardId, controllerId, cardComponent) in creatures) {
            val container = ComponentContainer()
                .with(cardComponent)
                .with(OwnerComponent(cardComponent.ownerId!!))
                .with(ControllerComponent(controllerId))
            state = state.withEntity(cardId, container)
            state = state.addToZone(ZoneKey(controllerId, Zone.BATTLEFIELD), cardId)
        }
        return state
    }

    fun context(
        controllerId: EntityId,
        collectionName: String,
        cards: List<EntityId>
    ) = EffectContext(
        sourceId = null,
        controllerId = controllerId,
        pipeline = PipelineState(storedCollections = mapOf(collectionName to cards))
    )

    fun destroyEffect(collectionName: String = "targets") = MoveCollectionEffect(
        from = collectionName,
        destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
        moveType = MoveType.Destroy
    )

    fun regenerationShield(entityId: EntityId) = ActiveFloatingEffect(
        id = EntityId.generate(),
        effect = FloatingEffectData(
            layer = Layer.ABILITY,
            modification = SerializableModification.RegenerationShield,
            affectedEntities = setOf(entityId)
        ),
        duration = Duration.EndOfTurn,
        sourceId = null,
        controllerId = playerId,
        timestamp = 1L
    )

    test("MoveType.Destroy moves creature from battlefield to owner's graveyard") {
        val card = creatureCard("Bear", playerId)
        val state = battlefieldState(Triple(cardId1, playerId, card))

        val effect = destroyEffect()
        val ctx = context(playerId, "targets", listOf(cardId1))
        val result = executor.execute(state, effect, ctx)

        result.isSuccess shouldBe true
        result.state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)) shouldContain cardId1
        result.state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)) shouldNotContain cardId1

        val zoneEvent = result.events.filterIsInstance<ZoneChangeEvent>().first()
        zoneEvent.entityId shouldBe cardId1
        zoneEvent.fromZone shouldBe Zone.BATTLEFIELD
        zoneEvent.toZone shouldBe Zone.GRAVEYARD
    }

    test("MoveType.Destroy skips indestructible creatures") {
        val card = creatureCard("Darksteel Colossus", playerId, keywords = setOf(Keyword.INDESTRUCTIBLE))
        val state = battlefieldState(Triple(cardId1, playerId, card))

        val effect = destroyEffect()
        val ctx = context(playerId, "targets", listOf(cardId1))
        val result = executor.execute(state, effect, ctx)

        result.isSuccess shouldBe true
        result.state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)) shouldContain cardId1
        result.state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)) shouldNotContain cardId1
        result.events.filterIsInstance<ZoneChangeEvent>() shouldBe emptyList()
    }

    test("MoveType.Destroy with mixed indestructible and normal — only normal creatures die") {
        val normalCard = creatureCard("Bear", playerId)
        val indestructibleCard = creatureCard("Colossus", playerId, keywords = setOf(Keyword.INDESTRUCTIBLE))
        val state = battlefieldState(
            Triple(cardId1, playerId, normalCard),
            Triple(cardId2, playerId, indestructibleCard)
        )

        val effect = destroyEffect()
        val ctx = context(playerId, "targets", listOf(cardId1, cardId2))
        val result = executor.execute(state, effect, ctx)

        result.isSuccess shouldBe true
        // Normal creature dies
        result.state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)) shouldContain cardId1
        result.state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)) shouldNotContain cardId1
        // Indestructible creature survives
        result.state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)) shouldContain cardId2
        result.state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)) shouldNotContain cardId2

        val zoneEvents = result.events.filterIsInstance<ZoneChangeEvent>()
        zoneEvents.size shouldBe 1
        zoneEvents[0].entityName shouldBe "Bear"
    }

    test("MoveType.Destroy respects regeneration shields — creature stays on battlefield tapped") {
        val card = creatureCard("Troll", playerId)
        val state = battlefieldState(Triple(cardId1, playerId, card))
            .copy(floatingEffects = listOf(regenerationShield(cardId1)))

        val effect = destroyEffect()
        val ctx = context(playerId, "targets", listOf(cardId1))
        val result = executor.execute(state, effect, ctx)

        result.isSuccess shouldBe true
        // Creature stays on battlefield (regenerated)
        result.state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)) shouldContain cardId1
        result.state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)) shouldNotContain cardId1
        // Regenerated creature becomes tapped
        result.state.getEntity(cardId1)!!.has<TappedComponent>() shouldBe true
        // Regeneration shield consumed
        result.state.floatingEffects.size shouldBe 0
        // No zone change event
        result.events.filterIsInstance<ZoneChangeEvent>() shouldBe emptyList()
    }

    test("MoveType.Destroy routes to owner's graveyard, not controller's") {
        // Opponent owns the card but player controls it (e.g., stolen via Control Magic).
        // The card is on player's battlefield but owned by opponent.
        val card = creatureCard("Stolen Bear", opponentId)
        val container = ComponentContainer()
            .with(card)
            .with(OwnerComponent(opponentId))
            .with(ControllerComponent(playerId))
        var state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())
            .withEntity(cardId1, container)
            .addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), cardId1)

        // findCurrentZone searches by ownerId, so also add to opponent's battlefield
        // to simulate how the engine tracks control-changed permanents.
        // In practice, GatherCardsEffect searches all players' battlefields.
        // For this unit test, put the card in the owner's battlefield zone since
        // findCurrentZone only searches ownerId's zones.
        state = state.copy(
            zones = state.zones
                - ZoneKey(playerId, Zone.BATTLEFIELD)
                + (ZoneKey(opponentId, Zone.BATTLEFIELD) to listOf(cardId1))
        )

        val effect = destroyEffect()
        val ctx = context(playerId, "targets", listOf(cardId1))
        val result = executor.execute(state, effect, ctx)

        result.isSuccess shouldBe true
        // Goes to owner's graveyard, not controller's
        result.state.getZone(ZoneKey(opponentId, Zone.GRAVEYARD)) shouldContain cardId1
        result.state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)) shouldNotContain cardId1
        result.state.getZone(ZoneKey(opponentId, Zone.BATTLEFIELD)) shouldNotContain cardId1
    }

    test("MoveType.Destroy strips battlefield components") {
        val card = creatureCard("Bear", playerId)
        val state = battlefieldState(Triple(cardId1, playerId, card))

        val effect = destroyEffect()
        val ctx = context(playerId, "targets", listOf(cardId1))
        val result = executor.execute(state, effect, ctx)

        result.isSuccess shouldBe true
        val container = result.state.getEntity(cardId1)!!
        container.has<ControllerComponent>() shouldBe false
    }

    test("MoveType.Destroy on empty collection is a no-op") {
        val state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        val effect = destroyEffect()
        val ctx = context(playerId, "targets", emptyList())
        val result = executor.execute(state, effect, ctx)

        result.isSuccess shouldBe true
        result.events shouldBe emptyList()
    }

    test("MoveType.Destroy removes floating effects targeting destroyed entity") {
        val card = creatureCard("Bear", playerId)
        val giantGrowthEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.POWER_TOUGHNESS,
                modification = SerializableModification.ModifyPowerToughness(3, 3),
                affectedEntities = setOf(cardId1)
            ),
            duration = Duration.EndOfTurn,
            sourceId = null,
            controllerId = playerId,
            timestamp = 1L
        )
        val state = battlefieldState(Triple(cardId1, playerId, card))
            .copy(floatingEffects = listOf(giantGrowthEffect))

        val effect = destroyEffect()
        val ctx = context(playerId, "targets", listOf(cardId1))
        val result = executor.execute(state, effect, ctx)

        result.isSuccess shouldBe true
        result.state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)) shouldContain cardId1
        // Floating effect targeting destroyed entity is removed
        result.state.floatingEffects.size shouldBe 0
    }
})
