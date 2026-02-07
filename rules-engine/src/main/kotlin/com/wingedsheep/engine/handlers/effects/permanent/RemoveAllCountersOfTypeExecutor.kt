package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.RemoveAllCountersOfTypeEffect
import kotlin.reflect.KClass

/**
 * Executor for RemoveAllCountersOfTypeEffect.
 * "Remove all [counter type] counters from all creatures."
 */
class RemoveAllCountersOfTypeExecutor : EffectExecutor<RemoveAllCountersOfTypeEffect> {

    override val effectType: KClass<RemoveAllCountersOfTypeEffect> = RemoveAllCountersOfTypeEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveAllCountersOfTypeEffect,
        context: EffectContext
    ): ExecutionResult {
        val counterType = try {
            CounterType.valueOf(
                effect.counterType.uppercase()
                    .replace(' ', '_')
                    .replace('+', 'P')
                    .replace('-', 'M')
                    .replace("/", "_")
            )
        } catch (e: IllegalArgumentException) {
            return ExecutionResult.success(state)
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            container.get<CardComponent>() ?: continue
            val counters = container.get<CountersComponent>() ?: continue

            val count = counters.getCount(counterType)
            if (count > 0) {
                newState = newState.updateEntity(entityId) { c ->
                    c.with(counters.withRemoved(counterType, count))
                }
                events.add(CountersRemovedEvent(entityId, effect.counterType, count))
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
