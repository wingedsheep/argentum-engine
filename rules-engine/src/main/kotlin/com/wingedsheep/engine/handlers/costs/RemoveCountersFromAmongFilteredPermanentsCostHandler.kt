package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.handlers.CostPaymentResult
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.permanent.counters.resolveCounterType
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost

object RemoveCountersFromAmongFilteredPermanentsCostHandler {

    private val predicateEvaluator = PredicateEvaluator()

    fun canPay(
        state: GameState,
        cost: AbilityCost.RemoveCountersFromAmongFilteredPermanents,
        controllerId: EntityId
    ): Boolean {
        val counterType = resolveCounterType(cost.counterType)
        val context = PredicateContext(controllerId = controllerId)
        val projected = state.projectedState
        val total = projected.getBattlefieldControlledBy(controllerId).sumOf { entityId ->
            if (!predicateEvaluator.matchesWithProjection(state, projected, entityId, cost.filter, context)) return@sumOf 0
            state.getEntity(entityId)?.get<CountersComponent>()?.getCount(counterType) ?: 0
        }
        return total >= cost.count
    }

    fun pay(
        state: GameState,
        cost: AbilityCost.RemoveCountersFromAmongFilteredPermanents,
        controllerId: EntityId,
        manaPool: ManaPool,
        counterRemovals: Map<EntityId, Int>
    ): CostPaymentResult {
        val counterType = resolveCounterType(cost.counterType)
        val totalChosen = counterRemovals.values.sum()
        if (totalChosen != cost.count) {
            return CostPaymentResult.failure(
                "Counter removal total ($totalChosen) does not match required count (${cost.count})"
            )
        }
        val context = PredicateContext(controllerId = controllerId)
        val projected = state.projectedState
        var newState = state
        for ((permanentId, toRemove) in counterRemovals) {
            if (toRemove <= 0) continue
            val container = state.getEntity(permanentId)
                ?: return CostPaymentResult.failure("Permanent not found for counter removal: $permanentId")
            if (projected.getController(permanentId) != controllerId) {
                return CostPaymentResult.failure("Cannot remove counters from a permanent you do not control")
            }
            if (!predicateEvaluator.matchesWithProjection(state, projected, permanentId, cost.filter, context)) {
                return CostPaymentResult.failure("Permanent does not match the required filter for counter removal")
            }
            val available = container.get<CountersComponent>()?.getCount(counterType) ?: 0
            if (available < toRemove) {
                return CostPaymentResult.failure(
                    "Permanent does not have enough ${cost.counterType} counters (need $toRemove, have $available)"
                )
            }
            newState = newState.updateEntity(permanentId) { c ->
                val counters = c.get<CountersComponent>() ?: CountersComponent()
                c.with(counters.withRemoved(counterType, toRemove))
            }
        }
        return CostPaymentResult.success(newState, manaPool)
    }
}
