package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.MoveCountersEachKindMissingEffect
import kotlin.reflect.KClass

/**
 * Executor for [MoveCountersEachKindMissingEffect].
 *
 * "Move a counter of each kind not on Goldberry from another target permanent you control
 * onto Goldberry." (Goldberry, River-Daughter — ability A.)
 *
 * Deterministic: for every counter kind present on the source that the destination does not
 * currently have, removes one of that kind from the source and adds one to the destination.
 * Kinds already on the destination are left untouched. Honors counter-placement replacement
 * effects (e.g., Hardened Scales). No-op when source/destination is missing or the destination
 * can't receive counters.
 */
class MoveCountersEachKindMissingExecutor : EffectExecutor<MoveCountersEachKindMissingEffect> {

    override val effectType: KClass<MoveCountersEachKindMissingEffect> =
        MoveCountersEachKindMissingEffect::class

    override fun execute(
        state: GameState,
        effect: MoveCountersEachKindMissingEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.resolveTarget(effect.source, state)
            ?: return EffectResult.success(state, emptyList())
        val destinationId = context.resolveTarget(effect.destination, state)
            ?: return EffectResult.success(state, emptyList())
        if (sourceId == destinationId) return EffectResult.success(state, emptyList())

        if (!state.projectedState.canReceiveCounters(destinationId)) {
            return EffectResult.success(state, emptyList())
        }

        val sourceCounters = state.getEntity(sourceId)?.get<CountersComponent>()
            ?: return EffectResult.success(state, emptyList())
        val destCounters = state.getEntity(destinationId)?.get<CountersComponent>() ?: CountersComponent()

        // Each kind present on the source that the destination does not already have.
        val kindsToMove = sourceCounters.counters
            .filter { it.value > 0 && destCounters.getCount(it.key) == 0 }
            .keys.toList()

        if (kindsToMove.isEmpty()) return EffectResult.success(state, emptyList())

        var newState = state
        val events = mutableListOf<GameEvent>()
        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: ""
        val destName = state.getEntity(destinationId)?.get<CardComponent>()?.name ?: ""

        for (counterType in kindsToMove) {
            // Remove one of this kind from the source.
            val curSource = newState.getEntity(sourceId)?.get<CountersComponent>() ?: CountersComponent()
            if (curSource.getCount(counterType) <= 0) continue
            newState = newState.updateEntity(sourceId) { container ->
                container.with(curSource.withRemoved(counterType, 1))
            }
            events.add(CountersRemovedEvent(sourceId, counterTypeToString(counterType), 1, sourceName))

            // Add one of this kind to the destination (honoring placement replacements).
            val modified = ReplacementEffectUtils.applyCounterPlacementModifiers(
                newState, destinationId, counterType, 1, placerId = context.controllerId
            )
            if (modified <= 0) continue
            val curDest = newState.getEntity(destinationId)?.get<CountersComponent>() ?: CountersComponent()
            newState = newState.updateEntity(destinationId) { container ->
                container.with(curDest.withAdded(counterType, modified))
            }
            val (afterMark, firstThisTurn) = DamageUtils.recordCounterPlacement(newState, destinationId)
            newState = afterMark
            events.add(CountersAddedEvent(destinationId, counterTypeToString(counterType), modified, destName, firstThisTurn))
        }

        return EffectResult.success(newState, events)
    }
}
