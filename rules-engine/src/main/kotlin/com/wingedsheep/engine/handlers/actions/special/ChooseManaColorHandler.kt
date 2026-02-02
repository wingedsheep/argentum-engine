package com.wingedsheep.engine.handlers.actions.special

import com.wingedsheep.engine.core.ChooseManaColor
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import kotlin.reflect.KClass

/**
 * Handler for the ChooseManaColor action.
 *
 * Adds mana of the chosen color to the player's mana pool.
 * Used for "add one mana of any color" effects.
 */
class ChooseManaColorHandler : ActionHandler<ChooseManaColor> {
    override val actionType: KClass<ChooseManaColor> = ChooseManaColor::class

    override fun validate(state: GameState, action: ChooseManaColor): String? {
        // TODO: Validate mana color choice - ensure there's a pending color choice
        return null
    }

    override fun execute(state: GameState, action: ChooseManaColor): ExecutionResult {
        val newState = state.updateEntity(action.playerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            container.with(manaPool.add(action.color))
        }
        return ExecutionResult.success(newState)
    }
}
