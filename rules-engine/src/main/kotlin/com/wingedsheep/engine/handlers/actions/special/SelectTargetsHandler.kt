package com.wingedsheep.engine.handlers.actions.special

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.SelectTargets
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.state.GameState
import kotlin.reflect.KClass

/**
 * Handler for the SelectTargets action.
 *
 * Used for target selection for triggered abilities.
 * TODO: Full implementation when target selection flow is complete.
 */
class SelectTargetsHandler : ActionHandler<SelectTargets> {
    override val actionType: KClass<SelectTargets> = SelectTargets::class

    override fun validate(state: GameState, action: SelectTargets): String? {
        // TODO: Validate target selection
        return null
    }

    override fun execute(state: GameState, action: SelectTargets): ExecutionResult {
        // TODO: Handle target selection for triggered abilities
        return ExecutionResult.success(state)
    }
}
