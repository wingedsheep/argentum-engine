package com.wingedsheep.engine.handlers.actions.mulligan

import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.actions.ActionHandlerModule

/**
 * Module providing handlers for mulligan actions.
 *
 * Mulligan actions include:
 * - TakeMulligan: Shuffle hand back and draw one fewer
 * - KeepHand: Keep current hand
 * - BottomCards: Put cards on bottom after keeping (London mulligan)
 */
class MulliganModule(private val context: ActionContext) : ActionHandlerModule {
    override fun handlers(): List<ActionHandler<*>> = listOf(
        TakeMulliganHandler.create(context),
        KeepHandHandler.create(context),
        BottomCardsHandler.create(context)
    )
}
