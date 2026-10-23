package com.wingedsheep.engine.handlers.continuations
import com.wingedsheep.sdk.dsl.Patterns

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.pipelineOutputs
import com.wingedsheep.engine.state.endResolutionControl

/**
 * Core auto-resumers that process continuations without player input:
 * - AdvanceStepContinuation / FinishUntapStepContinuation (the rest of a turn-based step)
 * - ForEachContinuation (remaining ForEach iterations, any iteration space)
 * - DrawReplacementRemainingDrawsContinuation (remaining draws after bounce)
 * - CycleDrawContinuation / TypecycleSearchContinuation (legacy saved games only)
 * - EffectProgram (the rest of an effect program, once the node at its cursor has finished)
 * - RepeatWhileContinuation (ask condition after body)
 */
class CoreAutoResumerModule(
    private val services: com.wingedsheep.engine.core.EngineServices,
    private val effectRunner: com.wingedsheep.engine.handlers.effects.composite.EffectProgramInterpreter
) : AutoResumerModule {

    override fun autoResumers(): List<AutoResumer<*>> = listOf(
        autoResumer(EndResolutionControlContinuation::class) { state, continuation, events, checkForMore ->
            mergeAndContinue(state.endResolutionControl(continuation.resolvingObject, wasResolving = true), events, checkForMore)
        },
        autoResumer(FinishResolvingSpellContinuation::class) { state, continuation, events, checkForMore ->
            val result = services.stackResolver.finishResolvingSpell(state, continuation)
            mergeAndContinue(result, events, checkForMore)
        },
        autoResumer(FinishResolvingAbilityContinuation::class) { state, continuation, events, checkForMore ->
            val result = services.stackResolver.finishResolvingAbility(state, continuation)
            mergeAndContinue(result, events, checkForMore)
        },
        autoResumer(AdvanceStepContinuation::class) { state, _, events, checkForMore ->
            mergeAndContinue(services.turnManager.advanceStep(state), events, checkForMore)
        },
        autoResumer(FinishTurnStartContinuation::class) { state, continuation, events, checkForMore ->
            mergeAndContinue(services.turnManager.finishTurnStart(state, continuation.activePlayerId, continuation.followUps), events, checkForMore)
        },
        autoResumer(FinishUntapStepContinuation::class) { state, continuation, events, checkForMore ->
            mergeAndContinue(services.turnManager.finishUntapStep(state, continuation.activePlayerId,
                continuation.skippedUntapStep, continuation.pendingSkipsToConsume), events, checkForMore)
        },

        autoResumer(ForEachContinuation::class, canResume = {
            it.remainingItems.isNotEmpty() || it.effect.collectCollections.isNotEmpty()
        }) { state, continuation, events, checkForMore ->
            val forEachExecutor = com.wingedsheep.engine.handlers.effects.composite.ForEachExecutor(
                services.effectExecutorRegistry::execute, services.predicateEvaluator
            )
            val result = forEachExecutor.processItems(
                state,
                continuation.effect,
                continuation.remainingItems,
                continuation.effectContext
            )
            if (result.outcome is Outcome.Paused) {
                return@autoResumer ExecutionResult.propagatePause(
                    result.state, events + result.events
                )
            }
            val published = exposeCollectionsToNextFrame(
                result.state, result.updatedCollections,
                sacrificed = continuation.effectContext.sacrificedPermanents + result.updatedSacrificedPermanents,
                departed = continuation.effectContext.departedPermanents + result.updatedDepartedPermanents,
            )
            checkForMore(published, events + result.events)
        },

        autoResumer(DrawReplacementRemainingDrawsContinuation::class) { state, continuation, events, checkForMore ->
            // The preceding draw and all its replacement work are complete.
            val nextDrawState = state.copy(activeReplacementChain = null)
            if (continuation.remainingDraws > 0) {
                // CR 121.2a is announced once per instruction; these draws are the tail
                // of one that already went through it, so don't re-announce.
                val announce = !continuation.announcementApplied
                if (continuation.isDrawStep) {
                    val drawResult = services.turnManager.drawCards(nextDrawState, continuation.drawingPlayerId, continuation.remainingDraws, announce)
                    mergeAndContinue(drawResult, events, checkForMore)
                } else {
                    val drawExecutor = com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor(
                        cardRegistry = services.cardRegistry,
                        effectExecutor = services.effectExecutorRegistry::execute,
                        replacementProcessor = services.replacementEffectProcessor,
                        amountEvaluator = services.dynamicAmountEvaluator
                    )
                    val drawResult = drawExecutor.executeDraws(
                        nextDrawState, continuation.drawingPlayerId, continuation.remainingDraws, announce = announce
                    ).toExecutionResult()
                    mergeAndContinue(drawResult, events, checkForMore)
                }
            } else {
                checkForMore(
                    if (continuation.isDrawStep) nextDrawState.withPriority(continuation.drawingPlayerId)
                    else nextDrawState,
                    events
                )
            }
        },

        autoResumer(DrawStepTeamDrawsContinuation::class) { state, continuation, events, checkForMore ->
            // A teammate's paused draw-step draw is complete; the rest of the team draws now.
            val drawResult = services.turnManager.drawPhaseManager.drawForTeam(
                state, continuation.activePlayer, continuation.remainingTeammates
            )
            mergeAndContinue(drawResult, events, checkForMore)
        },

        autoResumer(DrawStepOtherDrawsContinuation::class) { state, continuation, events, checkForMore ->
            // The draw before these is complete; the other players' draw-step draws follow it.
            val drawResult = services.turnManager.drawPhaseManager.drawForOthers(
                state, continuation.activePlayer, continuation.remainingDrawers
            )
            mergeAndContinue(drawResult, events, checkForMore)
        },

        autoResumer(CycleDrawContinuation::class) { state, continuation, events, checkForMore ->
            val drawExecutor = com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor(
                cardRegistry = services.cardRegistry,
                effectExecutor = services.effectExecutorRegistry::execute,
                replacementProcessor = services.replacementEffectProcessor,
                amountEvaluator = services.dynamicAmountEvaluator
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

        // The node at the program's cursor has finished (its frames have drained), or the program
        // hasn't started: run the rest. A finished program publishes to the frame beneath exactly
        // the outputs a run that never paused returns to its caller (EffectProgramInterpreter.run):
        // e.g. a DoAction gate scoring SuccessCriterion.CollectionNonEmpty, or a reflexive "when
        // you do" reading a number the action stored (Bolg of the North).
        autoResumer(EffectProgram::class) { state, continuation, events, checkForMore ->
            val runResult = effectRunner.resume(state, continuation)
            // Only a cursor that doesn't fit its root is rejected; a program never fails otherwise.
            if (runResult.error != null) return@autoResumer runResult.toExecutionResult()
            if (runResult.outcome is Outcome.Paused) {
                return@autoResumer ExecutionResult.propagatePause(runResult.state, events + runResult.events)
            }
            val outputs = if (continuation.origin == null) {
                legacyOutputs(continuation.effectContext, runResult.pipelineOutputs)
            } else {
                runResult.pipelineOutputs
            }
            checkForMore(exposeOutputsToNextFrame(runResult.state, outputs), events + runResult.events)
        },

        autoResumer(RepeatWhileContinuation::class) { state, continuation, events, checkForMore ->
            // The body paused for a decision this pass; its pipeline collections (e.g. `putting`,
            // the land put by Cultivator Colossus) drained into `bodyCollections` via
            // exposeCollectionsToNextFrame. Feed them to the repeat condition as bodyOutputs so a
            // WhileCondition sees this pass's outputs — matching the synchronous (non-pausing) path.
            val result = com.wingedsheep.engine.handlers.effects.composite.RepeatWhileExecutor.askCondition(
                state = state,
                loop = continuation,
                effectExecutor = services.effectExecutorRegistry::execute,
                priorEvents = events,
                bodyOutputs = com.wingedsheep.engine.handlers.effects.composite.RepeatWhileExecutor.Companion.BodyOutputs(
                    collections = continuation.bodyCollections,
                    numbers = continuation.bodyNumbers,
                ),
                conditionEvaluator = services.conditionEvaluator
            )
            if (result.outcome !is Outcome.Done) {
                return@autoResumer mergeAndContinue(result.toExecutionResult(), events = emptyList(), checkForMore)
            }
            // The loop stopped: its collected aggregates go to the frame beneath (the rest of the
            // enclosing pipeline), the same hand-off the synchronous path makes via updatedCollections.
            checkForMore(exposeCollectionsToNextFrame(result.state, result.updatedCollections), result.events)
        },

        autoResumer(ModalPreChosenContinuation::class, canResume = { it.remainingEntries.isNotEmpty() }) { state, continuation, events, checkForMore ->
            val ctx = com.wingedsheep.engine.handlers.effects.composite.PreTargetedEffectContext(
                resolvingTriggeredAbility = continuation.resolvingTriggeredAbility,
                controllerId = continuation.controllerId,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                xValue = continuation.xValue,
                triggeringEntityId = continuation.triggeringEntityId,
                triggerContext = continuation.triggerContext,
                pipeline = continuation.pipeline,
                objectReferences = continuation.objectReferences,
                baseContext = continuation.baseContext
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
                triggeringEntityId = null,
                objectReferences = continuation.objectReferences
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
                pipeline = continuation.pipeline,
                resolvingTriggeredAbility = continuation.resolvingTriggeredAbility,
                objectReferences = continuation.objectReferences,
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
                        amountEvaluator = services.dynamicAmountEvaluator,
                        targetFinder = services.targetFinder
                    ).createTokens(
                        state, e, continuation.context, continuation.controllerId,
                        continuation.remaining, auraHostId = null,
                    )
                is com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfSourceEffect ->
                    com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfSourceExecutor(
                        services.cardRegistry, staticAbilityHandler,
                        predicateEvaluator = services.predicateEvaluator
                    ).createTokens(
                        state, e, continuation.context, continuation.controllerId, continuation.remaining,
                    )
                else -> com.wingedsheep.engine.core.EffectResult.success(state)
            }
            val grouped = com.wingedsheep.engine.handlers.effects.token.TokenEntryGroups.stamp(
                result, com.wingedsheep.engine.handlers.effects.token.TokenEntryGroups.group(continuation.context))
            mergeAndContinue(grouped.toExecutionResult(), events, checkForMore)
        },

        // CR 605.3a — the player activated a mana ability while the engine was asking them for a
        // mana payment, and that ability needed a decision of its own. Now that it has resolved,
        // put the payment window back up (refreshed: the source they just tapped is gone from the
        // menu and the auto-pay suggestion covers only what the new floating mana doesn't).
        autoResumer(ReopenManaPaymentDecisionContinuation::class) { state, continuation, events, _ ->
            com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow.reopen(
                state, continuation.suspension, events, services.manaSolver
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

    /**
     * What a frame converted from the retired `EffectContinuation` (origin null) publishes when it
     * finishes: the old frame's runner handed the frame beneath its *whole* collection, number,
     * chosen-value and snapshot lists — values injected into it before the save included — not only
     * what it stored after resuming. A stack of old frames saved mid-resolution still passes an
     * inner frame's selection out to the outer frame's remaining steps.
     */
    private fun legacyOutputs(saved: EffectContext, ran: com.wingedsheep.engine.handlers.PipelineOutputs) = ran.copy(
        collections = saved.pipeline.storedCollections + ran.collections,
        numbers = saved.pipeline.storedNumbers + ran.numbers,
        chosenValues = saved.pipeline.chosenValues + ran.chosenValues,
        sacrificed = saved.sacrificedPermanents + ran.sacrificed,
        departed = saved.departedPermanents + ran.departed,
    )
}
