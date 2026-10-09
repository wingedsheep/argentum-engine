package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.MoveAllCountersEffect
import kotlin.reflect.KClass

/**
 * Executor for [MoveAllCountersEffect].
 *
 * "Move all counters from The Ozolith onto target creature." Clears every counter kind off the
 * source, then adds each kind to the destination as its own placement — honoring counter-placement
 * replacement effects (Hardened Scales) per kind, the same way [MoveAllLastKnownCountersExecutor]
 * places a last-known map. No-op when source/destination is missing, they're the same permanent,
 * the source has no counters, or the destination can't receive counters (nothing leaves the source).
 */
class MoveAllCountersExecutor(
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<MoveAllCountersEffect> {

    override val effectType: KClass<MoveAllCountersEffect> = MoveAllCountersEffect::class

    override fun execute(
        state: GameState,
        effect: MoveAllCountersEffect,
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

        val moving = state.getEntity(sourceId)?.get<CountersComponent>()?.counters
            ?.filterValues { it > 0 }
            .orEmpty()
        if (moving.isEmpty()) return EffectResult.success(state, emptyList())

        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: ""
        val destName = state.getEntity(destinationId)?.get<CardComponent>()?.name ?: ""
        val events = mutableListOf<GameEvent>()

        // Remove from the source first so a destination counter-placement modifier can't read
        // stale source state.
        var newState = state.updateEntity(sourceId) { it.with(CountersComponent()) }
        moving.forEach { (counterType, count) ->
            events.add(CountersRemovedEvent(sourceId, counterType, count, sourceName))
        }

        // One batch onto a single permanent ⇒ one "first this turn" flag, on the first event.
        var firstThisTurn = DamageUtils.isFirstCounterThisTurn(newState, destinationId)
        for ((counterType, count) in moving) {
            val placed = ReplacementEffectUtils.applyCounterPlacementModifiers(
                newState, destinationId, counterType, count, placerId = context.controllerId,
                predicateEvaluator = predicateEvaluator
            )
            if (placed <= 0) continue
            val firstOfTypeThisTurn = DamageUtils.isFirstCounterOfTypeThisTurn(newState, destinationId, counterType)
            val current = newState.getEntity(destinationId)?.get<CountersComponent>() ?: CountersComponent()
            newState = newState.updateEntity(destinationId) { it.with(current.withAdded(counterType, placed)) }
            events.add(
                CountersAddedEvent(
                    destinationId, counterType, placed, destName, firstThisTurn,
                    firstOfTypeThisTurn = firstOfTypeThisTurn, placedBy = context.controllerId
                )
            )
            newState = DamageUtils.markCounterPlacedOnCreature(newState, context.controllerId, destinationId, counterType)
            firstThisTurn = false
        }

        return EffectResult.success(newState, events)
    }
}
