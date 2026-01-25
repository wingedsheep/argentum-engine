package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.AddCountersEffect
import kotlin.reflect.KClass

/**
 * Executor for AddCountersEffect.
 * "Put X +1/+1 counters on target creature"
 */
class AddCountersExecutor : EffectExecutor<AddCountersEffect> {

    override val effectType: KClass<AddCountersEffect> = AddCountersEffect::class

    override fun execute(
        state: GameState,
        effect: AddCountersEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for counters")

        val counterType = try {
            CounterType.valueOf(
                effect.counterType.uppercase()
                    .replace(' ', '_')
                    .replace('+', 'P')
                    .replace('-', 'M')
                    .replace("/", "_")
            )
        } catch (e: IllegalArgumentException) {
            // Default to generic counter type if not found
            CounterType.PLUS_ONE_PLUS_ONE
        }

        val current = state.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()

        val newState = state.updateEntity(targetId) { container ->
            container.with(current.withAdded(counterType, effect.count))
        }

        return ExecutionResult.success(
            newState,
            listOf(CountersAddedEvent(targetId, effect.counterType, effect.count))
        )
    }
}
