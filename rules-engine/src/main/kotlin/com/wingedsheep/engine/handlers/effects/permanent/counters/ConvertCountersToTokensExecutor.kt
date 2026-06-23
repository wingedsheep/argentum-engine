package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.ConvertCountersToTokensContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EntersWithCountersHelper
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ConvertCountersToTokensEffect
import kotlin.reflect.KClass

/**
 * Executor for [ConvertCountersToTokensEffect] — "remove any number of [counterType] counters from
 * this permanent; for each removed, create one token."
 *
 * Prompts the controller for a number in `0..(count of that counter kind on the source)` and pushes
 * a [ConvertCountersToTokensContinuation]; the resumer removes the chosen number of counters and
 * mints that many tokens from the factory. No-op (no prompt) when the source has no such counters.
 */
class ConvertCountersToTokensExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ConvertCountersToTokensEffect> {

    override val effectType: KClass<ConvertCountersToTokensEffect> = ConvertCountersToTokensEffect::class

    override fun execute(
        state: GameState,
        effect: ConvertCountersToTokensEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        val sourceEntity = state.getEntity(sourceId) ?: return EffectResult.success(state)

        val counterType = EntersWithCountersHelper.resolveCounterType(effect.counterType)
        val available = sourceEntity.get<CountersComponent>()?.getCount(counterType) ?: 0
        if (available <= 0) return EffectResult.success(state)

        val sourceName = sourceEntity.get<CardComponent>()?.name ?: "this permanent"

        val decisionResult = decisionHandler.createNumberDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = "Remove how many ${effect.counterType.description} counters from $sourceName? (0-$available)",
            minValue = 0,
            maxValue = available,
            phase = DecisionPhase.RESOLUTION
        )

        val decision = decisionResult.pendingDecision!!

        val continuation = ConvertCountersToTokensContinuation(
            decisionId = decision.id,
            sourceId = sourceId,
            controllerId = context.controllerId,
            counterType = effect.counterType,
            tokenFactory = effect.tokenFactory
        )

        return EffectResult.paused(
            decisionResult.state.pushContinuation(continuation),
            decision,
            decisionResult.events
        )
    }
}
