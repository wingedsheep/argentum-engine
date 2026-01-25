package com.wingedsheep.gameserver.protocol

import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.GameEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Messages sent from server to client.
 */
@Serializable
sealed interface ServerMessage {
    /**
     * Connection confirmed with assigned player ID.
     */
    @Serializable
    @SerialName("connected")
    data class Connected(val playerId: String) : ServerMessage

    /**
     * Game created successfully, waiting for opponent.
     */
    @Serializable
    @SerialName("gameCreated")
    data class GameCreated(val sessionId: String) : ServerMessage

    /**
     * Game is starting with both players connected.
     */
    @Serializable
    @SerialName("gameStarted")
    data class GameStarted(val opponentName: String) : ServerMessage

    /**
     * Game state update after an action is executed.
     */
    @Serializable
    @SerialName("stateUpdate")
    data class StateUpdate(
        val state: ClientGameState,
        val events: List<GameEvent>,
        val legalActions: List<LegalActionInfo>
    ) : ServerMessage

    /**
     * Error response from the server.
     */
    @Serializable
    @SerialName("error")
    data class Error(
        val code: ErrorCode,
        val message: String
    ) : ServerMessage

    /**
     * Game has ended.
     */
    @Serializable
    @SerialName("gameOver")
    data class GameOver(
        val winnerId: EntityId?,
        val reason: GameOverReason
    ) : ServerMessage

    /**
     * Basic card info for display in mulligan UI.
     */
    @Serializable
    data class MulliganCardInfo(
        val name: String,
        val imageUri: String? = null
    )

    /**
     * Mulligan decision required. Sent when player must choose to keep or mulligan.
     */
    @Serializable
    @SerialName("mulliganDecision")
    data class MulliganDecision(
        /** The player's current hand card IDs */
        val hand: List<EntityId>,
        /** How many times this player has mulliganed (0 = first look at opening hand) */
        val mulliganCount: Int,
        /** If keeping, how many cards must be put on bottom */
        val cardsToPutOnBottom: Int,
        /** Card info for display (entity ID -> card info) */
        val cards: Map<EntityId, MulliganCardInfo> = emptyMap()
    ) : ServerMessage

    /**
     * Player must choose cards to put on bottom of library after keeping a mulliganed hand.
     */
    @Serializable
    @SerialName("chooseBottomCards")
    data class ChooseBottomCards(
        /** The player's current hand card IDs */
        val hand: List<EntityId>,
        /** How many cards must be put on bottom */
        val cardsToPutOnBottom: Int
    ) : ServerMessage

    /**
     * Mulligan phase is complete and the game is starting.
     */
    @Serializable
    @SerialName("mulliganComplete")
    data class MulliganComplete(
        /** Player's final hand size after putting cards on bottom */
        val finalHandSize: Int
    ) : ServerMessage
}

/**
 * Information about a legal action the player can take.
 */
@Serializable
data class LegalActionInfo(
    val actionType: String,
    val description: String,
    val action: GameAction,
    /** Valid target IDs if this action requires targeting */
    val validTargets: List<EntityId>? = null,
    /** Whether this action requires selecting targets before submission */
    val requiresTargets: Boolean = false,
    /** Number of targets required (default 1) */
    val targetCount: Int = 1,
    /** Description of the target requirement */
    val targetDescription: String? = null
)

/**
 * Error codes for server error responses.
 */
@Serializable
enum class ErrorCode {
    NOT_CONNECTED,
    ALREADY_CONNECTED,
    GAME_NOT_FOUND,
    GAME_FULL,
    NOT_YOUR_TURN,
    INVALID_ACTION,
    INVALID_DECK,
    INTERNAL_ERROR
}

/**
 * Reasons why a game ended.
 */
@Serializable
enum class GameOverReason {
    LIFE_ZERO,
    DECK_OUT,
    CONCESSION,
    POISON_COUNTERS,
    DISCONNECTION
}
