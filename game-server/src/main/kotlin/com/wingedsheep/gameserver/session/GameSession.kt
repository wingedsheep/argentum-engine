package com.wingedsheep.gameserver.session

import com.wingedsheep.gameserver.masking.MaskedGameState
import com.wingedsheep.gameserver.masking.StateMasker
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.rulesengine.ecs.DeckLoader
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameEngine
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.MulliganResult
import com.wingedsheep.rulesengine.ecs.SetupResult
import com.wingedsheep.rulesengine.ecs.action.GameAction
import com.wingedsheep.rulesengine.ecs.action.GameActionEvent
import com.wingedsheep.rulesengine.ecs.action.GameActionResult
import com.wingedsheep.rulesengine.sets.portal.PortalSet
import java.util.UUID
import kotlin.random.Random

/**
 * Represents an active game session between two players.
 */
class GameSession(
    val sessionId: String = UUID.randomUUID().toString(),
    private val stateMasker: StateMasker = StateMasker(),
    private val random: Random = Random.Default
) {
    private var gameState: GameState? = null
    private val players = mutableMapOf<EntityId, PlayerSession>()
    private val deckLists = mutableMapOf<EntityId, Map<String, Int>>()

    // Mulligan phase tracking
    private val mulliganCounts = mutableMapOf<EntityId, Int>()
    private val mulliganComplete = mutableMapOf<EntityId, Boolean>()
    private val awaitingBottomCards = mutableMapOf<EntityId, Int>()  // playerId -> cards to put on bottom

    val player1: PlayerSession? get() = players.values.firstOrNull()
    val player2: PlayerSession? get() = players.values.drop(1).firstOrNull()

    val isFull: Boolean get() = players.size >= 2
    val isReady: Boolean get() = players.size == 2 && deckLists.size == 2
    val isStarted: Boolean get() = gameState != null
    val isMulliganPhase: Boolean get() = gameState != null && !mulliganComplete.all { it.value }
    val allMulligansComplete: Boolean get() = mulliganComplete.size == 2 && mulliganComplete.all { it.value }

    /**
     * Add a player to this game session.
     * Returns the assigned EntityId for this player.
     */
    fun addPlayer(playerSession: PlayerSession, deckList: Map<String, Int>): EntityId {
        require(!isFull) { "Game session is full" }

        val playerId = playerSession.playerId
        players[playerId] = playerSession
        deckLists[playerId] = deckList
        playerSession.currentGameSessionId = sessionId

        return playerId
    }

    /**
     * Remove a player from the session.
     */
    fun removePlayer(playerId: EntityId) {
        players[playerId]?.currentGameSessionId = null
        players.remove(playerId)
        deckLists.remove(playerId)
    }

    /**
     * Get the opponent's player ID.
     */
    fun getOpponentId(playerId: EntityId): EntityId? {
        return players.keys.firstOrNull { it != playerId }
    }

    /**
     * Get the player session for a player ID.
     */
    fun getPlayerSession(playerId: EntityId): PlayerSession? = players[playerId]

    /**
     * Start the game. Both players must have joined with deck lists.
     * Initializes the mulligan phase - players must complete mulligans before the game begins.
     */
    fun startGame(): GameState {
        require(isReady) { "Game session not ready - need 2 players with deck lists" }

        val playerList = players.map { (playerId, session) ->
            playerId to session.playerName
        }

        // Create game state
        var state = GameEngine.createGame(playerList)

        // Load decks using DeckLoader
        val deckLoader = DeckLoader.create(PortalSet)

        val loadResult = GameEngine.loadDecks(state, deckLoader, deckLists)
        when (loadResult) {
            is DeckLoader.DeckLoadResult.Success -> {
                state = loadResult.state
            }
            is DeckLoader.DeckLoadResult.Failure -> {
                // If deck loading fails, continue with empty decks for now
                // In production, we would reject the game start
            }
        }

        // Shuffle libraries and draw initial hands (7 cards each)
        val setupResult = GameEngine.setupGame(state, random)
        when (setupResult) {
            is SetupResult.Success -> {
                state = setupResult.state
            }
            is SetupResult.Failure -> {
                // Setup failed, continue with current state
            }
        }

        gameState = state

        // Initialize mulligan tracking for both players
        for (playerId in players.keys) {
            mulliganCounts[playerId] = 0
            mulliganComplete[playerId] = false
        }

        return state
    }

    /**
     * Get the mulligan count for a player.
     */
    fun getMulliganCount(playerId: EntityId): Int = mulliganCounts[playerId] ?: 0

    /**
     * Check if a player has completed their mulligan.
     */
    fun hasMulliganComplete(playerId: EntityId): Boolean = mulliganComplete[playerId] == true

    /**
     * Check if a player is awaiting bottom card selection.
     */
    fun isAwaitingBottomCards(playerId: EntityId): Boolean = awaitingBottomCards.containsKey(playerId)

    /**
     * Get the player's current hand for mulligan decisions.
     */
    fun getHand(playerId: EntityId): List<EntityId> {
        val state = gameState ?: return emptyList()
        return state.getHand(playerId)
    }

    /**
     * Player chooses to keep their current hand.
     * If mulliganCount > 0, they will need to choose cards to put on bottom.
     */
    fun keepHand(playerId: EntityId): MulliganActionResult {
        if (hasMulliganComplete(playerId)) {
            return MulliganActionResult.Failure("Mulligan already complete for this player")
        }

        val count = mulliganCounts[playerId] ?: 0

        if (count > 0) {
            // Player needs to choose cards to put on bottom
            awaitingBottomCards[playerId] = count
            return MulliganActionResult.NeedsBottomCards(count)
        } else {
            // No mulligans taken, hand is final
            mulliganComplete[playerId] = true
            return MulliganActionResult.Success
        }
    }

    /**
     * Player chooses to mulligan - shuffle hand and draw 7 new cards.
     */
    fun takeMulligan(playerId: EntityId): MulliganActionResult {
        if (hasMulliganComplete(playerId)) {
            return MulliganActionResult.Failure("Mulligan already complete for this player")
        }

        val state = gameState ?: return MulliganActionResult.Failure("Game not started")
        val currentCount = mulliganCounts[playerId] ?: 0
        val newCount = currentCount + 1

        // Check if player can still mulligan (can't mulligan to 0 cards)
        if (newCount >= 7) {
            return MulliganActionResult.Failure("Cannot mulligan - would have no cards")
        }

        val result = GameEngine.startMulligan(state, playerId, newCount, random)
        return when (result) {
            is MulliganResult.Success -> {
                gameState = result.state
                mulliganCounts[playerId] = newCount
                MulliganActionResult.Success
            }
            is MulliganResult.Failure -> {
                MulliganActionResult.Failure(result.error)
            }
        }
    }

    /**
     * Player chooses which cards to put on the bottom of their library.
     */
    fun chooseBottomCards(playerId: EntityId, cardIds: List<EntityId>): MulliganActionResult {
        val expectedCount = awaitingBottomCards[playerId]
            ?: return MulliganActionResult.Failure("Not awaiting bottom card selection")

        if (cardIds.size != expectedCount) {
            return MulliganActionResult.Failure("Must choose exactly $expectedCount cards, got ${cardIds.size}")
        }

        val state = gameState ?: return MulliganActionResult.Failure("Game not started")

        val result = GameEngine.executeMulligan(state, playerId, cardIds, random)
        return when (result) {
            is MulliganResult.Success -> {
                gameState = result.state
                awaitingBottomCards.remove(playerId)
                mulliganComplete[playerId] = true
                MulliganActionResult.Success
            }
            is MulliganResult.Failure -> {
                MulliganActionResult.Failure(result.error)
            }
        }
    }

    /**
     * Get the mulligan decision message for a player.
     */
    fun getMulliganDecision(playerId: EntityId): ServerMessage.MulliganDecision {
        val hand = getHand(playerId)
        val count = mulliganCounts[playerId] ?: 0
        return ServerMessage.MulliganDecision(
            hand = hand,
            mulliganCount = count,
            cardsToPutOnBottom = count
        )
    }

    /**
     * Get the choose bottom cards message for a player.
     */
    fun getChooseBottomCardsMessage(playerId: EntityId): ServerMessage.ChooseBottomCards? {
        val count = awaitingBottomCards[playerId] ?: return null
        val hand = getHand(playerId)
        return ServerMessage.ChooseBottomCards(
            hand = hand,
            cardsToPutOnBottom = count
        )
    }

    sealed interface MulliganActionResult {
        data object Success : MulliganActionResult
        data class NeedsBottomCards(val count: Int) : MulliganActionResult
        data class Failure(val reason: String) : MulliganActionResult
    }

    /**
     * Execute a game action.
     */
    fun executeAction(playerId: EntityId, action: GameAction): ActionResult {
        val state = gameState ?: return ActionResult.Failure("Game not started")

        // Validate it's this player's turn/priority
        if (state.priorityPlayerId != playerId) {
            return ActionResult.Failure("Not your priority")
        }

        // Execute the action
        val result = GameEngine.executePlayerAction(state, action)
        return when (result) {
            is GameActionResult.Success -> {
                var newState = result.state
                val allEvents = result.events.toMutableList()

                // Check if all players have passed priority
                // If so, resolve the stack or advance the phase
                if (newState.turnState.allPlayersPassed()) {
                    newState = GameEngine.resolvePassedPriority(newState)
                }

                gameState = newState
                ActionResult.Success(newState, allEvents)
            }
            is GameActionResult.Failure -> {
                ActionResult.Failure(result.reason)
            }
        }
    }

    /**
     * Handle player concession.
     */
    fun playerConcedes(playerId: EntityId): GameState? {
        val state = gameState ?: return null
        val opponentId = getOpponentId(playerId) ?: return null

        val newState = state.endGame(opponentId)
        gameState = newState
        return newState
    }

    /**
     * Get the masked game state for a specific player.
     */
    fun getMaskedState(playerId: EntityId): MaskedGameState? {
        val state = gameState ?: return null
        return stateMasker.mask(state, playerId)
    }

    /**
     * Get legal actions for a player.
     * TODO: Implement proper legal action detection using rules engine
     */
    fun getLegalActions(playerId: EntityId): List<LegalActionInfo> {
        val state = gameState ?: return emptyList()

        // Only the player with priority can take actions
        if (state.priorityPlayerId != playerId) {
            return emptyList()
        }

        // TODO: Implement proper legal action detection
        // For now, return a basic list based on game state
        return emptyList()
    }

    /**
     * Create a state update message for a player.
     */
    fun createStateUpdate(playerId: EntityId, events: List<GameActionEvent>): ServerMessage.StateUpdate? {
        val maskedState = getMaskedState(playerId) ?: return null
        val legalActions = getLegalActions(playerId)
        return ServerMessage.StateUpdate(maskedState, events, legalActions)
    }

    /**
     * Check if the game is over.
     */
    fun isGameOver(): Boolean = gameState?.isGameOver == true

    /**
     * Get the winner ID if the game is over.
     */
    fun getWinnerId(): EntityId? = gameState?.winner

    /**
     * Determine the reason for game over.
     */
    fun getGameOverReason(): GameOverReason? {
        val state = gameState ?: return null
        if (!state.isGameOver) return null

        // TODO: Determine actual reason from game state
        return GameOverReason.LIFE_ZERO
    }

    sealed interface ActionResult {
        data class Success(
            val state: GameState,
            val events: List<GameActionEvent>
        ) : ActionResult

        data class Failure(val reason: String) : ActionResult
    }
}
