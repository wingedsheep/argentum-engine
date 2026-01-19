package com.wingedsheep.rulesengine.sets.portal

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.TappedComponent
import com.wingedsheep.rulesengine.ecs.script.EcsEvent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.handler.EffectHandlerRegistry
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Nature's Lore card.
 *
 * Nature's Lore - 1G
 * Sorcery
 * Search your library for a Forest card and put that card onto the battlefield.
 * Then shuffle your library.
 */
class NaturesLoreTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val forestDef = CardDefinition(
        name = "Forest",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            supertypes = setOf(Supertype.BASIC),
            cardTypes = setOf(CardType.LAND),
            subtypes = setOf(Subtype.FOREST)
        ),
        oracleText = "{T}: Add {G}."
    )

    val plainsDef = CardDefinition(
        name = "Plains",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            supertypes = setOf(Supertype.BASIC),
            cardTypes = setOf(CardType.LAND),
            subtypes = setOf(Subtype.PLAINS)
        ),
        oracleText = "{T}: Add {W}."
    )

    // A dual land with Forest subtype (like Tropical Island)
    val tropicalIslandDef = CardDefinition(
        name = "Tropical Island",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            supertypes = emptySet(),
            cardTypes = setOf(CardType.LAND),
            subtypes = setOf(Subtype.FOREST, Subtype.ISLAND)
        ),
        oracleText = "{T}: Add {G} or {U}."
    )

    fun newGame(): EcsGameState = EcsGameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun EcsGameState.addCardToLibrary(
        def: CardDefinition,
        ownerId: EntityId
    ): Pair<EntityId, EcsGameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, ownerId)
        )
        val (cardId, state1) = createEntity(EntityId.generate(), components)
        val libraryZone = ZoneId(ZoneType.LIBRARY, ownerId)
        return cardId to state1.addToZone(cardId, libraryZone)
    }

    val registry = EffectHandlerRegistry.default()

    // Helper to create the Nature's Lore effect
    fun naturesLoreEffect() = SearchLibraryEffect(
        filter = CardFilter.HasSubtype("Forest"),
        count = 1,
        destination = SearchDestination.BATTLEFIELD,
        entersTapped = false,
        shuffleAfter = true
    )

    context("Nature's Lore") {
        test("searches library for Forest and puts it onto battlefield") {
            var state = newGame()

            // Add a Forest and a Plains to the library
            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            val (plainsId, state2) = state1.addCardToLibrary(plainsDef, player1Id)
            state = state2

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)
            state.getZone(libraryZone) shouldHaveSize 2

            val effect = naturesLoreEffect()
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Forest should be on battlefield
            result.state.getBattlefield() shouldContain forestId
            // Plains should still be in library (it's not a Forest)
            result.state.getZone(libraryZone) shouldContain plainsId
            // Forest should NOT be in library anymore
            result.state.getZone(libraryZone) shouldNotContain forestId
        }

        test("Forest enters battlefield untapped") {
            var state = newGame()

            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            state = state1

            val effect = naturesLoreEffect()
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Forest should be on battlefield and NOT tapped
            result.state.getBattlefield() shouldContain forestId
            result.state.hasComponent<TappedComponent>(forestId).shouldBeFalse()
        }

        test("shuffles library after searching") {
            var state = newGame()

            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            state = state1

            val effect = naturesLoreEffect()
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Should generate shuffle event
            result.events.any { it is EcsEvent.LibraryShuffled } shouldBe true
        }

        test("can find dual lands with Forest subtype") {
            var state = newGame()

            // Add a Tropical Island (has Forest subtype) and a Plains
            val (tropicalId, state1) = state.addCardToLibrary(tropicalIslandDef, player1Id)
            val (plainsId, state2) = state1.addCardToLibrary(plainsDef, player1Id)
            state = state2

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            val effect = naturesLoreEffect()
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Tropical Island should be on battlefield (it has Forest subtype)
            result.state.getBattlefield() shouldContain tropicalId
            // Plains should still be in library
            result.state.getZone(libraryZone) shouldContain plainsId
        }

        test("handles no Forest in library gracefully") {
            var state = newGame()

            // Only add Plains - no Forests
            val (plainsId, state1) = state.addCardToLibrary(plainsDef, player1Id)
            state = state1

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            val effect = naturesLoreEffect()
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Battlefield should be empty (no Forest found)
            result.state.getBattlefield().filter { entityId ->
                result.state.getEntity(entityId)?.get<CardComponent>()?.definition?.name == "Forest" ||
                result.state.getEntity(entityId)?.get<CardComponent>()?.definition?.name == "Tropical Island"
            } shouldHaveSize 0

            // Plains should still be in library
            result.state.getZone(libraryZone) shouldContain plainsId

            // Should still shuffle (even if nothing found)
            result.events.any { it is EcsEvent.LibraryShuffled } shouldBe true
        }

        test("generates correct events") {
            var state = newGame()

            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            state = state1

            val effect = naturesLoreEffect()
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Should have search event
            result.events.any { it is EcsEvent.LibrarySearched } shouldBe true

            // Should have card moved event
            result.events.any { it is EcsEvent.CardMovedToZone } shouldBe true

            // Should have shuffle event
            result.events.any { it is EcsEvent.LibraryShuffled } shouldBe true
        }
    }

    context("Card Registration") {
        test("Nature's Lore is registered in PortalSet") {
            val naturesLore = PortalSet.getCardDefinition("Nature's Lore")
            naturesLore.shouldNotBeNull()
            naturesLore.name shouldBe "Nature's Lore"
            naturesLore.manaCost.toString() shouldBe "{1}{G}"
            naturesLore.isSorcery shouldBe true
        }

        test("Nature's Lore has correct script") {
            val script = PortalSet.getCardScript("Nature's Lore")
            script.shouldNotBeNull()
            script.spellEffect.shouldNotBeNull()

            val effect = script.spellEffect!!.effect
            effect.shouldBeInstanceOf<SearchLibraryEffect>()

            val searchEffect = effect as SearchLibraryEffect
            searchEffect.filter.shouldBeInstanceOf<CardFilter.HasSubtype>()
            (searchEffect.filter as CardFilter.HasSubtype).subtype shouldBe "Forest"
            searchEffect.destination shouldBe SearchDestination.BATTLEFIELD
            searchEffect.entersTapped shouldBe false
            searchEffect.shuffleAfter shouldBe true
        }
    }
})
