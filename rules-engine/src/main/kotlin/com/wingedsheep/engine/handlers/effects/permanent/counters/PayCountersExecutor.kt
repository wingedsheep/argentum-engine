package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PayCountersContinuation
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.drawing.DrawUpToExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.PayCountersEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [PayCountersEffect].
 *
 * "Pay any amount of {E}" (CR 107.14 generalized to a chosen amount) — evaluates the paying
 * player's current [PayCountersEffect.counterType] total once, then prompts a single
 * [ChooseNumberDecision] (0..current total). On resume ([PayCountersContinuation]) the chosen
 * amount is removed through the standard `RemoveCountersEffect` path and stored in the pipeline
 * under [PayCountersEffect.storeAmountAs] so a composed follow-up effect can read it via
 * `DynamicAmount.VariableReference`.
 *
 * No prompt — immediately stores 0 — when the player currently has none of that counter kind.
 */
class PayCountersExecutor : EffectExecutor<PayCountersEffect> {

    override val effectType: KClass<PayCountersEffect> = PayCountersEffect::class

    override fun execute(
        state: GameState,
        effect: PayCountersEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = TargetResolutionUtils.resolvePlayerRef(effect.player, context, state)
            ?: return EffectResult.error(state, "PayCounters: could not resolve paying player")

        val current = state.getEntity(playerId)?.get<CountersComponent>()?.getCount(resolveCounterType(effect.counterType)) ?: 0
        if (current <= 0) {
            return DrawUpToExecutor.injectStoredNumber(state, effect.storeAmountAs, 0)
        }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseNumberDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Pay how many ${effect.counterType} counters? (0-$current)",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            minValue = 0,
            maxValue = current
        )

        val continuation = PayCountersContinuation(
            decisionId = decisionId,
            playerId = playerId,
            counterType = effect.counterType,
            storeAmountAs = effect.storeAmountAs,
            sourceId = context.sourceId
        )

        val newState = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)

        val events = listOf(
            DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = playerId,
                decisionType = "CHOOSE_NUMBER",
                prompt = decision.prompt
            )
        )

        return EffectResult.paused(newState, decision, events)
    }
}
