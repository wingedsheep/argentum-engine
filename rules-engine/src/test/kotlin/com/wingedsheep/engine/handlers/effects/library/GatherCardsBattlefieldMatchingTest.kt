package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class GatherCardsBattlefieldMatchingTest : FunSpec({

    val executor = GatherCardsExecutor()

    val playerId = EntityId.generate()
    val opponentId = EntityId.generate()
    val creatureId1 = EntityId.generate()
    val creatureId2 = EntityId.generate()
    val creatureId3 = EntityId.generate()
    val enchantmentId = EntityId.generate()

    fun cardComponent(
        name: String,
        ownerId: EntityId,
        cardTypes: Set<CardType> = setOf(CardType.CREATURE),
        subtypes: Set<Subtype> = emptySet()
    ) = CardComponent(
        cardDefinitionId = name,
        name = name,
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = cardTypes, subtypes = subtypes),
        ownerId = ownerId
    )

    fun battlefieldState(
        vararg permanents: Triple<EntityId, EntityId, CardComponent>
    ): GameState {
        var state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        for ((cardId, controllerId, cardComp) in permanents) {
            val container = ComponentContainer()
                .with(cardComp)
                .with(OwnerComponent(cardComp.ownerId!!))
                .with(ControllerComponent(controllerId))
            state = state.withEntity(cardId, container)
            state = state.addToZone(ZoneKey(controllerId, Zone.BATTLEFIELD), cardId)
        }
        return state
    }

    fun context(controllerId: EntityId) = EffectContext(
        sourceId = null,
        controllerId = controllerId,
    )

    fun gatherEffect(
        filter: GameObjectFilter = GameObjectFilter.Any,
        player: Player = Player.Each
    ) = GatherCardsEffect(
        source = CardSource.BattlefieldMatching(filter = filter, player = player),
        storeAs = "gathered"
    )

    test("gathers all permanents on the battlefield when no filter") {
        val state = battlefieldState(
            Triple(creatureId1, playerId, cardComponent("Bear", playerId)),
            Triple(creatureId2, opponentId, cardComponent("Wolf", opponentId))
        )

        val result = executor.execute(state, gatherEffect(), context(playerId))

        result.isSuccess shouldBe true
        result.updatedCollections["gathered"]!!.shouldContainExactlyInAnyOrder(creatureId1, creatureId2)
    }

    test("filters by card type — only creatures") {
        val creature = cardComponent("Bear", playerId, cardTypes = setOf(CardType.CREATURE))
        val enchantment = cardComponent("Pacifism", playerId, cardTypes = setOf(CardType.ENCHANTMENT))
        val state = battlefieldState(
            Triple(creatureId1, playerId, creature),
            Triple(enchantmentId, playerId, enchantment)
        )

        val filter = GameObjectFilter.Creature
        val result = executor.execute(state, gatherEffect(filter), context(playerId))

        result.isSuccess shouldBe true
        result.updatedCollections["gathered"]!!.shouldContainExactlyInAnyOrder(creatureId1)
    }

    test("filters by subtype") {
        val goblin = cardComponent("Goblin Piker", playerId, subtypes = setOf(Subtype.GOBLIN))
        val elf = cardComponent("Llanowar Elves", playerId, subtypes = setOf(Subtype.ELF))
        val state = battlefieldState(
            Triple(creatureId1, playerId, goblin),
            Triple(creatureId2, playerId, elf)
        )

        val filter = GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN)
        val result = executor.execute(state, gatherEffect(filter), context(playerId))

        result.isSuccess shouldBe true
        result.updatedCollections["gathered"]!!.shouldContainExactlyInAnyOrder(creatureId1)
    }

    test("gathers from both players' battlefields with Player.Each") {
        val state = battlefieldState(
            Triple(creatureId1, playerId, cardComponent("Bear", playerId)),
            Triple(creatureId2, opponentId, cardComponent("Wolf", opponentId)),
            Triple(creatureId3, opponentId, cardComponent("Elk", opponentId))
        )

        val result = executor.execute(state, gatherEffect(), context(playerId))

        result.isSuccess shouldBe true
        result.updatedCollections["gathered"]!!.shouldContainExactlyInAnyOrder(creatureId1, creatureId2, creatureId3)
    }

    test("gathers only controller's permanents with Player.You") {
        val state = battlefieldState(
            Triple(creatureId1, playerId, cardComponent("Bear", playerId)),
            Triple(creatureId2, opponentId, cardComponent("Wolf", opponentId))
        )

        val result = executor.execute(state, gatherEffect(player = Player.You), context(playerId))

        result.isSuccess shouldBe true
        result.updatedCollections["gathered"]!!.shouldContainExactlyInAnyOrder(creatureId1)
    }

    test("returns empty collection when no permanents match") {
        val enchantment = cardComponent("Pacifism", playerId, cardTypes = setOf(CardType.ENCHANTMENT))
        val state = battlefieldState(
            Triple(enchantmentId, playerId, enchantment)
        )

        val filter = GameObjectFilter.Creature
        val result = executor.execute(state, gatherEffect(filter), context(playerId))

        result.isSuccess shouldBe true
        result.updatedCollections["gathered"]!!.shouldBeEmpty()
    }

    test("returns empty collection when battlefield is empty") {
        val state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        val result = executor.execute(state, gatherEffect(), context(playerId))

        result.isSuccess shouldBe true
        result.updatedCollections["gathered"]!!.shouldBeEmpty()
    }

    test("uses projected state — type-changing continuous effect is respected") {
        // Create a land that has a continuous effect making it a creature
        val land = cardComponent("Mishra's Factory", playerId, cardTypes = setOf(CardType.LAND))
        val creature = cardComponent("Bear", playerId, cardTypes = setOf(CardType.CREATURE))
        val state = battlefieldState(
            Triple(creatureId1, playerId, land),
            Triple(creatureId2, playerId, creature)
        ).copy(
            floatingEffects = listOf(
                ActiveFloatingEffect(
                    id = EntityId.generate(),
                    effect = FloatingEffectData(
                        layer = Layer.TYPE,
                        modification = SerializableModification.AddType(CardType.CREATURE.name),
                        affectedEntities = setOf(creatureId1)
                    ),
                    duration = Duration.EndOfTurn,
                    sourceId = null,
                    controllerId = playerId,
                    timestamp = 1L
                )
            )
        )

        // Filter for creatures — should find both the natural creature AND the animated land
        val filter = GameObjectFilter.Creature
        val result = executor.execute(state, gatherEffect(filter), context(playerId))

        result.isSuccess shouldBe true
        result.updatedCollections["gathered"]!!.shouldContainExactlyInAnyOrder(creatureId1, creatureId2)
    }
})
