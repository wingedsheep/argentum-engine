package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.MoveCountersEffect
import kotlin.reflect.KClass

/**
 * Executor for [MoveCountersEffect].
 *
 * "Move X +1/+1 counters from this creature onto another target creature." (Tester of the
 * Tangential.) Deterministic — the count is fixed at resolution from a [DynamicAmount]
 * (e.g. the X paid via a may-pay-{X} reflexive), then capped at the number actually on the
 * source so the engine never removes counters that aren't there.
 *
 * Removes the (capped) amount of [MoveCountersEffect.counterType] from the source and adds the
 * same number to the destination, honoring counter-placement replacement effects on the
 * destination (Hardened Scales etc.) via [ReplacementEffectUtils.applyCounterPlacementModifiers].
 * No-op when source/destination is missing, they're the same permanent, the amount resolves to
 * <= 0, or the source has none of [counterType].
 */
class MoveCountersExecutor : EffectExecutor<MoveCountersEffect> {

    override val effectType: KClass<MoveCountersEffect> = MoveCountersEffect::class

    override fun execute(
        state: GameState,
        effect: MoveCountersEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.resolveTarget(effect.source, state)
            ?: return EffectResult.success(state, emptyList())
        val destinationId = context.resolveTarget(effect.destination, state)
            ?: return EffectResult.success(state, emptyList())
        if (sourceId == destinationId) return EffectResult.success(state, emptyList())

        val counterType = resolveCounterType(effect.counterType)

        val requested = DynamicAmountEvaluator().evaluate(state, effect.amount, context)
        if (requested <= 0) return EffectResult.success(state, emptyList())

        val sourceCounters = state.getEntity(sourceId)?.get<CountersComponent>() ?: CountersComponent()
        val present = sourceCounters.counters[counterType] ?: 0
        val moveCount = minOf(requested, present)
        if (moveCount <= 0) return EffectResult.success(state, emptyList())

        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: ""
        val destName = state.getEntity(destinationId)?.get<CardComponent>()?.name ?: ""

        // Remove from the source first so a destination counter-placement modifier can't read
        // stale source state.
        val afterRemoval = state.updateEntity(sourceId) { container ->
            container.with(sourceCounters.withRemoved(counterType, moveCount))
        }

        // Adding to the destination honors counter-placement replacement effects (Hardened Scales).
        val placedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
            afterRemoval, destinationId, counterType, moveCount, placerId = context.controllerId
        )
        val destCounters = afterRemoval.getEntity(destinationId)?.get<CountersComponent>() ?: CountersComponent()
        val firstThisTurn = DamageUtils.isFirstCounterThisTurn(afterRemoval, destinationId)

        val newState = afterRemoval.updateEntity(destinationId) { container ->
            container.with(destCounters.withAdded(counterType, placedCount))
        }.let { DamageUtils.markCounterPlacedOnCreature(it, context.controllerId, destinationId, counterTypeToString(counterType)) }

        return EffectResult.success(
            newState,
            listOf(
                CountersRemovedEvent(sourceId, effect.counterType, moveCount, sourceName),
                CountersAddedEvent(destinationId, effect.counterType, placedCount, destName, firstThisTurn, placedBy = context.controllerId)
            )
        )
    }
}
