package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.scripting.effects.PayFixedCountersEffect
import kotlin.reflect.KClass

/**
 * Executor for [PayFixedCountersEffect].
 *
 * Atomic, all-or-nothing removal — unlike [RemoveCountersExecutor] (which clamps to whatever's
 * present), this either removes exactly [PayFixedCountersEffect.amount] or fails outright. Used
 * as the `action` half of a "you may pay {E}{E}{E}. When you do, ..." [com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect]
 * (Guide of Souls): `ReflexiveTriggerEffectExecutor.isActionFeasible` checks affordability before
 * ever offering the "may pay" prompt, so this failure path is defense in depth, not the normal case.
 */
class PayFixedCountersExecutor : EffectExecutor<PayFixedCountersEffect> {

    override val effectType: KClass<PayFixedCountersEffect> = PayFixedCountersEffect::class

    override fun execute(
        state: GameState,
        effect: PayFixedCountersEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = TargetResolutionUtils.resolvePlayerRef(effect.player, context, state)
            ?: return EffectResult.error(state, "PayFixedCounters: could not resolve paying player")

        val counterType = resolveCounterType(effect.counterType)
        val current = state.getEntity(playerId)?.get<CountersComponent>() ?: CountersComponent()
        val count = current.getCount(counterType)
        if (count < effect.amount) {
            return EffectResult.error(state, "Not enough ${effect.counterType} counters to pay ${effect.amount}")
        }

        val newState = state.updateEntity(playerId) { container ->
            container.with(current.withRemoved(counterType, effect.amount))
        }

        return EffectResult.success(
            newState,
            listOf(CountersRemovedEvent(playerId, effect.counterType, effect.amount, ""))
        )
    }
}
