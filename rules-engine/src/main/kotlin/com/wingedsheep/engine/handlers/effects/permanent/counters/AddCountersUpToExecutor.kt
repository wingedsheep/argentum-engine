package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.AddCountersUpToContinuation
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.AddCountersUpToEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [AddCountersUpToEffect].
 *
 * "Put up to N [counterType] counters on target" — the additive, single-kind mirror of
 * [RemoveAnyNumberOfCountersExecutor]. Evaluates the [AddCountersUpToEffect.max] ceiling once,
 * then prompts the controller with a single [ChooseNumberDecision] (0..max). On resume
 * ([AddCountersUpToContinuation]) the chosen number of counters is placed through the standard
 * `AddCountersEffect` path, so counter-placement replacement effects (Hardened Scales) and any
 * downstream triggers (Saga chapter abilities off lore counters) fire normally.
 *
 * No-op — no prompt — when the target is gone, can't receive counters, or the ceiling resolves
 * to <= 0.
 */
class AddCountersUpToExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<AddCountersUpToEffect> {

    override val effectType: KClass<AddCountersUpToEffect> = AddCountersUpToEffect::class

    override fun execute(
        state: GameState,
        effect: AddCountersUpToEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state, emptyList())

        if (!state.projectedState.canReceiveCounters(targetId)) {
            return EffectResult.success(state, emptyList())
        }

        val max = amountEvaluator.evaluate(state, effect.max, context)
        if (max <= 0) return EffectResult.success(state, emptyList())

        val targetName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseNumberDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Put how many ${effect.counterType} counters on $targetName? (0-$max)",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            minValue = 0,
            maxValue = max
        )

        val continuation = AddCountersUpToContinuation(
            decisionId = decisionId,
            targetId = targetId,
            controllerId = context.controllerId,
            counterType = effect.counterType,
            sourceId = context.sourceId
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
