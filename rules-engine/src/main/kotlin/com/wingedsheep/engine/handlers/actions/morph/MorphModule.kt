package com.wingedsheep.engine.handlers.actions.morph

import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.actions.ActionHandlerModule

/**
 * Module providing handlers for morph actions.
 */
class MorphModule(private val context: ActionContext) : ActionHandlerModule {
    override fun handlers(): List<ActionHandler<*>> = listOf(
        TurnFaceUpHandler.create(context)
    )
}
