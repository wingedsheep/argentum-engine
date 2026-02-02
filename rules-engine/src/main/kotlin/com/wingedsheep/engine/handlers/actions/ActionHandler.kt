package com.wingedsheep.engine.handlers.actions

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.GameState
import kotlin.reflect.KClass

/**
 * Interface for action handlers.
 *
 * Each concrete action type has a corresponding handler that handles its
 * validation and execution logic. This follows the Strategy pattern, allowing
 * action processing to be modular and testable.
 *
 * @param T The specific action type this handler handles
 */
interface ActionHandler<T : GameAction> {
    /**
     * The action type this handler handles.
     * Used for automatic registration in the handler registry.
     */
    val actionType: KClass<T>

    /**
     * Validate that an action is legal.
     *
     * @param state The current game state
     * @param action The action to validate
     * @return An error message if invalid, null if valid
     */
    fun validate(state: GameState, action: T): String?

    /**
     * Execute the action against the game state.
     *
     * This method should only be called after validation passes.
     *
     * @param state The current game state
     * @param action The action to execute
     * @return The execution result with new state and events
     */
    fun execute(state: GameState, action: T): ExecutionResult
}
