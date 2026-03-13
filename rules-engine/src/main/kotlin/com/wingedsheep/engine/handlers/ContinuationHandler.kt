package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.continuations.*
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Handles resumption of execution after a player decision.
 *
 * When the engine pauses for player input, it pushes a ContinuationFrame
 * onto the state's continuation stack. When the player submits their decision,
 * this handler pops the frame and resumes execution based on the frame type.
 *
 * Delegates to specialized resumer modules via the ContinuationResumerRegistry.
 */
class ContinuationHandler(
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val stackResolver: StackResolver = StackResolver(),
    private val triggerProcessor: com.wingedsheep.engine.event.TriggerProcessor? = null,
    private val triggerDetector: com.wingedsheep.engine.event.TriggerDetector? = null,
    private val combatManager: CombatManager? = null,
    private val targetFinder: TargetFinder = TargetFinder()
) {

    private val ctx = ContinuationContext(
        effectExecutorRegistry = effectExecutorRegistry,
        stackResolver = stackResolver,
        triggerProcessor = triggerProcessor,
        triggerDetector = triggerDetector,
        combatManager = combatManager,
        targetFinder = targetFinder
    )

    private val registry = ContinuationResumerRegistry().apply {
        // Core engine resumers
        registerModule(CoreContinuationResumers())

        // Specialized resumer modules
        registerModule(CombatContinuationResumer(ctx))
        registerModule(ColorChoiceContinuationResumer(ctx))
        registerModule(ChainSpellContinuationResumer(ctx))
        registerModule(CreatureTypeChoiceContinuationResumer(ctx))
        registerModule(DrawReplacementContinuationResumer(ctx, ::entityIdToChosenTarget))
        registerModule(CardSpecificContinuationResumer(ctx))
        registerModule(DiscardAndDrawContinuationResumer(ctx))
        registerModule(StateBasedContinuationResumer(ctx))
        registerModule(SacrificeAndPayContinuationResumer(ctx))
        registerModule(ManaPaymentContinuationResumer(ctx))
        registerModule(LibraryAndZoneContinuationResumer(ctx))
        registerModule(ModalAndCloneContinuationResumer(ctx))
    }

    /**
     * Resume execution after a decision is submitted.
     *
     * @param state The game state with the pending decision already cleared
     * @param response The player's decision response
     * @return The result of resuming execution
     */
    fun resume(state: GameState, response: DecisionResponse): ExecutionResult {
        val (continuation, stateAfterPop) = state.popContinuation()

        if (continuation == null) {
            return ExecutionResult.success(state)
        }

        if (continuation.decisionId != response.decisionId) {
            return ExecutionResult.error(
                state,
                "Decision ID mismatch: expected ${continuation.decisionId}, got ${response.decisionId}"
            )
        }

        return registry.resume(stateAfterPop, continuation, response, ::checkForMoreContinuations)
    }

    // ─── Core engine resumers ───

    private inner class CoreContinuationResumers : ContinuationResumerModule {
        override fun resumers(): List<ContinuationResumer<*>> = listOf(
            resumer(EffectContinuation::class, ::resumeEffect),
            resumer(TriggeredAbilityContinuation::class, ::resumeTriggeredAbility),
            resumer(ResolveSpellContinuation::class) { state, _, _, _ ->
                ExecutionResult.success(state)
            },
            resumer(MayAbilityContinuation::class, ::resumeMayAbility),
            resumer(MayTriggerContinuation::class, ::resumeMayTrigger),

            // Standalone resumers
            resumer(DrawUpToContinuation::class, ::resumeDrawUpTo),
            resumer(RepeatWhileContinuation::class, ::resumeRepeatWhile),
            resumer(StormCopyTargetContinuation::class, ::resumeStormCopyTarget),
            resumer(DistributeCountersContinuation::class, ::resumeDistributeCounters),
            resumer(AddDynamicManaContinuation::class, ::resumeAddDynamicMana),
            resumer(ReturnFromLinkedExileContinuation::class) { state, continuation, response, checkForMore ->
                resumeReturnFromLinkedExile(state, continuation, response)
            },

            // Error-returning resumers: these should not be at top of stack during decision resume
            errorResumer<PendingTriggersContinuation>("PendingTriggersContinuation"),
            errorResumer<CycleDrawContinuation>("CycleDrawContinuation"),
            errorResumer<TypecycleSearchContinuation>("TypecycleSearchContinuation"),
            errorResumer<DrawReplacementRemainingDrawsContinuation>("DrawReplacementRemainingDrawsContinuation"),
            errorResumer<ForEachTargetContinuation>("ForEachTargetContinuation"),
            errorResumer<ForEachPlayerContinuation>("ForEachPlayerContinuation")
        )
    }

    // ─── Core engine methods (kept here, tightly coupled to checkForMoreContinuations) ───

    private fun resumeEffect(
        state: GameState,
        continuation: EffectContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        var currentContext = continuation.toEffectContext()
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for ((index, effect) in continuation.remainingEffects.withIndex()) {
            val stillRemaining = continuation.remainingEffects.drop(index + 1)

            val stateForExecution = if (stillRemaining.isNotEmpty()) {
                val remainingContinuation = EffectContinuation(
                    decisionId = "pending",
                    remainingEffects = stillRemaining,
                    sourceId = continuation.sourceId,
                    controllerId = continuation.controllerId,
                    opponentId = continuation.opponentId,
                    xValue = continuation.xValue,
                    targets = continuation.targets,
                    storedCollections = currentContext.storedCollections,
                    chosenCreatureType = continuation.chosenCreatureType,
                    namedTargets = continuation.namedTargets,
                    chosenValues = continuation.chosenValues,
                    storedStringLists = currentContext.storedStringLists
                )
                currentState.pushContinuation(remainingContinuation)
            } else {
                currentState
            }

            val result = effectExecutorRegistry.execute(stateForExecution, effect, currentContext)

            if (!result.isSuccess && !result.isPaused) {
                currentState = if (stillRemaining.isNotEmpty()) {
                    val (_, stateWithoutCont) = result.state.popContinuation()
                    stateWithoutCont
                } else {
                    result.state
                }
                allEvents.addAll(result.events)
                continue
            }

            if (result.isPaused) {
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            currentState = if (stillRemaining.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)

            if (result.updatedCollections.isNotEmpty()) {
                currentContext = currentContext.copy(
                    storedCollections = currentContext.storedCollections + result.updatedCollections
                )
            }
        }

        return checkForMore(currentState, allEvents)
    }

    private fun resumeTriggeredAbility(
        state: GameState,
        continuation: TriggeredAbilityContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected target selection response for triggered ability")
        }

        val selectedTargets = response.selectedTargets.flatMap { (_, targetIds) ->
            targetIds.map { entityId -> entityIdToChosenTarget(state, entityId) }
        }

        if (selectedTargets.isEmpty()) {
            if (continuation.elseEffect != null) {
                val elseComponent = TriggeredAbilityOnStackComponent(
                    sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName,
                    controllerId = continuation.controllerId,
                    effect = continuation.elseEffect,
                    description = continuation.description,
                    triggerDamageAmount = continuation.triggerDamageAmount,
                    triggeringEntityId = continuation.triggeringEntityId,
                    triggerCounterCount = continuation.triggerCounterCount
                )
                val stackResult = stackResolver.putTriggeredAbility(state, elseComponent, emptyList())
                if (!stackResult.isSuccess) return stackResult
                return checkForMore(stackResult.newState, stackResult.events.toList())
            }
            return checkForMore(state, emptyList())
        }

        val abilityComponent = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description,
            triggerDamageAmount = continuation.triggerDamageAmount,
            triggeringEntityId = continuation.triggeringEntityId,
            triggerCounterCount = continuation.triggerCounterCount
        )

        val stackResult = stackResolver.putTriggeredAbility(
            state, abilityComponent, selectedTargets, continuation.targetRequirements
        )

        if (!stackResult.isSuccess) {
            return stackResult
        }

        return checkForMore(stackResult.newState, stackResult.events.toList())
    }

    private fun resumeMayTrigger(
        state: GameState,
        continuation: MayTriggerContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may trigger")
        }

        if (!response.choice) {
            return checkForMore(state, emptyList())
        }

        val trigger = continuation.trigger
        val mayEffect = trigger.ability.effect as MayEffect
        val innerEffect = mayEffect.effect

        val unwrappedAbility = trigger.ability.copy(effect = innerEffect)
        val unwrappedTrigger = trigger.copy(ability = unwrappedAbility)

        val processor = triggerProcessor
            ?: return ExecutionResult.error(state, "TriggerProcessor not available for may trigger continuation")

        val result = processor.processTargetedTrigger(state, unwrappedTrigger, continuation.targetRequirement)

        if (result.isPaused) {
            return result
        }

        if (!result.isSuccess) {
            return result
        }

        return checkForMore(result.newState, result.events.toList())
    }

    private fun resumeMayAbility(
        state: GameState,
        continuation: MayAbilityContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may ability")
        }

        val context = continuation.toEffectContext()
        val effectToExecute = if (response.choice) {
            continuation.effectIfYes
        } else {
            continuation.effectIfNo
        }

        if (effectToExecute == null) {
            return checkForMore(state, emptyList())
        }

        val result = effectExecutorRegistry.execute(state, effectToExecute, context)

        if (result.isPaused) {
            return result
        }

        return checkForMore(result.state, result.events.toList())
    }

    // ─── Central coordinator ───

    private fun checkForMoreContinuations(
        state: GameState,
        events: List<GameEvent>
    ): ExecutionResult {
        val nextContinuation = state.peekContinuation()

        if (nextContinuation is PendingTriggersContinuation && triggerProcessor != null) {
            val (_, stateAfterPop) = state.popContinuation()
            val triggerResult = triggerProcessor.processTriggers(stateAfterPop, nextContinuation.remainingTriggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    events + triggerResult.events
                )
            }

            if (!triggerResult.isSuccess) {
                return ExecutionResult(
                    state = triggerResult.state,
                    events = events + triggerResult.events,
                    error = triggerResult.error
                )
            }

            return ExecutionResult.success(triggerResult.newState, events + triggerResult.events)
        }

        if (nextContinuation is ForEachTargetContinuation && nextContinuation.remainingTargets.isNotEmpty()) {
            val (_, stateAfterPop) = state.popContinuation()
            val forEachTargetExecutor = com.wingedsheep.engine.handlers.effects.composite.ForEachTargetExecutor { s, e, c ->
                effectExecutorRegistry.execute(s, e, c)
            }
            val outerContext = EffectContext(
                sourceId = nextContinuation.sourceId,
                controllerId = nextContinuation.controllerId,
                opponentId = nextContinuation.opponentId,
                xValue = nextContinuation.xValue,
                targets = nextContinuation.remainingTargets,
                namedTargets = nextContinuation.namedTargets
            )
            val result = forEachTargetExecutor.processTargets(
                stateAfterPop,
                nextContinuation.effects,
                nextContinuation.remainingTargets,
                outerContext
            )

            if (result.isPaused) {
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    events + result.events
                )
            }

            // Recursively check for more continuations
            return checkForMoreContinuations(result.state, events.toMutableList().apply { addAll(result.events) })
        }

        if (nextContinuation is ForEachPlayerContinuation && nextContinuation.remainingPlayers.isNotEmpty()) {
            val (_, stateAfterPop) = state.popContinuation()
            val forEachPlayerExecutor = com.wingedsheep.engine.handlers.effects.composite.ForEachPlayerExecutor { s, e, c ->
                effectExecutorRegistry.execute(s, e, c)
            }
            val outerContext = EffectContext(
                sourceId = nextContinuation.sourceId,
                controllerId = nextContinuation.controllerId,
                opponentId = nextContinuation.opponentId,
                xValue = nextContinuation.xValue,
                storedStringLists = nextContinuation.storedStringLists
            )
            val result = forEachPlayerExecutor.processPlayers(
                stateAfterPop,
                nextContinuation.effects,
                nextContinuation.remainingPlayers,
                outerContext
            )

            if (result.isPaused) {
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    events + result.events
                )
            }

            // Recursively check for more continuations
            return checkForMoreContinuations(result.state, events.toMutableList().apply { addAll(result.events) })
        }

        if (nextContinuation is DrawReplacementRemainingDrawsContinuation) {
            val (_, stateAfterPop) = state.popContinuation()
            if (nextContinuation.remainingDraws > 0) {
                if (nextContinuation.isDrawStep) {
                    val turnManager = com.wingedsheep.engine.core.TurnManager(cardRegistry = stackResolver.cardRegistry, effectExecutor = effectExecutorRegistry::execute)
                    val drawResult = turnManager.drawCards(stateAfterPop, nextContinuation.drawingPlayerId, nextContinuation.remainingDraws)
                    if (drawResult.isPaused) {
                        return ExecutionResult.paused(
                            drawResult.state,
                            drawResult.pendingDecision!!,
                            events + drawResult.events
                        )
                    }
                    return checkForMoreContinuations(drawResult.newState, events + drawResult.events)
                } else {
                    val drawExecutor = com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor(cardRegistry = stackResolver.cardRegistry, effectExecutor = effectExecutorRegistry::execute)
                    val drawResult = drawExecutor.executeDraws(stateAfterPop, nextContinuation.drawingPlayerId, nextContinuation.remainingDraws)
                    if (drawResult.isPaused) {
                        return ExecutionResult.paused(
                            drawResult.state,
                            drawResult.pendingDecision!!,
                            events + drawResult.events
                        )
                    }
                    return checkForMoreContinuations(drawResult.state, events + drawResult.events)
                }
            }
            return checkForMoreContinuations(stateAfterPop, events)
        }

        if (nextContinuation is CycleDrawContinuation) {
            val (_, stateAfterPop) = state.popContinuation()
            val drawExecutor = com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor(cardRegistry = stackResolver.cardRegistry, effectExecutor = effectExecutorRegistry::execute)
            val drawResult = drawExecutor.executeDraws(stateAfterPop, nextContinuation.playerId, 1)
            if (drawResult.isPaused) {
                return ExecutionResult.paused(
                    drawResult.state,
                    drawResult.pendingDecision!!,
                    events + drawResult.events
                )
            }
            return checkForMoreContinuations(drawResult.state, events + drawResult.events)
        }

        if (nextContinuation is TypecycleSearchContinuation) {
            val (_, stateAfterPop) = state.popContinuation()
            val searchFilter = com.wingedsheep.sdk.scripting.GameObjectFilter.Any.withSubtype(nextContinuation.subtypeFilter)
            val searchEffect = com.wingedsheep.sdk.dsl.EffectPatterns.searchLibrary(
                filter = searchFilter,
                count = 1,
                reveal = true
            )
            val effectContext = EffectContext(
                sourceId = nextContinuation.cardId,
                controllerId = nextContinuation.playerId,
                opponentId = stateAfterPop.getOpponent(nextContinuation.playerId)
            )
            val searchResult = effectExecutorRegistry.execute(stateAfterPop, searchEffect, effectContext)
            if (searchResult.isPaused) {
                return ExecutionResult.paused(
                    searchResult.state,
                    searchResult.pendingDecision!!,
                    events + searchResult.events
                )
            }
            return checkForMoreContinuations(searchResult.state, events + searchResult.events)
        }

        if (nextContinuation is EffectContinuation && nextContinuation.remainingEffects.isNotEmpty()) {
            val (_, stateAfterPop) = state.popContinuation()
            var currentContext = nextContinuation.toEffectContext()
            var currentState = stateAfterPop
            val allEvents = events.toMutableList()

            for ((index, effect) in nextContinuation.remainingEffects.withIndex()) {
                val stillRemaining = nextContinuation.remainingEffects.drop(index + 1)

                val stateForExecution = if (stillRemaining.isNotEmpty()) {
                    val remainingContinuation = EffectContinuation(
                        decisionId = "pending",
                        remainingEffects = stillRemaining,
                        sourceId = nextContinuation.sourceId,
                        controllerId = nextContinuation.controllerId,
                        opponentId = nextContinuation.opponentId,
                        xValue = nextContinuation.xValue,
                        targets = nextContinuation.targets,
                        storedCollections = currentContext.storedCollections,
                        chosenCreatureType = nextContinuation.chosenCreatureType,
                        namedTargets = nextContinuation.namedTargets,
                        chosenValues = nextContinuation.chosenValues,
                        storedNumbers = currentContext.storedNumbers,
                        storedStringLists = currentContext.storedStringLists
                    )
                    currentState.pushContinuation(remainingContinuation)
                } else {
                    currentState
                }

                val result = effectExecutorRegistry.execute(stateForExecution, effect, currentContext)

                if (!result.isSuccess && !result.isPaused) {
                    currentState = if (stillRemaining.isNotEmpty()) {
                        val (_, stateWithoutCont) = result.state.popContinuation()
                        stateWithoutCont
                    } else {
                        result.state
                    }
                    allEvents.addAll(result.events)
                    continue
                }

                if (result.isPaused) {
                    return ExecutionResult.paused(
                        result.state,
                        result.pendingDecision!!,
                        allEvents + result.events
                    )
                }

                currentState = if (stillRemaining.isNotEmpty()) {
                    val (_, stateWithoutCont) = result.state.popContinuation()
                    stateWithoutCont
                } else {
                    result.state
                }
                allEvents.addAll(result.events)

                if (result.updatedCollections.isNotEmpty()) {
                    currentContext = currentContext.copy(
                        storedCollections = currentContext.storedCollections + result.updatedCollections
                    )
                }
            }

            // Recursively check for more continuations (e.g., outer CompositeEffect)
            return checkForMoreContinuations(currentState, allEvents)
        }

        if (nextContinuation is RepeatWhileContinuation && nextContinuation.phase == RepeatWhilePhase.AFTER_BODY) {
            val (_, stateAfterPop) = state.popContinuation()
            val context = nextContinuation.toEffectContext()

            val result = com.wingedsheep.engine.handlers.effects.composite.RepeatWhileExecutor.askCondition(
                state = stateAfterPop,
                body = nextContinuation.body,
                repeatCondition = nextContinuation.repeatCondition,
                resolvedDeciderId = nextContinuation.resolvedDeciderId,
                context = context,
                sourceName = nextContinuation.sourceName,
                effectExecutor = effectExecutorRegistry::execute,
                priorEvents = events
            )

            if (result.isPaused) {
                return result
            }

            return checkForMoreContinuations(result.state, result.events.toList())
        }

        return ExecutionResult.success(state, events)
    }

    // ─── Generic drawing/repeat ───

    private fun resumeDrawUpTo(
        state: GameState,
        continuation: DrawUpToContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number chosen response for DrawUpTo")
        }

        val chosenCount = response.number

        // Store cards-not-drawn count in the next EffectContinuation if requested
        var currentState = state
        if (continuation.storeNotDrawnAs != null) {
            val cardsNotDrawn = continuation.originalMaxCards - chosenCount
            val injectResult = com.wingedsheep.engine.handlers.effects.drawing.DrawUpToExecutor.injectStoredNumber(
                currentState, continuation.storeNotDrawnAs, cardsNotDrawn
            )
            currentState = injectResult.state
        }

        if (chosenCount <= 0) {
            return checkForMore(currentState, emptyList())
        }

        // Draw through the registry so draw replacement effects (Words of Wind, etc.) work
        val drawEffect = com.wingedsheep.sdk.scripting.effects.DrawCardsEffect(chosenCount, com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller)
        val drawContext = EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.playerId,
            opponentId = null
        )
        val result = effectExecutorRegistry.execute(currentState, drawEffect, drawContext)

        if (result.isPaused) {
            return result
        }

        return checkForMore(result.state, result.events.toList())
    }

    private fun resumeRepeatWhile(
        state: GameState,
        continuation: RepeatWhileContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        return when (continuation.phase) {
            RepeatWhilePhase.AFTER_BODY -> {
                // Should not be at top of stack during decision resume
                ExecutionResult.error(state, "RepeatWhileContinuation AFTER_BODY should not be at top of stack during decision resume")
            }
            RepeatWhilePhase.AFTER_DECISION -> {
                if (response !is YesNoResponse) {
                    return ExecutionResult.error(state, "Expected yes/no response for RepeatWhile")
                }

                if (!response.choice) {
                    // Player chose not to repeat — done
                    return checkForMore(state, emptyList())
                }

                // Player chose to repeat — execute another iteration
                val context = continuation.toEffectContext()
                val result = com.wingedsheep.engine.handlers.effects.composite.RepeatWhileExecutor.executeIteration(
                    state = state,
                    body = continuation.body,
                    repeatCondition = continuation.repeatCondition,
                    resolvedDeciderId = continuation.resolvedDeciderId,
                    context = context,
                    sourceName = continuation.sourceName,
                    effectExecutor = effectExecutorRegistry::execute,
                    priorEvents = emptyList()
                )

                if (result.isPaused) {
                    return result
                }

                return checkForMore(result.state, result.events.toList())
            }
        }
    }

    // ─── Utility ───

    private fun entityIdToChosenTarget(state: GameState, entityId: EntityId): ChosenTarget {
        return when {
            entityId in state.turnOrder -> ChosenTarget.Player(entityId)
            entityId in state.getBattlefield() -> ChosenTarget.Permanent(entityId)
            entityId in state.stack -> ChosenTarget.Spell(entityId)
            else -> {
                val graveyardOwner = state.turnOrder.find { playerId ->
                    val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
                    entityId in state.getZone(graveyardZone)
                }
                if (graveyardOwner != null) {
                    ChosenTarget.Card(entityId, graveyardOwner, Zone.GRAVEYARD)
                } else {
                    ChosenTarget.Permanent(entityId)
                }
            }
        }
    }

    private fun resumeStormCopyTarget(
        state: GameState,
        continuation: StormCopyTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected target selection response for Storm copy")
        }

        val selectedTargets = response.selectedTargets.flatMap { (_, targetIds) ->
            targetIds.map { entityId -> entityIdToChosenTarget(state, entityId) }
        }

        val allEvents = mutableListOf<GameEvent>()
        var currentState = state

        // Create the copy with the selected targets
        val copyAbility = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId,
            sourceName = continuation.spellName,
            controllerId = continuation.controllerId,
            effect = continuation.spellEffect,
            description = "Storm copy of ${continuation.spellName}"
        )
        val stackResult = stackResolver.putTriggeredAbility(
            currentState, copyAbility, selectedTargets, continuation.spellTargetRequirements
        )
        if (!stackResult.isSuccess) return stackResult
        currentState = stackResult.newState
        allEvents.addAll(stackResult.events)

        val remainingAfterThis = continuation.remainingCopies - 1
        if (remainingAfterThis <= 0) {
            return checkForMore(currentState, allEvents)
        }

        // Prompt for next copy's targets
        val decisionId = "storm-copy-target-${System.nanoTime()}"
        val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
        for ((index, requirement) in continuation.spellTargetRequirements.withIndex()) {
            val legalTargets = targetFinder.findLegalTargets(
                currentState, requirement, continuation.controllerId, continuation.sourceId
            )
            legalTargetsMap[index] = legalTargets
        }

        // If no legal targets available, skip remaining copies
        val hasNoLegalTargets = legalTargetsMap.any { (_, targets) -> targets.isEmpty() }
        if (hasNoLegalTargets) {
            return checkForMore(currentState, allEvents)
        }

        val nextContinuation = StormCopyTargetContinuation(
            decisionId = decisionId,
            remainingCopies = remainingAfterThis,
            spellEffect = continuation.spellEffect,
            spellTargetRequirements = continuation.spellTargetRequirements,
            spellName = continuation.spellName,
            controllerId = continuation.controllerId,
            sourceId = continuation.sourceId
        )
        val targetReqInfos = continuation.spellTargetRequirements.mapIndexed { index, req ->
            TargetRequirementInfo(
                index = index,
                description = req.description
            )
        }

        val totalCopies = continuation.remainingCopies
        val copyNumber = totalCopies - remainingAfterThis + 1
        val decision = ChooseTargetsDecision(
            id = decisionId,
            playerId = continuation.controllerId,
            prompt = "Choose targets for Storm copy $copyNumber of ${continuation.spellName}",
            context = DecisionContext(
                phase = DecisionPhase.CASTING,
                sourceName = continuation.spellName
            ),
            targetRequirements = targetReqInfos,
            legalTargets = legalTargetsMap
        )

        currentState = currentState.withPendingDecision(decision)
        currentState = currentState.pushContinuation(nextContinuation)

        return ExecutionResult.paused(currentState, decision, allEvents)
    }

    private fun resumeDistributeCounters(
        state: GameState,
        continuation: DistributeCountersContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is DistributionResponse) {
            return ExecutionResult.error(state, "Expected distribution response for counter distribution")
        }

        val counterType = try {
            com.wingedsheep.sdk.core.CounterType.valueOf(
                continuation.counterType.uppercase()
                    .replace(' ', '_')
                    .replace('+', 'P')
                    .replace('-', 'M')
                    .replace("/", "_")
            )
        } catch (e: IllegalArgumentException) {
            com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE
        }

        val distribution = response.distribution
        val totalMoved = distribution.values.sum()

        if (totalMoved <= 0) {
            return checkForMore(state, emptyList())
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Remove counters from source
        val sourceCounters = newState.getEntity(continuation.sourceId)
            ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
            ?: com.wingedsheep.engine.state.components.battlefield.CountersComponent()

        newState = newState.updateEntity(continuation.sourceId) { container ->
            container.with(sourceCounters.withRemoved(counterType, totalMoved))
        }

        val sourceName = newState.getEntity(continuation.sourceId)
            ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name ?: ""
        events.add(CountersRemovedEvent(continuation.sourceId, continuation.counterType, totalMoved, sourceName))

        // Add counters to each target (applying replacement effects like Hardened Scales)
        for ((targetId, amount) in distribution) {
            if (amount > 0) {
                val modifiedAmount = EffectExecutorUtils.applyCounterPlacementModifiers(
                    newState, targetId, counterType, amount
                )
                val targetCounters = newState.getEntity(targetId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                    ?: com.wingedsheep.engine.state.components.battlefield.CountersComponent()

                newState = newState.updateEntity(targetId) { container ->
                    container.with(targetCounters.withAdded(counterType, modifiedAmount))
                }

                val targetName = newState.getEntity(targetId)
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name ?: ""
                events.add(CountersAddedEvent(targetId, continuation.counterType, modifiedAmount, targetName))
            }
        }

        return checkForMore(newState, events)
    }

    private fun resumeReturnFromLinkedExile(
        state: GameState,
        continuation: ReturnFromLinkedExileContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected cards selected response for linked exile return")
        }

        val selectedCard = response.selectedCards.firstOrNull()
            ?: return checkForMoreContinuations(state, emptyList())

        // Validate that the selected card is in the eligible list
        if (selectedCard !in continuation.eligibleCards) {
            return ExecutionResult.error(state, "Selected card is not in the eligible linked exile cards")
        }

        val result = com.wingedsheep.engine.handlers.effects.removal.ReturnOneFromLinkedExileExecutor
            .returnCardToBattlefield(state, selectedCard, continuation.sourceId)

        return checkForMoreContinuations(result.state, result.events)
    }

    private fun resumeAddDynamicMana(
        state: GameState,
        continuation: AddDynamicManaContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number chosen response for AddDynamicMana")
        }

        val firstAmount = response.number.coerceIn(0, continuation.totalAmount)
        val secondAmount = continuation.totalAmount - firstAmount

        val newState = com.wingedsheep.engine.handlers.effects.mana.AddDynamicManaExecutor.addMana(
            state, continuation.playerId,
            mapOf(continuation.firstColor to firstAmount, continuation.secondColor to secondAmount)
        )

        return checkForMore(newState, emptyList())
    }
}

/**
 * Creates a [ContinuationResumer] that always returns an error for continuation types
 * that should never be at the top of the stack during a decision resume.
 */
private inline fun <reified T : ContinuationFrame> errorResumer(
    typeName: String
): ContinuationResumer<T> = resumer(T::class) { state, _, _, _ ->
    ExecutionResult.error(state, "$typeName should not be at top of stack during decision resume")
}
