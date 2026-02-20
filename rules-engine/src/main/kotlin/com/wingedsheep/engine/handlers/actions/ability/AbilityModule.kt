package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.actions.ActionHandlerModule

/**
 * Module providing handlers for ability actions.
 *
 * Ability actions include:
 * - ActivateAbility: Activate an ability on a permanent
 * - CycleCard: Cycle a card from hand
 */
class AbilityModule(private val context: ActionContext) : ActionHandlerModule {
    override fun handlers(): List<ActionHandler<*>> = listOf(
        ActivateAbilityHandler.create(context),
        CycleCardHandler.create(context),
        TypecycleCardHandler.create(context)
    )
}
