package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Result of executing an effect within the effect pipeline.
 *
 * Extends the core [ExecutionResult] fields with pipeline-internal data
 * ([updatedCollections], [updatedSubtypeGroups]) that composite executors
 * merge into [com.wingedsheep.engine.handlers.PipelineState] between
 * sub-effect steps. These fields never leave the effect execution subsystem.
 */
data class EffectResult(
    val state: GameState,
    val events: List<GameEvent> = emptyList(),
    val error: String? = null,
    val pendingDecision: PendingDecision? = null,
    /** Card collections produced by pipeline effects (GatherCards, SelectFromCollection, etc.) */
    val updatedCollections: Map<String, List<EntityId>> = emptyMap(),
    /** Subtype-group lists produced by pipeline effects (GatherSubtypes, etc.) */
    val updatedSubtypeGroups: Map<String, List<Set<String>>> = emptyMap()
) {
    val isSuccess: Boolean get() = error == null && pendingDecision == null
    val isPaused: Boolean get() = pendingDecision != null
    val newState: GameState get() = state

    fun toExecutionResult() = ExecutionResult(state, events, error, pendingDecision)

    companion object {
        /** Wrap an [ExecutionResult] from a non-effect subsystem (e.g., StackResolver). */
        fun from(result: ExecutionResult) = EffectResult(
            result.state, result.events, result.error, result.pendingDecision
        )

        fun success(state: GameState): EffectResult =
            EffectResult(state)

        fun success(state: GameState, events: List<GameEvent>): EffectResult =
            EffectResult(state, events)

        fun error(state: GameState, message: String): EffectResult =
            EffectResult(state, error = message)

        fun paused(state: GameState, decision: PendingDecision, events: List<GameEvent> = emptyList()): EffectResult =
            EffectResult(state, events, pendingDecision = decision)
    }
}
