package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.DoubleCountersEffect
import kotlin.reflect.KClass

/**
 * Executor for [DoubleCountersEffect].
 * "Double the number of +1/+1 counters on that creature."
 *
 * Reads the current count of the named counter kind on the target and puts that
 * many more on it, so the total doubles. The added counters go through the normal
 * counter-placement replacement path (e.g., Hardened Scales), mirroring the rules
 * treatment of doubling as additional counter placement. No-op when the target has
 * no counters of that kind or can't receive counters.
 *
 * With `counterType == null` the effect is "double the number of each kind of counter"
 * (Zimone, Paradox Sculptor): every kind currently on the target is doubled in one go,
 * each as its own placement so per-kind replacements still apply. The kinds present are
 * snapshotted before any placement, so counters added by this very effect are never
 * re-doubled.
 */
class DoubleCountersExecutor : EffectExecutor<DoubleCountersEffect> {

    override val effectType: KClass<DoubleCountersEffect> = DoubleCountersEffect::class

    override fun execute(
        state: GameState,
        effect: DoubleCountersEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target to double counters on")

        if (!state.projectedState.canReceiveCounters(targetId)) {
            return EffectResult.success(state, emptyList())
        }

        val counters = state.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
        // Snapshot the kinds to double before placing anything, so a kind is never doubled twice.
        val namedType = effect.counterType
        val toDouble = if (namedType == null) {
            counters.counters.filterValues { it > 0 }.toList()
        } else {
            val type = resolveCounterType(namedType)
            val existing = counters.getCount(type)
            if (existing > 0) listOf(type to existing) else emptyList()
        }
        if (toDouble.isEmpty()) {
            return EffectResult.success(state, emptyList())
        }

        val entityName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""
        // One "put counters" batch on a single creature ⇒ one "first this turn" flag, carried on
        // the first emitted event so an intervening-if trigger fires once.
        var firstThisTurn = DamageUtils.isFirstCounterThisTurn(state, targetId)
        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for ((counterType, existing) in toDouble) {
            // Doubling places `existing` additional counters; honor placement replacements.
            val added = ReplacementEffectUtils.applyCounterPlacementModifiers(
                newState, targetId, counterType, existing, placerId = context.controllerId
            )
            if (added <= 0) continue

            val current = newState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
            newState = newState.updateEntity(targetId) { container ->
                container.with(current.withAdded(counterType, added))
            }
            events.add(
                CountersAddedEvent(
                    targetId, counterTypeToString(counterType), added, entityName,
                    firstThisTurn, placedBy = context.controllerId
                )
            )
            firstThisTurn = false
        }

        if (events.isNotEmpty()) {
            newState = DamageUtils.markCounterPlacedOnCreature(newState, context.controllerId, targetId)
        }

        return EffectResult.success(newState, events)
    }
}
