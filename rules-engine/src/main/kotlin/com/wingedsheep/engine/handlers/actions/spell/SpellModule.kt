package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.actions.ActionHandlerModule

/**
 * Module providing handlers for spell actions.
 */
class SpellModule(private val context: ActionContext) : ActionHandlerModule {
    override fun handlers(): List<ActionHandler<*>> = listOf(
        CastSpellHandler.create(context)
    )
}
