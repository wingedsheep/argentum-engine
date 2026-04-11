package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import kotlin.reflect.KClass

/**
 * Executor for AddDynamicCountersEffect.
 * "Put N counters on target, where N is a dynamic amount"
 */
class AddDynamicCountersExecutor : EffectExecutor<AddDynamicCountersEffect> {

    override val effectType: KClass<AddDynamicCountersEffect> = AddDynamicCountersEffect::class

    override fun execute(
        state: GameState,
        effect: AddDynamicCountersEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return ExecutionResult.error(state, "No valid target for counters")

        val evaluator = DynamicAmountEvaluator()
        val count = evaluator.evaluate(state, effect.amount, context)

        if (count <= 0) {
            return ExecutionResult.success(state, emptyList())
        }

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

        val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state, targetId, counterType, count
        )

        val newState = state.updateEntity(targetId) { container ->
            container.with(current.withAdded(counterType, modifiedCount))
        }

        val entityName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""

        return ExecutionResult.success(
            newState,
            listOf(CountersAddedEvent(targetId, effect.counterType, modifiedCount, entityName))
        )
    }
}
