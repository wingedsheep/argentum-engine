package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
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
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for counters")

        val counterType = when (effect.counterType) {
            "+1/+1" -> CounterType.PLUS_ONE_PLUS_ONE
            "-1/-1" -> CounterType.MINUS_ONE_MINUS_ONE
            else -> try {
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
        }

        val current = state.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()

        // Apply counter placement replacement effects (e.g., Hardened Scales)
        val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state, targetId, counterType, effect.count
        )

        val newState = state.updateEntity(targetId) { container ->
            container.with(current.withAdded(counterType, modifiedCount))
        }

        val entityName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""

        return EffectResult.success(
            newState,
            listOf(CountersAddedEvent(targetId, effect.counterType, modifiedCount, entityName))
        )
    }
}
