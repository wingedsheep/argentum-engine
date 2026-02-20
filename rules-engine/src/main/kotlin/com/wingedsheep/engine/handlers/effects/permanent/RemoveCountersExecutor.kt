package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import kotlin.reflect.KClass

/**
 * Executor for RemoveCountersEffect.
 * "Remove X -1/-1 counters from target creature"
 */
class RemoveCountersExecutor : EffectExecutor<RemoveCountersEffect> {

    override val effectType: KClass<RemoveCountersEffect> = RemoveCountersEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveCountersEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for counter removal")

        val counterType = try {
            CounterType.valueOf(
                effect.counterType.uppercase()
                    .replace(' ', '_')
                    .replace('+', 'P')
                    .replace('-', 'M')
                    .replace("/", "_")
            )
        } catch (e: IllegalArgumentException) {
            CounterType.PLUS_ONE_PLUS_ONE
        }

        val current = state.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()

        val newState = state.updateEntity(targetId) { container ->
            container.with(current.withRemoved(counterType, effect.count))
        }

        val entityName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""

        return ExecutionResult.success(
            newState,
            listOf(CountersRemovedEvent(targetId, effect.counterType, effect.count, entityName))
        )
    }
}
