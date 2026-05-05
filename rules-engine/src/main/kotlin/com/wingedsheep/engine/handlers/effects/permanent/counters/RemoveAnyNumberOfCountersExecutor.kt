package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.RemoveAnyNumberOfCountersContinuation
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RemoveAnyNumberOfCountersEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [RemoveAnyNumberOfCountersEffect].
 *
 * "Remove any number of counters from target creature you control."
 *
 * Enumerates each counter kind currently on the target and prompts the controller
 * with a [ChooseNumberDecision] (0..count) per kind, sequentially. The continuation
 * applies each chosen amount and queues the next prompt.
 */
class RemoveAnyNumberOfCountersExecutor : EffectExecutor<RemoveAnyNumberOfCountersEffect> {

    override val effectType: KClass<RemoveAnyNumberOfCountersEffect> = RemoveAnyNumberOfCountersEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveAnyNumberOfCountersEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state, emptyList())

        val targetEntity = state.getEntity(targetId)
            ?: return EffectResult.success(state, emptyList())

        val counters = targetEntity.get<CountersComponent>() ?: return EffectResult.success(state, emptyList())
        val present = counters.counters.entries
            .filter { it.value > 0 }
            .map { counterTypeToString(it.key) to it.value }

        if (present.isEmpty()) return EffectResult.success(state, emptyList())

        val targetName = targetEntity.get<CardComponent>()?.name ?: ""
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val (firstType, firstMax) = present.first()
        val remaining = present.drop(1)

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseNumberDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Remove how many $firstType counters from $targetName? (0-$firstMax)",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            minValue = 0,
            maxValue = firstMax
        )

        val continuation = RemoveAnyNumberOfCountersContinuation(
            decisionId = decisionId,
            targetId = targetId,
            controllerId = context.controllerId,
            currentCounterType = firstType,
            currentMaxAmount = firstMax,
            remainingCounterTypes = remaining,
            targetName = targetName,
            sourceId = context.sourceId,
            sourceName = sourceName
        )

        val newState = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)

        val events = listOf(
            DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = context.controllerId,
                decisionType = "CHOOSE_NUMBER",
                prompt = decision.prompt
            )
        )

        return EffectResult.paused(newState, decision, events)
    }
}
