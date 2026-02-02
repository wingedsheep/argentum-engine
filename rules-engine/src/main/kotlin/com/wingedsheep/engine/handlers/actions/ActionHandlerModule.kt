package com.wingedsheep.engine.handlers.actions

/**
 * Interface for grouping related action handlers into modules.
 *
 * Each module represents a category of actions (e.g., combat, mulligan, spell)
 * and provides all handlers for that category. This enables modular organization
 * and reduces merge conflicts when adding new handlers.
 */
interface ActionHandlerModule {
    /**
     * Returns all handlers provided by this module.
     */
    fun handlers(): List<ActionHandler<*>>
}
