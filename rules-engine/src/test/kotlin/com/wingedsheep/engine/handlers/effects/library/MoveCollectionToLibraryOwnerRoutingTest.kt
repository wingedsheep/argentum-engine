package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
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
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Permanents leaving the battlefield go to their owner's zone (Rule 110.5a / 614.10).
 * MoveCollection used to honour this only for HAND, EXILE, and GRAVEYARD; LIBRARY routed
 * every card to a single destination player. These tests cover the LIBRARY case.
 */
class MoveCollectionToLibraryOwnerRoutingTest : FunSpec({

    val cardRegistry = com.wingedsheep.engine.registry.CardRegistry()
    val executor = MoveCollectionExecutor(cardRegistry)

    val playerId = EntityId.generate()
    val opponentId = EntityId.generate()
    val cardId1 = EntityId.generate()
    val cardId2 = EntityId.generate()

    fun creatureCard(name: String, ownerId: EntityId) = CardComponent(
        cardDefinitionId = name,
        name = name,
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
        ownerId = ownerId
    )

    fun emptyState(): GameState = GameState()
        .withEntity(playerId, ComponentContainer())
        .withEntity(opponentId, ComponentContainer())

    /** Place [cardId] on [ownerId]'s battlefield (so findCurrentZone resolves it) but with
     *  [controllerId] as its controller. Mirrors how a control-changed permanent is tracked
     *  in MoveCollectionDestroyTest. */
    fun stateWithStolenCard(
        cardId: EntityId,
        ownerId: EntityId,
        controllerId: EntityId,
        cardName: String = "Stolen Bear"
    ): GameState {
        val card = creatureCard(cardName, ownerId)
        val container = ComponentContainer()
            .with(card)
            .with(OwnerComponent(ownerId))
            .with(ControllerComponent(controllerId))
        return emptyState()
            .withEntity(cardId, container)
            .addToZone(ZoneKey(ownerId, Zone.BATTLEFIELD), cardId)
    }

    fun context(controllerId: EntityId, collectionName: String, cards: List<EntityId>) = EffectContext(
        sourceId = null,
        controllerId = controllerId,
        opponentId = opponentId,
        pipeline = PipelineState(storedCollections = mapOf(collectionName to cards))
    )

    test("Battlefield → library (top): permanent goes to its owner's library, not controller's") {
        val state = stateWithStolenCard(cardId1, ownerId = opponentId, controllerId = playerId)

        val effect = MoveCollectionEffect(
            from = "targets",
            destination = CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Top)
        )
        val result = executor.execute(state, effect, context(playerId, "targets", listOf(cardId1)))

        result.isSuccess shouldBe true
        result.state.getZone(ZoneKey(opponentId, Zone.LIBRARY)) shouldContain cardId1
        result.state.getZone(ZoneKey(playerId, Zone.LIBRARY)) shouldNotContain cardId1
        result.state.getEntity(cardId1)!!.has<ControllerComponent>() shouldBe false
    }

    test("Battlefield → library (shuffled): permanent goes to its owner's library and that library is shuffled") {
        val state = stateWithStolenCard(cardId1, ownerId = opponentId, controllerId = playerId)
            // Give the opponent's library a few extra cards so shuffling is meaningful.
            .copy(zones = mapOf(
                ZoneKey(playerId, Zone.BATTLEFIELD) to emptyList(),
                ZoneKey(playerId, Zone.LIBRARY) to emptyList(),
                ZoneKey(opponentId, Zone.BATTLEFIELD) to listOf(cardId1),
                ZoneKey(opponentId, Zone.LIBRARY) to emptyList()
            ))

        val effect = MoveCollectionEffect(
            from = "targets",
            destination = CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled)
        )
        val result = executor.execute(state, effect, context(playerId, "targets", listOf(cardId1)))

        result.isSuccess shouldBe true
        result.state.getZone(ZoneKey(opponentId, Zone.LIBRARY)) shouldContain cardId1
        result.state.getZone(ZoneKey(playerId, Zone.LIBRARY)) shouldNotContain cardId1

        val shuffleEvents = result.events.filterIsInstance<LibraryShuffledEvent>()
        shuffleEvents.size shouldBe 1
        shuffleEvents.first().playerId shouldBe opponentId
    }

    test("Battlefield → library (shuffled) with mixed owners: each card goes to its owner's library and both libraries shuffle") {
        // P1 controls two permanents: one they own, one stolen from P2.
        val mineCard = creatureCard("My Bear", playerId)
        val stolenCard = creatureCard("Stolen Bear", opponentId)
        val mineContainer = ComponentContainer()
            .with(mineCard)
            .with(OwnerComponent(playerId))
            .with(ControllerComponent(playerId))
        val stolenContainer = ComponentContainer()
            .with(stolenCard)
            .with(OwnerComponent(opponentId))
            .with(ControllerComponent(playerId))

        val state = emptyState()
            .withEntity(cardId1, mineContainer)
            .withEntity(cardId2, stolenContainer)
            .copy(zones = mapOf(
                ZoneKey(playerId, Zone.BATTLEFIELD) to listOf(cardId1),
                ZoneKey(playerId, Zone.LIBRARY) to emptyList(),
                ZoneKey(opponentId, Zone.BATTLEFIELD) to listOf(cardId2),
                ZoneKey(opponentId, Zone.LIBRARY) to emptyList()
            ))

        val effect = MoveCollectionEffect(
            from = "targets",
            destination = CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled)
        )
        val result = executor.execute(state, effect, context(playerId, "targets", listOf(cardId1, cardId2)))

        result.isSuccess shouldBe true
        result.state.getZone(ZoneKey(playerId, Zone.LIBRARY)) shouldContain cardId1
        result.state.getZone(ZoneKey(opponentId, Zone.LIBRARY)) shouldContain cardId2
        result.state.getZone(ZoneKey(playerId, Zone.LIBRARY)) shouldNotContain cardId2
        result.state.getZone(ZoneKey(opponentId, Zone.LIBRARY)) shouldNotContain cardId1

        val shuffledOwners = result.events.filterIsInstance<LibraryShuffledEvent>().map { it.playerId }.toSet()
        shuffledOwners shouldBe setOf(playerId, opponentId)
    }
})
