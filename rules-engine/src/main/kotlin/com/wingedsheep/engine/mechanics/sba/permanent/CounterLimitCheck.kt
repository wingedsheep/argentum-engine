package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.MaxCounters

/**
 * 704.5r (and 122.4) - a permanent with an ability that says it can't have more than N counters
 * of a kind, and has more than N of them, has all but N removed.
 *
 * The placement chokepoint already stops counters going past a [MaxCounters] limit; this catches
 * the permanent that gains the limit while holding more (a granted limit, a copy effect). The
 * limit is read off projected statics, so one the permanent has lost no longer trims anything.
 */
class CounterLimitCheck : StateBasedActionCheck {
    override val name = "704.5r Counter Limit"
    override val order = SbaOrder.COUNTER_LIMIT

    override fun check(state: GameState): ExecutionResult {
        val projected = state.projectedState
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield()) {
            val limits = projected.getStaticAbilities(entityId).filterIsInstance<MaxCounters>()
            if (limits.isEmpty()) continue
            val container = newState.getEntity(entityId) ?: continue
            var counters = container.get<CountersComponent>() ?: continue
            for ((type, max) in limits.groupBy { it.counterType }.mapValues { (_, l) -> l.minOf { it.max } }) {
                val excess = counters.getCount(type) - max
                if (excess <= 0) continue
                counters = counters.withRemoved(type, excess)
                events += CountersRemovedEvent(
                    entityId, type, excess, container.get<CardComponent>()?.name ?: "",
                    remainingCount = counters.getCount(type)
                )
            }
            if (counters !== container.get<CountersComponent>()) {
                newState = newState.updateEntity(entityId) { it.with(counters) }
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
