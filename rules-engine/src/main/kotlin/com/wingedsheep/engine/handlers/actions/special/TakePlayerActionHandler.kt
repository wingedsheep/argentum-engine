package com.wingedsheep.engine.handlers.actions.special

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.cost.CostPaymentContext
import com.wingedsheep.engine.mechanics.cost.PaymentResult
import com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.PlayerActionTiming
import kotlin.reflect.KClass

class TakePlayerActionHandler(private val services: EngineServices) : ActionHandler<TakePlayerAction> {
    override val actionType: KClass<TakePlayerAction> = TakePlayerAction::class

    override fun validate(state: GameState, action: TakePlayerAction): String? {
        val permission = state.playerActionPermissions.firstOrNull { it.id == action.permissionId }
            ?: return "Player action permission no longer exists"
        if (permission.playerId != action.playerId) return "This permission belongs to another player"
        val window = ManaPaymentWindow.openFor(state, state.actorFor(action.playerId))
        if (state.pendingDecision != null) {
            if (window == null || window.playerId != permission.playerId || permission.action.timing != PlayerActionTiming.ManaAbility)
                return "A decision must be completed first"
        } else {
            if (!com.wingedsheep.engine.event.canTakePlayerActionAtPriority(state, permission.playerId)) return "You don't have priority"
            if (permission.action.timing == PlayerActionTiming.Sorcery &&
                !services.turnManager.canPlaySorcerySpeed(state, permission.playerId))
                return "This action requires sorcery timing"
        }
        val source = permission.context.sourceId ?: permission.playerId
        if (!services.costPaymentService.canAfford(state, permission.playerId, permission.action.cost, source))
            return "Cannot pay the player action cost"
        return null
    }

    override fun execute(state: GameState, action: TakePlayerAction): ExecutionResult {
        val permission = state.playerActionPermissions.first { it.id == action.permissionId }
        val window = ManaPaymentWindow.openFor(state, state.actorFor(action.playerId))
        val current = if (window != null) ManaPaymentWindow.suspend(state, window) else state.withPriority(permission.playerId)
        val source = permission.context.sourceId ?: permission.playerId
        val context = permission.executionContext(current)
        val atom = (permission.action.cost as? PayCost.Atom)?.atom
        val taken = PlayerActionTakenEvent(permission.playerId, permission.action.actionDescription)
        val directlyPayable = atom is CostAtom.PayLife || (atom is CostAtom.Mana &&
            (ManaPaymentWindow.floatingManaCovers(current, permission.playerId, atom.cost) ||
                services.manaSolver.solve(current, permission.playerId, atom.cost) != null))
        val result = if (directlyPayable) {
            val paid = services.costPaymentService.performPayment(current, permission.playerId, permission.action.cost, source, emptyMap())
            if (!paid.success) return ExecutionResult.error(state, "Cannot pay the player action cost")
            val resolved = services.effectExecutorRegistry.execute(paid.state, permission.action.effect, permission.executionContext(paid.state)).toExecutionResult()
            resolved.copy(events = paid.events + taken + resolved.events)
        } else {
            when (val paid = services.costPaymentService.pay(
                current, permission.playerId, permission.action.cost, source,
                CostPaymentContext(onPaid = permission.action.effect, effectContext = context),
            )) {
                is PaymentResult.Unaffordable -> return ExecutionResult.error(state, "Cannot pay the player action cost")
                is PaymentResult.Pending -> ExecutionResult.propagatePause(paid.state, listOf(taken) + paid.events)
                is PaymentResult.Declined -> ExecutionResult.success(paid.state, paid.events)
                is PaymentResult.Paid -> services.effectExecutorRegistry.execute(paid.state, permission.action.effect, context)
                    .toExecutionResult().let { it.copy(events = paid.events + taken + it.events) }
            }
        }
        if (window == null || result.outcome is Outcome.Paused) return result
        val restored = result.state.copy(priorityPlayerId = state.priorityPlayerId, priorityPassedBy = state.priorityPassedBy)
        return ManaPaymentWindow.resumeIfPending(restored, result.events, services.manaSolver) ?: result
    }
}
