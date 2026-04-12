package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.effects.DistributeCountersAmongTargetsEffect
import kotlin.reflect.KClass

/**
 * Executor for DistributeCountersAmongTargetsEffect.
 * "Distribute N counters among one or more target creatures."
 *
 * Distribution logic:
 * - 1 target: all counters on it
 * - Multiple targets: divide evenly, remainder to first target
 *
 * Per MTG rules, each target must receive at least minPerTarget counters.
 * If one target becomes illegal, the counters that would have gone on it are lost.
 */
class DistributeCountersAmongTargetsExecutor : EffectExecutor<DistributeCountersAmongTargetsEffect> {

    override val effectType: KClass<DistributeCountersAmongTargetsEffect> = DistributeCountersAmongTargetsEffect::class

    override fun execute(
        state: GameState,
        effect: DistributeCountersAmongTargetsEffect,
        context: EffectContext
    ): EffectResult {
        val targetIds = context.targets
            .map { it.toEntityId() }
            .filter { state.getEntity(it) != null }

        if (targetIds.isEmpty()) {
            return EffectResult.success(state)
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

        // Calculate distribution: each target gets at least minPerTarget, remainder to first
        val distribution = calculateDistribution(effect.totalCounters, targetIds.size)

        var currentState = state
        val events = mutableListOf<GameEvent>()

        for ((index, targetId) in targetIds.withIndex()) {
            val countersForTarget = distribution.getOrElse(index) { 0 }
            if (countersForTarget <= 0) continue

            val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                currentState, targetId, counterType, countersForTarget
            )

            val current = currentState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
            currentState = currentState.updateEntity(targetId) { container ->
                container.with(current.withAdded(counterType, modifiedCount))
            }

            val entityName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""
            events.add(CountersAddedEvent(targetId, effect.counterType, modifiedCount, entityName))
        }

        return EffectResult.success(currentState, events)
    }

    private fun calculateDistribution(totalCounters: Int, targetCount: Int): List<Int> {
        if (targetCount == 1) return listOf(totalCounters)

        val base = totalCounters / targetCount
        val remainder = totalCounters % targetCount

        return List(targetCount) { index ->
            if (index < remainder) base + 1 else base
        }
    }
}
