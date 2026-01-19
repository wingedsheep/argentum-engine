package com.wingedsheep.rulesengine.sets.portal

import com.wingedsheep.rulesengine.ability.AdditionalCost
import com.wingedsheep.rulesengine.ability.AdditionalCostPayment
import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.action.GameActionHandler
import com.wingedsheep.rulesengine.ecs.action.GameActionResult
import com.wingedsheep.rulesengine.ecs.action.CastSpell
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.handler.EffectHandlerRegistry
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Natural Order card.
 *
 * Natural Order - 2GG
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a green creature.
 * Search your library for a green creature card and put it onto the battlefield.
 * Then shuffle your library.
 */
class NaturalOrderTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    // Green creatures (can be sacrificed and searched for)
    val llanowarElvesDef = CardDefinition.creature(
        name = "Llanowar Elves",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype(value = "Elf"), Subtype(value = "Druid")),
        power = 1,
        toughness = 1
    )

    val elvishMysticDef = CardDefinition.creature(
        name = "Elvish Mystic",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype(value = "Elf"), Subtype(value = "Druid")),
        power = 1,
        toughness = 1
    )

    // Big green creature to search for
    val craterhofBehemothDef = CardDefinition.creature(
        name = "Craterhoof Behemoth",
        manaCost = ManaCost.parse("{5}{G}{G}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 5,
        toughness = 5,
        keywords = setOf(Keyword.HASTE)
    )

    // Non-green creature (cannot be sacrificed for Natural Order)
    val savannnahLionsDef = CardDefinition.creature(
        name = "Savannah Lions",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype.CAT),
        power = 2,
        toughness = 1
    )

    // Natural Order spell definition (for hand)
    val naturalOrderDef = CardDefinition.sorcery(
        name = "Natural Order",
        manaCost = ManaCost.parse("{2}{G}{G}"),
        oracleText = "As an additional cost to cast this spell, sacrifice a green creature.\n" +
                "Search your library for a green creature card and put it onto the battlefield. " +
                "Then shuffle your library."
    )

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun GameState.addCreatureToBattlefield(
        def: CardDefinition,
        controllerId: EntityId
    ): Pair<EntityId, GameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        val (creatureId, state1) = createEntity(EntityId.generate(), components)
        return creatureId to state1.addToZone(creatureId, ZoneId.BATTLEFIELD)
    }

    fun GameState.addCardToHand(
        def: CardDefinition,
        ownerId: EntityId
    ): Pair<EntityId, GameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, ownerId)
        )
        val (cardId, state1) = createEntity(EntityId.generate(), components)
        val handZone = ZoneId(ZoneType.HAND, ownerId)
        return cardId to state1.addToZone(cardId, handZone)
    }

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

    val handler = GameActionHandler()
    val registry = EffectHandlerRegistry.default()

    context("Card Registration") {
        test("Natural Order is registered in PortalSet") {
            val naturalOrder = PortalSet.getCardDefinition("Natural Order")
            naturalOrder.shouldNotBeNull()
            naturalOrder.name shouldBe "Natural Order"
            naturalOrder.manaCost.toString() shouldBe "{2}{G}{G}"
            naturalOrder.isSorcery shouldBe true
        }

        test("Natural Order has sacrifice cost") {
            val script = PortalSet.getCardScript("Natural Order")
            script.shouldNotBeNull()
            script.additionalCosts shouldHaveSize 1

            val cost = script.additionalCosts[0]
            cost.shouldBeInstanceOf<AdditionalCost.SacrificePermanent>()

            val sacrificeCost = cost as AdditionalCost.SacrificePermanent
            sacrificeCost.filter.shouldBeInstanceOf<CardFilter.And>()
            sacrificeCost.count shouldBe 1
        }

        test("Natural Order has search effect") {
            val script = PortalSet.getCardScript("Natural Order")
            script.shouldNotBeNull()
            script.spellEffect.shouldNotBeNull()

            val effect = script.spellEffect!!.effect
            effect.shouldBeInstanceOf<SearchLibraryEffect>()

            val searchEffect = effect as SearchLibraryEffect
            searchEffect.filter.shouldBeInstanceOf<CardFilter.And>()
            searchEffect.destination shouldBe SearchDestination.BATTLEFIELD
            searchEffect.shuffleAfter shouldBe true
        }
    }

    context("Casting Natural Order") {
        test("sacrifices green creature and searches for green creature") {
            var state = newGame()

            // Add Llanowar Elves to battlefield (to sacrifice)
            val (elvesId, state1) = state.addCreatureToBattlefield(llanowarElvesDef, player1Id)
            state = state1

            // Add Craterhoof Behemoth to library (to search for)
            val (craterhofId, state2) = state.addCardToLibrary(craterhofBehemothDef, player1Id)
            state = state2

            // Add Natural Order to hand
            val (naturalOrderId, state3) = state.addCardToHand(naturalOrderDef, player1Id)
            state = state3

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)
            val handZone = ZoneId(ZoneType.HAND, player1Id)

            // Verify initial state
            state.getBattlefield() shouldContain elvesId
            state.getZone(libraryZone) shouldContain craterhofId
            state.getZone(handZone) shouldContain naturalOrderId

            // Cast Natural Order, sacrificing the Elves
            val payment = AdditionalCostPayment(
                sacrificedPermanents = listOf(elvesId)
            )
            val action = CastSpell(
                cardId = naturalOrderId,
                casterId = player1Id,
                fromZone = handZone,
                additionalCostPayment = payment
            )

            val result = handler.execute(state, action)
            result.shouldBeInstanceOf<GameActionResult.Success>()

            val newState = (result as GameActionResult.Success).state

            // Elves should be in graveyard (sacrificed)
            newState.getBattlefield() shouldNotContain elvesId
            newState.getGraveyard(player1Id) shouldContain elvesId

            // Natural Order should be on stack
            newState.getStack() shouldContain naturalOrderId

            // Note: The search effect would resolve when the spell resolves from the stack
            // For now we just verify the sacrifice happened and the spell went to stack
        }

        test("search effect finds green creature and puts on battlefield") {
            var state = newGame()

            // Add creatures to library
            val (craterhofId, state1) = state.addCardToLibrary(craterhofBehemothDef, player1Id)
            val (elvesId, state2) = state1.addCardToLibrary(llanowarElvesDef, player1Id)
            state = state2

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            // The green creature filter used by Natural Order
            val greenCreatureFilter = CardFilter.And(listOf(
                CardFilter.CreatureCard,
                CardFilter.HasColor(Color.GREEN)
            ))

            // Execute search effect directly (simulating spell resolution)
            val effect = SearchLibraryEffect(
                filter = greenCreatureFilter,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = false,
                shuffleAfter = true
            )

            val context = ExecutionContext(player1Id, player1Id)
            val initialResult = registry.execute(state, effect, context)

            // With multiple matches, we get a pending decision
            initialResult.needsPlayerInput shouldBe true
            initialResult.continuation.shouldNotBeNull()

            // Player chooses Craterhoof
            val finalResult = initialResult.continuation!!.resume(listOf(craterhofId))

            // Craterhoof should be on battlefield
            val battlefield = finalResult.state.getBattlefield()
            val greenCreaturesOnBattlefield = battlefield.filter { id ->
                finalResult.state.getEntity(id)?.get<CardComponent>()?.definition?.let { def ->
                    def.isCreature && def.colors.contains(Color.GREEN)
                } ?: false
            }
            greenCreaturesOnBattlefield shouldHaveSize 1
            battlefield shouldContain craterhofId
        }

        test("does not find non-green creatures") {
            var state = newGame()

            // Add only a white creature to library
            val (lionsId, state1) = state.addCardToLibrary(savannnahLionsDef, player1Id)
            state = state1

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            // The green creature filter
            val greenCreatureFilter = CardFilter.And(listOf(
                CardFilter.CreatureCard,
                CardFilter.HasColor(Color.GREEN)
            ))

            val effect = SearchLibraryEffect(
                filter = greenCreatureFilter,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = false,
                shuffleAfter = false
            )

            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Lions should still be in library (not green)
            result.state.getZone(libraryZone) shouldContain lionsId

            // Battlefield should not have the lions
            result.state.getBattlefield() shouldNotContain lionsId
        }
    }

    context("Full Natural Order flow simulation") {
        test("complete flow: sacrifice elf, search library, get big creature") {
            var state = newGame()

            // Setup: Elvish Mystic on battlefield, Craterhoof in library
            val (mysticId, state1) = state.addCreatureToBattlefield(elvishMysticDef, player1Id)
            val (craterhofId, state2) = state1.addCardToLibrary(craterhofBehemothDef, player1Id)
            val (naturalOrderId, state3) = state2.addCardToHand(naturalOrderDef, player1Id)
            state = state3

            val handZone = ZoneId(ZoneType.HAND, player1Id)
            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            // Step 1: Cast Natural Order with sacrifice payment
            val payment = AdditionalCostPayment(sacrificedPermanents = listOf(mysticId))
            val castAction = CastSpell(
                cardId = naturalOrderId,
                casterId = player1Id,
                fromZone = handZone,
                additionalCostPayment = payment
            )

            val castResult = handler.execute(state, castAction)
            castResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (castResult as GameActionResult.Success).state

            // Verify: Mystic sacrificed, Natural Order on stack
            state.getBattlefield() shouldNotContain mysticId
            state.getGraveyard(player1Id) shouldContain mysticId
            state.getStack() shouldContain naturalOrderId

            // Step 2: Simulate spell resolution by executing the search effect
            val greenCreatureFilter = CardFilter.And(listOf(
                CardFilter.CreatureCard,
                CardFilter.HasColor(Color.GREEN)
            ))
            val searchEffect = SearchLibraryEffect(
                filter = greenCreatureFilter,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = false,
                shuffleAfter = true
            )

            val context = ExecutionContext(player1Id, naturalOrderId)
            val searchResult = registry.execute(state, searchEffect, context)

            // Verify: Craterhoof is now on battlefield
            searchResult.state.getBattlefield() shouldContain craterhofId
            searchResult.state.getZone(libraryZone) shouldNotContain craterhofId
        }
    }

    context("Player selection with multiple green creatures") {
        test("player can choose Craterhoof when both Craterhoof and Llanowar Elves are in library") {
            var state = newGame()

            // Add both green creatures to library
            val (craterhofId, state1) = state.addCardToLibrary(craterhofBehemothDef, player1Id)
            val (elvesId, state2) = state1.addCardToLibrary(llanowarElvesDef, player1Id)
            state = state2

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            // Verify both are in library
            state.getZone(libraryZone) shouldContain craterhofId
            state.getZone(libraryZone) shouldContain elvesId

            // Execute search with explicit selection of Craterhoof
            val greenCreatureFilter = CardFilter.And(listOf(
                CardFilter.CreatureCard,
                CardFilter.HasColor(Color.GREEN)
            ))
            val effect = SearchLibraryEffect(
                filter = greenCreatureFilter,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = false,
                shuffleAfter = false,
                selectedCardIds = listOf(craterhofId)  // Player selects Craterhoof
            )

            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Craterhoof should be on battlefield (selected)
            result.state.getBattlefield() shouldContain craterhofId

            // Llanowar Elves should still be in library (not selected)
            result.state.getZone(libraryZone) shouldContain elvesId
        }

        test("player can choose Llanowar Elves when both Craterhoof and Llanowar Elves are in library") {
            var state = newGame()

            // Add both green creatures to library
            val (craterhofId, state1) = state.addCardToLibrary(craterhofBehemothDef, player1Id)
            val (elvesId, state2) = state1.addCardToLibrary(llanowarElvesDef, player1Id)
            state = state2

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            // Execute search with explicit selection of Elves
            val greenCreatureFilter = CardFilter.And(listOf(
                CardFilter.CreatureCard,
                CardFilter.HasColor(Color.GREEN)
            ))
            val effect = SearchLibraryEffect(
                filter = greenCreatureFilter,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = false,
                shuffleAfter = false,
                selectedCardIds = listOf(elvesId)  // Player selects Elves
            )

            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Elves should be on battlefield (selected)
            result.state.getBattlefield() shouldContain elvesId

            // Craterhoof should still be in library (not selected)
            result.state.getZone(libraryZone) shouldContain craterhofId
        }

        test("selecting non-green creature is ignored") {
            var state = newGame()

            // Add one green creature and one white creature
            val (craterhofId, state1) = state.addCardToLibrary(craterhofBehemothDef, player1Id)
            val (lionsId, state2) = state1.addCardToLibrary(savannnahLionsDef, player1Id)
            state = state2

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            // Try to select the white creature (should be ignored)
            val greenCreatureFilter = CardFilter.And(listOf(
                CardFilter.CreatureCard,
                CardFilter.HasColor(Color.GREEN)
            ))
            val effect = SearchLibraryEffect(
                filter = greenCreatureFilter,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = false,
                shuffleAfter = false,
                selectedCardIds = listOf(lionsId)  // Player tries to select white creature
            )

            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Lions should still be in library (selection invalid - not green)
            result.state.getZone(libraryZone) shouldContain lionsId

            // Craterhoof should still be in library (wasn't selected)
            result.state.getZone(libraryZone) shouldContain craterhofId

            // Battlefield should be empty (no valid selection was made)
            val greenCreaturesOnBattlefield = result.state.getBattlefield().filter { id ->
                result.state.getEntity(id)?.get<CardComponent>()?.definition?.let { def ->
                    def.isCreature && def.colors.contains(Color.GREEN)
                } ?: false
            }
            greenCreaturesOnBattlefield shouldHaveSize 0
        }

        test("player can choose from three green creatures") {
            var state = newGame()

            // Add three green creatures
            val (craterhofId, state1) = state.addCardToLibrary(craterhofBehemothDef, player1Id)
            val (elvesId, state2) = state1.addCardToLibrary(llanowarElvesDef, player1Id)
            val (mysticId, state3) = state2.addCardToLibrary(elvishMysticDef, player1Id)
            state = state3

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            // All three should be in library
            state.getZone(libraryZone) shouldContain craterhofId
            state.getZone(libraryZone) shouldContain elvesId
            state.getZone(libraryZone) shouldContain mysticId

            // Select Elvish Mystic specifically
            val greenCreatureFilter = CardFilter.And(listOf(
                CardFilter.CreatureCard,
                CardFilter.HasColor(Color.GREEN)
            ))
            val effect = SearchLibraryEffect(
                filter = greenCreatureFilter,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = false,
                shuffleAfter = false,
                selectedCardIds = listOf(mysticId)  // Player selects Mystic
            )

            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Mystic should be on battlefield
            result.state.getBattlefield() shouldContain mysticId

            // Other two should still be in library
            result.state.getZone(libraryZone) shouldContain craterhofId
            result.state.getZone(libraryZone) shouldContain elvesId
        }
    }
})
