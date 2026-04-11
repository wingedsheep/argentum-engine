package com.wingedsheep.engine.handlers.actions.special

import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.actions.ActionHandlerModule

/**
 * Module providing handlers for special actions.
 *
 * Special actions include:
 * - Concede: Player gives up
 * - ChooseManaColor: Choosing a color for "any color" mana effects
 *
 * Modal spell mode selection and target selection go through [SubmitDecision] with the
 * appropriate [DecisionResponse] subtype (`ModesChosenResponse`, `TargetsResponse`) — see
 * `handlers/actions/decision/SubmitDecisionHandler.kt`. There is no separate action type for them.
 */
class SpecialActionsModule : ActionHandlerModule {
    override fun handlers(): List<ActionHandler<*>> = listOf(
        ConcedeHandler(),
        ChooseManaColorHandler()
    )
}
