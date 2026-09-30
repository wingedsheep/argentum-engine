package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.ChooseNumberThenContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ChooseNumberThenEffect
import kotlin.reflect.KClass

/**
 * Executor for [ChooseNumberThenEffect] — generic "choose a number, then run X".
 *
 * Pauses for a number decision and pushes a [ChooseNumberThenContinuation] carrying the
 * inner effect plus a snapshot of the current [EffectContext]. The resumer stamps the
 * chosen number onto the context as the X value and dispatches the inner effect, so atomic
 * effects/filters read the chosen number uniformly (e.g. `manaValueEqualsX()`). Used by Void.
 */
class ChooseNumberThenExecutor(
    private val decisionHandler: DecisionHandler,
    private val amountEvaluator: com.wingedsheep.engine.handlers.DynamicAmountEvaluator
) : EffectExecutor<ChooseNumberThenEffect> {

    override val effectType: KClass<ChooseNumberThenEffect> = ChooseNumberThenEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseNumberThenEffect,
        context: EffectContext
    ): EffectResult {
        val max = amountEvaluator.evaluate(state, effect.maxValue, context)
        if (max < effect.minValue) return EffectResult.success(state, emptyList())

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"

        val continuation = ChooseNumberThenContinuation(
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            then = effect.then,
            baseContext = context
        )

        val decisionResult = decisionHandler.createNumberDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = effect.prompt,
            minValue = effect.minValue,
            maxValue = max,
            phase = DecisionPhase.RESOLUTION,
            answer = continuation
        )

        return EffectResult.propagatePause(
            decisionResult.state,
            decisionResult.events
        )
    }
}
