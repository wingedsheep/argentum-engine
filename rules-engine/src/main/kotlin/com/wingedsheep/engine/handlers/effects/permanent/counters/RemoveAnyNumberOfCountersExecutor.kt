package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RemoveAnyNumberOfCountersEffect
import kotlin.reflect.KClass

/**
 * Executor for [RemoveAnyNumberOfCountersEffect].
 *
 * "Remove any number of counters from target creature you control." — with the effect's `maxTotal`
 * set, its budget-capped form "Remove up to N counters from target creature" (Heartless Act), and
 * with `minTotal` set too, its mandatory form "Remove a counter from it" (Leatherhead, Swamp
 * Stalker), where the player picks the kind but not whether.
 *
 * The prompt-per-kind walk itself — the budget cap, the floor, and the forced steps that are
 * applied rather than asked — lives in [RemoveAnyNumberOfCountersFlow], shared with the
 * continuation resumer that carries it on after each answer.
 */
class RemoveAnyNumberOfCountersExecutor : EffectExecutor<RemoveAnyNumberOfCountersEffect> {

    override val effectType: KClass<RemoveAnyNumberOfCountersEffect> = RemoveAnyNumberOfCountersEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveAnyNumberOfCountersEffect,
        context: EffectContext
    ): EffectResult {
        val maxTotal = effect.maxTotal
        if (maxTotal != null && maxTotal <= 0) return EffectResult.success(state, emptyList())

        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state, emptyList())

        val targetEntity = state.getEntity(targetId)
            ?: return EffectResult.success(state, emptyList())

        val counters = targetEntity.get<CountersComponent>() ?: return EffectResult.success(state, emptyList())
        val present = counters.counters.entries
            .filter { it.value > 0 }
            .map { counterTypeToString(it.key) }

        if (present.isEmpty()) return EffectResult.success(state, emptyList())

        val targetName = targetEntity.get<CardComponent>()?.name ?: ""
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // The floor can't exceed the budget or what's actually there — a permanent carrying one
        // counter satisfies "remove a counter" by losing it, and can't be asked for more.
        val available = counters.counters.values.sum()
        val floor = effect.minTotal
            .coerceAtMost(maxTotal ?: Int.MAX_VALUE)
            .coerceAtMost(available)
            .coerceAtLeast(0)

        return when (
            val outcome = RemoveAnyNumberOfCountersFlow.advance(
                state = state,
                targetId = targetId,
                controllerId = context.controllerId,
                targetName = targetName,
                sourceId = context.sourceId,
                sourceName = sourceName,
                order = present,
                budget = maxTotal,
                floor = floor
            )
        ) {
            is RemoveAnyNumberOfCountersFlow.Outcome.Done ->
                EffectResult.success(outcome.state, outcome.events)
            is RemoveAnyNumberOfCountersFlow.Outcome.Prompt ->
                EffectResult.paused(outcome.state, outcome.decision, outcome.events)
        }
    }
}
