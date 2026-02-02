package com.wingedsheep.engine.handlers.actions.combat

import com.wingedsheep.engine.core.BlockerOrderDeclaredEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.OrderBlockers
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent
import com.wingedsheep.sdk.core.Step
import kotlin.reflect.KClass

/**
 * Handler for the OrderBlockers action.
 *
 * When an attacker is blocked by multiple creatures, the attacking
 * player must order the blockers for damage assignment purposes.
 */
class OrderBlockersHandler : ActionHandler<OrderBlockers> {
    override val actionType: KClass<OrderBlockers> = OrderBlockers::class

    override fun validate(state: GameState, action: OrderBlockers): String? {
        if (state.activePlayerId != action.playerId) {
            return "You can only order blockers on your turn"
        }
        if (state.step != Step.DECLARE_BLOCKERS) {
            return "You can only order blockers during the declare blockers step"
        }
        return null
    }

    override fun execute(state: GameState, action: OrderBlockers): ExecutionResult {
        val attackerId = action.attackerId

        // Validate the attacker exists and is actually blocked
        val attackerContainer = state.getEntity(attackerId)
            ?: return ExecutionResult.error(state, "Attacker not found: $attackerId")

        val blockedComponent = attackerContainer.get<BlockedComponent>()
            ?: return ExecutionResult.error(state, "Creature is not blocked")

        // Validate all ordered blockers are actually blocking this attacker
        val actualBlockers = blockedComponent.blockerIds.toSet()
        val orderedBlockers = action.orderedBlockers.toSet()

        if (actualBlockers != orderedBlockers) {
            return ExecutionResult.error(
                state,
                "Ordered blockers must contain exactly the creatures blocking this attacker"
            )
        }

        // Store the damage assignment order on the attacker
        val newState = state.updateEntity(attackerId) { container ->
            container.with(DamageAssignmentOrderComponent(action.orderedBlockers))
        }

        return ExecutionResult.success(
            newState,
            listOf(BlockerOrderDeclaredEvent(attackerId, action.orderedBlockers))
        )
    }
}
