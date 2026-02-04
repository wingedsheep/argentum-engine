package com.wingedsheep.gameserver.sealed

import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * State of a sealed draft session.
 */
enum class SealedSessionState {
    /** Waiting for second player to join */
    WAITING_FOR_PLAYERS,
    /** Both players are building their decks */
    DECK_BUILDING,
    /** At least one player has submitted, waiting for the other */
    WAITING_FOR_DECKS,
    /** Both players have submitted decks, ready to start the game */
    READY
}

/**
 * State for a single player in a sealed session.
 */
data class SealedPlayerState(
    val session: PlayerSession,
    val cardPool: List<CardDefinition>,
    var submittedDeck: Map<String, Int>? = null
) {
    val hasSubmittedDeck: Boolean get() = submittedDeck != null
}

/**
 * Manages a sealed draft session where players open boosters and build decks.
 *
 * Lifecycle:
 * 1. First player creates session (WAITING_FOR_PLAYERS)
 * 2. Second player joins, pools are generated for both (DECK_BUILDING)
 * 3. Players build and submit 40+ card decks (WAITING_FOR_DECKS)
 * 4. Once both submit, session is READY to start the actual game
 */
class SealedSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val setCodes: List<String>,
    val setNames: List<String>,
    private val boosterGenerator: BoosterGenerator
) {

    /** Player states indexed by player ID */
    val players = ConcurrentHashMap<EntityId, SealedPlayerState>()

    /** Current session state */
    @Volatile
    var state: SealedSessionState = SealedSessionState.WAITING_FOR_PLAYERS
        private set

    /** Basic lands available for deck building */
    val basicLands: Map<String, CardDefinition> by lazy {
        boosterGenerator.getBasicLands(setCodes)
    }

    val isFull: Boolean get() = players.size >= 2

    val player1: SealedPlayerState? get() = players.values.firstOrNull()
    val player2: SealedPlayerState? get() = players.values.drop(1).firstOrNull()

    /**
     * Add a player to the session.
     * Does NOT generate the pool yet - that happens when second player joins.
     *
     * @return The player's EntityId
     */
    fun addPlayer(session: PlayerSession): EntityId {
        require(!isFull) { "Sealed session is full" }

        val playerId = session.playerId

        // Add player with empty pool initially
        players[playerId] = SealedPlayerState(
            session = session,
            cardPool = emptyList()
        )

        session.currentGameSessionId = sessionId

        return playerId
    }

    /**
     * Generate sealed pools for all players and transition to DECK_BUILDING state.
     * Should be called when the second player joins.
     */
    fun generatePools() {
        require(players.size == 2) { "Need exactly 2 players to generate pools" }
        require(state == SealedSessionState.WAITING_FOR_PLAYERS) { "Pools already generated" }

        // Generate a shared distribution seed so all players get the same
        // set distribution (e.g., both get 3 Portal + 2 Onslaught boosters)
        val distributionSeed = System.currentTimeMillis()

        // Generate unique pools for each player (card contents differ, but set distribution is the same)
        players.forEach { (playerId, playerState) ->
            val pool = boosterGenerator.generateSealedPool(setCodes, distributionSeed = distributionSeed)
            players[playerId] = playerState.copy(cardPool = pool)
        }

        state = SealedSessionState.DECK_BUILDING
    }

    /**
     * Get the card pool for a specific player.
     */
    fun getPlayerPool(playerId: EntityId): List<CardDefinition> {
        return players[playerId]?.cardPool ?: emptyList()
    }

    /**
     * Get the player session for a player ID.
     */
    fun getPlayerSession(playerId: EntityId): PlayerSession? {
        return players[playerId]?.session
    }

    /**
     * Submit a deck for a player.
     *
     * @param playerId The player submitting
     * @param deckList Map of card name to count
     * @return true if deck is valid and accepted
     */
    fun submitDeck(playerId: EntityId, deckList: Map<String, Int>): DeckSubmissionResult {
        val playerState = players[playerId]
            ?: return DeckSubmissionResult.Error("Player not in session")

        if (playerState.hasSubmittedDeck) {
            return DeckSubmissionResult.Error("Deck already submitted")
        }

        // Validate deck
        val validationResult = validateDeck(playerState.cardPool, deckList)
        if (validationResult != null) {
            return DeckSubmissionResult.Error(validationResult)
        }

        // Update player state with submitted deck
        players[playerId] = playerState.copy(submittedDeck = deckList)

        // Update session state
        state = if (bothPlayersSubmitted()) {
            SealedSessionState.READY
        } else {
            SealedSessionState.WAITING_FOR_DECKS
        }

        return DeckSubmissionResult.Success(bothReady = state == SealedSessionState.READY)
    }

    /**
     * Check if both players have submitted their decks.
     */
    fun bothPlayersSubmitted(): Boolean {
        return players.values.all { it.hasSubmittedDeck }
    }

    /**
     * Check if the session is ready to start the game.
     */
    fun bothReady(): Boolean {
        return state == SealedSessionState.READY
    }

    /**
     * Get the opponent's player ID.
     */
    fun getOpponentId(playerId: EntityId): EntityId? {
        return players.keys.firstOrNull { it != playerId }
    }

    /**
     * Get the submitted deck for a player.
     */
    fun getSubmittedDeck(playerId: EntityId): Map<String, Int>? {
        return players[playerId]?.submittedDeck
    }

    /**
     * Validate a submitted deck against the player's pool.
     *
     * Rules:
     * - Minimum 40 cards
     * - Non-basic land cards must come from the player's pool
     * - Can't include more copies than available in pool
     * - Basic lands have no limit
     *
     * @return null if valid, error message if invalid
     */
    private fun validateDeck(pool: List<CardDefinition>, deckList: Map<String, Int>): String? {
        // Check minimum deck size
        val totalCards = deckList.values.sum()
        if (totalCards < 40) {
            return "Deck must have at least 40 cards (has $totalCards)"
        }

        // Count available copies from pool (excluding basic lands)
        val poolCounts = pool
            .filterNot { it.typeLine.isBasicLand }
            .groupingBy { it.name }
            .eachCount()

        // Basic land names
        val basicLandNames = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

        // Validate each card in the deck
        for ((cardName, count) in deckList) {
            if (count <= 0) continue

            // Basic lands are unlimited
            if (cardName in basicLandNames) {
                continue
            }

            // Check if card is in pool
            val availableInPool = poolCounts[cardName] ?: 0
            if (availableInPool == 0) {
                return "Card not in pool: $cardName"
            }

            // Check count against pool
            if (count > availableInPool) {
                return "Not enough copies of $cardName in pool (have $availableInPool, trying to use $count)"
            }

            // Enforce 4-copy limit
            if (count > 4) {
                return "Cannot have more than 4 copies of $cardName (have $count)"
            }
        }

        return null
    }

    sealed interface DeckSubmissionResult {
        data class Success(val bothReady: Boolean) : DeckSubmissionResult
        data class Error(val message: String) : DeckSubmissionResult
    }
}
