package com.wingedsheep.gameserver.session

import com.wingedsheep.gameserver.masking.MaskedGameState
import com.wingedsheep.gameserver.masking.StateMasker
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameEngine
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.action.GameAction
import com.wingedsheep.rulesengine.ecs.action.GameActionEvent
import com.wingedsheep.rulesengine.ecs.action.GameActionResult
import java.util.UUID

/**
 * Represents an active game session between two players.
 */
class GameSession(
    val sessionId: String = UUID.randomUUID().toString(),
    private val stateMasker: StateMasker = StateMasker()
) {
    private var gameState: GameState? = null
    private val players = mutableMapOf<EntityId, PlayerSession>()
    private val deckLists = mutableMapOf<EntityId, Map<String, Int>>()

    val player1: PlayerSession? get() = players.values.firstOrNull()
    val player2: PlayerSession? get() = players.values.drop(1).firstOrNull()

    val isFull: Boolean get() = players.size >= 2
    val isReady: Boolean get() = players.size == 2 && deckLists.size == 2
    val isStarted: Boolean get() = gameState != null

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
     */
    fun startGame(): GameState {
        require(isReady) { "Game session not ready - need 2 players with deck lists" }

        val playerList = players.map { (playerId, session) ->
            playerId to session.playerName
        }

        // Create game state
        var state = GameEngine.createGame(playerList)

        // TODO: Load decks using DeckLoader when card registries are available
        // For now, just set up basic game state without actual cards

        gameState = state
        return state
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
                gameState = result.state
                ActionResult.Success(result.state, result.events)
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
