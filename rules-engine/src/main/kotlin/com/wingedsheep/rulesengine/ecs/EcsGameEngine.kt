package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.ecs.action.*
import com.wingedsheep.rulesengine.ecs.components.LifeComponent
import kotlin.random.Random

/**
 * Main entry point for the ECS-based game engine.
 * Handles game setup, execution, and flow control.
 *
 * This replaces the old GameEngine by working directly with EcsGameState
 * and EcsActionHandler instead of the old GameState and ActionExecutor.
 */
object EcsGameEngine {

    /** Standard opening hand size */
    const val OPENING_HAND_SIZE = 7

    /** Standard starting life total */
    const val STARTING_LIFE = LifeComponent.STARTING_LIFE

    private val actionHandler = EcsActionHandler()

    /**
     * Create a new game with the given players.
     *
     * @param players List of (EntityId, playerName) pairs
     * @param startingPlayerId Optional player to start the game. If null, first player is used.
     * @param random Random source (for future use in determining starting player)
     * @return The initial game state (before hands are drawn)
     */
    fun createGame(
        players: List<Pair<EntityId, String>>,
        startingPlayerId: EntityId? = null,
        random: Random = Random
    ): EcsGameState {
        require(players.size >= 2) { "Game requires at least 2 players" }

        // Create the base game state
        val state = EcsGameState.newGame(players)

        // If a specific starting player is requested, we would update the turn state
        // For now, the first player in the list starts (matching TurnState.newGame behavior)
        return state
    }

    /**
     * Set up the game: shuffle libraries and draw opening hands.
     *
     * @param state The game state to set up
     * @param random Random source for shuffling
     * @return Result containing the setup game state
     */
    fun setupGame(
        state: EcsGameState,
        random: Random = Random
    ): EcsSetupResult {
        var currentState = state
        val events = mutableListOf<EcsActionEvent>()

        // Shuffle each player's library
        for (playerId in currentState.getPlayerIds()) {
            val libraryZone = ZoneId.library(playerId)
            val library = currentState.getZone(libraryZone)
            val shuffled = library.shuffled(random)
            currentState = currentState.copy(
                zones = currentState.zones + (libraryZone to shuffled)
            )
        }

        // Draw opening hands (7 cards each)
        for (playerId in currentState.getPlayerIds()) {
            val result = actionHandler.execute(currentState, EcsDrawCard(playerId, OPENING_HAND_SIZE))
            when (result) {
                is EcsActionResult.Success -> {
                    currentState = result.state
                    events.addAll(result.events)
                }
                is EcsActionResult.Failure -> {
                    return EcsSetupResult.Failure(result.reason)
                }
            }
        }

        return EcsSetupResult.Success(currentState, events)
    }

    /**
     * Load decks for all players using a DeckLoader.
     *
     * This is a convenience method that integrates DeckLoader with the game setup flow.
     *
     * @param state The game state to load decks into
     * @param deckLoader The DeckLoader configured with card registries
     * @param decks Map of player IDs to their deck lists
     * @return Result containing the updated state or failure info
     */
    fun loadDecks(
        state: EcsGameState,
        deckLoader: DeckLoader,
        decks: Map<EntityId, Map<String, Int>>
    ): DeckLoader.DeckLoadResult {
        return deckLoader.loadDecks(state, decks)
    }

    /**
     * Execute a mulligan for a player (London mulligan rules).
     *
     * @param state Current game state
     * @param playerId Player taking the mulligan
     * @param cardsToBottom Cards to put on the bottom of the library after drawing new hand
     * @param random Random source for shuffling
     * @return Result of the mulligan
     */
    fun executeMulligan(
        state: EcsGameState,
        playerId: EntityId,
        cardsToBottom: List<EntityId>,
        random: Random = Random
    ): EcsMulliganResult {
        val hand = state.getHand(playerId)
        val mulliganCount = OPENING_HAND_SIZE - hand.size + cardsToBottom.size

        // Validate that the player has the cards to put on bottom
        for (cardId in cardsToBottom) {
            if (!state.isInZone(cardId, ZoneId.hand(playerId))) {
                return EcsMulliganResult.Failure("Card not in hand: $cardId")
            }
        }

        var currentState = state
        val events = mutableListOf<EcsActionEvent>()

        // Put the specified cards on the bottom of the library
        for (cardId in cardsToBottom) {
            val handZone = ZoneId.hand(playerId)
            val libraryZone = ZoneId.library(playerId)
            currentState = currentState
                .removeFromZone(cardId, handZone)
                .addToZoneBottom(cardId, libraryZone)
        }

        return EcsMulliganResult.Success(currentState, events, mulliganCount)
    }

    /**
     * Execute a full mulligan cycle: shuffle hand into library, draw 7, prepare to put cards on bottom.
     *
     * @param state Current game state
     * @param playerId Player taking the mulligan
     * @param mulliganNumber Which mulligan this is (1 for first mulligan, 2 for second, etc.)
     * @param random Random source for shuffling
     * @return Result with new hand drawn (player must then choose cards to put on bottom)
     */
    fun startMulligan(
        state: EcsGameState,
        playerId: EntityId,
        mulliganNumber: Int,
        random: Random = Random
    ): EcsMulliganResult {
        require(mulliganNumber >= 1) { "Mulligan number must be at least 1" }

        val handZone = ZoneId.hand(playerId)
        val libraryZone = ZoneId.library(playerId)
        var currentState = state
        val events = mutableListOf<EcsActionEvent>()

        // Shuffle hand into library
        val handCards = currentState.getZone(handZone)
        for (cardId in handCards) {
            currentState = currentState
                .removeFromZone(cardId, handZone)
                .addToZone(cardId, libraryZone)
        }

        // Shuffle library
        val library = currentState.getZone(libraryZone)
        val shuffled = library.shuffled(random)
        currentState = currentState.copy(
            zones = currentState.zones + (libraryZone to shuffled)
        )

        // Draw new hand of 7
        val result = actionHandler.execute(currentState, EcsDrawCard(playerId, OPENING_HAND_SIZE))
        when (result) {
            is EcsActionResult.Success -> {
                currentState = result.state
                events.addAll(result.events)
            }
            is EcsActionResult.Failure -> {
                return EcsMulliganResult.Failure(result.reason)
            }
        }

        // Player now needs to put mulliganNumber cards on the bottom
        return EcsMulliganResult.Success(currentState, events, mulliganNumber)
    }

    /**
     * Execute an action and return the result.
     */
    fun executeAction(state: EcsGameState, action: EcsAction): EcsActionResult {
        return actionHandler.execute(state, action)
    }

    /**
     * Execute multiple actions in sequence.
     */
    fun executeActions(state: EcsGameState, actions: List<EcsAction>): EcsActionResult {
        return actionHandler.executeAll(state, actions)
    }

    /**
     * Check state-based actions and return the result.
     */
    fun checkStateBasedActions(state: EcsGameState): EcsActionResult {
        return actionHandler.execute(state, EcsCheckStateBasedActions())
    }

    /**
     * Check if the game is over.
     */
    fun isGameOver(state: EcsGameState): Boolean {
        return state.isGameOver
    }

    /**
     * Get the winner of the game (null if game is not over or was a draw).
     */
    fun getWinner(state: EcsGameState): EntityId? {
        return state.winner
    }

    // =========================================================================
    // Priority State Machine (Rule 116)
    // =========================================================================

    /**
     * Process priority: Run SBA -> Check Triggers -> Grant Priority.
     * This is the core game loop that should be called after any action.
     *
     * @param state Current game state
     * @return Result indicating who has priority or if the game is over
     */
    fun processPriority(state: EcsGameState): PriorityResult {
        var currentState = state

        // Check if game is over
        if (currentState.isGameOver) {
            return PriorityResult.GameOver(currentState, currentState.winner)
        }

        // Step 1: Check State-Based Actions (loop until none)
        val sbaResult = checkStateBasedActions(currentState)
        if (sbaResult is EcsActionResult.Success) {
            currentState = sbaResult.state
        }

        // Check again if game ended due to SBAs
        if (currentState.isGameOver) {
            return PriorityResult.GameOver(currentState, currentState.winner)
        }

        // Step 2: Triggered abilities would be stacked here (future implementation)
        // TODO: Detect triggered abilities from events and add to stack in APNAP order

        // Step 3: Return state with priority granted
        return PriorityResult.PriorityGranted(
            state = currentState,
            priorityPlayer = currentState.turnState.priorityPlayer
        )
    }

    /**
     * Handle when all players pass priority in succession.
     *
     * If the stack is not empty: resolve the top object and reset passes.
     * If the stack is empty: advance to the next step/phase.
     *
     * @param state Current game state (should have allPlayersPassed() == true)
     * @return Updated game state
     */
    fun resolvePassedPriority(state: EcsGameState): EcsGameState {
        val turnState = state.turnState

        return if (state.getStack().isNotEmpty()) {
            // Stack not empty: resolve top of stack, reset passes
            val resolveResult = actionHandler.execute(state, EcsResolveTopOfStack())
            when (resolveResult) {
                is EcsActionResult.Success -> resolveResult.state.copy(
                    turnState = turnState.resetConsecutivePasses().resetPriorityToActivePlayer()
                )
                is EcsActionResult.Failure -> state // Resolution failed, keep current state
            }
        } else {
            // Stack empty: advance step/phase, reset passes
            state.copy(
                turnState = turnState.advanceStep()
            )
        }
    }

    /**
     * Execute a player action that resets the consecutive passes counter.
     * Use this for any action other than passing priority.
     *
     * @param state Current game state
     * @param action The action to execute
     * @return Result with updated state (passes reset to 0 on success)
     */
    fun executePlayerAction(state: EcsGameState, action: EcsAction): EcsActionResult {
        val result = actionHandler.execute(state, action)
        return when (result) {
            is EcsActionResult.Success -> {
                // Reset consecutive passes after any non-pass action
                val newTurnState = result.state.turnState.resetConsecutivePasses()
                EcsActionResult.Success(
                    result.state.copy(turnState = newTurnState),
                    result.action,
                    result.events
                )
            }
            is EcsActionResult.Failure -> result
        }
    }
}

/**
 * Result of game setup.
 */
sealed interface EcsSetupResult {
    data class Success(
        val state: EcsGameState,
        val events: List<EcsActionEvent>
    ) : EcsSetupResult

    data class Failure(val error: String) : EcsSetupResult
}

/**
 * Result of a mulligan operation.
 */
sealed interface EcsMulliganResult {
    /**
     * @param state New game state after mulligan
     * @param events Events that occurred
     * @param cardsToPutOnBottom Number of cards the player must put on the bottom of their library
     */
    data class Success(
        val state: EcsGameState,
        val events: List<EcsActionEvent>,
        val cardsToPutOnBottom: Int
    ) : EcsMulliganResult

    data class Failure(val error: String) : EcsMulliganResult
}

/**
 * Result of processing priority.
 */
sealed interface PriorityResult {
    /**
     * Priority has been granted to a player.
     * The player can now take actions or pass priority.
     */
    data class PriorityGranted(
        val state: EcsGameState,
        val priorityPlayer: EntityId
    ) : PriorityResult

    /**
     * The game has ended.
     */
    data class GameOver(
        val state: EcsGameState,
        val winner: EntityId?
    ) : PriorityResult
}
