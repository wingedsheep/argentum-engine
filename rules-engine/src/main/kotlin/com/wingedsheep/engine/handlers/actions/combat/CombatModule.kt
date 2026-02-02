package com.wingedsheep.engine.handlers.actions.combat

import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.actions.ActionHandlerModule

/**
 * Module providing handlers for combat actions.
 *
 * Combat actions include:
 * - DeclareAttackers: Declare which creatures are attacking
 * - DeclareBlockers: Declare which creatures are blocking
 * - OrderBlockers: Order blockers for damage assignment
 */
class CombatModule(private val context: ActionContext) : ActionHandlerModule {
    override fun handlers(): List<ActionHandler<*>> = listOf(
        DeclareAttackersHandler.create(context),
        DeclareBlockersHandler.create(context),
        OrderBlockersHandler()
    )
}
