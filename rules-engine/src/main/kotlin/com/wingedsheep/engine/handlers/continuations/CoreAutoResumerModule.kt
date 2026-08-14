package com.wingedsheep.engine.handlers.continuations
import com.wingedsheep.sdk.dsl.Patterns

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext

/**
 * Core auto-resumers that process continuations without player input:
 * - PendingTriggersContinuation (remaining triggers after first pauses)
 * - ForEachContinuation (remaining ForEach iterations, any iteration space)
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

        autoResumer(ForEachContinuation::class, canResume = { it.remainingItems.isNotEmpty() }) { state, continuation, events, checkForMore ->
            val forEachExecutor = com.wingedsheep.engine.handlers.effects.composite.ForEachExecutor { s, e, c ->
                services.effectExecutorRegistry.execute(s, e, c)
            }
            val result = forEachExecutor.processItems(
                state,
                continuation.effect,
                continuation.remainingItems,
                continuation.effectContext
            ).toExecutionResult()
            mergeAndContinue(result, events, checkForMore)
        },

        autoResumer(DrawReplacementRemainingDrawsContinuation::class) { state, continuation, events, checkForMore ->
            if (continuation.remainingDraws > 0) {
                // CR 121.2a is announced once per instruction; these draws are the tail
                // of one that already went through it, so don't re-announce.
                val announce = !continuation.announcementApplied
                if (continuation.isDrawStep) {
                    val turnManager = com.wingedsheep.engine.core.TurnManager(
                        cardRegistry = services.cardRegistry,
                        effectExecutor = services.effectExecutorRegistry::execute,
                        replacementProcessor = services.replacementEffectProcessor
                    )
                    val drawResult = turnManager.drawCards(state, continuation.drawingPlayerId, continuation.remainingDraws, announce)
                    mergeAndContinue(drawResult, events, checkForMore)
                } else {
                    val drawExecutor = com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor(
                        cardRegistry = services.cardRegistry,
                        effectExecutor = services.effectExecutorRegistry::execute,
                        replacementProcessor = services.replacementEffectProcessor
                    )
                    val drawResult = drawExecutor.executeDraws(
                        state, continuation.drawingPlayerId, continuation.remainingDraws, announce = announce
                    ).toExecutionResult()
                    mergeAndContinue(drawResult, events, checkForMore)
                }
            } else {
                checkForMore(state, events)
            }
        },

        autoResumer(CycleDrawContinuation::class) { state, continuation, events, checkForMore ->
            val drawExecutor = com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor(
                cardRegistry = services.cardRegistry,
                effectExecutor = services.effectExecutorRegistry::execute,
                replacementProcessor = services.replacementEffectProcessor
            )
            val drawResult = drawExecutor.executeDraws(state, continuation.playerId, 1).toExecutionResult()
            mergeAndContinue(drawResult, events, checkForMore)
        },

        autoResumer(TypecycleSearchContinuation::class) { state, continuation, events, checkForMore ->
            val searchEffect = com.wingedsheep.sdk.dsl.Patterns.Library.searchLibrary(
                filter = continuation.searchFilter,
                count = 1,
                reveal = true
            )
            val effectContext = EffectContext(
                sourceId = continuation.cardId,
                controllerId = continuation.playerId,
            )
            val searchResult = services.effectExecutorRegistry.execute(state, searchEffect, effectContext).toExecutionResult()
            mergeAndContinue(searchResult, events, checkForMore)
        },

        autoResumer(GatedActionContinuation::class) { state, continuation, events, checkForMore ->
            val branchResult = com.wingedsheep.engine.handlers.effects.composite.GatedEffectExecutor.evaluateAndDispatch(
                state = state,
                then = continuation.then,
                otherwise = continuation.otherwise,
                criterion = continuation.successCriterion,
                snapshot = continuation.snapshot,
                effectContext = continuation.effectContext,
                priorEvents = emptyList(),
                effectExecutor = services.effectExecutorRegistry::execute,
                // The gated action may have paused (e.g. a discard decision) and resumed; its
                // damage events are in the accumulated `events` here. Event-based criteria
                // (SuccessCriterion.DamageDealt) read them from here without re-prepending.
                evaluationEvents = events
            )
            mergeAndContinue(branchResult.toExecutionResult(), events, checkForMore)
        },

        autoResumer(EffectContinuation::class, canResume = { it.remainingEffects.isNotEmpty() }) { state, continuation, events, checkForMore ->
            val runResult = effectRunner.executeRemainingEffects(state, continuation.remainingEffects, continuation.effectContext)
            if (runResult.isPaused) {
                return@autoResumer ExecutionResult.paused(runResult.state, runResult.pendingDecision!!, events + runResult.events)
            }
            // A drained composite hands its pipeline storage to the frame beneath — e.g. a DoAction
            // gate scoring SuccessCriterion.CollectionNonEmpty, or a reflexive "when you do" reading
            // a number the action stored (Bolg of the North). The full frame maps (not just this
            // drain's accumulation) are what propagate: keys injected into this frame by an earlier
            // select-resume are part of them. Numbers and chosen values ride along with collections
            // so pausing mid-composite preserves exactly what completing it synchronously would.
            val stateWithCollections = exposeCollectionsToNextFrame(
                runResult.state,
                continuation.effectContext.pipeline.storedCollections + runResult.updatedCollections,
                continuation.effectContext.pipeline.storedNumbers + runResult.updatedStoredNumbers,
                continuation.effectContext.pipeline.chosenValues + runResult.updatedChosenValues,
            )
            checkForMore(stateWithCollections, events + runResult.events)
        },

        autoResumer(RepeatWhileContinuation::class, canResume = { it.phase == RepeatWhilePhase.AFTER_BODY }) { state, continuation, events, checkForMore ->
            // The body paused for a decision this pass; its pipeline collections (e.g. `putting`,
            // the land put by Cultivator Colossus) drained into `bodyCollections` via
            // exposeCollectionsToNextFrame. Feed them to the repeat condition as bodyOutputs so a
            // WhileCondition sees this pass's outputs — matching the synchronous (non-pausing) path.
            val result = com.wingedsheep.engine.handlers.effects.composite.RepeatWhileExecutor.askCondition(
                state = state,
                body = continuation.body,
                repeatCondition = continuation.repeatCondition,
                resolvedDeciderId = continuation.resolvedDeciderId,
                context = continuation.effectContext,
                sourceName = continuation.sourceName,
                effectExecutor = services.effectExecutorRegistry::execute,
                priorEvents = events,
                bodyOutputs = com.wingedsheep.engine.handlers.effects.composite.RepeatWhileExecutor.Companion.BodyOutputs(
                    collections = continuation.bodyCollections
                )
            )
            mergeAndContinue(result.toExecutionResult(), events = emptyList(), checkForMore)
        },

        autoResumer(ModalPreChosenContinuation::class, canResume = { it.remainingEntries.isNotEmpty() }) { state, continuation, events, checkForMore ->
            val ctx = com.wingedsheep.engine.handlers.effects.composite.PreTargetedEffectContext(
                controllerId = continuation.controllerId,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                xValue = continuation.xValue,
                triggeringEntityId = continuation.triggeringEntityId
            )
            val result = com.wingedsheep.engine.handlers.effects.composite.processPreTargetedEffectQueue(
                state = state,
                entries = continuation.remainingEntries,
                ctx = ctx,
                effectExecutor = { s, e, c -> services.effectExecutorRegistry.execute(s, e, c) },
                targetValidator = services.targetValidator,
                accumulatedEvents = emptyList()
            ).toExecutionResult()
            mergeAndContinue(result, events, checkForMore)
        },

        // Splice (CR 702.47b): the main spell's effect paused for a decision of its own, so the spliced
        // cards' text runs now that the inner chain has resolved — still after the main spell, still in
        // the caster's chosen splice order.
        autoResumer(SpliceTailContinuation::class, canResume = { it.remainingEntries.isNotEmpty() }) { state, continuation, events, checkForMore ->
            val ctx = com.wingedsheep.engine.handlers.effects.composite.PreTargetedEffectContext(
                controllerId = continuation.controllerId,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                xValue = null,
                triggeringEntityId = null
            )
            val result = com.wingedsheep.engine.handlers.effects.composite.processPreTargetedEffectQueue(
                state = state,
                entries = continuation.remainingEntries,
                ctx = ctx,
                effectExecutor = { s, e, c -> services.effectExecutorRegistry.execute(s, e, c) },
                targetValidator = services.targetValidator,
                accumulatedEvents = emptyList()
            ).toExecutionResult()
            mergeAndContinue(result, events, checkForMore)
        },

        autoResumer(ModalChosenModeTailContinuation::class, canResume = { it.remainingChosenModes.isNotEmpty() }) { state, continuation, events, checkForMore ->
            // A resolution-time modal ability (Bumi's "choose up to X") paused inside one
            // chosen mode (e.g. a targeted Scry's reorder prompt); drain the remaining
            // chosen modes now that the inner chain has resolved.
            processChosenModeQueue(
                services = services,
                state = state,
                queue = continuation.remainingChosenModes,
                controllerId = continuation.controllerId,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                xValue = continuation.xValue,
                triggeringEntityId = continuation.triggeringEntityId,
                allowCancelBackToModesList = null,
                outerTargets = continuation.outerTargets,
                outerNamedTargets = continuation.outerNamedTargets,
                accumulatedEvents = events,
                checkForMore = checkForMore
            )
        },

        // A multi-token create-token-copy effect paused mid-batch because one token owed an
        // as-enters choice (printed EntersWithChoice or granted riot). That token's choice, every
        // granted-riot instance, and its ETB triggers have now resolved — create the remaining
        // token copies, each of which runs its own as-enters pipeline and may pause again.
        autoResumer(CreateTokenCopyRemainingContinuation::class, canResume = { it.remaining > 0 }) { state, continuation, events, checkForMore ->
            val staticAbilityHandler = com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler(services.cardRegistry)
            val result = when (val e = continuation.effect) {
                is com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect ->
                    com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfTargetExecutor(
                        staticAbilityHandler = staticAbilityHandler,
                        cardRegistry = services.cardRegistry,
                    ).createTokens(
                        state, e, continuation.context, continuation.controllerId,
                        continuation.remaining, auraHostId = null,
                    )
                is com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfSourceEffect ->
                    com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfSourceExecutor(
                        services.cardRegistry, staticAbilityHandler,
                    ).createTokens(
                        state, e, continuation.context, continuation.controllerId, continuation.remaining,
                    )
                else -> com.wingedsheep.engine.core.EffectResult.success(state)
            }
            mergeAndContinue(result.toExecutionResult(), events, checkForMore)
        },

        // CR 605.3a — the player activated a mana ability while the engine was asking them for a
        // mana payment, and that ability needed a decision of its own. Now that it has resolved,
        // put the payment window back up (refreshed: the source they just tapped is gone from the
        // menu and the auto-pay suggestion covers only what the new floating mana doesn't).
        autoResumer(ReopenManaPaymentDecisionContinuation::class) { state, continuation, events, _ ->
            com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow.reopen(
                state, continuation.decision, events, services.cardRegistry
            )
        },

        // The action half of a `ReflexiveTriggerEffect` completed after pausing for its own
        // decision (e.g. "sacrifice a creature" needing target selection) — emit the reflexive
        // trigger event now instead of resolving it inline (CR 603.12).
        autoResumer(ReflexiveTriggerTargetContinuation::class) { state, continuation, events, checkForMore ->
            val event = com.wingedsheep.engine.handlers.effects.composite.ReflexiveTriggerEffectExecutor
                .buildReflexiveTriggeredEvent(
                    state,
                    continuation.reflexiveEffect,
                    continuation.reflexiveTargetRequirements,
                    continuation.descriptionOverride,
                    continuation.effectContext
                )
            checkForMore(state, events + event)
        }
    )
}
