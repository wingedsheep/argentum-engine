package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
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
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class FilterCollectionMatchesFilterTest : FunSpec({

    val executor = FilterCollectionExecutor()

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

    fun context(
        controllerId: EntityId,
        collection: Map<String, List<EntityId>> = emptyMap()
    ) = EffectContext(
        sourceId = null,
        controllerId = controllerId,
        pipeline = PipelineState(storedCollections = collection)
    )

    fun filterEffect(
        filter: GameObjectFilter,
        from: String = "gathered",
        storeMatching: String = "matching",
        storeNonMatching: String? = "nonMatching"
    ) = FilterCollectionEffect(
        from = from,
        filter = CollectionFilter.MatchesFilter(filter),
        storeMatching = storeMatching,
        storeNonMatching = storeNonMatching
    )

    test("filters by card type — keeps only creatures") {
        val creature = cardComponent("Bear", playerId, cardTypes = setOf(CardType.CREATURE))
        val enchantment = cardComponent("Pacifism", playerId, cardTypes = setOf(CardType.ENCHANTMENT))
        val state = battlefieldState(
            Triple(creatureId1, playerId, creature),
            Triple(enchantmentId, playerId, enchantment)
        )

        val ctx = context(playerId, mapOf("gathered" to listOf(creatureId1, enchantmentId)))
        val result = executor.execute(state, filterEffect(GameObjectFilter.Creature), ctx)

        result.isSuccess shouldBe true
        result.updatedCollections["matching"]!!.shouldContainExactlyInAnyOrder(creatureId1)
        result.updatedCollections["nonMatching"]!!.shouldContainExactlyInAnyOrder(enchantmentId)
    }

    test("filters by subtype") {
        val goblin = cardComponent("Goblin Piker", playerId, subtypes = setOf(Subtype.GOBLIN))
        val elf = cardComponent("Llanowar Elves", playerId, subtypes = setOf(Subtype.ELF))
        val state = battlefieldState(
            Triple(creatureId1, playerId, goblin),
            Triple(creatureId2, playerId, elf)
        )

        val filter = GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN)
        val ctx = context(playerId, mapOf("gathered" to listOf(creatureId1, creatureId2)))
        val result = executor.execute(state, filterEffect(filter), ctx)

        result.isSuccess shouldBe true
        result.updatedCollections["matching"]!!.shouldContainExactlyInAnyOrder(creatureId1)
        result.updatedCollections["nonMatching"]!!.shouldContainExactlyInAnyOrder(creatureId2)
    }

    test("all entities match — non-matching is empty") {
        val state = battlefieldState(
            Triple(creatureId1, playerId, cardComponent("Bear", playerId)),
            Triple(creatureId2, playerId, cardComponent("Wolf", playerId))
        )

        val ctx = context(playerId, mapOf("gathered" to listOf(creatureId1, creatureId2)))
        val result = executor.execute(state, filterEffect(GameObjectFilter.Creature), ctx)

        result.isSuccess shouldBe true
        result.updatedCollections["matching"]!!.shouldContainExactlyInAnyOrder(creatureId1, creatureId2)
        result.updatedCollections["nonMatching"]!!.shouldBeEmpty()
    }

    test("no entities match — matching is empty") {
        val state = battlefieldState(
            Triple(enchantmentId, playerId, cardComponent("Pacifism", playerId, cardTypes = setOf(CardType.ENCHANTMENT)))
        )

        val ctx = context(playerId, mapOf("gathered" to listOf(enchantmentId)))
        val result = executor.execute(state, filterEffect(GameObjectFilter.Creature), ctx)

        result.isSuccess shouldBe true
        result.updatedCollections["matching"]!!.shouldBeEmpty()
        result.updatedCollections["nonMatching"]!!.shouldContainExactlyInAnyOrder(enchantmentId)
    }

    test("non-matching collection is not stored when storeNonMatching is null") {
        val state = battlefieldState(
            Triple(creatureId1, playerId, cardComponent("Bear", playerId)),
            Triple(enchantmentId, playerId, cardComponent("Pacifism", playerId, cardTypes = setOf(CardType.ENCHANTMENT)))
        )

        val effect = FilterCollectionEffect(
            from = "gathered",
            filter = CollectionFilter.MatchesFilter(GameObjectFilter.Creature),
            storeMatching = "matching",
            storeNonMatching = null
        )
        val ctx = context(playerId, mapOf("gathered" to listOf(creatureId1, enchantmentId)))
        val result = executor.execute(state, effect, ctx)

        result.isSuccess shouldBe true
        result.updatedCollections["matching"]!!.shouldContainExactlyInAnyOrder(creatureId1)
        result.updatedCollections.containsKey("nonMatching") shouldBe false
    }

    test("uses projected state — type-changing continuous effect is respected") {
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

        val ctx = context(playerId, mapOf("gathered" to listOf(creatureId1, creatureId2)))
        val result = executor.execute(state, filterEffect(GameObjectFilter.Creature), ctx)

        result.isSuccess shouldBe true
        result.updatedCollections["matching"]!!.shouldContainExactlyInAnyOrder(creatureId1, creatureId2)
    }

    test("empty collection input produces empty results") {
        val state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        val ctx = context(playerId, mapOf("gathered" to emptyList()))
        val result = executor.execute(state, filterEffect(GameObjectFilter.Creature), ctx)

        result.isSuccess shouldBe true
        result.updatedCollections["matching"]!!.shouldBeEmpty()
        result.updatedCollections["nonMatching"]!!.shouldBeEmpty()
    }

    test("returns error when source collection does not exist") {
        val state = GameState()
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())

        val ctx = context(playerId)
        val result = executor.execute(state, filterEffect(GameObjectFilter.Creature), ctx)

        result.isSuccess shouldBe false
    }
})
