package com.wingedsheep.engine.core

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardEntry
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.model.PrintingRef
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Configuration for a player joining a game.
 *
 * @property playerId Optional pre-existing player ID. If null, a new ID will be generated.
 */
data class PlayerConfig(
    val name: String,
    val deck: Deck,
    val startingLife: Int = 20,
    val playerId: EntityId? = null,
    /**
     * Name of the card in [deck] that should be designated this player's commander. Required when
     * [GameConfig.format] is [Format.Commander]. Phase 1 supports a single commander; partner /
     * Background pairings are Phase 4 territory.
     */
    val commanderCardName: String? = null,
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
    val startingPlayerIndex: Int? = null,
    /**
     * Game-mode configuration. Defaults to [Format.Standard] so existing callers keep their
     * 20-life / 7-card / no-commander behaviour without changes.
     */
    val format: Format = Format.Standard,

    /**
     * Which opponents creatures may attack (CR 802 / 803). Defaults to [AttackMode.MULTIPLE].
     * Set by the Free-for-All lobby (CR 806.2b); irrelevant in two-player games.
     */
    val attackMode: com.wingedsheep.sdk.core.AttackMode = com.wingedsheep.sdk.core.AttackMode.MULTIPLE,

    /**
     * Seed for the game's deterministic RNG (turn order, library shuffles, coin flips, every
     * "at random" choice). When null, the initializer draws a fresh seed from entropy so live
     * play stays random — but the chosen seed is always recorded on [InitializationResult.seed],
     * so any game can be reproduced after the fact by replaying with that seed. Supply an explicit
     * value for reproducible runs (replays, MCTS, the cross-engine parity harness, tests).
     */
    val seed: Long? = null,
)

/**
 * Result of game initialization.
 */
data class InitializationResult(
    val state: GameState,
    val events: List<GameEvent>,
    val playerIds: List<EntityId>,
    /**
     * The seed actually used to initialize [state]'s RNG — either [GameConfig.seed] or, when that
     * was null, the freshly drawn entropy seed. Persist this alongside the game to make it
     * reproducible.
     */
    val seed: Long,
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
    private val cardRegistry: CardRegistry,
    /**
     * Optional registry of per-printing metadata. When supplied, deck entries that pin a
     * specific [PrintingRef] cause the engine to stamp that printing's image URLs onto the
     * per-entity [com.wingedsheep.engine.state.components.identity.CardComponent].
     * Null is fine for tests and any path that never pins printings — the engine falls
     * back to the canonical [CardDefinition.metadata] image.
     */
    private val printingRegistry: PrintingRegistry? = null,
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
        // Resolve the seed up front: explicit when supplied, otherwise fresh entropy. Either way it
        // is recorded on the result so the game is reproducible later. This clock read is the one
        // sanctioned non-determinism boundary — once seeded, the engine is a pure function again.
        val resolvedSeed: Long = config.seed ?: System.nanoTime()
        var state = GameState(format = config.format, attackMode = config.attackMode, rng = GameRng.seeded(resolvedSeed))
        val playerIds = mutableListOf<EntityId>()

        // Validate Commander-format prerequisites up front. Each player must designate a
        // commander card name. The commander is NOT counted in [Deck.cards] (matches the deck
        // validator's convention and CR 903.6a) — it's instantiated separately in step 3 below
        // and routed to Zone.COMMAND.
        if (config.format is Format.Commander) {
            for (playerConfig in config.players) {
                val name = playerConfig.commanderCardName
                require(!name.isNullOrBlank()) {
                    "Commander format requires PlayerConfig.commanderCardName for player '${playerConfig.name}'"
                }
                // Make sure the registry can resolve the commander before we touch any state —
                // we'd rather fail here than mid-init with a half-built game.
                cardRegistry.requireCard(name)
            }
        }

        // Momir Basic gives every player the same Vanguard avatar in the command zone; fail fast
        // if the registry can't resolve it rather than mid-init.
        if (config.format is Format.MomirBasic) {
            cardRegistry.requireCard(config.format.avatarCardName)
        }

        // Format-driven starting life. Commander / Momir Basic override the per-player default of
        // 20 so callers don't have to remember to set startingLife alongside the format.
        val formatStartingLife: Int? = when (val f = config.format) {
            is Format.Commander -> f.startingLife
            is Format.MomirBasic -> f.startingLife
            else -> null
        }

        // 1. Create player entities
        for (playerConfig in config.players) {
            val playerId = playerConfig.playerId ?: run {
                val (id, s) = state.newEntity()
                state = s
                id
            }
            playerIds.add(playerId)

            val startingLife = formatStartingLife ?: playerConfig.startingLife

            val playerContainer = ComponentContainer.of(
                PlayerComponent(playerConfig.name, startingLife),
                LifeTotalComponent(startingLife),
                ManaPoolComponent(),
                LandDropsComponent(),
                PlayerTurnsTakenComponent(count = 0),
                MulliganStateComponent(
                    mulligansTaken = 0,
                    hasKept = config.skipMulligans,  // Auto-keep if skipping mulligans
                    // CR 800.6: in a multiplayer game (began with >2 players) the first mulligan
                    // is free. Two-player games keep the plain London Mulligan.
                    freeMulligan = config.players.size > 2
                )
            )

            state = state.withEntity(playerId, playerContainer)
        }

        // 2. Set turn order
        val shuffledOrder = if (config.startingPlayerIndex != null) {
            val idx = config.startingPlayerIndex
            playerIds.subList(idx, playerIds.size) + playerIds.subList(0, idx)
        } else {
            val (order, shuffledState) = state.nextRandom { shuffle(playerIds) }
            state = shuffledState
            order
        }
        state = state.copy(
            turnOrder = shuffledOrder,
            activePlayerId = shuffledOrder.first(),
            priorityPlayerId = shuffledOrder.first(),
            turnNumber = 1  // First turn is turn 1, not turn 0
        )
        // The very first turn of the game doesn't go through TurnManager.startTurn
        // (it's set up directly here), so seed the active player's
        // PlayerTurnsTakenComponent to 1 to keep the "your Nth turn of the game"
        // semantics consistent. All subsequent turn transitions go through startTurn
        // and increment normally.
        state = state.updateEntity(shuffledOrder.first()) { container ->
            container.with(PlayerTurnsTakenComponent(count = 1))
        }

        // 3. Instantiate cards and place in libraries (or command zone for commanders).
        // Commander setup runs first so a CommanderRegistryComponent is attached to the player
        // entity before the rest of the deck flows into the library. Phase 1 supports a single
        // commander per player; CommanderRegistryComponent is a list so Partner / Background
        // (Phase 4) can append without a schema change.
        for ((index, playerConfig) in config.players.withIndex()) {
            val playerId = playerIds[index]

            val commanderName: String? = when {
                config.format is Format.Commander -> playerConfig.commanderCardName
                else -> null
            }
            val commanderEntityIds = mutableListOf<EntityId>()

            if (commanderName != null) {
                val cardDef = cardRegistry.requireCard(commanderName)
                val (cardId, stateWithId) = state.newEntity()
                state = stateWithId
                val cardContainer = createCardEntity(cardDef, playerId, playerConfig.deck.commanderPrinting).with(
                    com.wingedsheep.engine.state.components.identity.CommanderComponent(ownerId = playerId)
                )
                state = state.withEntity(cardId, cardContainer)
                state = state.addToZone(ZoneKey(playerId, Zone.COMMAND), cardId)
                commanderEntityIds.add(cardId)
            }

            // Momir Basic: place this player's Vanguard avatar in the command zone. It never enters
            // the battlefield or stack — it grants its activated ability from the command zone (CR
            // for Vanguard avatars), surfaced by CommandZoneAbilityEnumerator.
            (config.format as? Format.MomirBasic)?.let { momir ->
                val avatarDef = cardRegistry.requireCard(momir.avatarCardName)
                val (avatarId, stateWithAvatar) = state.newEntity()
                state = stateWithAvatar
                val avatarContainer = createCardEntity(avatarDef, playerId).with(
                    com.wingedsheep.engine.state.components.identity.VanguardAvatarComponent(ownerId = playerId)
                )
                state = state.withEntity(avatarId, avatarContainer)
                state = state.addToZone(ZoneKey(playerId, Zone.COMMAND), avatarId)
            }

            // Prefer rich [CardEntry] iteration when the deck has it; fall back to the legacy
            // name-only [Deck.cards] list. Both paths produce identical libraries when no
            // printings are pinned — the rich path additionally honours per-entry printings.
            val libraryEntries: List<CardEntry> = playerConfig.deck.cardEntries.ifEmpty {
                playerConfig.deck.cards.map { CardEntry(it) }
            }
            for (entry in libraryEntries) {
                val cardDef = cardRegistry.requireCard(entry.name)
                val (cardId, stateWithId) = state.newEntity()
                state = stateWithId
                val cardContainer = createCardEntity(cardDef, playerId, entry.printing)
                state = state.withEntity(cardId, cardContainer)
                state = state.addToZone(ZoneKey(playerId, Zone.LIBRARY), cardId)
            }

            if (commanderEntityIds.isNotEmpty()) {
                state = state.updateEntity(playerId) { c ->
                    c.with(
                        com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent(commanderEntityIds)
                    )
                }
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

        return InitializationResult(state, events, playerIds, resolvedSeed)
    }

    /**
     * Create a card entity from a card definition.
     *
     * When [printingRef] is non-null and resolves in [printingRegistry], the per-entity
     * [CardComponent] image URLs are taken from the chosen printing rather than from
     * [CardDefinition.metadata]. Falls back gracefully when the registry is absent or
     * the ref doesn't resolve — that's the legacy single-printing path.
     */
    private fun createCardEntity(
        cardDef: CardDefinition,
        ownerId: EntityId,
        printingRef: PrintingRef? = null,
    ): ComponentContainer = CardEntityFactory.create(cardDef, ownerId, printingRef, printingRegistry)

    /**
     * Shuffle a player's library.
     */
    private fun shuffleLibrary(state: GameState, playerId: EntityId): GameState {
        val libraryKey = ZoneKey(playerId, Zone.LIBRARY)
        val (library, newState) = state.nextRandom { shuffle(state.getZone(libraryKey)) }
        return newState.copy(zones = newState.zones + (libraryKey to library))
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

        println("[HandSmoother] deck=${library.size} cards, landRatio=${"%.3f".format(deckLandRatio)}, colorRatios=$deckColorRatios")

        // Generate candidate hands. Thread the RNG through each candidate shuffle so hand smoothing
        // is reproducible too; carry the advanced state into the rest of the routine below.
        var workingState = state
        val candidates = mutableListOf<List<EntityId>>()
        val effectiveCandidateCount = candidateCount.coerceIn(2, 3)

        repeat(effectiveCandidateCount) {
            val (shuffledLibrary, advanced) = workingState.nextRandom { shuffle(library) }
            workingState = advanced
            candidates.add(shuffledLibrary.takeLast(count))
        }

        // Score each candidate and select the best one
        val bestCandidate = candidates.minByOrNull { candidate ->
            scoreHand(state, candidate, deckLandRatio, deckColorRatios)
        } ?: candidates.first()

        // Log candidate details for debugging
        candidates.forEachIndexed { index, candidate ->
            val landCount = candidate.count { cardId ->
                state.getEntity(cardId)?.get<CardComponent>()?.typeLine?.isLand == true
            }
            val score = scoreHand(state, candidate, deckLandRatio, deckColorRatios)
            val selected = if (candidate === bestCandidate) " [SELECTED]" else ""
            println("[HandSmoother] candidate ${index + 1}: ${landCount} lands, score=${"%.4f".format(score)}$selected")
        }

        // Apply the selected hand to the state (continuing from the RNG-advanced workingState).
        var currentState = workingState
        val events = mutableListOf<GameEvent>()
        val drawnCardIds = mutableListOf<EntityId>()

        // Remove selected cards from library
        var newLibrary = library.toMutableList()
        for (cardId in bestCandidate) {
            newLibrary.remove(cardId)
        }

        // Shuffle the remaining library
        val (shuffledRemaining, advancedState) = currentState.nextRandom { shuffle(newLibrary) }
        newLibrary = shuffledRemaining.toMutableList()
        currentState = advancedState.copy(zones = advancedState.zones + (libraryKey to newLibrary))

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

        // Heavily penalize extreme land counts (0 lands or all lands) in mixed decks.
        // Without this, a 0-land hand gets zero color deviation (no lands to evaluate),
        // which can make it outscore hands that have lands but poor color distribution.
        if (lands.isEmpty() && deckLandRatio > 0.0) return 10.0
        if (lands.size == hand.size && deckLandRatio < 1.0) return 10.0

        // Land ratio deviation
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

        // Land ratio is primary, colour distribution is a light tiebreaker.
        // The color weight must be low enough that it can never make a hand with
        // correct land count score worse than one with wrong land count.
        return landRatioDeviation + colorDeviation * 0.15
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
