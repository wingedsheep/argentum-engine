package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.effects.RepeatWhileEffect
import kotlin.reflect.KClass

/**
 * Executor for RepeatWhileEffect.
 *
 * Executes the body at least once, then evaluates the repeat condition:
 * - [RepeatCondition.PlayerChooses]: pauses for a yes/no decision
 * - [RepeatCondition.WhileCondition]: evaluates synchronously
 *
 * Uses the pre-push pattern (same as CompositeEffectExecutor):
 * 1. Pre-push RepeatWhileContinuation(phase=AFTER_BODY)
 * 2. Execute body via effectExecutor
 * 3. If body pauses → return (AFTER_BODY continuation sits below body's)
 * 4. If body succeeds → pop AFTER_BODY → ask condition
 *
 * For PlayerChooses, askDecider() creates a yes/no decision and pushes
 * AFTER_DECISION continuation. For WhileCondition, evaluates synchronously
 * and either starts another iteration or completes.
 */
class RepeatWhileExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<RepeatWhileEffect> {

    override val effectType: KClass<RepeatWhileEffect> = RepeatWhileEffect::class

    override fun execute(
        state: GameState,
        effect: RepeatWhileEffect,
        context: EffectContext
    ): ExecutionResult {
        // Resolve the decider ID (for PlayerChooses) once at the start
        val resolvedDeciderId = when (val cond = effect.repeatCondition) {
            is RepeatCondition.PlayerChooses ->
                EffectExecutorUtils.resolvePlayerTarget(cond.decider, context)
                    ?: return ExecutionResult.error(state, "RepeatWhile: could not resolve decider target")
            is RepeatCondition.WhileCondition -> null
        }

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        return executeIteration(
            state = state,
            body = effect.body,
            repeatCondition = effect.repeatCondition,
            resolvedDeciderId = resolvedDeciderId,
            context = context,
            sourceName = sourceName,
            effectExecutor = effectExecutor,
            priorEvents = emptyList()
        )
    }

    companion object {
        /**
         * Execute one iteration of the repeat loop.
         *
         * Pre-pushes an AFTER_BODY continuation, then executes the body.
         * If the body completes synchronously, pops the continuation and asks the condition.
         */
        fun executeIteration(
            state: GameState,
            body: Effect,
            repeatCondition: RepeatCondition,
            resolvedDeciderId: EntityId?,
            context: EffectContext,
            sourceName: String?,
            effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult,
            priorEvents: List<GameEvent>
        ): ExecutionResult {
            // Pre-push AFTER_BODY continuation
            val afterBodyContinuation = RepeatWhileContinuation(
                decisionId = "pending",
                body = body,
                repeatCondition = repeatCondition,
                resolvedDeciderId = resolvedDeciderId,
                sourceId = context.sourceId,
                sourceName = sourceName,
                controllerId = context.controllerId,
                opponentId = context.opponentId,
                xValue = context.xValue,
                targets = context.targets,
                phase = RepeatWhilePhase.AFTER_BODY,
                namedTargets = context.namedTargets
            )

            val stateWithContinuation = state.pushContinuation(afterBodyContinuation)

            // Execute the body
            val result = effectExecutor(stateWithContinuation, body, context)

            if (result.isPaused) {
                // Body paused — AFTER_BODY continuation is below body's continuation on the stack.
                // checkForMoreContinuations will handle AFTER_BODY after the body's decision resolves.
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    priorEvents + result.events
                )
            }

            if (!result.isSuccess) {
                // Body failed — pop AFTER_BODY and return error
                val (_, stateWithoutCont) = result.state.popContinuation()
                return ExecutionResult(stateWithoutCont, priorEvents + result.events, result.error)
            }

            // Body completed synchronously — pop AFTER_BODY and ask condition
            val (_, stateAfterPop) = result.state.popContinuation()
            return askCondition(
                state = stateAfterPop,
                body = body,
                repeatCondition = repeatCondition,
                resolvedDeciderId = resolvedDeciderId,
                context = context,
                sourceName = sourceName,
                effectExecutor = effectExecutor,
                priorEvents = priorEvents + result.events
            )
        }

        /**
         * After the body completes, evaluate the repeat condition.
         *
         * For PlayerChooses: create yes/no decision and push AFTER_DECISION continuation.
         * For WhileCondition: evaluate synchronously and either repeat or finish.
         */
        fun askCondition(
            state: GameState,
            body: Effect,
            repeatCondition: RepeatCondition,
            resolvedDeciderId: EntityId?,
            context: EffectContext,
            sourceName: String?,
            effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult,
            priorEvents: List<GameEvent>,
            conditionEvaluator: com.wingedsheep.engine.handlers.ConditionEvaluator? = null
        ): ExecutionResult {
            return when (repeatCondition) {
                is RepeatCondition.PlayerChooses -> {
                    askDecider(
                        state = state,
                        body = body,
                        repeatCondition = repeatCondition,
                        resolvedDeciderId = resolvedDeciderId!!,
                        context = context,
                        sourceName = sourceName,
                        priorEvents = priorEvents
                    )
                }
                is RepeatCondition.WhileCondition -> {
                    val evaluator = conditionEvaluator ?: com.wingedsheep.engine.handlers.ConditionEvaluator()
                    val shouldRepeat = evaluator.evaluate(state, repeatCondition.condition, context)
                    if (shouldRepeat) {
                        executeIteration(
                            state = state,
                            body = body,
                            repeatCondition = repeatCondition,
                            resolvedDeciderId = null,
                            context = context,
                            sourceName = sourceName,
                            effectExecutor = effectExecutor,
                            priorEvents = priorEvents
                        )
                    } else {
                        ExecutionResult.success(state, priorEvents)
                    }
                }
            }
        }

        /**
         * Ask a player whether to repeat (PlayerChooses condition).
         * Creates a YesNoDecision and pushes an AFTER_DECISION continuation.
         */
        fun askDecider(
            state: GameState,
            body: Effect,
            repeatCondition: RepeatCondition.PlayerChooses,
            resolvedDeciderId: EntityId,
            context: EffectContext,
            sourceName: String?,
            priorEvents: List<GameEvent>
        ): ExecutionResult {
            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createYesNoDecision(
                state = state,
                playerId = resolvedDeciderId,
                sourceId = context.sourceId,
                sourceName = sourceName,
                prompt = repeatCondition.prompt,
                yesText = repeatCondition.yesText,
                noText = repeatCondition.noText,
                phase = DecisionPhase.RESOLUTION
            )

            val continuation = RepeatWhileContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                body = body,
                repeatCondition = repeatCondition,
                resolvedDeciderId = resolvedDeciderId,
                sourceId = context.sourceId,
                sourceName = sourceName,
                controllerId = context.controllerId,
                opponentId = context.opponentId,
                xValue = context.xValue,
                targets = context.targets,
                phase = RepeatWhilePhase.AFTER_DECISION,
                namedTargets = context.namedTargets
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                priorEvents + decisionResult.events
            )
        }
    }
}
