package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext

/**
 * Core auto-resumers that process continuations without player input:
 * - PendingTriggersContinuation (remaining triggers after first pauses)
 * - ForEachTargetContinuation (iterate sub-pipeline per target)
 * - ForEachPlayerContinuation (iterate sub-pipeline per player)
 * - DrawReplacementRemainingDrawsContinuation (remaining draws after bounce)
 * - CycleDrawContinuation (draw after cycling triggers)
 * - TypecycleSearchContinuation (search after typecycling triggers)
 * - EffectContinuation (auto-resume remaining effects)
 * - RepeatWhileContinuation (ask condition after body)
 */
class CoreAutoResumerModule(
    private val services: com.wingedsheep.engine.core.EngineServices,
    private val effectRunner: EffectContinuationRunner
) : AutoResumerModule {

    override fun autoResumers(): List<AutoResumer<*>> = listOf(
        autoResumer(PendingTriggersContinuation::class) { state, continuation, events, _ ->
            val result = services.triggerProcessor.processTriggers(state, continuation.remainingTriggers)
            mergeAndContinue(result, events)
        },

        autoResumer(ForEachTargetContinuation::class, canResume = { it.remainingTargets.isNotEmpty() }) { state, continuation, events, checkForMore ->
            val forEachTargetExecutor = com.wingedsheep.engine.handlers.effects.composite.ForEachTargetExecutor { s, e, c ->
                services.effectExecutorRegistry.execute(s, e, c)
            }
            val outerContext = continuation.effectContext.copy(
                targets = continuation.remainingTargets
            )
            val result = forEachTargetExecutor.processTargets(
                state,
                continuation.effects,
                continuation.remainingTargets,
                outerContext
            ).toExecutionResult()
            mergeAndContinue(result, events, checkForMore)
        },

        autoResumer(ForEachPlayerContinuation::class, canResume = { it.remainingPlayers.isNotEmpty() }) { state, continuation, events, checkForMore ->
            val forEachPlayerExecutor = com.wingedsheep.engine.handlers.effects.composite.ForEachPlayerExecutor { s, e, c ->
                services.effectExecutorRegistry.execute(s, e, c)
            }
            val result = forEachPlayerExecutor.processPlayers(
                state,
                continuation.effects,
                continuation.remainingPlayers,
                continuation.effectContext
            ).toExecutionResult()
            mergeAndContinue(result, events, checkForMore)
        },

        autoResumer(DrawReplacementRemainingDrawsContinuation::class) { state, continuation, events, checkForMore ->
            if (continuation.remainingDraws > 0) {
                if (continuation.isDrawStep) {
                    val turnManager = com.wingedsheep.engine.core.TurnManager(cardRegistry = services.cardRegistry, effectExecutor = services.effectExecutorRegistry::execute)
                    val drawResult = turnManager.drawCards(state, continuation.drawingPlayerId, continuation.remainingDraws)
                    mergeAndContinue(drawResult, events, checkForMore)
                } else {
                    val drawExecutor = com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor(cardRegistry = services.cardRegistry, effectExecutor = services.effectExecutorRegistry::execute)
                    val drawResult = drawExecutor.executeDraws(state, continuation.drawingPlayerId, continuation.remainingDraws).toExecutionResult()
                    mergeAndContinue(drawResult, events, checkForMore)
                }
            } else {
                checkForMore(state, events)
            }
        },

        autoResumer(CycleDrawContinuation::class) { state, continuation, events, checkForMore ->
            val drawExecutor = com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor(cardRegistry = services.cardRegistry, effectExecutor = services.effectExecutorRegistry::execute)
            val drawResult = drawExecutor.executeDraws(state, continuation.playerId, 1).toExecutionResult()
            mergeAndContinue(drawResult, events, checkForMore)
        },

        autoResumer(TypecycleSearchContinuation::class) { state, continuation, events, checkForMore ->
            val searchEffect = com.wingedsheep.sdk.dsl.EffectPatterns.searchLibrary(
                filter = continuation.searchFilter,
                count = 1,
                reveal = true
            )
            val effectContext = EffectContext(
                sourceId = continuation.cardId,
                controllerId = continuation.playerId,
                opponentId = state.getOpponent(continuation.playerId)
            )
            val searchResult = services.effectExecutorRegistry.execute(state, searchEffect, effectContext).toExecutionResult()
            mergeAndContinue(searchResult, events, checkForMore)
        },

        autoResumer(IfYouDoContinuation::class) { state, continuation, events, checkForMore ->
            val branchResult = com.wingedsheep.engine.handlers.effects.composite.IfYouDoEffectExecutor.evaluateAndDispatch(
                state = state,
                ifYouDo = continuation.ifYouDo,
                ifYouDont = continuation.ifYouDont,
                criterion = continuation.successCriterion,
                snapshot = continuation.snapshot,
                effectContext = continuation.effectContext,
                priorEvents = emptyList(),
                effectExecutor = services.effectExecutorRegistry::execute
            )
            mergeAndContinue(branchResult.toExecutionResult(), events, checkForMore)
        },

        autoResumer(EffectContinuation::class, canResume = { it.remainingEffects.isNotEmpty() }) { state, continuation, events, checkForMore ->
            val runResult = effectRunner.executeRemainingEffects(state, continuation.remainingEffects, continuation.effectContext)
            if (runResult.isPaused) {
                return@autoResumer ExecutionResult.paused(runResult.state, runResult.pendingDecision!!, events + runResult.events)
            }
            checkForMore(runResult.state, events + runResult.events)
        },

        autoResumer(RepeatWhileContinuation::class, canResume = { it.phase == RepeatWhilePhase.AFTER_BODY }) { state, continuation, events, checkForMore ->
            val result = com.wingedsheep.engine.handlers.effects.composite.RepeatWhileExecutor.askCondition(
                state = state,
                body = continuation.body,
                repeatCondition = continuation.repeatCondition,
                resolvedDeciderId = continuation.resolvedDeciderId,
                context = continuation.effectContext,
                sourceName = continuation.sourceName,
                effectExecutor = services.effectExecutorRegistry::execute,
                priorEvents = events
            )
            mergeAndContinue(result.toExecutionResult(), events = emptyList(), checkForMore)
        },

        autoResumer(ModalPreChosenContinuation::class, canResume = { it.remainingEntries.isNotEmpty() }) { state, continuation, events, checkForMore ->
            val ctx = com.wingedsheep.engine.handlers.effects.composite.ModalPreChosenBaseContext(
                controllerId = continuation.controllerId,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                opponentId = continuation.opponentId,
                xValue = continuation.xValue,
                triggeringEntityId = continuation.triggeringEntityId
            )
            val result = com.wingedsheep.engine.handlers.effects.composite.processPreChosenModeQueue(
                state = state,
                entries = continuation.remainingEntries,
                ctx = ctx,
                effectExecutor = { s, e, c -> services.effectExecutorRegistry.execute(s, e, c) },
                targetValidator = services.targetValidator,
                accumulatedEvents = emptyList()
            ).toExecutionResult()
            mergeAndContinue(result, events, checkForMore)
        },

        autoResumer(ReflexiveTriggerTargetContinuation::class) { state, continuation, events, checkForMore ->
            // After the action completes, present target selection for the reflexive effect
            val executor = com.wingedsheep.engine.handlers.effects.composite.ReflexiveTriggerEffectExecutor(
                effectExecutor = services.effectExecutorRegistry::execute,
                targetFinder = services.targetFinder,
                decisionHandler = com.wingedsheep.engine.handlers.DecisionHandler()
            )
            val result = executor.presentReflexiveTargets(
                state,
                continuation.reflexiveEffect,
                continuation.reflexiveTargetRequirements,
                continuation.effectContext,
                events
            )
            // Don't mergeAndContinue here — presentReflexiveTargets already returns
            // the correct result (paused with target decision, or success if no targets)
            result.toExecutionResult()
        }
    )
}
