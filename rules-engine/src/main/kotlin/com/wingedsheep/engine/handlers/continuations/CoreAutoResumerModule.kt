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
    private val ctx: ContinuationContext,
    private val effectRunner: EffectContinuationRunner
) : AutoResumerModule {

    override fun autoResumers(): List<AutoResumer<*>> = listOf(
        autoResumer(PendingTriggersContinuation::class, canResume = { ctx.triggerProcessor != null }) { state, continuation, events, _ ->
            val result = ctx.triggerProcessor!!.processTriggers(state, continuation.remainingTriggers)
            mergeAndContinue(result, events)
        },

        autoResumer(ForEachTargetContinuation::class, canResume = { it.remainingTargets.isNotEmpty() }) { state, continuation, events, checkForMore ->
            val forEachTargetExecutor = com.wingedsheep.engine.handlers.effects.composite.ForEachTargetExecutor { s, e, c ->
                ctx.effectExecutorRegistry.execute(s, e, c)
            }
            val outerContext = continuation.effectContext.copy(
                targets = continuation.remainingTargets
            )
            val result = forEachTargetExecutor.processTargets(
                state,
                continuation.effects,
                continuation.remainingTargets,
                outerContext
            )
            mergeAndContinue(result, events, checkForMore)
        },

        autoResumer(ForEachPlayerContinuation::class, canResume = { it.remainingPlayers.isNotEmpty() }) { state, continuation, events, checkForMore ->
            val forEachPlayerExecutor = com.wingedsheep.engine.handlers.effects.composite.ForEachPlayerExecutor { s, e, c ->
                ctx.effectExecutorRegistry.execute(s, e, c)
            }
            val result = forEachPlayerExecutor.processPlayers(
                state,
                continuation.effects,
                continuation.remainingPlayers,
                continuation.effectContext
            )
            mergeAndContinue(result, events, checkForMore)
        },

        autoResumer(DrawReplacementRemainingDrawsContinuation::class) { state, continuation, events, checkForMore ->
            if (continuation.remainingDraws > 0) {
                if (continuation.isDrawStep) {
                    val turnManager = com.wingedsheep.engine.core.TurnManager(cardRegistry = ctx.stackResolver.cardRegistry, effectExecutor = ctx.effectExecutorRegistry::execute)
                    val drawResult = turnManager.drawCards(state, continuation.drawingPlayerId, continuation.remainingDraws)
                    mergeAndContinue(drawResult, events, checkForMore)
                } else {
                    val drawExecutor = com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor(cardRegistry = ctx.stackResolver.cardRegistry, effectExecutor = ctx.effectExecutorRegistry::execute)
                    val drawResult = drawExecutor.executeDraws(state, continuation.drawingPlayerId, continuation.remainingDraws)
                    mergeAndContinue(drawResult, events, checkForMore)
                }
            } else {
                checkForMore(state, events)
            }
        },

        autoResumer(CycleDrawContinuation::class) { state, continuation, events, checkForMore ->
            val drawExecutor = com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor(cardRegistry = ctx.stackResolver.cardRegistry, effectExecutor = ctx.effectExecutorRegistry::execute)
            val drawResult = drawExecutor.executeDraws(state, continuation.playerId, 1)
            mergeAndContinue(drawResult, events, checkForMore)
        },

        autoResumer(TypecycleSearchContinuation::class) { state, continuation, events, checkForMore ->
            val searchFilter = com.wingedsheep.sdk.scripting.GameObjectFilter.Any.withSubtype(continuation.subtypeFilter)
            val searchEffect = com.wingedsheep.sdk.dsl.EffectPatterns.searchLibrary(
                filter = searchFilter,
                count = 1,
                reveal = true
            )
            val effectContext = EffectContext(
                sourceId = continuation.cardId,
                controllerId = continuation.playerId,
                opponentId = state.getOpponent(continuation.playerId)
            )
            val searchResult = ctx.effectExecutorRegistry.execute(state, searchEffect, effectContext)
            mergeAndContinue(searchResult, events, checkForMore)
        },

        autoResumer(EffectContinuation::class, canResume = { it.remainingEffects.isNotEmpty() }) { state, continuation, events, checkForMore ->
            val (currentState, effectEvents) = effectRunner.executeRemainingEffects(state, continuation.remainingEffects, continuation.effectContext)
            checkForMore(currentState, events + effectEvents)
        },

        autoResumer(RepeatWhileContinuation::class, canResume = { it.phase == RepeatWhilePhase.AFTER_BODY }) { state, continuation, events, checkForMore ->
            val result = com.wingedsheep.engine.handlers.effects.composite.RepeatWhileExecutor.askCondition(
                state = state,
                body = continuation.body,
                repeatCondition = continuation.repeatCondition,
                resolvedDeciderId = continuation.resolvedDeciderId,
                context = continuation.effectContext,
                sourceName = continuation.sourceName,
                effectExecutor = ctx.effectExecutorRegistry::execute,
                priorEvents = events
            )
            mergeAndContinue(result, events = emptyList(), checkForMore)
        }
    )
}
