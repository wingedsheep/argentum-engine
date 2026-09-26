package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.EffectContext
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
 *
 * The loop's state between passes is one [RepeatWhileContinuation] value — the same frame that is
 * pre-pushed around each body — so the synchronous path and both resumers thread exactly the same
 * data, including the [RepeatWhileContinuation.accumulatedCollections] the effect's
 * `collectCollections` fold across passes.
 */
class RepeatWhileExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val conditionEvaluator: ConditionEvaluator
) : EffectExecutor<RepeatWhileEffect> {

    override val effectType: KClass<RepeatWhileEffect> = RepeatWhileEffect::class

    override fun execute(
        state: GameState,
        effect: RepeatWhileEffect,
        context: EffectContext
    ): EffectResult {
        // Resolve the decider ID (for PlayerChooses) once at the start
        val resolvedDeciderId = when (val cond = effect.repeatCondition) {
            is RepeatCondition.PlayerChooses ->
                context.resolvePlayerTarget(cond.decider)
                    ?: return EffectResult.error(state, "RepeatWhile: could not resolve decider target")
            is RepeatCondition.WhileCondition -> null
        }

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        return executeIteration(
            state = state,
            loop = RepeatWhileContinuation(
                body = effect.body,
                repeatCondition = effect.repeatCondition,
                resolvedDeciderId = resolvedDeciderId,
                sourceName = sourceName,
                effectContext = context,
                collectCollections = effect.collectCollections
            ),
            effectExecutor = effectExecutor,
            priorEvents = emptyList(),
            conditionEvaluator = conditionEvaluator
        )
    }

    companion object {
        /**
         * Execute one iteration of the repeat loop.
         *
         * Pre-pushes [loop] (with this pass's [RepeatWhileContinuation.bodyCollections] cleared) as
         * the AFTER_BODY continuation, then executes the body. If the body completes synchronously,
         * pops the continuation and asks the condition.
         */
        fun executeIteration(
            state: GameState,
            loop: RepeatWhileContinuation,
            effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
            priorEvents: List<GameEvent>,
            conditionEvaluator: ConditionEvaluator
        ): EffectResult {
            val afterBodyContinuation = loop.copy(bodyCollections = emptyMap())
            val stateWithContinuation = state.pushContinuation(afterBodyContinuation)

            // Execute the body
            val result = effectExecutor(stateWithContinuation, loop.body, loop.effectContext)

            if (result.outcome is Outcome.Paused) {
                // Body paused — AFTER_BODY continuation is below body's continuation on the stack.
                // checkForMoreContinuations will handle AFTER_BODY after the body's decision resolves.
                return EffectResult.propagatePause(
                    result.state,
                    priorEvents + result.events
                )
            }

            if (result.outcome is Outcome.Rejected) {
                // Body failed — pop AFTER_BODY and return the rejection
                val (_, stateWithoutCont) = result.state.popContinuation()
                return EffectResult(stateWithoutCont, priorEvents + result.events, result.outcome)
            }

            // Body completed synchronously — pop AFTER_BODY and ask condition.
            //
            // A WhileCondition is evaluated against the *body's own outputs this iteration* (e.g.
            // CollectionSharesCardType over the cards milled this pass — The Tale of Tamiyo), so the
            // body's pipeline collections are handed to askCondition as [bodyOutputs]. They are
            // merged onto the context for the condition check only — never into the next iteration's
            // body context, which must stay the pristine pre-loop context: CompositeEffectExecutor
            // only surfaces *new* collection keys, so a stale `milled` carried forward would mask
            // the next pass's mill and the loop would never terminate.
            val (_, stateAfterPop) = result.state.popContinuation()
            return askCondition(
                state = stateAfterPop,
                loop = loop,
                effectExecutor = effectExecutor,
                priorEvents = priorEvents + result.events,
                bodyOutputs = BodyOutputs(
                    collections = result.updatedCollections,
                    subtypeGroups = result.updatedSubtypeGroups,
                    numbers = result.updatedStoredNumbers,
                    chosenValues = result.updatedChosenValues,
                ),
                conditionEvaluator = conditionEvaluator
            )
        }

        /** The pipeline outputs of one body execution, merged onto the context for the condition. */
        data class BodyOutputs(
            val collections: Map<String, List<EntityId>> = emptyMap(),
            val subtypeGroups: Map<String, List<Set<String>>> = emptyMap(),
            val numbers: Map<String, Int> = emptyMap(),
            val chosenValues: Map<String, String> = emptyMap(),
        ) {
            val isEmpty: Boolean
                get() = collections.isEmpty() && subtypeGroups.isEmpty() &&
                    numbers.isEmpty() && chosenValues.isEmpty()
        }

        /**
         * Append this pass's [bodyCollections] onto the loop's running aggregates, per the effect's
         * `collectCollections` map. A no-op for a loop that collects nothing.
         */
        fun foldPass(
            loop: RepeatWhileContinuation,
            bodyCollections: Map<String, List<EntityId>>
        ): RepeatWhileContinuation {
            if (loop.collectCollections.isEmpty()) return loop
            var accumulated = loop.accumulatedCollections
            for ((localName, aggregateName) in loop.collectCollections) {
                val passOutput = bodyCollections[localName].orEmpty()
                if (passOutput.isNotEmpty()) {
                    accumulated = accumulated + (aggregateName to accumulated[aggregateName].orEmpty() + passOutput)
                }
            }
            return loop.copy(accumulatedCollections = accumulated)
        }

        /**
         * What the loop publishes once it stops: every aggregate, empty if no pass wrote to it (so a
         * reader after the loop sees "no cards", never a missing key).
         */
        fun published(loop: RepeatWhileContinuation): Map<String, List<EntityId>> =
            loop.collectCollections.values.associateWith { loop.accumulatedCollections[it].orEmpty() }

        /**
         * After the body completes, fold its outputs into the aggregates and evaluate the repeat
         * condition.
         *
         * For PlayerChooses: create yes/no decision and push AFTER_DECISION continuation.
         * For WhileCondition: evaluate synchronously (against the loop's context merged with
         * [bodyOutputs]) and either repeat or finish. The recursion uses the pristine context so each
         * iteration's body starts fresh (see executeIteration's note on why stale collections must
         * not leak). Finishing returns the aggregates as the result's `updatedCollections`.
         */
        fun askCondition(
            state: GameState,
            loop: RepeatWhileContinuation,
            effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
            priorEvents: List<GameEvent>,
            conditionEvaluator: ConditionEvaluator,
            bodyOutputs: BodyOutputs = BodyOutputs(),
        ): EffectResult {
            val folded = foldPass(loop, bodyOutputs.collections)
            return when (val repeatCondition = folded.repeatCondition) {
                is RepeatCondition.PlayerChooses -> {
                    askDecider(
                        state = state,
                        loop = folded,
                        repeatCondition = repeatCondition,
                        priorEvents = priorEvents
                    )
                }
                is RepeatCondition.WhileCondition -> {
                    val context = folded.effectContext
                    val conditionContext = if (bodyOutputs.isEmpty) context else context.copy(
                        pipeline = context.pipeline.copy(
                            storedCollections = context.pipeline.storedCollections + bodyOutputs.collections,
                            storedSubtypeGroups = context.pipeline.storedSubtypeGroups + bodyOutputs.subtypeGroups,
                            storedNumbers = context.pipeline.storedNumbers + bodyOutputs.numbers,
                            chosenValues = context.pipeline.chosenValues + bodyOutputs.chosenValues,
                        )
                    )
                    val shouldRepeat = conditionEvaluator.evaluate(state, repeatCondition.condition, conditionContext)
                    if (shouldRepeat) {
                        // Deepen resolution depth per iteration so a WhileCondition that never goes
                        // false is caught by the EffectExecutorRegistry depth guard (this recursion
                        // is on the JVM call stack, not via pushContinuation) instead of overflowing
                        // it. See GameLimits.MAX_RESOLUTION_DEPTH.
                        executeIteration(
                            state = state,
                            loop = folded.copy(
                                effectContext = context.copy(resolutionDepth = context.resolutionDepth + 1)
                            ),
                            effectExecutor = effectExecutor,
                            priorEvents = priorEvents,
                            conditionEvaluator = conditionEvaluator
                        )
                    } else {
                        EffectResult(state, priorEvents, updatedCollections = published(folded))
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
            loop: RepeatWhileContinuation,
            repeatCondition: RepeatCondition.PlayerChooses,
            priorEvents: List<GameEvent>
        ): EffectResult {
            val decisionHandler = DecisionHandler()
            val continuation = RepeatWhileDecisionContinuation(loop = loop.copy(bodyCollections = emptyMap()))

            val decisionResult = decisionHandler.createYesNoDecision(
                state = state,
                playerId = loop.resolvedDeciderId!!,
                sourceId = loop.effectContext.sourceId,
                sourceName = loop.sourceName,
                prompt = repeatCondition.prompt,
                yesText = repeatCondition.yesText,
                noText = repeatCondition.noText,
                phase = DecisionPhase.RESOLUTION,
                answer = continuation
            )

            return EffectResult.propagatePause(
                decisionResult.state,
                priorEvents + decisionResult.events
            )
        }
    }
}
