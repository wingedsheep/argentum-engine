package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RemoveAllCountersOfTypeEffect
import kotlin.reflect.KClass

/** Executor for mandatory, non-interactive removal of every counter of one named kind. */
class RemoveAllCountersOfTypeExecutor : EffectExecutor<RemoveAllCountersOfTypeEffect> {
    override val effectType: KClass<RemoveAllCountersOfTypeEffect> = RemoveAllCountersOfTypeEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveAllCountersOfTypeEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state, emptyList())
        val targetEntity = state.getEntity(targetId)
            ?: return EffectResult.success(state, emptyList())
        val counters = targetEntity.get<CountersComponent>()
            ?: return EffectResult.success(state, emptyList())
        val type = effect.counterType
        val amount = counters.counters[type] ?: 0
        if (amount <= 0) return EffectResult.success(state, emptyList())

        val remaining = counters.counters - type
        val newState = state.updateEntity(targetId) { container ->
            if (remaining.isEmpty()) container.without<CountersComponent>()
            else container.with(CountersComponent(remaining))
        }
        val entityName = targetEntity.get<CardComponent>()?.name
            ?: targetEntity.get<com.wingedsheep.engine.state.components.identity.PlayerComponent>()?.name.orEmpty()
        return EffectResult.success(
            newState,
            listOf(CountersRemovedEvent(targetId, effect.counterType, amount, entityName))
        )
    }
}
