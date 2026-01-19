package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.ability.ShuffleLibraryEffect
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.core.Supertype
import com.wingedsheep.rulesengine.core.TypeLine
import com.wingedsheep.rulesengine.core.CardType
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.decision.ChooseCards
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.TappedComponent
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class LibraryHandlersTest : FunSpec({

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

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    val elfDef = CardDefinition.creature(
        name = "Llanowar Elves",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype(value = "Elf"), Subtype(value = "Druid")),
        power = 1,
        toughness = 1
    )

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun GameState.addCardToLibrary(
        def: CardDefinition,
        ownerId: EntityId
    ): Pair<EntityId, GameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, ownerId)
        )
        val (cardId, state1) = createEntity(EntityId.generate(), components)
        val libraryZone = ZoneId(ZoneType.LIBRARY, ownerId)
        return cardId to state1.addToZone(cardId, libraryZone)
    }

    val registry = EffectHandlerRegistry.default()

    context("ShuffleLibraryEffect") {
        test("shuffles controller's library") {
            var state = newGame()

            // Add some cards to the library
            val (forest1Id, state1) = state.addCardToLibrary(forestDef, player1Id)
            val (forest2Id, state2) = state1.addCardToLibrary(forestDef, player1Id)
            val (forest3Id, state3) = state2.addCardToLibrary(forestDef, player1Id)
            state = state3

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)
            val originalOrder = state.getZone(libraryZone)
            originalOrder shouldHaveSize 3

            val effect = ShuffleLibraryEffect()
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Library should still have 3 cards
            result.state.getZone(libraryZone) shouldHaveSize 3

            // Should generate shuffle event
            result.events.any { it is EffectEvent.LibraryShuffled } shouldBe true
        }
    }

    context("SearchLibraryEffect") {
        test("finds and moves card matching subtype filter to hand") {
            var state = newGame()

            // Add a Forest and a Plains to the library
            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            val (plainsId, state2) = state1.addCardToLibrary(plainsDef, player1Id)
            state = state2

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)
            val handZone = ZoneId(ZoneType.HAND, player1Id)

            state.getZone(libraryZone) shouldHaveSize 2
            state.getZone(handZone) shouldHaveSize 0

            val effect = SearchLibraryEffect(
                filter = CardFilter.HasSubtype("Forest"),
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = true
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Forest should be in hand
            result.state.getZone(handZone) shouldContain forestId
            // Library should have 1 card (Plains)
            result.state.getZone(libraryZone) shouldHaveSize 1
            // Library should contain Plains, not Forest
            result.state.getZone(libraryZone) shouldContain plainsId
        }

        test("moves card to battlefield when destination is BATTLEFIELD") {
            var state = newGame()

            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            state = state1

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            val effect = SearchLibraryEffect(
                filter = CardFilter.HasSubtype("Forest"),
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                shuffleAfter = false
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Forest should be on battlefield
            result.state.getBattlefield() shouldContain forestId
            // Library should be empty
            result.state.getZone(libraryZone) shouldHaveSize 0
        }

        test("enters battlefield tapped when entersTapped is true") {
            var state = newGame()

            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            state = state1

            val effect = SearchLibraryEffect(
                filter = CardFilter.HasSubtype("Forest"),
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
                shuffleAfter = false
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Forest should be on battlefield and tapped
            result.state.getBattlefield() shouldContain forestId
            result.state.hasComponent<TappedComponent>(forestId).shouldBeTrue()
        }

        test("does not tap when entersTapped is false") {
            var state = newGame()

            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            state = state1

            val effect = SearchLibraryEffect(
                filter = CardFilter.HasSubtype("Forest"),
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = false,
                shuffleAfter = false
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Forest should be on battlefield and NOT tapped
            result.state.getBattlefield() shouldContain forestId
            result.state.hasComponent<TappedComponent>(forestId).shouldBeFalse()
        }

        test("finds creature cards with CreatureCard filter") {
            var state = newGame()

            val (bearId, state1) = state.addCardToLibrary(bearDef, player1Id)
            val (forestId, state2) = state1.addCardToLibrary(forestDef, player1Id)
            state = state2

            val handZone = ZoneId(ZoneType.HAND, player1Id)

            val effect = SearchLibraryEffect(
                filter = CardFilter.CreatureCard,
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = false
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Bear should be in hand (it's a creature)
            result.state.getZone(handZone) shouldContain bearId
            // Forest should still be in library (it's not a creature)
            result.state.getZone(ZoneId(ZoneType.LIBRARY, player1Id)) shouldContain forestId
        }

        test("finds land cards with LandCard filter") {
            var state = newGame()

            val (bearId, state1) = state.addCardToLibrary(bearDef, player1Id)
            val (forestId, state2) = state1.addCardToLibrary(forestDef, player1Id)
            state = state2

            val handZone = ZoneId(ZoneType.HAND, player1Id)

            val effect = SearchLibraryEffect(
                filter = CardFilter.LandCard,
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = false
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Forest should be in hand (it's a land)
            result.state.getZone(handZone) shouldContain forestId
            // Bear should still be in library (it's not a land)
            result.state.getZone(ZoneId(ZoneType.LIBRARY, player1Id)) shouldContain bearId
        }

        test("finds basic land cards with BasicLandCard filter") {
            var state = newGame()

            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            state = state1

            val handZone = ZoneId(ZoneType.HAND, player1Id)

            val effect = SearchLibraryEffect(
                filter = CardFilter.BasicLandCard,
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = false
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Forest should be in hand (it's a basic land)
            result.state.getZone(handZone) shouldContain forestId
        }

        test("respects count limit when multiple matches exist") {
            var state = newGame()

            val (forest1Id, state1) = state.addCardToLibrary(forestDef, player1Id)
            val (forest2Id, state2) = state1.addCardToLibrary(forestDef, player1Id)
            val (forest3Id, state3) = state2.addCardToLibrary(forestDef, player1Id)
            state = state3

            val handZone = ZoneId(ZoneType.HAND, player1Id)
            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            val effect = SearchLibraryEffect(
                filter = CardFilter.HasSubtype("Forest"),
                count = 2,  // Only get 2 of the 3 forests
                destination = SearchDestination.HAND,
                shuffleAfter = false
            )
            val context = ExecutionContext(player1Id, player1Id)
            val initialResult = registry.execute(state, effect, context)

            // With multiple matches, we should get a pending decision
            initialResult.needsPlayerInput shouldBe true
            initialResult.pendingDecision.shouldNotBeNull()
            initialResult.pendingDecision.shouldBeInstanceOf<ChooseCards>()
            initialResult.continuation.shouldNotBeNull()

            val decision = initialResult.pendingDecision as ChooseCards
            decision.cards shouldHaveSize 3  // All 3 forests available to choose
            decision.maxCount shouldBe 2     // Can pick up to 2

            // Player selects forest1 and forest2
            val finalResult = initialResult.continuation!!.resume(listOf(forest1Id, forest2Id))

            // Should have exactly 2 cards in hand
            finalResult.state.getZone(handZone) shouldHaveSize 2
            // Should have 1 card remaining in library
            finalResult.state.getZone(libraryZone) shouldHaveSize 1
        }

        test("handles no matching cards gracefully") {
            var state = newGame()

            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            state = state1

            val handZone = ZoneId(ZoneType.HAND, player1Id)

            val effect = SearchLibraryEffect(
                filter = CardFilter.CreatureCard,  // Looking for creatures, but only have Forest
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = true
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Hand should still be empty (no creatures found)
            result.state.getZone(handZone) shouldHaveSize 0
            // Should still generate search and shuffle events
            result.events.any { it is EffectEvent.LibrarySearched } shouldBe true
            result.events.any { it is EffectEvent.LibraryShuffled } shouldBe true
        }

        test("generates correct events") {
            var state = newGame()

            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            state = state1

            val effect = SearchLibraryEffect(
                filter = CardFilter.HasSubtype("Forest"),
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = true
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Should have search, move, and shuffle events
            result.events.any { it is EffectEvent.LibrarySearched } shouldBe true
            result.events.any { it is EffectEvent.CardMovedToZone } shouldBe true
            result.events.any { it is EffectEvent.LibraryShuffled } shouldBe true
        }

        test("puts card on top of library when destination is TOP_OF_LIBRARY") {
            var state = newGame()

            // Add bear first, then forest
            val (bearId, state1) = state.addCardToLibrary(bearDef, player1Id)
            val (forestId, state2) = state1.addCardToLibrary(forestDef, player1Id)
            state = state2

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            val effect = SearchLibraryEffect(
                filter = CardFilter.CreatureCard,
                count = 1,
                destination = SearchDestination.TOP_OF_LIBRARY,
                shuffleAfter = false  // Don't shuffle so we can verify position
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Bear should be found and moved to top of library
            // Library should contain both cards
            result.state.getZone(libraryZone) shouldHaveSize 2
            result.state.getZone(libraryZone) shouldContain bearId
            result.state.getZone(libraryZone) shouldContain forestId
        }
    }

    context("CardFilter matching") {
        test("And filter requires all conditions to match") {
            // A green creature - should match And(CreatureCard, HasColor(GREEN))
            val greenCreature = bearDef  // Bears are green creatures

            val filter = CardFilter.And(listOf(
                CardFilter.CreatureCard,
                CardFilter.HasColor(Color.GREEN)
            ))

            SearchLibraryHandler.matchesFilter(greenCreature, filter) shouldBe true

            // Forest is green but not a creature - should not match
            SearchLibraryHandler.matchesFilter(forestDef, filter) shouldBe false
        }

        test("Or filter matches if any condition matches") {
            val filter = CardFilter.Or(listOf(
                CardFilter.HasSubtype("Forest"),
                CardFilter.HasSubtype("Plains")
            ))

            SearchLibraryHandler.matchesFilter(forestDef, filter) shouldBe true
            SearchLibraryHandler.matchesFilter(plainsDef, filter) shouldBe true
            SearchLibraryHandler.matchesFilter(bearDef, filter) shouldBe false
        }

        test("AnyCard matches everything") {
            SearchLibraryHandler.matchesFilter(forestDef, CardFilter.AnyCard) shouldBe true
            SearchLibraryHandler.matchesFilter(bearDef, CardFilter.AnyCard) shouldBe true
            SearchLibraryHandler.matchesFilter(elfDef, CardFilter.AnyCard) shouldBe true
        }
    }

    context("Multi-step search flow") {
        test("single match auto-selects without pending decision") {
            var state = newGame()

            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            state = state1

            val handZone = ZoneId(ZoneType.HAND, player1Id)

            val effect = SearchLibraryEffect(
                filter = CardFilter.HasSubtype("Forest"),
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = false
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Single match should auto-select without needing player input
            result.needsPlayerInput shouldBe false
            result.pendingDecision shouldBe null
            result.state.getZone(handZone) shouldContain forestId
        }

        test("multiple matches returns pending decision with card options") {
            var state = newGame()

            val (forest1Id, state1) = state.addCardToLibrary(forestDef, player1Id)
            val (forest2Id, state2) = state1.addCardToLibrary(forestDef, player1Id)
            state = state2

            val effect = SearchLibraryEffect(
                filter = CardFilter.HasSubtype("Forest"),
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = false
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Multiple matches should require player input
            result.needsPlayerInput shouldBe true
            result.pendingDecision.shouldNotBeNull()
            result.pendingDecision.shouldBeInstanceOf<ChooseCards>()

            val decision = result.pendingDecision as ChooseCards
            decision.playerId shouldBe player1Id
            decision.cards shouldHaveSize 2
            decision.cards.map { it.entityId } shouldContain forest1Id
            decision.cards.map { it.entityId } shouldContain forest2Id
            decision.minCount shouldBe 0  // Can fail to find
            decision.maxCount shouldBe 1
            decision.mayChooseNone shouldBe true
        }

        test("player can fail to find even with valid matches") {
            var state = newGame()

            val (forestId, state1) = state.addCardToLibrary(forestDef, player1Id)
            val (forest2Id, state2) = state1.addCardToLibrary(forestDef, player1Id)
            state = state2

            val handZone = ZoneId(ZoneType.HAND, player1Id)
            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            val effect = SearchLibraryEffect(
                filter = CardFilter.HasSubtype("Forest"),
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = false
            )
            val context = ExecutionContext(player1Id, player1Id)
            val initialResult = registry.execute(state, effect, context)

            // Player chooses nothing (fail to find)
            val finalResult = initialResult.continuation!!.resume(emptyList())

            // Both forests should still be in library
            finalResult.state.getZone(libraryZone) shouldHaveSize 2
            finalResult.state.getZone(handZone) shouldHaveSize 0
        }

        test("pending decision includes card display information") {
            var state = newGame()

            val (bearId, state1) = state.addCardToLibrary(bearDef, player1Id)
            val (elfId, state2) = state1.addCardToLibrary(elfDef, player1Id)
            state = state2

            val effect = SearchLibraryEffect(
                filter = CardFilter.CreatureCard,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                shuffleAfter = false
            )
            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            result.needsPlayerInput shouldBe true
            val decision = result.pendingDecision as ChooseCards

            // Card options should include display information
            val bearOption = decision.cards.find { it.entityId == bearId }
            bearOption.shouldNotBeNull()
            bearOption.name shouldBe "Grizzly Bears"

            val elfOption = decision.cards.find { it.entityId == elfId }
            elfOption.shouldNotBeNull()
            elfOption.name shouldBe "Llanowar Elves"
        }

        test("continuation completes with selected cards and shuffles") {
            var state = newGame()

            val (forest1Id, state1) = state.addCardToLibrary(forestDef, player1Id)
            val (forest2Id, state2) = state1.addCardToLibrary(forestDef, player1Id)
            state = state2

            val handZone = ZoneId(ZoneType.HAND, player1Id)

            val effect = SearchLibraryEffect(
                filter = CardFilter.HasSubtype("Forest"),
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = true  // Shuffle after
            )
            val context = ExecutionContext(player1Id, player1Id)
            val initialResult = registry.execute(state, effect, context)

            // Player selects forest1
            val finalResult = initialResult.continuation!!.resume(listOf(forest1Id))

            // Forest1 should be in hand
            finalResult.state.getZone(handZone) shouldContain forest1Id

            // Events should include shuffle
            finalResult.events.any { it is EffectEvent.LibraryShuffled } shouldBe true
        }
    }
})
