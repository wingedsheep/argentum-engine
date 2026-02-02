package com.wingedsheep.engine.handlers.actions.priority

import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.actions.ActionHandlerModule

/**
 * Module providing handlers for priority actions.
 */
class PriorityModule(private val context: ActionContext) : ActionHandlerModule {
    override fun handlers(): List<ActionHandler<*>> = listOf(
        PassPriorityHandler.create(context)
    )
}
