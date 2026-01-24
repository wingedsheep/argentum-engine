package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Result of executing a game action or engine step.
 *
 * The engine operates as a reentrant state machine. Every operation returns one of:
 * - **Success**: The action completed, here's the new state and events
 * - **PausedForDecision**: The engine needs player input before continuing
 * - **Failure**: The action was invalid, state unchanged
 * - **GameOver**: The game has ended
 *
 * This design allows the engine to be used in both synchronous (tests) and
 * asynchronous (networked) environments without blocking.
 */
@Serializable
sealed interface EngineResult {

    /**
     * The action completed successfully.
     *
     * @property newState The updated game state after the action
     * @property events Side effects that occurred (for animation/logging)
     */
    @Serializable
    data class Success(
        val newState: GameState,
        val events: List<GameEvent> = emptyList()
    ) : EngineResult

    /**
     * The engine paused because it needs player input.
     *
     * The game state is frozen in `partialState` until the player submits
     * a DecisionResponse via the SubmitDecision action.
     *
     * @property partialState The state at the moment of pause
     * @property decision Describes what input is needed
     * @property events Events that occurred before the pause
     */
    @Serializable
    data class PausedForDecision(
        val partialState: GameState,
        val decision: PendingDecision,
        val events: List<GameEvent> = emptyList()
    ) : EngineResult

    /**
     * The action was invalid and could not be performed.
     *
     * @property originalState The unchanged state
     * @property reason Category of failure
     * @property message Human-readable explanation
     */
    @Serializable
    data class Failure(
        val originalState: GameState,
        val reason: FailureReason,
        val message: String
    ) : EngineResult

    /**
     * The game has ended.
     *
     * @property finalState The terminal game state
     * @property winnerId The winning player, or null for a draw
     * @property reason How the game ended
     */
    @Serializable
    data class GameOver(
        val finalState: GameState,
        val winnerId: EntityId?,
        val reason: GameEndReason,
        val events: List<GameEvent> = emptyList()
    ) : EngineResult
}

/**
 * Categories of action failures.
 */
@Serializable
enum class FailureReason {
    /** Not enough mana or wrong colors */
    MANA,
    /** Invalid or illegal target */
    TARGET,
    /** Wrong phase/step or no priority */
    TIMING,
    /** General rules violation */
    RULE,
    /** Player doesn't control the object */
    CONTROL,
    /** Object not found */
    NOT_FOUND,
    /** Invalid decision response */
    INVALID_RESPONSE
}

/**
 * Legacy compatibility wrapper.
 *
 * This maintains the original ExecutionResult API for existing code while
 * the codebase transitions to EngineResult. New code should use EngineResult directly.
 */
@Serializable
data class ExecutionResult(
    val state: GameState,
    val events: List<GameEvent> = emptyList(),
    val error: String? = null,
    val pendingDecision: PendingDecision? = null
) {
    val isSuccess: Boolean get() = error == null && pendingDecision == null
    val isPaused: Boolean get() = pendingDecision != null

    /** Alias for state to indicate we're getting the resulting state after execution */
    val newState: GameState get() = state

    /**
     * Chain another action result.
     * Stops chaining if we hit an error or a pause.
     */
    fun andThen(action: (GameState) -> ExecutionResult): ExecutionResult {
        if (error != null) return this
        if (pendingDecision != null) return this
        val next = action(state)
        return ExecutionResult(
            state = next.state,
            events = events + next.events,
            error = next.error,
            pendingDecision = next.pendingDecision
        )
    }

    /**
     * Add events to this result.
     */
    fun withEvents(vararg newEvents: GameEvent): ExecutionResult {
        return copy(events = events + newEvents.toList())
    }

    /**
     * Convert to the new EngineResult type.
     */
    fun toEngineResult(): EngineResult {
        return when {
            error != null -> EngineResult.Failure(state, FailureReason.RULE, error)
            pendingDecision != null -> EngineResult.PausedForDecision(state, pendingDecision, events)
            state.gameOver -> EngineResult.GameOver(state, state.winnerId, GameEndReason.UNKNOWN, events)
            else -> EngineResult.Success(state, events)
        }
    }

    companion object {
        /**
         * Create a successful result with no events.
         */
        fun success(state: GameState): ExecutionResult =
            ExecutionResult(state)

        /**
         * Create a successful result with events.
         */
        fun success(state: GameState, events: List<GameEvent>): ExecutionResult =
            ExecutionResult(state, events)

        /**
         * Create an error result.
         */
        fun error(state: GameState, message: String): ExecutionResult =
            ExecutionResult(state, error = message)

        /**
         * Create a paused result awaiting player input.
         */
        fun paused(state: GameState, decision: PendingDecision, events: List<GameEvent> = emptyList()): ExecutionResult =
            ExecutionResult(state, events, pendingDecision = decision)

        /**
         * Convert from the new EngineResult type.
         */
        fun fromEngineResult(result: EngineResult): ExecutionResult {
            return when (result) {
                is EngineResult.Success -> ExecutionResult(result.newState, result.events)
                is EngineResult.PausedForDecision -> ExecutionResult(
                    result.partialState,
                    result.events,
                    pendingDecision = result.decision
                )
                is EngineResult.Failure -> ExecutionResult(result.originalState, error = result.message)
                is EngineResult.GameOver -> ExecutionResult(
                    result.finalState.copy(gameOver = true, winnerId = result.winnerId),
                    result.events
                )
            }
        }
    }
}
