package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import kotlinx.serialization.Serializable

/**
 * Result of executing a game action.
 * Contains the new state and any events that occurred.
 */
@Serializable
data class ExecutionResult(
    val state: GameState,
    val events: List<GameEvent> = emptyList(),
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null

    /** Alias for state to indicate we're getting the resulting state after execution */
    val newState: GameState get() = state

    /**
     * Chain another action result.
     */
    fun andThen(action: (GameState) -> ExecutionResult): ExecutionResult {
        if (error != null) return this
        val next = action(state)
        return ExecutionResult(
            state = next.state,
            events = events + next.events,
            error = next.error
        )
    }

    /**
     * Add events to this result.
     */
    fun withEvents(vararg newEvents: GameEvent): ExecutionResult {
        return copy(events = events + newEvents.toList())
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
    }
}
