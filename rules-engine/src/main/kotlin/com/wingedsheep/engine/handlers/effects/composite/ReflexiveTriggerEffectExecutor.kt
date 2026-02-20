package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ReflexiveTriggerEffect.
 * Handles "You may [action]. When you do, [reflexiveEffect]." abilities.
 *
 * When optional=true:
 *   Present yes/no. If yes, execute CompositeEffect(action, reflexiveEffect).
 *   Uses MayAbilityContinuation to delegate to the existing composite flow.
 *
 * When optional=false:
 *   Execute action, then reflexiveEffect sequentially using the same
 *   pre-push EffectContinuation pattern as CompositeEffectExecutor.
 *
 * @param effectExecutor Function to execute sub-effects (provided by registry)
 */
class ReflexiveTriggerEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<ReflexiveTriggerEffect> {

    override val effectType: KClass<ReflexiveTriggerEffect> = ReflexiveTriggerEffect::class

    override fun execute(
        state: GameState,
        effect: ReflexiveTriggerEffect,
        context: EffectContext
    ): ExecutionResult {
        if (effect.optional) {
            return presentOptionalChoice(state, effect, context)
        }
        // Non-optional: delegate to composite effect executor pattern
        return executeAsComposite(state, effect, context)
    }

    private fun presentOptionalChoice(
        state: GameState,
        effect: ReflexiveTriggerEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = effect.description,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Yes",
            noText = "No"
        )

        // If yes: execute action then reflexive effect as a composite
        val compositeEffect = CompositeEffect(listOf(effect.action, effect.reflexiveEffect))

        val continuation = MayAbilityContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            effectIfYes = compositeEffect,
            effectIfNo = null,
            controllerId = context.controllerId,
            opponentId = context.opponentId,
            xValue = context.xValue,
            targets = context.targets
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Execute action + reflexiveEffect as a composite.
     * Uses pre-push EffectContinuation for the reflexive effect (same pattern as CompositeEffectExecutor).
     */
    private fun executeAsComposite(
        state: GameState,
        effect: ReflexiveTriggerEffect,
        context: EffectContext
    ): ExecutionResult {
        val compositeEffect = CompositeEffect(listOf(effect.action, effect.reflexiveEffect))
        return effectExecutor(state, compositeEffect, context)
    }
}
