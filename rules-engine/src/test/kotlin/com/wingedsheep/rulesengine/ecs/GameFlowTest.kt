package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.ManaSymbol
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.core.Supertype
import com.wingedsheep.rulesengine.ecs.action.*
import com.wingedsheep.rulesengine.ecs.components.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.random.Random

class GameFlowTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun createTestCreature(name: String, ownerId: EntityId): CardDefinition {
        return CardDefinition.creature(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.generic(2))),
            subtypes = setOf(Subtype.HUMAN),
            power = 2,
            toughness = 2
        )
    }

    fun createLegendaryCreature(name: String): CardDefinition {
        return CardDefinition.creature(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.W, ManaSymbol.W)),
            subtypes = setOf(Subtype.HUMAN),
            power = 3,
            toughness = 3,
            supertypes = setOf(Supertype.LEGENDARY)
        )
    }

    fun createGameWithDeck(cardCount: Int = 60): GameState {
        var state = GameState.newGame(listOf(player1Id to "Alice", player2Id to "Bob"))

        // Add cards to both players' libraries
        for (playerId in listOf(player1Id, player2Id)) {
            repeat(cardCount) { i ->
                val def = createTestCreature("Test Creature ${i + 1}", playerId)
                val (cardId, newState) = state.createEntity(
                    EntityId.generate(),
                    CardComponent(def, playerId),
                    ControllerComponent(playerId)
                )
                state = newState.addToZone(cardId, ZoneId.library(playerId))
            }
        }

        return state
    }

    context("Game Setup") {
        test("createGame creates initial state with correct players") {
            val state = GameEngine.createGame(listOf(player1Id to "Alice", player2Id to "Bob"))

            state.getPlayerIds() shouldHaveSize 2
            state.turnState.playerOrder shouldHaveSize 2
            state.isGameOver shouldBe false
        }

        test("setupGame shuffles libraries and draws opening hands") {
            val state = createGameWithDeck(60)
            val random = Random(42) // Fixed seed for reproducibility

            val result = GameEngine.setupGame(state, random)

            result.shouldBeInstanceOf<SetupResult.Success>()
            val setupState = (result as SetupResult.Success).state

            // Each player should have 7 cards in hand
            setupState.getHand(player1Id) shouldHaveSize 7
            setupState.getHand(player2Id) shouldHaveSize 7

            // Libraries should have 60 - 7 = 53 cards
            setupState.getLibrary(player1Id) shouldHaveSize 53
            setupState.getLibrary(player2Id) shouldHaveSize 53
        }

        test("starting life total is 20") {
            val state = GameEngine.createGame(listOf(player1Id to "Alice", player2Id to "Bob"))

            state.getComponent<LifeComponent>(player1Id)?.life shouldBe 20
            state.getComponent<LifeComponent>(player2Id)?.life shouldBe 20
        }
    }

    context("Mulligan") {
        test("startMulligan shuffles hand into library and draws new hand") {
            val state = createGameWithDeck(60)
            val random = Random(42)

            val setupResult = GameEngine.setupGame(state, random) as SetupResult.Success
            val setupState = setupResult.state

            val result = GameEngine.startMulligan(setupState, player1Id, 1, random)

            result.shouldBeInstanceOf<MulliganResult.Success>()
            val mulliganState = (result as MulliganResult.Success).state

            // Player should still have 7 cards in hand
            mulliganState.getHand(player1Id) shouldHaveSize 7

            // Player needs to put 1 card on bottom
            result.cardsToPutOnBottom shouldBe 1
        }

        test("executeMulligan puts specified cards on bottom of library") {
            val state = createGameWithDeck(60)
            val random = Random(42)

            val setupResult = GameEngine.setupGame(state, random) as SetupResult.Success
            var currentState = setupResult.state

            // Start mulligan
            val mulliganResult = GameEngine.startMulligan(currentState, player1Id, 1, random) as MulliganResult.Success
            currentState = mulliganResult.state

            // Get a card from hand to put on bottom
            val cardToPutOnBottom = currentState.getHand(player1Id).first()

            // Execute mulligan
            val finalResult = GameEngine.executeMulligan(currentState, player1Id, listOf(cardToPutOnBottom), random)

            finalResult.shouldBeInstanceOf<MulliganResult.Success>()
            val finalState = (finalResult as MulliganResult.Success).state

            // Player should have 6 cards in hand (7 - 1 put on bottom)
            finalState.getHand(player1Id) shouldHaveSize 6

            // Library should have the card on bottom
            finalState.getLibrary(player1Id).first() shouldBe cardToPutOnBottom
        }

        test("second mulligan requires putting 2 cards on bottom") {
            val state = createGameWithDeck(60)
            val random = Random(42)

            val setupResult = GameEngine.setupGame(state, random) as SetupResult.Success
            val setupState = setupResult.state

            val result = GameEngine.startMulligan(setupState, player1Id, 2, random)

            result.shouldBeInstanceOf<MulliganResult.Success>()
            (result as MulliganResult.Success).cardsToPutOnBottom shouldBe 2
        }
    }

    context("Win/Lose Conditions") {
        test("player loses when life reaches 0") {
            var state = GameEngine.createGame(listOf(player1Id to "Alice", player2Id to "Bob"))

            // Deal 20 damage to player 1
            val result = GameEngine.executeAction(state, LoseLife(player1Id, 20))
            result.shouldBeInstanceOf<GameActionResult.Success>()
            state = (result as GameActionResult.Success).state

            // Check state-based actions
            val sbaResult = GameEngine.checkStateBasedActions(state)
            state = (sbaResult as GameActionResult.Success).state

            state.hasComponent<LostGameComponent>(player1Id) shouldBe true
            state.isGameOver shouldBe true
            state.winner shouldBe player2Id
        }

        test("player loses when drawing from empty library") {
            val state = GameEngine.createGame(listOf(player1Id to "Alice", player2Id to "Bob"))

            // Try to draw a card from empty library
            val result = GameEngine.executeAction(state, DrawCard(player1Id, 1))
            val success = result as GameActionResult.Success

            // Player should have lost
            success.state.hasComponent<LostGameComponent>(player1Id) shouldBe true

            // Check events
            success.events.any { it is GameActionEvent.DrawFailed } shouldBe true
        }

        test("player loses with 10 or more poison counters") {
            var state = GameEngine.createGame(listOf(player1Id to "Alice", player2Id to "Bob"))

            // Give player 1 ten poison counters
            val result = GameEngine.executeAction(state, AddPoisonCounters(player1Id, 10))
            state = (result as GameActionResult.Success).state

            // Check state-based actions
            val sbaResult = GameEngine.checkStateBasedActions(state)
            state = (sbaResult as GameActionResult.Success).state

            state.hasComponent<LostGameComponent>(player1Id) shouldBe true
            state.isGameOver shouldBe true
            state.winner shouldBe player2Id
        }

        test("game is a draw when all players lose simultaneously") {
            var state = GameEngine.createGame(listOf(player1Id to "Alice", player2Id to "Bob"))

            // Set both players to 0 life
            state = state.updateEntity(player1Id) { it.with(LifeComponent(0)) }
            state = state.updateEntity(player2Id) { it.with(LifeComponent(0)) }

            // Check state-based actions
            val sbaResult = GameEngine.checkStateBasedActions(state)
            state = (sbaResult as GameActionResult.Success).state

            state.hasComponent<LostGameComponent>(player1Id) shouldBe true
            state.hasComponent<LostGameComponent>(player2Id) shouldBe true
            state.isGameOver shouldBe true
            state.winner shouldBe null // Draw
        }
    }

    context("State-Based Actions") {
        test("creature with 0 toughness dies") {
            var state = GameEngine.createGame(listOf(player1Id to "Alice", player2Id to "Bob"))

            // Create a creature with 0 toughness
            val def = CardDefinition.creature(
                name = "Fragile Creature",
                manaCost = ManaCost(listOf(ManaSymbol.W)),
                subtypes = setOf(Subtype.HUMAN),
                power = 2,
                toughness = 0
            )
            val (creatureId, newState) = state.createEntity(
                EntityId.generate(),
                CardComponent(def, player1Id),
                ControllerComponent(player1Id)
            )
            state = newState.addToZone(creatureId, ZoneId.BATTLEFIELD)

            // Check state-based actions
            val sbaResult = GameEngine.checkStateBasedActions(state)
            state = (sbaResult as GameActionResult.Success).state

            // Creature should be in graveyard
            state.getBattlefield().contains(creatureId) shouldBe false
            state.getGraveyard(player1Id).contains(creatureId) shouldBe true
        }

        test("creature with lethal damage dies") {
            var state = GameEngine.createGame(listOf(player1Id to "Alice", player2Id to "Bob"))

            // Create a 2/2 creature and deal 2 damage to it
            val def = CardDefinition.creature(
                name = "Bear",
                manaCost = ManaCost(listOf(ManaSymbol.G, ManaSymbol.G)),
                subtypes = setOf(Subtype("Bear")),
                power = 2,
                toughness = 2
            )
            val (creatureId, newState) = state.createEntity(
                EntityId.generate(),
                CardComponent(def, player1Id),
                ControllerComponent(player1Id),
                DamageComponent(2)
            )
            state = newState.addToZone(creatureId, ZoneId.BATTLEFIELD)

            // Check state-based actions
            val sbaResult = GameEngine.checkStateBasedActions(state)
            state = (sbaResult as GameActionResult.Success).state

            // Creature should be in graveyard
            state.getBattlefield().contains(creatureId) shouldBe false
            state.getGraveyard(player1Id).contains(creatureId) shouldBe true
        }
    }

    context("Game Engine utilities") {
        test("isGameOver returns correct value") {
            var state = GameEngine.createGame(listOf(player1Id to "Alice", player2Id to "Bob"))

            GameEngine.isGameOver(state) shouldBe false

            state = state.endGame(player1Id)

            GameEngine.isGameOver(state) shouldBe true
        }

        test("getWinner returns correct value") {
            var state = GameEngine.createGame(listOf(player1Id to "Alice", player2Id to "Bob"))

            GameEngine.getWinner(state) shouldBe null

            state = state.endGame(player1Id)

            GameEngine.getWinner(state) shouldBe player1Id
        }

        test("executeAction delegates to action handler") {
            val state = GameEngine.createGame(listOf(player1Id to "Alice", player2Id to "Bob"))
            val result = GameEngine.executeAction(state, GainLife(player1Id, 5))

            result.shouldBeInstanceOf<GameActionResult.Success>()
            (result as GameActionResult.Success).state.getComponent<LifeComponent>(player1Id)?.life shouldBe 25
        }

        test("executeActions executes multiple actions") {
            val state = GameEngine.createGame(listOf(player1Id to "Alice", player2Id to "Bob"))
            val actions = listOf(
                GainLife(player1Id, 5),
                LoseLife(player2Id, 3)
            )
            val result = GameEngine.executeActions(state, actions)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            success.state.getComponent<LifeComponent>(player1Id)?.life shouldBe 25
            success.state.getComponent<LifeComponent>(player2Id)?.life shouldBe 17
        }
    }
})
