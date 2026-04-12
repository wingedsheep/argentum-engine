package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Result of executing a game action or engine step.
 *
 * The engine operates as a reentrant state machine. Every operation returns one of:
 * - **Success**: error == null && pendingDecision == null
 * - **PausedForDecision**: pendingDecision != null (needs player input)
 * - **Error**: error != null (action was invalid, state unchanged)
 *
 * Game-over is signaled via `state.gameOver` + a [GameEndedEvent] in `events`.
 */
@Serializable
data class ExecutionResult(
    val state: GameState,
    val events: List<GameEvent> = emptyList(),
    val error: String? = null,
    val pendingDecision: PendingDecision? = null,
    /** Updated card collections from pipeline effects (GatherCards, SelectFromCollection) */
    val updatedCollections: Map<String, List<EntityId>> = emptyMap(),
    /**
     * Updated subtype-group lists from pipeline effects (e.g., `GatherSubtypesEffect`).
     * Each entry is a list of subtype sets — one `Set<String>` per source entity.
     * Consumed by `CardPredicate.HasSubtypeInEachStoredGroup`.
     */
    val updatedSubtypeGroups: Map<String, List<Set<String>>> = emptyMap()
) {
    val isSuccess: Boolean get() = error == null && pendingDecision == null
    val isPaused: Boolean get() = pendingDecision != null

    /** Alias for state to indicate we're getting the resulting state after execution */
    val newState: GameState get() = state

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
    }
}
