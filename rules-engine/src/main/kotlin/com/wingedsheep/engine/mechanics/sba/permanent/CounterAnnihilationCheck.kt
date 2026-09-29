package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType

/**
 * 704.5q - +1/+1 and -1/-1 counters on the same permanent annihilate in pairs.
 *
 * The counters are removed, so each kind emits a [CountersRemovedEvent] like any other removal.
 */
class CounterAnnihilationCheck : StateBasedActionCheck {
    override val name = "704.5q Counter Annihilation"
    override val order = SbaOrder.COUNTER_ANNIHILATION

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val counters = container.get<CountersComponent>() ?: continue

            val plusCounters = counters.getCount(CounterType.PLUS_ONE_PLUS_ONE)
            val minusCounters = counters.getCount(CounterType.MINUS_ONE_MINUS_ONE)

            if (plusCounters > 0 && minusCounters > 0) {
                val toRemove = minOf(plusCounters, minusCounters)
                val newCounters = counters
                    .withRemoved(CounterType.PLUS_ONE_PLUS_ONE, toRemove)
                    .withRemoved(CounterType.MINUS_ONE_MINUS_ONE, toRemove)

                newState = newState.updateEntity(entityId) { c ->
                    c.with(newCounters)
                }
                val name = container.get<CardComponent>()?.name ?: ""
                events += CountersRemovedEvent(
                    entityId, CounterType.PLUS_ONE_PLUS_ONE, toRemove, name,
                    remainingCount = newCounters.getCount(CounterType.PLUS_ONE_PLUS_ONE)
                )
                events += CountersRemovedEvent(
                    entityId, CounterType.MINUS_ONE_MINUS_ONE, toRemove, name,
                    remainingCount = newCounters.getCount(CounterType.MINUS_ONE_MINUS_ONE)
                )
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
