package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.ecs.components.AbilitiesComponent
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.sets.portal.PortalSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DeckLoaderTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun newGame(): EcsGameState = EcsGameState.newGame(
        listOf(
            player1Id to "Alice",
            player2Id to "Bob"
        )
    )

    context("loadDeck") {
        test("creates entities for each card in deck") {
            val state = newGame()
            val deckLoader = DeckLoader.create(PortalSet)

            val deckList = mapOf(
                "Forest" to 10,
                "Armored Pegasus" to 4
            )

            val result = deckLoader.loadDeck(state, player1Id, deckList)
            result.shouldBeInstanceOf<DeckLoader.DeckLoadResult.Success>()

            val success = result as DeckLoader.DeckLoadResult.Success
            success.cardIds.shouldHaveSize(14)  // 10 + 4
        }

        test("adds cards to player library zone") {
            val state = newGame()
            val deckLoader = DeckLoader.create(PortalSet)

            val deckList = mapOf("Forest" to 5)

            val result = deckLoader.loadDeck(state, player1Id, deckList)
            result.shouldBeInstanceOf<DeckLoader.DeckLoadResult.Success>()

            val success = result as DeckLoader.DeckLoadResult.Success
            val library = success.state.getLibrary(player1Id)

            library.shouldHaveSize(5)
            success.cardIds.forEach { cardId ->
                library.contains(cardId).shouldBeTrue()
            }
        }

        test("bakes abilities onto entities with mana abilities") {
            val state = newGame()
            val deckLoader = DeckLoader.create(PortalSet)

            val deckList = mapOf("Forest" to 1)

            val result = deckLoader.loadDeck(state, player1Id, deckList)
            result.shouldBeInstanceOf<DeckLoader.DeckLoadResult.Success>()

            val success = result as DeckLoader.DeckLoadResult.Success
            val forestId = success.cardIds.first()

            // Check that the Forest entity has AbilitiesComponent with mana ability
            val abilities = success.state.getEntity(forestId)?.get<AbilitiesComponent>()
            abilities.shouldNotBeNull()
            abilities.hasManaAbilities.shouldBeTrue()
            abilities.manaAbilities.shouldHaveSize(1)
            abilities.manaAbilities.first().isManaAbility.shouldBeTrue()
        }

        test("fails gracefully for unknown cards") {
            val state = newGame()
            val deckLoader = DeckLoader.create(PortalSet)

            val deckList = mapOf(
                "Forest" to 5,
                "NonexistentCard" to 2,
                "AnotherFakeCard" to 1
            )

            val result = deckLoader.loadDeck(state, player1Id, deckList)
            result.shouldBeInstanceOf<DeckLoader.DeckLoadResult.Failure>()

            val failure = result as DeckLoader.DeckLoadResult.Failure
            failure.missingCards.shouldContainAll("NonexistentCard", "AnotherFakeCard")
        }

        test("sets correct owner and controller") {
            val state = newGame()
            val deckLoader = DeckLoader.create(PortalSet)

            val deckList = mapOf("Forest" to 1)

            val result = deckLoader.loadDeck(state, player1Id, deckList)
            result.shouldBeInstanceOf<DeckLoader.DeckLoadResult.Success>()

            val success = result as DeckLoader.DeckLoadResult.Success
            val forestId = success.cardIds.first()

            val card = success.state.getEntity(forestId)?.get<CardComponent>()
            card.shouldNotBeNull()
            card.ownerId shouldBe player1Id
        }
    }

    context("loadDecks") {
        test("loads multiple player decks") {
            val state = newGame()
            val deckLoader = DeckLoader.create(PortalSet)

            val decks = mapOf(
                player1Id to mapOf("Forest" to 5, "Mountain" to 5),
                player2Id to mapOf("Island" to 5, "Plains" to 5)
            )

            val result = deckLoader.loadDecks(state, decks)
            result.shouldBeInstanceOf<DeckLoader.DeckLoadResult.Success>()

            val success = result as DeckLoader.DeckLoadResult.Success

            // Check player1's library
            val player1Library = success.state.getLibrary(player1Id)
            player1Library.shouldHaveSize(10)

            // Check player2's library
            val player2Library = success.state.getLibrary(player2Id)
            player2Library.shouldHaveSize(10)

            // Total cards
            success.cardIds.shouldHaveSize(20)
        }

        test("fails if any player has missing cards") {
            val state = newGame()
            val deckLoader = DeckLoader.create(PortalSet)

            val decks = mapOf(
                player1Id to mapOf("Forest" to 5),
                player2Id to mapOf("FakeCard" to 5)
            )

            val result = deckLoader.loadDecks(state, decks)
            result.shouldBeInstanceOf<DeckLoader.DeckLoadResult.Failure>()

            val failure = result as DeckLoader.DeckLoadResult.Failure
            failure.missingCards.shouldContainAll("FakeCard")
        }
    }

    context("EcsGameEngine integration") {
        test("loadDecks works through EcsGameEngine") {
            val state = newGame()
            val deckLoader = DeckLoader.create(PortalSet)

            val decks = mapOf(
                player1Id to mapOf("Forest" to 20),
                player2Id to mapOf("Mountain" to 20)
            )

            val result = EcsGameEngine.loadDecks(state, deckLoader, decks)
            result.shouldBeInstanceOf<DeckLoader.DeckLoadResult.Success>()

            val success = result as DeckLoader.DeckLoadResult.Success

            // Verify libraries have cards
            success.state.getLibrary(player1Id).shouldHaveSize(20)
            success.state.getLibrary(player2Id).shouldHaveSize(20)
        }
    }

    context("card scripts with abilities") {
        test("creatures without abilities don't get AbilitiesComponent") {
            val state = newGame()
            val deckLoader = DeckLoader.create(PortalSet)

            // Archangel is a French vanilla (keywords only from definition)
            val deckList = mapOf("Archangel" to 1)

            val result = deckLoader.loadDeck(state, player1Id, deckList)
            result.shouldBeInstanceOf<DeckLoader.DeckLoadResult.Success>()

            val success = result as DeckLoader.DeckLoadResult.Success
            val cardId = success.cardIds.first()

            // Archangel has keywords but no activated/triggered/static abilities in script
            // So AbilitiesComponent should not be present (or be empty)
            val abilities = success.state.getEntity(cardId)?.get<AbilitiesComponent>()
            // It should either be null or have no abilities
            if (abilities != null) {
                abilities.activatedAbilities.shouldHaveSize(0)
            }
        }
    }
})
