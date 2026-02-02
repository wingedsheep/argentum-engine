package com.wingedsheep.engine.handlers.actions.special

import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.actions.ActionHandlerModule

/**
 * Module providing handlers for special actions.
 *
 * Special actions include:
 * - Concede: Player gives up
 * - MakeChoice: Modal spell or ability choices
 * - SelectTargets: Target selection for triggered abilities
 * - ChooseManaColor: Choosing a color for "any color" mana effects
 */
class SpecialActionsModule : ActionHandlerModule {
    override fun handlers(): List<ActionHandler<*>> = listOf(
        ConcedeHandler(),
        MakeChoiceHandler(),
        SelectTargetsHandler(),
        ChooseManaColorHandler()
    )
}
