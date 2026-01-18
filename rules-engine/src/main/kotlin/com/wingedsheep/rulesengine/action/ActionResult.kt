package com.wingedsheep.rulesengine.action

import com.wingedsheep.rulesengine.game.GameState
import kotlinx.serialization.Serializable

/**
 * Result of executing an action.
 */
@Serializable
sealed interface ActionResult {
    val state: GameState

    @Serializable
    data class Success(
        override val state: GameState,
        val action: Action,
        val events: List<GameEvent> = emptyList()
    ) : ActionResult

    @Serializable
    data class Failure(
        override val state: GameState,
        val action: Action,
        val reason: String
    ) : ActionResult

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrThrow(): GameState = when (this) {
        is Success -> state
        is Failure -> throw IllegalStateException("Action failed: $reason")
    }
}

/**
 * Events generated during action execution.
 * These are for tracking/UI purposes, not game logic.
 */
@Serializable
sealed interface GameEvent {
    @Serializable
    data class LifeChanged(
        val playerId: String,
        val oldLife: Int,
        val newLife: Int,
        val delta: Int
    ) : GameEvent

    @Serializable
    data class CardDrawn(
        val playerId: String,
        val cardId: String,
        val cardName: String
    ) : GameEvent

    @Serializable
    data class CardMoved(
        val cardId: String,
        val cardName: String,
        val fromZone: String,
        val toZone: String
    ) : GameEvent

    @Serializable
    data class CardTapped(
        val cardId: String,
        val cardName: String
    ) : GameEvent

    @Serializable
    data class CardUntapped(
        val cardId: String,
        val cardName: String
    ) : GameEvent

    @Serializable
    data class ManaAdded(
        val playerId: String,
        val color: String,
        val amount: Int
    ) : GameEvent

    @Serializable
    data class DamageDealt(
        val sourceId: String?,
        val targetId: String,
        val amount: Int,
        val isPlayer: Boolean
    ) : GameEvent

    @Serializable
    data class CreatureDied(
        val cardId: String,
        val cardName: String,
        val ownerId: String
    ) : GameEvent

    @Serializable
    data class PlayerLost(
        val playerId: String,
        val reason: String
    ) : GameEvent

    @Serializable
    data class GameEnded(
        val winnerId: String?
    ) : GameEvent

    @Serializable
    data class TriedToDrawFromEmptyLibrary(
        val playerId: String
    ) : GameEvent

    @Serializable
    data class LegendaryRuleApplied(
        val cardId: String,
        val cardName: String,
        val playerId: String
    ) : GameEvent
}
