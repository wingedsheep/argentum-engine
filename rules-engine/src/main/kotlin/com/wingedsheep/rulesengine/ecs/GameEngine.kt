package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.ecs.action.*
import com.wingedsheep.rulesengine.ecs.components.LifeComponent
import kotlin.random.Random

/**
 * Main entry point for the ECS-based game engine.
 * Handles game setup, execution, and flow control.
 *
 * This replaces the old GameEngine by working directly with GameState
 * and GameActionHandler instead of the old GameState and ActionExecutor.
 */
object GameEngine {

    /** Standard opening hand size */
    const val OPENING_HAND_SIZE = 7

    /** Standard starting life total */
    const val STARTING_LIFE = LifeComponent.STARTING_LIFE

    private val actionHandler = GameActionHandler()

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
    ): GameState {
        require(players.size >= 2) { "Game requires at least 2 players" }

        // Create the base game state
        val state = GameState.newGame(players)

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
        state: GameState,
        random: Random = Random
    ): SetupResult {
        var currentState = state
        val events = mutableListOf<GameActionEvent>()

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
            val result = actionHandler.execute(currentState, DrawCard(playerId, OPENING_HAND_SIZE))
            when (result) {
                is GameActionResult.Success -> {
                    currentState = result.state
                    events.addAll(result.events)
                }
                is GameActionResult.Failure -> {
                    return SetupResult.Failure(result.reason)
                }
            }
        }

        return SetupResult.Success(currentState, events)
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
        state: GameState,
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
        state: GameState,
        playerId: EntityId,
        cardsToBottom: List<EntityId>,
        random: Random = Random
    ): MulliganResult {
        val hand = state.getHand(playerId)
        val mulliganCount = OPENING_HAND_SIZE - hand.size + cardsToBottom.size

        // Validate that the player has the cards to put on bottom
        for (cardId in cardsToBottom) {
            if (!state.isInZone(cardId, ZoneId.hand(playerId))) {
                return MulliganResult.Failure("Card not in hand: $cardId")
            }
        }

        var currentState = state
        val events = mutableListOf<GameActionEvent>()

        // Put the specified cards on the bottom of the library
        for (cardId in cardsToBottom) {
            val handZone = ZoneId.hand(playerId)
            val libraryZone = ZoneId.library(playerId)
            currentState = currentState
                .removeFromZone(cardId, handZone)
                .addToZoneBottom(cardId, libraryZone)
        }

        return MulliganResult.Success(currentState, events, mulliganCount)
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
        state: GameState,
        playerId: EntityId,
        mulliganNumber: Int,
        random: Random = Random
    ): MulliganResult {
        require(mulliganNumber >= 1) { "Mulligan number must be at least 1" }

        val handZone = ZoneId.hand(playerId)
        val libraryZone = ZoneId.library(playerId)
        var currentState = state
        val events = mutableListOf<GameActionEvent>()

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
        val result = actionHandler.execute(currentState, DrawCard(playerId, OPENING_HAND_SIZE))
        when (result) {
            is GameActionResult.Success -> {
                currentState = result.state
                events.addAll(result.events)
            }
            is GameActionResult.Failure -> {
                return MulliganResult.Failure(result.reason)
            }
        }

        // Player now needs to put mulliganNumber cards on the bottom
        return MulliganResult.Success(currentState, events, mulliganNumber)
    }

    /**
     * Execute an action and return the result.
     */
    fun executeAction(state: GameState, action: GameAction): GameActionResult {
        return actionHandler.execute(state, action)
    }

    /**
     * Execute multiple actions in sequence.
     */
    fun executeActions(state: GameState, actions: List<GameAction>): GameActionResult {
        return actionHandler.executeAll(state, actions)
    }

    /**
     * Check state-based actions and return the result.
     */
    fun checkStateBasedActions(state: GameState): GameActionResult {
        return actionHandler.execute(state, CheckStateBasedActions())
    }

    /**
     * Check if the game is over.
     */
    fun isGameOver(state: GameState): Boolean {
        return state.isGameOver
    }

    /**
     * Get the winner of the game (null if game is not over or was a draw).
     */
    fun getWinner(state: GameState): EntityId? {
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
    fun processPriority(state: GameState): PriorityResult {
        var currentState = state

        // Check if game is over
        if (currentState.isGameOver) {
            return PriorityResult.GameOver(currentState, currentState.winner)
        }

        // Step 1: Check State-Based Actions (loop until none)
        val sbaResult = checkStateBasedActions(currentState)
        if (sbaResult is GameActionResult.Success) {
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
    fun resolvePassedPriority(state: GameState): GameState {
        val turnState = state.turnState

        return if (state.getStack().isNotEmpty()) {
            // Stack not empty: resolve top of stack, reset passes
            val resolveResult = actionHandler.execute(state, ResolveTopOfStack())
            when (resolveResult) {
                is GameActionResult.Success -> resolveResult.state.copy(
                    turnState = turnState.resetConsecutivePasses().resetPriorityToActivePlayer()
                )
                is GameActionResult.Failure -> state // Resolution failed, keep current state
            }
        } else {
            // Stack empty: advance step/phase, reset passes
            val newTurnState = turnState.advanceStep()
            var newState = state.copy(turnState = newTurnState)

            // Detect maintenance actions if we advanced to a new turn
            if (newTurnState.turnNumber > turnState.turnNumber) {
                newState = handleTurnStart(newState, newTurnState.activePlayer)
            }

            // Handle step-specific turn-based actions
            newState = handleStepBasedActions(newState, newTurnState)

            // When transitioning to DECLARE_BLOCKERS, capture eligible blockers
            // (MTG Rule 509.1a: creatures entering after blockers are declared cannot block)
            if (newTurnState.step == com.wingedsheep.rulesengine.game.Step.DECLARE_BLOCKERS) {
                newState = captureEligibleBlockers(newState)
            }

            newState
        }
    }

    /**
     * Handle turn-based actions that occur at the beginning of specific steps.
     * - UNTAP: Untap all permanents controlled by active player (Rule 502.3)
     * - DRAW: Active player draws a card (Rule 504.1)
     */
    private fun handleStepBasedActions(state: GameState, turnState: com.wingedsheep.rulesengine.game.TurnState): GameState {
        var currentState = state
        val activePlayerId = turnState.activePlayer

        when (turnState.step) {
            com.wingedsheep.rulesengine.game.Step.UNTAP -> {
                // Rule 502.3: Untap all permanents controlled by active player
                val untapResult = actionHandler.execute(currentState, UntapAll(activePlayerId))
                if (untapResult is GameActionResult.Success) {
                    currentState = untapResult.state
                }
            }
            com.wingedsheep.rulesengine.game.Step.DRAW -> {
                // Rule 504.1: Active player draws a card
                // Exception: First player on first turn doesn't draw (Rule 103.7a)
                val skipFirstDraw = turnState.turnNumber == 1 && turnState.isFirstTurn
                if (!skipFirstDraw) {
                    val drawResult = actionHandler.execute(currentState, DrawCard(activePlayerId, 1))
                    if (drawResult is GameActionResult.Success) {
                        currentState = drawResult.state
                    }
                }
            }
            else -> {
                // No automatic actions for other steps
            }
        }

        return currentState
    }

    /**
     * Handle maintenance actions at the start of a turn.
     * - Reset lands played for all players.
     * - Remove summoning sickness from the active player's creatures.
     */
    private fun handleTurnStart(state: GameState, activePlayerId: EntityId): GameState {
        var currentState = state

        // Reset lands played for all players
        for (playerId in currentState.getPlayerIds()) {
            val result = actionHandler.execute(currentState, ResetLandsPlayed(playerId))
            if (result is GameActionResult.Success) {
                currentState = result.state
            }
        }

        // Remove summoning sickness for the active player's creatures
        val sicknessResult = actionHandler.execute(currentState, RemoveAllSummoningSickness(activePlayerId))
        if (sicknessResult is GameActionResult.Success) {
            currentState = sicknessResult.state
        }

        return currentState
    }

    /**
     * Capture the set of creatures eligible to be declared as blockers.
     *
     * This is called when transitioning to the DECLARE_BLOCKERS step.
     * Only creatures that are on the battlefield at this moment can be declared
     * as blockers - creatures entering later in the step cannot block.
     */
    private fun captureEligibleBlockers(state: GameState): GameState {
        val combat = state.combat ?: return state

        // Get all creatures controlled by the defending player that are currently on battlefield
        val eligibleBlockers = state.getCreaturesControlledBy(combat.defendingPlayer).toSet()

        // Update the combat state with eligible blockers
        val updatedCombat = combat.withEligibleBlockers(eligibleBlockers)
        return state.copy(combat = updatedCombat)
    }

    /**
     * Execute a player action, handling the consecutive passes counter appropriately.
     *
     * For PassPriority: The action handler already increments consecutive passes.
     * For other actions: Reset consecutive passes to 0 (the action "breaks" the pass chain).
     *
     * @param state Current game state
     * @param action The action to execute
     * @return Result with updated state
     */
    fun executePlayerAction(state: GameState, action: GameAction): GameActionResult {
        val result = actionHandler.execute(state, action)
        return when (result) {
            is GameActionResult.Success -> {
                // Only reset consecutive passes for non-pass actions.
                // PassPriority already handles the pass counter in the action handler.
                val newState = if (action is PassPriority) {
                    result.state
                } else {
                    val newTurnState = result.state.turnState.resetConsecutivePasses()
                    result.state.copy(turnState = newTurnState)
                }
                GameActionResult.Success(newState, result.action, result.events)
            }
            is GameActionResult.Failure -> result
        }
    }
}

/**
 * Result of game setup.
 */
sealed interface SetupResult {
    data class Success(
        val state: GameState,
        val events: List<GameActionEvent>
    ) : SetupResult

    data class Failure(val error: String) : SetupResult
}

/**
 * Result of a mulligan operation.
 */
sealed interface MulliganResult {
    /**
     * @param state New game state after mulligan
     * @param events Events that occurred
     * @param cardsToPutOnBottom Number of cards the player must put on the bottom of their library
     */
    data class Success(
        val state: GameState,
        val events: List<GameActionEvent>,
        val cardsToPutOnBottom: Int
    ) : MulliganResult

    data class Failure(val error: String) : MulliganResult
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
        val state: GameState,
        val priorityPlayer: EntityId
    ) : PriorityResult

    /**
     * The game has ended.
     */
    data class GameOver(
        val state: GameState,
        val winner: EntityId?
    ) : PriorityResult
}
