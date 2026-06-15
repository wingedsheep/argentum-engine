package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.MoveChosenCountersToTargetContinuation
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.MoveChosenCountersToTargetEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [MoveChosenCountersToTargetEffect].
 *
 * "Move one or more counters from Goldberry onto another target permanent you control.
 * If you do, draw a card." (Goldberry, River-Daughter — ability B.)
 *
 * Enumerates each counter kind currently on the source and prompts the controller with a
 * [ChooseNumberDecision] (0..count) per kind, sequentially. The continuation removes each
 * chosen amount from the source, adds it to the destination, queues the next prompt, and —
 * after the last kind — draws a card if [MoveChosenCountersToTargetEffect.drawCardOnMove] is
 * set and at least one counter was moved.
 */
class MoveChosenCountersToTargetExecutor : EffectExecutor<MoveChosenCountersToTargetEffect> {

    override val effectType: KClass<MoveChosenCountersToTargetEffect> =
        MoveChosenCountersToTargetEffect::class

    override fun execute(
        state: GameState,
        effect: MoveChosenCountersToTargetEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.resolveTarget(effect.source, state)
            ?: return EffectResult.success(state, emptyList())
        val destinationId = context.resolveTarget(effect.destination, state)
            ?: return EffectResult.success(state, emptyList())
        if (sourceId == destinationId) return EffectResult.success(state, emptyList())

        val counters = state.getEntity(sourceId)?.get<CountersComponent>()
            ?: return EffectResult.success(state, emptyList())
        val present = counters.counters.entries
            .filter { it.value > 0 }
            .map { counterTypeToString(it.key) to it.value }

        if (present.isEmpty()) return EffectResult.success(state, emptyList())

        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: ""
        val destName = state.getEntity(destinationId)?.get<CardComponent>()?.name ?: ""

        val (firstType, firstMax) = present.first()
        val remaining = present.drop(1)

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseNumberDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Move how many $firstType counters from $sourceName onto $destName? (0-$firstMax)",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            minValue = 0,
            maxValue = firstMax
        )

        val continuation = MoveChosenCountersToTargetContinuation(
            decisionId = decisionId,
            sourceId = sourceId,
            destinationId = destinationId,
            controllerId = context.controllerId,
            currentCounterType = firstType,
            currentMaxAmount = firstMax,
            remainingCounterTypes = remaining,
            sourceName = sourceName,
            destinationName = destName,
            drawCardOnMove = effect.drawCardOnMove,
            anyMovedSoFar = false
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
