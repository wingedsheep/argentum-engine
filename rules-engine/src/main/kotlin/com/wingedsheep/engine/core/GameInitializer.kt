package com.wingedsheep.engine.core

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.sdk.core.Color
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
    val handSmootherCandidates: Int = 3,
    val startingPlayerIndex: Int? = null
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

        // 2. Set turn order
        val shuffledOrder = if (config.startingPlayerIndex != null) {
            val idx = config.startingPlayerIndex
            playerIds.subList(idx, playerIds.size) + playerIds.subList(0, idx)
        } else {
            playerIds.shuffled()
        }
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
                baseFlags = cardDef.flags,
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

        val cardNames = drawnCardIds.map { currentState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
        events.add(CardsDrawnEvent(playerId, drawnCardIds.size, drawnCardIds, cardNames))

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

        // Calculate deck land ratios (overall + per-colour)
        val deckLandRatio = calculateDeckLandRatio(state, library)
        val deckColorRatios = calculateDeckColorRatios(state, library)

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
            scoreHand(state, candidate, deckLandRatio, deckColorRatios)
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

        val cardNames = drawnCardIds.map { currentState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
        events.add(CardsDrawnEvent(playerId, drawnCardIds.size, drawnCardIds, cardNames))

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
     * Calculate the per-colour land ratios in the deck (among lands only).
     *
     * For each colour, this returns what fraction of the deck's lands produce that colour.
     * Uses basic land subtypes (Plains→W, Island→U, etc.) and the card's color identity
     * for non-basic lands. Dual lands count toward multiple colours.
     *
     * @param state Current game state for entity lookups
     * @param library List of card entity IDs in the library
     * @return Map of colour to ratio among lands (0.0 to 1.0), empty if no lands
     */
    private fun calculateDeckColorRatios(state: GameState, library: List<EntityId>): Map<Color, Double> {
        val colorCounts = mutableMapOf<Color, Int>()
        var totalLands = 0

        for (cardId in library) {
            val card = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (!card.typeLine.isLand) continue
            totalLands++

            val colors = getLandColors(card)
            for (color in colors) {
                colorCounts[color] = (colorCounts[color] ?: 0) + 1
            }
        }

        if (totalLands == 0) return emptyMap()
        return colorCounts.mapValues { (_, count) -> count.toDouble() / totalLands }
    }

    /**
     * Determine which colours of mana a land card can produce.
     *
     * Uses basic land subtypes first (Plains→W, Island→U, Swamp→B, Mountain→R, Forest→G).
     * For non-basic lands without basic land subtypes, falls back to the card's color identity.
     */
    private fun getLandColors(card: CardComponent): Set<Color> {
        val colors = mutableSetOf<Color>()
        val subtypes = card.typeLine.subtypes.map { it.value }.toSet()

        if ("Plains" in subtypes) colors.add(Color.WHITE)
        if ("Island" in subtypes) colors.add(Color.BLUE)
        if ("Swamp" in subtypes) colors.add(Color.BLACK)
        if ("Mountain" in subtypes) colors.add(Color.RED)
        if ("Forest" in subtypes) colors.add(Color.GREEN)

        // For non-basic lands without basic land subtypes, use color identity from mana cost
        if (colors.isEmpty()) {
            colors.addAll(card.colors)
        }

        return colors
    }

    /**
     * Score a candidate hand based on deviation from both the expected land ratio
     * and the expected colour distribution among lands.
     *
     * The score combines two factors:
     * - Land ratio deviation: how far the hand's land/spell ratio is from the deck's
     * - Colour deviation: how far the hand's land colour distribution is from the deck's
     *
     * Lower score = better match to deck composition.
     *
     * @param state Current game state for entity lookups
     * @param hand List of card entity IDs in the candidate hand
     * @param deckLandRatio Expected land ratio based on deck composition
     * @param deckColorRatios Expected colour distribution among lands
     * @return Combined deviation score (0.0 = perfect match)
     */
    private fun scoreHand(
        state: GameState,
        hand: List<EntityId>,
        deckLandRatio: Double,
        deckColorRatios: Map<Color, Double>
    ): Double {
        if (hand.isEmpty()) return Double.MAX_VALUE

        val lands = mutableListOf<CardComponent>()
        for (cardId in hand) {
            val card = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (card.typeLine.isLand) lands.add(card)
        }

        // Land ratio deviation (same as before)
        val handLandRatio = lands.size.toDouble() / hand.size
        val landRatioDeviation = kotlin.math.abs(handLandRatio - deckLandRatio)

        // Colour distribution deviation among lands in hand
        val colorDeviation = if (lands.isNotEmpty() && deckColorRatios.isNotEmpty()) {
            val handColorCounts = mutableMapOf<Color, Int>()
            for (land in lands) {
                for (color in getLandColors(land)) {
                    handColorCounts[color] = (handColorCounts[color] ?: 0) + 1
                }
            }

            val allColors = deckColorRatios.keys + handColorCounts.keys
            allColors.sumOf { color ->
                val deckRatio = deckColorRatios[color] ?: 0.0
                val handRatio = (handColorCounts[color] ?: 0).toDouble() / lands.size
                kotlin.math.abs(handRatio - deckRatio)
            }
        } else {
            0.0
        }

        // Weight: land ratio is most important, colour distribution is secondary
        return landRatioDeviation + colorDeviation * 0.5
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
