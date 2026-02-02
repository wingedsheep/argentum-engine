package com.wingedsheep.engine.handlers.actions.special

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.MakeChoice
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.state.GameState
import kotlin.reflect.KClass

/**
 * Handler for the MakeChoice action.
 *
 * Used for modal choices, card selections, etc.
 * TODO: Full implementation when modal spells are supported.
 */
class MakeChoiceHandler : ActionHandler<MakeChoice> {
    override val actionType: KClass<MakeChoice> = MakeChoice::class

    override fun validate(state: GameState, action: MakeChoice): String? {
        // TODO: Validate choice context
        return null
    }

    override fun execute(state: GameState, action: MakeChoice): ExecutionResult {
        // TODO: Handle modal choices, card selections, etc.
        return ExecutionResult.success(state)
    }
}
