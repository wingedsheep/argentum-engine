package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
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
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for counter removal")

        val counterType = resolveCounterType(effect.counterType)

        val current = state.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()

        val newState = state.updateEntity(targetId) { container ->
            container.with(current.withRemoved(counterType, effect.count))
        }

        val entityName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""

        return EffectResult.success(
            newState,
            listOf(CountersRemovedEvent(targetId, effect.counterType, effect.count, entityName))
        )
    }
}
