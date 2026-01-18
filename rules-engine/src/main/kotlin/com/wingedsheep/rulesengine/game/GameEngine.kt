package com.wingedsheep.rulesengine.game

import com.wingedsheep.rulesengine.action.*
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import kotlin.random.Random

/**
 * Main entry point for the game engine.
 * Handles game setup, execution, and flow control.
 */
object GameEngine {

    /**
     * Create a new game with the given players.
     * Players should already have their decks loaded into their libraries.
     *
     * @param players The players in the game
     * @param startingPlayerId Optional player to start the game. If null, randomly determined.
     * @param random Random source for shuffling and starting player determination
     * @return The initial game state (before hands are drawn)
     */
    fun createGame(
        players: List<Player>,
        startingPlayerId: PlayerId? = null,
        random: Random = Random
    ): GameState {
        require(players.size >= 2) { "Game requires at least 2 players" }

        // Determine starting player
        val startingPlayer = startingPlayerId ?: players.random(random).id

        // Create initial game state
        val playerMap = players.associateBy { it.id }
        val playerOrder = players.map { it.id }

        return GameState(
            players = playerMap,
            battlefield = com.wingedsheep.rulesengine.zone.Zone.battlefield(),
            stack = com.wingedsheep.rulesengine.zone.Zone.stack(),
            exile = com.wingedsheep.rulesengine.zone.Zone.exile(),
            turnState = TurnState.newGame(playerOrder, startingPlayer)
        )
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
        val events = mutableListOf<GameEvent>()

        // Shuffle each player's library
        for (playerId in state.turnState.playerOrder) {
            currentState = currentState.updatePlayer(playerId) { player ->
                player.copy(library = player.library.shuffle(random))
            }
        }

        // Draw opening hands (7 cards each)
        for (playerId in state.turnState.playerOrder) {
            val drawResult = ActionExecutor.execute(currentState, DrawCard(playerId, OPENING_HAND_SIZE))
            when (drawResult) {
                is ActionResult.Success -> {
                    currentState = drawResult.state
                    events.addAll(drawResult.events)
                }
                is ActionResult.Failure -> {
                    return SetupResult.Failure(drawResult.reason)
                }
            }
        }

        return SetupResult.Success(currentState, events)
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
        playerId: PlayerId,
        cardsToBottom: List<com.wingedsheep.rulesengine.core.CardId>,
        random: Random = Random
    ): MulliganResult {
        val player = state.getPlayer(playerId)
        val mulliganCount = OPENING_HAND_SIZE - player.handSize + cardsToBottom.size

        // Validate that the player has the cards to put on bottom
        for (cardId in cardsToBottom) {
            if (player.hand.getCard(cardId) == null) {
                return MulliganResult.Failure("Card not in hand: $cardId")
            }
        }

        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Put the specified cards on the bottom of the library
        for (cardId in cardsToBottom) {
            val card = currentState.getPlayer(playerId).hand.getCard(cardId)!!
            currentState = currentState.updatePlayer(playerId) { p ->
                p.copy(
                    hand = p.hand.remove(cardId),
                    library = p.library.addToBottom(card)
                )
            }
            events.add(GameEvent.CardMoved(cardId.value, card.name, "HAND", "LIBRARY"))
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
        playerId: PlayerId,
        mulliganNumber: Int,
        random: Random = Random
    ): MulliganResult {
        require(mulliganNumber >= 1) { "Mulligan number must be at least 1" }

        val player = state.getPlayer(playerId)
        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Shuffle hand into library
        for (card in player.hand.cards) {
            currentState = currentState.updatePlayer(playerId) { p ->
                p.copy(
                    hand = p.hand.remove(card.id),
                    library = p.library.addToTop(card)
                )
            }
        }

        // Shuffle library
        currentState = currentState.updatePlayer(playerId) { p ->
            p.copy(library = p.library.shuffle(random))
        }

        // Draw new hand of 7
        val drawResult = ActionExecutor.execute(currentState, DrawCard(playerId, OPENING_HAND_SIZE))
        when (drawResult) {
            is ActionResult.Success -> {
                currentState = drawResult.state
                events.addAll(drawResult.events)
            }
            is ActionResult.Failure -> {
                return MulliganResult.Failure(drawResult.reason)
            }
        }

        // Player now needs to put mulliganNumber cards on the bottom
        return MulliganResult.Success(currentState, events, mulliganNumber)
    }

    /**
     * Execute an action and return the new state.
     */
    fun executeAction(state: GameState, action: Action): ActionResult {
        return ActionExecutor.execute(state, action)
    }

    /**
     * Execute multiple actions in sequence.
     */
    fun executeActions(state: GameState, actions: List<Action>): ActionResult {
        return ActionExecutor.executeAll(state, actions)
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
    fun getWinner(state: GameState): PlayerId? {
        return state.winner
    }

    /**
     * Get all available actions for a player in the current state.
     * This is a simplified version - a full implementation would consider
     * all possible plays, targets, etc.
     */
    fun getAvailableActions(state: GameState, playerId: PlayerId): List<Action> {
        if (state.isGameOver) return emptyList()

        val actions = mutableListOf<Action>()
        val player = state.getPlayer(playerId)
        val hasPriority = state.turnState.priorityPlayer == playerId
        val isActivePlayer = state.turnState.activePlayer == playerId

        if (!hasPriority) {
            return emptyList()
        }

        // Can always pass priority
        actions.add(PassPriority(playerId))

        // Can play lands during main phase if active player and no lands played
        if (isActivePlayer && state.isMainPhase && state.stackIsEmpty && player.canPlayLand) {
            for (card in player.hand.cards) {
                if (card.isLand) {
                    actions.add(PlayLand(card.id, playerId))
                }
            }
        }

        // Can cast spells if timing allows
        for (card in player.hand.cards) {
            if (!card.isLand) {
                val canCast = if (card.definition.isInstant || card.hasKeyword(com.wingedsheep.rulesengine.core.Keyword.FLASH)) {
                    // Instant speed
                    true
                } else {
                    // Sorcery speed
                    isActivePlayer && state.isMainPhase && state.stackIsEmpty
                }

                if (canCast && com.wingedsheep.rulesengine.casting.ManaPaymentValidator.canPay(
                        state, card, playerId
                    ) is com.wingedsheep.rulesengine.casting.ManaPaymentValidator.PaymentResult.Valid) {
                    actions.add(CastSpell(card.id, playerId))
                }
            }
        }

        // Can activate abilities on permanents (simplified - just tap lands for mana)
        if (isActivePlayer) {
            for (permanent in state.getPermanentsControlledBy(playerId)) {
                if (permanent.isLand && !permanent.isTapped) {
                    // Basic lands can tap for mana
                    actions.add(TapCard(permanent.id))
                }
            }
        }

        return actions
    }

    /**
     * Check state-based actions and return the new state.
     */
    fun checkStateBasedActions(state: GameState): ActionResult {
        return ActionExecutor.execute(state, CheckStateBasedActions())
    }

    // Constants
    const val OPENING_HAND_SIZE = 7
    const val STARTING_LIFE = 20
}

/**
 * Result of game setup.
 */
sealed interface SetupResult {
    data class Success(val state: GameState, val events: List<GameEvent>) : SetupResult
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
        val events: List<GameEvent>,
        val cardsToPutOnBottom: Int
    ) : MulliganResult
    data class Failure(val error: String) : MulliganResult
}
