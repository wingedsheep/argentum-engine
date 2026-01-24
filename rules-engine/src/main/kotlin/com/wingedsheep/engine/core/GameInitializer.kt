package com.wingedsheep.engine.core

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId

/**
 * Configuration for a player joining a game.
 */
data class PlayerConfig(
    val name: String,
    val deck: Deck,
    val startingLife: Int = 20
)

/**
 * Configuration for initializing a new game.
 */
data class GameConfig(
    val players: List<PlayerConfig>,
    val startingHandSize: Int = 7,
    val skipMulligans: Boolean = false
)

/**
 * Result of game initialization.
 */
data class InitializationResult(
    val state: GameState,
    val events: List<GameEvent>,
    val playerIds: List<EntityId>
)

/**
 * Initializes a new game from player configurations and decks.
 *
 * The GameInitializer handles:
 * 1. Creating player entities with life totals and mana pools
 * 2. Instantiating card entities from deck lists
 * 3. Placing cards in library zones
 * 4. Shuffling libraries
 * 5. Drawing initial hands
 * 6. Setting up turn order and priority
 *
 * ## Mulligan Process
 * By default, the game starts in mulligan phase. Players must submit
 * MulliganDecision actions until all players have kept. Use [skipMulligans]
 * in GameConfig to bypass this for testing.
 *
 * ## Usage
 * ```kotlin
 * val initializer = GameInitializer(cardRegistry)
 * val result = initializer.initializeGame(GameConfig(
 *     players = listOf(
 *         PlayerConfig("Alice", aliceDeck),
 *         PlayerConfig("Bob", bobDeck)
 *     )
 * ))
 * val gameState = result.state
 * ```
 */
class GameInitializer(
    private val cardRegistry: CardRegistry
) {
    /**
     * Initialize a new game.
     *
     * @param config Game configuration including player info and decks
     * @return The initialized game state and events
     */
    fun initializeGame(config: GameConfig): InitializationResult {
        require(config.players.size >= 2) { "Need at least 2 players" }

        val events = mutableListOf<GameEvent>()
        var state = GameState()
        val playerIds = mutableListOf<EntityId>()

        // 1. Create player entities
        for (playerConfig in config.players) {
            val playerId = EntityId.generate()
            playerIds.add(playerId)

            val playerContainer = ComponentContainer.of(
                PlayerComponent(playerConfig.name, playerConfig.startingLife),
                LifeTotalComponent(playerConfig.startingLife),
                ManaPoolComponent(),
                LandDropsComponent(),
                MulliganStateComponent(
                    mulligansTaken = 0,
                    hasKept = config.skipMulligans  // Auto-keep if skipping mulligans
                )
            )

            state = state.withEntity(playerId, playerContainer)
        }

        // 2. Set turn order (randomized)
        val shuffledOrder = playerIds.shuffled()
        state = state.copy(
            turnOrder = shuffledOrder,
            activePlayerId = shuffledOrder.first(),
            priorityPlayerId = shuffledOrder.first()
        )

        // 3. Instantiate cards and place in libraries
        for ((index, playerConfig) in config.players.withIndex()) {
            val playerId = playerIds[index]

            for (cardName in playerConfig.deck.cards) {
                val cardDef = cardRegistry.requireCard(cardName)
                val cardId = EntityId.generate()

                val cardContainer = createCardEntity(cardDef, playerId)
                state = state.withEntity(cardId, cardContainer)

                val libraryKey = ZoneKey(playerId, ZoneType.LIBRARY)
                state = state.addToZone(libraryKey, cardId)
            }
        }

        // 4. Shuffle libraries
        for (playerId in playerIds) {
            state = shuffleLibrary(state, playerId)
            events.add(LibraryShuffledEvent(playerId))
        }

        // 5. Draw initial hands
        for (playerId in playerIds) {
            val (newState, drawEvents) = drawCards(state, playerId, config.startingHandSize)
            state = newState
            events.addAll(drawEvents)
        }

        // 6. Set initial game phase
        // If mulligans are enabled, players will need to submit mulligan decisions
        // before the game proceeds to the first untap step
        if (!config.skipMulligans) {
            // Game stays in a "pre-game" state until mulligans resolve
            // The first player in turn order decides first
            state = state.copy(
                phase = com.wingedsheep.sdk.core.Phase.BEGINNING,
                step = com.wingedsheep.sdk.core.Step.UNTAP
            )
        }

        return InitializationResult(state, events, playerIds)
    }

    /**
     * Create a card entity from a card definition.
     */
    private fun createCardEntity(cardDef: CardDefinition, ownerId: EntityId): ComponentContainer {
        return ComponentContainer.of(
            CardComponent(
                cardDefinitionId = cardDef.name,
                name = cardDef.name,
                manaCost = cardDef.manaCost,
                typeLine = cardDef.typeLine,
                oracleText = cardDef.oracleText,
                baseStats = cardDef.creatureStats,
                baseKeywords = cardDef.keywords,
                colors = cardDef.colors,
                ownerId = ownerId,
                spellEffect = cardDef.spellEffect
            ),
            OwnerComponent(ownerId),
            ControllerComponent(ownerId)
        )
    }

    /**
     * Shuffle a player's library.
     */
    private fun shuffleLibrary(state: GameState, playerId: EntityId): GameState {
        val libraryKey = ZoneKey(playerId, ZoneType.LIBRARY)
        val library = state.getZone(libraryKey).shuffled()
        return state.copy(zones = state.zones + (libraryKey to library))
    }

    /**
     * Draw cards for a player.
     */
    private fun drawCards(
        state: GameState,
        playerId: EntityId,
        count: Int
    ): Pair<GameState, List<GameEvent>> {
        var currentState = state
        val events = mutableListOf<GameEvent>()
        val drawnCardIds = mutableListOf<EntityId>()

        val libraryKey = ZoneKey(playerId, ZoneType.LIBRARY)
        val handKey = ZoneKey(playerId, ZoneType.HAND)

        repeat(count) {
            val library = currentState.getZone(libraryKey)
            if (library.isEmpty()) {
                events.add(DrawFailedEvent(playerId, "Library is empty"))
                return currentState to events
            }

            // Draw from top of library (last element)
            val cardId = library.last()
            drawnCardIds.add(cardId)

            // Move card from library to hand
            currentState = currentState.removeFromZone(libraryKey, cardId)
            currentState = currentState.addToZone(handKey, cardId)

            events.add(ZoneChangeEvent(
                entityId = cardId,
                entityName = currentState.getEntity(cardId)
                    ?.get<CardComponent>()?.name ?: "Unknown",
                fromZone = ZoneType.LIBRARY,
                toZone = ZoneType.HAND,
                ownerId = playerId
            ))
        }

        events.add(CardsDrawnEvent(playerId, drawnCardIds.size, drawnCardIds))

        return currentState to events
    }

    companion object {
        /**
         * Create a simple two-player game for testing.
         * Both players get the same deck.
         */
        fun createTestGame(
            cardRegistry: CardRegistry,
            deck: Deck,
            player1Name: String = "Player 1",
            player2Name: String = "Player 2"
        ): InitializationResult {
            val initializer = GameInitializer(cardRegistry)
            return initializer.initializeGame(
                GameConfig(
                    players = listOf(
                        PlayerConfig(player1Name, deck),
                        PlayerConfig(player2Name, deck)
                    ),
                    skipMulligans = true
                )
            )
        }
    }
}
