package com.wingedsheep.rulesengine.sets.portal

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.ConditionalEffect
import com.wingedsheep.rulesengine.ability.OpponentControlsMoreLands
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.handler.EcsConditionEvaluator
import com.wingedsheep.rulesengine.ecs.script.handler.EffectHandlerRegistry
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Gift of Estates card and OpponentControlsMoreLands condition.
 *
 * Gift of Estates - 1W
 * Sorcery
 * If an opponent controls more lands than you, search your library for up to
 * three Plains cards, reveal them, put them into your hand, then shuffle.
 */
class GiftOfEstatesTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val plainsDef = CardDefinition.basicLand("Plains", Subtype.PLAINS)
    val forestDef = CardDefinition.basicLand("Forest", Subtype.FOREST)

    // Non-basic Plains (like Tundra or Temple of Enlightenment)
    val templeDef = CardDefinition(
        name = "Temple of Enlightenment",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            cardTypes = setOf(CardType.LAND),
            subtypes = setOf(Subtype.PLAINS, Subtype(value = "Island"))
        ),
        oracleText = "{T}: Add {W} or {U}."
    )

    fun newGame(): EcsGameState = EcsGameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun EcsGameState.addLandToBattlefield(
        def: CardDefinition,
        controllerId: EntityId
    ): Pair<EntityId, EcsGameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        val (landId, state1) = createEntity(EntityId.generate(), components)
        return landId to state1.addToZone(landId, ZoneId.BATTLEFIELD)
    }

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

    context("OpponentControlsMoreLands condition") {
        test("returns false when player controls more lands") {
            var state = newGame()

            // Player 1 has 3 lands, Player 2 has 1 land
            val (_, state1) = state.addLandToBattlefield(plainsDef, player1Id)
            val (_, state2) = state1.addLandToBattlefield(plainsDef, player1Id)
            val (_, state3) = state2.addLandToBattlefield(forestDef, player1Id)
            val (_, state4) = state3.addLandToBattlefield(forestDef, player2Id)
            state = state4

            val context = ExecutionContext(player1Id, player1Id)
            val result = EcsConditionEvaluator.evaluate(state, OpponentControlsMoreLands, context)

            result.shouldBeFalse()
        }

        test("returns false when land counts are equal") {
            var state = newGame()

            // Both players have 2 lands
            val (_, state1) = state.addLandToBattlefield(plainsDef, player1Id)
            val (_, state2) = state1.addLandToBattlefield(forestDef, player1Id)
            val (_, state3) = state2.addLandToBattlefield(forestDef, player2Id)
            val (_, state4) = state3.addLandToBattlefield(forestDef, player2Id)
            state = state4

            val context = ExecutionContext(player1Id, player1Id)
            val result = EcsConditionEvaluator.evaluate(state, OpponentControlsMoreLands, context)

            result.shouldBeFalse()
        }

        test("returns true when opponent controls more lands") {
            var state = newGame()

            // Player 1 has 1 land, Player 2 has 3 lands
            val (_, state1) = state.addLandToBattlefield(plainsDef, player1Id)
            val (_, state2) = state1.addLandToBattlefield(forestDef, player2Id)
            val (_, state3) = state2.addLandToBattlefield(forestDef, player2Id)
            val (_, state4) = state3.addLandToBattlefield(forestDef, player2Id)
            state = state4

            val context = ExecutionContext(player1Id, player1Id)
            val result = EcsConditionEvaluator.evaluate(state, OpponentControlsMoreLands, context)

            result.shouldBeTrue()
        }

        test("returns true when player has no lands and opponent has some") {
            var state = newGame()

            // Player 1 has 0 lands, Player 2 has 1 land
            val (_, state1) = state.addLandToBattlefield(forestDef, player2Id)
            state = state1

            val context = ExecutionContext(player1Id, player1Id)
            val result = EcsConditionEvaluator.evaluate(state, OpponentControlsMoreLands, context)

            result.shouldBeTrue()
        }
    }

    context("Gift of Estates card registration") {
        test("is registered in PortalSet") {
            val giftOfEstates = PortalSet.getCardDefinition("Gift of Estates")
            giftOfEstates.shouldNotBeNull()
            giftOfEstates.name shouldBe "Gift of Estates"
            giftOfEstates.manaCost.toString() shouldBe "{1}{W}"
            giftOfEstates.isSorcery shouldBe true
        }

        test("has correct conditional effect") {
            val script = PortalSet.getCardScript("Gift of Estates")
            script.shouldNotBeNull()
            script.spellEffect.shouldNotBeNull()

            val effect = script.spellEffect!!.effect
            effect.shouldBeInstanceOf<ConditionalEffect>()

            val conditionalEffect = effect as ConditionalEffect
            conditionalEffect.condition shouldBe OpponentControlsMoreLands
            conditionalEffect.effect.shouldBeInstanceOf<SearchLibraryEffect>()

            val searchEffect = conditionalEffect.effect as SearchLibraryEffect
            searchEffect.filter.shouldBeInstanceOf<CardFilter.HasSubtype>()
            (searchEffect.filter as CardFilter.HasSubtype).subtype shouldBe "Plains"
            searchEffect.count shouldBe 3
            searchEffect.destination shouldBe SearchDestination.HAND
            searchEffect.reveal shouldBe true
            searchEffect.shuffleAfter shouldBe true
        }
    }

    context("Gift of Estates effect execution") {
        test("does nothing when opponent doesn't control more lands") {
            var state = newGame()

            // Player 1 has 2 lands, Player 2 has 1 land
            val (_, state1) = state.addLandToBattlefield(plainsDef, player1Id)
            val (_, state2) = state1.addLandToBattlefield(forestDef, player1Id)
            val (_, state3) = state2.addLandToBattlefield(forestDef, player2Id)
            state = state3

            // Add Plains to library
            val (plains1Id, state4) = state.addCardToLibrary(plainsDef, player1Id)
            state = state4

            val handZone = ZoneId(ZoneType.HAND, player1Id)
            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            // Execute Gift of Estates effect
            val effect = ConditionalEffect(
                condition = OpponentControlsMoreLands,
                effect = SearchLibraryEffect(
                    filter = CardFilter.HasSubtype("Plains"),
                    count = 3,
                    destination = SearchDestination.HAND,
                    shuffleAfter = true,
                    reveal = true
                )
            )

            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Plains should still be in library (condition not met)
            result.state.getZone(libraryZone) shouldContain plains1Id
            result.state.getZone(handZone) shouldHaveSize 0
        }

        test("searches for Plains when opponent controls more lands") {
            var state = newGame()

            // Player 1 has 1 land, Player 2 has 3 lands
            val (_, state1) = state.addLandToBattlefield(forestDef, player1Id)
            val (_, state2) = state1.addLandToBattlefield(forestDef, player2Id)
            val (_, state3) = state2.addLandToBattlefield(forestDef, player2Id)
            val (_, state4) = state3.addLandToBattlefield(forestDef, player2Id)
            state = state4

            // Add Plains to library
            val (plains1Id, state5) = state.addCardToLibrary(plainsDef, player1Id)
            state = state5

            val handZone = ZoneId(ZoneType.HAND, player1Id)

            // Execute Gift of Estates effect
            val effect = ConditionalEffect(
                condition = OpponentControlsMoreLands,
                effect = SearchLibraryEffect(
                    filter = CardFilter.HasSubtype("Plains"),
                    count = 3,
                    destination = SearchDestination.HAND,
                    shuffleAfter = true,
                    reveal = true,
                    selectedCardIds = listOf(plains1Id)  // Explicit selection for test
                )
            )

            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Plains should be in hand
            result.state.getZone(handZone) shouldContain plains1Id
        }

        test("can find up to three Plains") {
            var state = newGame()

            // Opponent has more lands
            val (_, state1) = state.addLandToBattlefield(forestDef, player2Id)
            val (_, state2) = state1.addLandToBattlefield(forestDef, player2Id)
            state = state2

            // Add 4 Plains to library
            val (plains1Id, state3) = state.addCardToLibrary(plainsDef, player1Id)
            val (plains2Id, state4) = state3.addCardToLibrary(plainsDef, player1Id)
            val (plains3Id, state5) = state4.addCardToLibrary(plainsDef, player1Id)
            val (plains4Id, state6) = state5.addCardToLibrary(plainsDef, player1Id)
            state = state6

            val handZone = ZoneId(ZoneType.HAND, player1Id)
            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)

            // Execute search with explicit selection of 3 Plains
            val effect = ConditionalEffect(
                condition = OpponentControlsMoreLands,
                effect = SearchLibraryEffect(
                    filter = CardFilter.HasSubtype("Plains"),
                    count = 3,
                    destination = SearchDestination.HAND,
                    shuffleAfter = false,  // Don't shuffle for easier testing
                    reveal = true,
                    selectedCardIds = listOf(plains1Id, plains2Id, plains3Id)
                )
            )

            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // 3 Plains should be in hand
            result.state.getZone(handZone) shouldHaveSize 3
            result.state.getZone(handZone) shouldContain plains1Id
            result.state.getZone(handZone) shouldContain plains2Id
            result.state.getZone(handZone) shouldContain plains3Id

            // 1 Plains should remain in library
            result.state.getZone(libraryZone) shouldContain plains4Id
        }

        test("can find non-basic lands with Plains type") {
            var state = newGame()

            // Opponent has more lands
            val (_, state1) = state.addLandToBattlefield(forestDef, player2Id)
            val (_, state2) = state1.addLandToBattlefield(forestDef, player2Id)
            state = state2

            // Add Temple of Enlightenment (non-basic Plains) to library
            val (templeId, state3) = state.addCardToLibrary(templeDef, player1Id)
            state = state3

            val handZone = ZoneId(ZoneType.HAND, player1Id)

            // Execute search
            val effect = ConditionalEffect(
                condition = OpponentControlsMoreLands,
                effect = SearchLibraryEffect(
                    filter = CardFilter.HasSubtype("Plains"),
                    count = 1,
                    destination = SearchDestination.HAND,
                    shuffleAfter = false,
                    reveal = true,
                    selectedCardIds = listOf(templeId)
                )
            )

            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, effect, context)

            // Temple should be in hand (has Plains subtype)
            result.state.getZone(handZone) shouldContain templeId
        }
    }
})
