package com.wingedsheep.rulesengine.game

import com.wingedsheep.rulesengine.action.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.zone.Zone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.random.Random

class GameFlowTest : FunSpec({

    fun createTestDeck(ownerId: String, cardCount: Int = 60): List<CardInstance> {
        return (1..cardCount).map { i ->
            val def = CardDefinition.creature(
                name = "Test Creature $i",
                manaCost = ManaCost(listOf(ManaSymbol.generic(2))),
                subtypes = setOf(Subtype.HUMAN),
                power = 2,
                toughness = 2
            )
            CardInstance.create(def, ownerId)
        }
    }

    fun createPlayerWithDeck(id: String, deckSize: Int = 60): Player {
        val player = Player.create(PlayerId.of(id), "$id's Deck")
        val deck = createTestDeck(id, deckSize)
        return player.copy(library = Zone.library(id, deck))
    }

    fun createLegendaryCreature(name: String, controllerId: String): CardInstance {
        val def = CardDefinition.creature(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.W, ManaSymbol.W)),
            subtypes = setOf(Subtype.HUMAN),
            power = 3,
            toughness = 3,
            supertypes = setOf(Supertype.LEGENDARY)
        )
        return CardInstance.create(def, controllerId)
    }

    context("Game Setup") {
        test("createGame creates initial state with correct players") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")

            val state = GameEngine.createGame(listOf(player1, player2))

            state.players.size shouldBe 2
            state.turnState.playerOrder shouldHaveSize 2
            state.isGameOver shouldBe false
        }

        test("createGame with starting player sets correct active player") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")
            val player2Id = PlayerId.of("player2")

            val state = GameEngine.createGame(listOf(player1, player2), startingPlayerId = player2Id)

            state.turnState.activePlayer shouldBe player2Id
        }

        test("setupGame shuffles libraries and draws opening hands") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")
            val random = Random(42) // Fixed seed for reproducibility

            val initialState = GameEngine.createGame(listOf(player1, player2), random = random)
            val result = GameEngine.setupGame(initialState, random)

            result.shouldBeInstanceOf<SetupResult.Success>()
            val setupState = (result as SetupResult.Success).state

            // Each player should have 7 cards in hand
            setupState.getPlayer(PlayerId.of("player1")).handSize shouldBe 7
            setupState.getPlayer(PlayerId.of("player2")).handSize shouldBe 7

            // Libraries should have 60 - 7 = 53 cards
            setupState.getPlayer(PlayerId.of("player1")).librarySize shouldBe 53
            setupState.getPlayer(PlayerId.of("player2")).librarySize shouldBe 53
        }

        test("starting life total is 20") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")

            val state = GameEngine.createGame(listOf(player1, player2))

            state.getPlayer(PlayerId.of("player1")).life shouldBe 20
            state.getPlayer(PlayerId.of("player2")).life shouldBe 20
        }
    }

    context("Mulligan") {
        test("startMulligan shuffles hand into library and draws new hand") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")
            val random = Random(42)

            val initialState = GameEngine.createGame(listOf(player1, player2), random = random)
            val setupResult = GameEngine.setupGame(initialState, random) as SetupResult.Success
            val setupState = setupResult.state

            val player1Id = PlayerId.of("player1")
            val result = GameEngine.startMulligan(setupState, player1Id, 1, random)

            result.shouldBeInstanceOf<MulliganResult.Success>()
            val mulliganState = (result as MulliganResult.Success).state

            // Player should still have 7 cards in hand
            mulliganState.getPlayer(player1Id).handSize shouldBe 7

            // Player needs to put 1 card on bottom
            result.cardsToPutOnBottom shouldBe 1
        }

        test("executeMulligan puts specified cards on bottom of library") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")
            val random = Random(42)

            val initialState = GameEngine.createGame(listOf(player1, player2), random = random)
            val setupResult = GameEngine.setupGame(initialState, random) as SetupResult.Success
            var state = setupResult.state

            val player1Id = PlayerId.of("player1")

            // Start mulligan
            val mulliganResult = GameEngine.startMulligan(state, player1Id, 1, random) as MulliganResult.Success
            state = mulliganResult.state

            // Get a card from hand to put on bottom
            val cardToPutOnBottom = state.getPlayer(player1Id).hand.cards.first().id

            // Execute mulligan
            val finalResult = GameEngine.executeMulligan(state, player1Id, listOf(cardToPutOnBottom), random)

            finalResult.shouldBeInstanceOf<MulliganResult.Success>()
            val finalState = (finalResult as MulliganResult.Success).state

            // Player should have 6 cards in hand (7 - 1 put on bottom)
            finalState.getPlayer(player1Id).handSize shouldBe 6

            // Library should have the card on bottom
            finalState.getPlayer(player1Id).library.bottomCard()?.id shouldBe cardToPutOnBottom
        }

        test("second mulligan requires putting 2 cards on bottom") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")
            val random = Random(42)

            val initialState = GameEngine.createGame(listOf(player1, player2), random = random)
            val setupResult = GameEngine.setupGame(initialState, random) as SetupResult.Success
            val state = setupResult.state

            val player1Id = PlayerId.of("player1")
            val result = GameEngine.startMulligan(state, player1Id, 2, random)

            result.shouldBeInstanceOf<MulliganResult.Success>()
            (result as MulliganResult.Success).cardsToPutOnBottom shouldBe 2
        }
    }

    context("Win/Lose Conditions") {
        test("player loses when life reaches 0") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")

            var state = GameEngine.createGame(listOf(player1, player2))

            // Deal 20 damage to player 1
            val result = ActionExecutor.execute(state, LoseLife(PlayerId.of("player1"), 20))
            state = (result as ActionResult.Success).state

            // Check state-based actions
            val sbaResult = ActionExecutor.execute(state, CheckStateBasedActions())
            state = (sbaResult as ActionResult.Success).state

            state.getPlayer(PlayerId.of("player1")).hasLost shouldBe true
            state.isGameOver shouldBe true
            state.winner shouldBe PlayerId.of("player2")
        }

        test("player loses when drawing from empty library") {
            val player1 = Player.create(PlayerId.of("player1"), "Player 1's Deck")
                .copy(library = Zone.library("player1", emptyList())) // Empty library
            val player2 = createPlayerWithDeck("player2")

            var state = GameEngine.createGame(listOf(player1, player2))

            // Try to draw a card from empty library
            val result = ActionExecutor.execute(state, DrawCard(PlayerId.of("player1"), 1))
            state = (result as ActionResult.Success).state

            // Player should have lost
            state.getPlayer(PlayerId.of("player1")).hasLost shouldBe true

            // Check events
            result.events.any { it is GameEvent.TriedToDrawFromEmptyLibrary } shouldBe true
            result.events.any { it is GameEvent.PlayerLost } shouldBe true
        }

        test("player loses with 10 or more poison counters") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")

            var state = GameEngine.createGame(listOf(player1, player2))

            // Give player 1 ten poison counters
            val result = ActionExecutor.execute(state, AddPoisonCounters(PlayerId.of("player1"), 10))
            state = (result as ActionResult.Success).state

            // Check state-based actions
            val sbaResult = ActionExecutor.execute(state, CheckStateBasedActions())
            state = (sbaResult as ActionResult.Success).state

            state.getPlayer(PlayerId.of("player1")).hasLost shouldBe true
            state.isGameOver shouldBe true
            state.winner shouldBe PlayerId.of("player2")
        }

        test("game is a draw when all players lose simultaneously") {
            val player1 = Player.create(PlayerId.of("player1"), "Player 1's Deck")
                .copy(life = 0)
            val player2 = Player.create(PlayerId.of("player2"), "Player 2's Deck")
                .copy(life = 0)

            var state = GameState.newGame(player1, player2)

            // Check state-based actions
            val sbaResult = ActionExecutor.execute(state, CheckStateBasedActions())
            state = (sbaResult as ActionResult.Success).state

            state.getPlayer(PlayerId.of("player1")).hasLost shouldBe true
            state.getPlayer(PlayerId.of("player2")).hasLost shouldBe true
            state.isGameOver shouldBe true
            state.winner shouldBe null // Draw
        }
    }

    context("State-Based Actions") {
        test("creature with 0 toughness dies") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")

            var state = GameEngine.createGame(listOf(player1, player2))

            // Create a creature with 0 toughness (simulated by a creature that's been modified)
            val def = CardDefinition.creature(
                name = "Fragile Creature",
                manaCost = ManaCost(listOf(ManaSymbol.W)),
                subtypes = setOf(Subtype.HUMAN),
                power = 2,
                toughness = 0
            )
            val creature = CardInstance.create(def, "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            // Check state-based actions
            val sbaResult = ActionExecutor.execute(state, CheckStateBasedActions())
            state = (sbaResult as ActionResult.Success).state

            // Creature should be in graveyard
            state.battlefield.getCard(creature.id) shouldBe null
            state.getPlayer(PlayerId.of("player1")).graveyard.getCard(creature.id) shouldNotBe null
        }

        test("creature with lethal damage dies") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")

            var state = GameEngine.createGame(listOf(player1, player2))

            // Create a 2/2 creature and deal 2 damage to it
            val def = CardDefinition.creature(
                name = "Bear",
                manaCost = ManaCost(listOf(ManaSymbol.G, ManaSymbol.G)),
                subtypes = setOf(Subtype("Bear")),
                power = 2,
                toughness = 2
            )
            val creature = CardInstance.create(def, "player1").dealDamage(2)
            state = state.updateBattlefield { it.addToTop(creature) }

            // Check state-based actions
            val sbaResult = ActionExecutor.execute(state, CheckStateBasedActions())
            state = (sbaResult as ActionResult.Success).state

            // Creature should be in graveyard
            state.battlefield.getCard(creature.id) shouldBe null
            state.getPlayer(PlayerId.of("player1")).graveyard.getCard(creature.id) shouldNotBe null
        }

        test("legendary rule removes duplicate legendary permanents") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")

            var state = GameEngine.createGame(listOf(player1, player2))

            // Create two legendary creatures with the same name
            val legendary1 = createLegendaryCreature("Isamaru, Hound of Konda", "player1")
            val legendary2 = createLegendaryCreature("Isamaru, Hound of Konda", "player1")

            state = state.updateBattlefield { it.addToTop(legendary1).addToTop(legendary2) }

            // Both should be on battlefield initially
            state.battlefield.cards.size shouldBe 2

            // Check state-based actions
            val sbaResult = ActionExecutor.execute(state, CheckStateBasedActions())
            state = (sbaResult as ActionResult.Success).state

            // Only one should remain
            state.battlefield.cards.size shouldBe 1

            // One should be in graveyard
            state.getPlayer(PlayerId.of("player1")).graveyard.cards.size shouldBe 1

            // Event should have been emitted
            sbaResult.events.any { it is GameEvent.LegendaryRuleApplied } shouldBe true
        }

        test("legendary rule does not affect different legendary permanents") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")

            var state = GameEngine.createGame(listOf(player1, player2))

            // Create two different legendary creatures
            val legendary1 = createLegendaryCreature("Isamaru, Hound of Konda", "player1")
            val legendary2 = createLegendaryCreature("Reki, the History of Kamigawa", "player1")

            state = state.updateBattlefield { it.addToTop(legendary1).addToTop(legendary2) }

            // Check state-based actions
            val sbaResult = ActionExecutor.execute(state, CheckStateBasedActions())
            state = (sbaResult as ActionResult.Success).state

            // Both should remain
            state.battlefield.cards.size shouldBe 2
        }

        test("legendary rule only affects same controller") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")

            var state = GameEngine.createGame(listOf(player1, player2))

            // Create same legendary creature controlled by different players
            val legendary1 = createLegendaryCreature("Isamaru, Hound of Konda", "player1")
            val legendary2 = createLegendaryCreature("Isamaru, Hound of Konda", "player2")

            state = state.updateBattlefield { it.addToTop(legendary1).addToTop(legendary2) }

            // Check state-based actions
            val sbaResult = ActionExecutor.execute(state, CheckStateBasedActions())
            state = (sbaResult as ActionResult.Success).state

            // Both should remain (different controllers)
            state.battlefield.cards.size shouldBe 2
        }
    }

    context("Game Engine utilities") {
        test("isGameOver returns correct value") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")

            var state = GameEngine.createGame(listOf(player1, player2))

            GameEngine.isGameOver(state) shouldBe false

            state = state.endGame(PlayerId.of("player1"))

            GameEngine.isGameOver(state) shouldBe true
        }

        test("getWinner returns correct value") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")

            var state = GameEngine.createGame(listOf(player1, player2))

            GameEngine.getWinner(state) shouldBe null

            state = state.endGame(PlayerId.of("player1"))

            GameEngine.getWinner(state) shouldBe PlayerId.of("player1")
        }

        test("getAvailableActions returns pass priority when player has priority") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")
            val player1Id = PlayerId.of("player1")

            // Explicitly set player1 as starting player to ensure they have priority
            val state = GameEngine.createGame(listOf(player1, player2), startingPlayerId = player1Id)
                .advanceToPhase(Phase.PRECOMBAT_MAIN)
                .advanceToStep(Step.PRECOMBAT_MAIN)

            val actions = GameEngine.getAvailableActions(state, player1Id)

            actions.any { it is PassPriority } shouldBe true
        }

        test("getAvailableActions returns empty for player without priority") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")
            val player1Id = PlayerId.of("player1")

            // Explicitly set player1 as starting player to ensure priority
            val state = GameEngine.createGame(listOf(player1, player2), startingPlayerId = player1Id)
                .advanceToPhase(Phase.PRECOMBAT_MAIN)
                .advanceToStep(Step.PRECOMBAT_MAIN)

            // Player 1 has priority, so player 2 should have no actions
            val actions = GameEngine.getAvailableActions(state, PlayerId.of("player2"))

            actions shouldHaveSize 0
        }

        test("getAvailableActions returns empty when game is over") {
            val player1 = createPlayerWithDeck("player1")
            val player2 = createPlayerWithDeck("player2")

            val state = GameEngine.createGame(listOf(player1, player2))
                .endGame(PlayerId.of("player1"))

            val actions = GameEngine.getAvailableActions(state, PlayerId.of("player1"))

            actions shouldHaveSize 0
        }
    }
})
