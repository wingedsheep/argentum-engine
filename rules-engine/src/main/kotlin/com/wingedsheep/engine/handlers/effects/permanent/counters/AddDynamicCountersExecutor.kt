package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
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
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for counters")

        val evaluator = DynamicAmountEvaluator()
        val count = evaluator.evaluate(state, effect.amount, context)

        if (count <= 0) {
            return EffectResult.success(state, emptyList())
        }

        val counterType = resolveCounterType(effect.counterType)

        val current = state.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()

        val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state, targetId, counterType, count
        )

        val newState = state.updateEntity(targetId) { container ->
            container.with(current.withAdded(counterType, modifiedCount))
        }.let { DamageUtils.markCounterPlacedOnCreature(it, context.controllerId, targetId) }

        val entityName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""

        return EffectResult.success(
            newState,
            listOf(CountersAddedEvent(targetId, effect.counterType, modifiedCount, entityName))
        )
    }
}
