package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
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
class RemoveCountersExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<RemoveCountersEffect> {

    override val effectType: KClass<RemoveCountersEffect> = RemoveCountersEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveCountersEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for counter removal")

        val counterType = effect.counterType

        val current = state.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()

        // Report what was *actually* taken off, not what was asked for: a permanent carrying fewer
        // counters than the effect names (or none at all — it may have left the battlefield and had
        // its counters stripped) has that many removed and no more. Emitting the requested amount
        // regardless would tell "whenever a counter is removed" observers, and the
        // SuccessCriterion.CountersRemoved "if you do" gate, that something happened when nothing did.
        val requested = amountEvaluator.evaluate(state, effect.count, context)
        val removed = minOf(current.getCount(counterType), requested)
        if (removed <= 0) return EffectResult.success(state, emptyList())

        val newState = state.updateEntity(targetId) { container ->
            container.with(current.withRemoved(counterType, removed))
        }

        val entityName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""

        return EffectResult.success(
            newState,
            listOf(CountersRemovedEvent(targetId, effect.counterType, removed, entityName))
        )
    }
}
