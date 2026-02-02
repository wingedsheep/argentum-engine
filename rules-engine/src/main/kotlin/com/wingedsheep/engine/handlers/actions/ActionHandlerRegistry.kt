package com.wingedsheep.engine.handlers.actions

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.GameState
import kotlin.reflect.KClass

/**
 * Registry that maps action types to their handlers.
 *
 * This implements the Strategy pattern, allowing each action type to have
 * its own dedicated handler class while providing a unified dispatch mechanism.
 *
 * The registry uses a map-based dispatch system with modular sub-registries
 * for each category of actions, reducing merge conflicts and enabling
 * dynamic handler registration.
 */
class ActionHandlerRegistry {
    private val handlers = mutableMapOf<KClass<out GameAction>, ActionHandler<*>>()

    /**
     * Register all handlers from a module.
     */
    fun registerModule(module: ActionHandlerModule) {
        module.handlers().forEach { handler ->
            handlers[handler.actionType] = handler
        }
    }

    /**
     * Register a single handler.
     * Useful for dynamic registration at runtime.
     */
    fun <T : GameAction> register(handler: ActionHandler<T>) {
        handlers[handler.actionType] = handler
    }

    /**
     * Validate an action using the appropriate handler.
     *
     * @param state The current game state
     * @param action The action to validate
     * @return An error message if invalid, null if valid, or an error if no handler found
     */
    @Suppress("UNCHECKED_CAST")
    fun validate(state: GameState, action: GameAction): String? {
        val handler = handlers[action::class] as? ActionHandler<GameAction>
            ?: return "No handler registered for action type: ${action::class.simpleName}"
        return handler.validate(state, action)
    }

    /**
     * Execute an action using the appropriate handler.
     *
     * @param state The current game state
     * @param action The action to execute
     * @return The execution result with new state and events
     */
    @Suppress("UNCHECKED_CAST")
    fun execute(state: GameState, action: GameAction): ExecutionResult {
        val handler = handlers[action::class] as? ActionHandler<GameAction>
            ?: return ExecutionResult.error(state, "No handler registered for action type: ${action::class.simpleName}")
        return handler.execute(state, action)
    }

    /**
     * Check if a handler is registered for the given action type.
     */
    fun hasHandler(actionType: KClass<out GameAction>): Boolean {
        return handlers.containsKey(actionType)
    }

    /**
     * Returns the number of registered handlers.
     * Useful for testing and diagnostics.
     */
    fun handlerCount(): Int = handlers.size
}
