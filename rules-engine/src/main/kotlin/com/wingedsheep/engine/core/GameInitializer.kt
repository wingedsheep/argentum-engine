package com.wingedsheep.engine.core

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Configuration for a player joining a game.
 *
 * @property playerId Optional pre-existing player ID. If null, a new ID will be generated.
 */
data class PlayerConfig(
    val name: String,
    val deck: Deck,
    val startingLife: Int = 20,
    val playerId: EntityId? = null
)

/**
 * Configuration for initializing a new game.
 *
 * @property players List of player configurations
 * @property startingHandSize Number of cards to draw for opening hand (default: 7)
 * @property skipMulligans Whether to skip the mulligan phase (default: false)
 * @property useHandSmoother Whether to use MTGA-style hand smoothing for initial draw (default: false)
 * @property handSmootherCandidates Number of candidate hands to generate when smoothing (default: 3, range: 2-3)
 */
data class GameConfig(
    val players: List<PlayerConfig>,
    val startingHandSize: Int = 7,
    val skipMulligans: Boolean = false,
    val useHandSmoother: Boolean = false,
    val handSmootherCandidates: Int = 3
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
            val playerId = playerConfig.playerId ?: EntityId.generate()
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
            priorityPlayerId = shuffledOrder.first(),
            turnNumber = 1  // First turn is turn 1, not turn 0
        )

        // 3. Instantiate cards and place in libraries
        for ((index, playerConfig) in config.players.withIndex()) {
            val playerId = playerIds[index]

            for (cardName in playerConfig.deck.cards) {
                val cardDef = cardRegistry.requireCard(cardName)
                val cardId = EntityId.generate()

                val cardContainer = createCardEntity(cardDef, playerId)
                state = state.withEntity(cardId, cardContainer)

                val libraryKey = ZoneKey(playerId, Zone.LIBRARY)
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
            val (newState, drawEvents) = if (config.useHandSmoother) {
                drawSmoothedHand(state, playerId, config.startingHandSize, config.handSmootherCandidates)
            } else {
                drawCards(state, playerId, config.startingHandSize)
            }
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
        val protectionColors = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.ProtectionFromColor>()
            .map { it.color }
            .toSet() +
            cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.ProtectionFromColors>()
                .flatMap { it.colors }
                .toSet()

        val protectionSubtypes = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.ProtectionFromCreatureSubtype>()
            .map { it.subtype }
            .toSet()

        var container = ComponentContainer.of(
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
                spellEffect = cardDef.spellEffect,
                imageUri = cardDef.metadata.imageUri
            ),
            OwnerComponent(ownerId),
            ControllerComponent(ownerId)
        )

        if (protectionColors.isNotEmpty() || protectionSubtypes.isNotEmpty()) {
            container = container.with(ProtectionComponent(protectionColors, protectionSubtypes))
        }

        return container
    }

    /**
     * Shuffle a player's library.
     */
    private fun shuffleLibrary(state: GameState, playerId: EntityId): GameState {
        val libraryKey = ZoneKey(playerId, Zone.LIBRARY)
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

        val libraryKey = ZoneKey(playerId, Zone.LIBRARY)
        val handKey = ZoneKey(playerId, Zone.HAND)

        repeat(count) {
            val library = currentState.getZone(libraryKey)
            if (library.isEmpty()) {
                events.add(DrawFailedEvent(playerId, "Library is empty"))
                return currentState to events
            }

            // Draw from top of library (first element)
            val cardId = library.first()
            drawnCardIds.add(cardId)

            // Move card from library to hand
            currentState = currentState.removeFromZone(libraryKey, cardId)
            currentState = currentState.addToZone(handKey, cardId)

            events.add(ZoneChangeEvent(
                entityId = cardId,
                entityName = currentState.getEntity(cardId)
                    ?.get<CardComponent>()?.name ?: "Unknown",
                fromZone = Zone.LIBRARY,
                toZone = Zone.HAND,
                ownerId = playerId
            ))
        }

        events.add(CardsDrawnEvent(playerId, drawnCardIds.size, drawnCardIds))

        return currentState to events
    }

    /**
     * Draw a "smoothed" opening hand using MTGA-style hand smoothing algorithm.
     *
     * This algorithm generates multiple candidate hands and selects the one whose
     * land ratio most closely matches the deck's overall land-to-spell ratio.
     *
     * ## Algorithm
     * 1. Calculate the deck's land ratio (lands / deck_size)
     * 2. Generate [candidateCount] candidate hands by shuffling and taking top cards
     * 3. Score each candidate based on deviation from expected land ratio
     * 4. Select the hand with the lowest deviation score
     * 5. Apply the selected hand to the game state
     *
     * ## Notes
     * - Only applies to the FIRST hand drawn (not mulligan redraws)
     * - Subsequent mulligans use standard random draws
     *
     * @param state Current game state
     * @param playerId Player to draw for
     * @param count Number of cards to draw
     * @param candidateCount Number of candidate hands to generate (2-3 recommended)
     * @return Pair of new state and events
     */
    private fun drawSmoothedHand(
        state: GameState,
        playerId: EntityId,
        count: Int,
        candidateCount: Int
    ): Pair<GameState, List<GameEvent>> {
        val libraryKey = ZoneKey(playerId, Zone.LIBRARY)
        val handKey = ZoneKey(playerId, Zone.HAND)
        val library = state.getZone(libraryKey)

        // Need enough cards for the hand
        if (library.size < count) {
            return drawCards(state, playerId, count)
        }

        // Calculate deck land ratio
        val deckLandRatio = calculateDeckLandRatio(state, library)

        // Generate candidate hands
        val candidates = mutableListOf<List<EntityId>>()
        val effectiveCandidateCount = candidateCount.coerceIn(2, 3)

        repeat(effectiveCandidateCount) {
            val shuffledLibrary = library.shuffled()
            val candidateHand = shuffledLibrary.takeLast(count)
            candidates.add(candidateHand)
        }

        // Score each candidate and select the best one
        val bestCandidate = candidates.minByOrNull { candidate ->
            scoreHand(state, candidate, deckLandRatio)
        } ?: candidates.first()

        // Apply the selected hand to the state
        var currentState = state
        val events = mutableListOf<GameEvent>()
        val drawnCardIds = mutableListOf<EntityId>()

        // Remove selected cards from library
        var newLibrary = library.toMutableList()
        for (cardId in bestCandidate) {
            newLibrary.remove(cardId)
        }

        // Shuffle the remaining library
        newLibrary = newLibrary.shuffled().toMutableList()
        currentState = currentState.copy(zones = currentState.zones + (libraryKey to newLibrary))

        // Add cards to hand
        for (cardId in bestCandidate) {
            drawnCardIds.add(cardId)
            currentState = currentState.addToZone(handKey, cardId)

            events.add(ZoneChangeEvent(
                entityId = cardId,
                entityName = currentState.getEntity(cardId)
                    ?.get<CardComponent>()?.name ?: "Unknown",
                fromZone = Zone.LIBRARY,
                toZone = Zone.HAND,
                ownerId = playerId
            ))
        }

        events.add(CardsDrawnEvent(playerId, drawnCardIds.size, drawnCardIds))

        return currentState to events
    }

    /**
     * Calculate the land ratio in a deck (library).
     *
     * @param state Current game state for entity lookups
     * @param library List of card entity IDs in the library
     * @return Ratio of lands to total cards (0.0 to 1.0)
     */
    private fun calculateDeckLandRatio(state: GameState, library: List<EntityId>): Double {
        if (library.isEmpty()) return 0.0

        val landCount = library.count { cardId ->
            state.getEntity(cardId)?.get<CardComponent>()?.typeLine?.isLand == true
        }

        return landCount.toDouble() / library.size
    }

    /**
     * Score a candidate hand based on its deviation from the expected land ratio.
     *
     * Lower score = better match to deck composition.
     *
     * @param state Current game state for entity lookups
     * @param hand List of card entity IDs in the candidate hand
     * @param deckLandRatio Expected land ratio based on deck composition
     * @return Absolute deviation from expected ratio (0.0 = perfect match)
     */
    private fun scoreHand(state: GameState, hand: List<EntityId>, deckLandRatio: Double): Double {
        if (hand.isEmpty()) return Double.MAX_VALUE

        val landCount = hand.count { cardId ->
            state.getEntity(cardId)?.get<CardComponent>()?.typeLine?.isLand == true
        }

        val handLandRatio = landCount.toDouble() / hand.size
        return kotlin.math.abs(handLandRatio - deckLandRatio)
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
