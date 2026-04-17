package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Handles miscellaneous continuation types:
 * - StormCopyTargetContinuation
 * - DistributeCountersContinuation
 * - DrawUpToContinuation
 * - RepeatWhileContinuation
 * - ReturnFromLinkedExileContinuation
 * - AddDynamicManaContinuation
 */
class MiscContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices,
    private val effectRunner: EffectContinuationRunner
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(DrawUpToContinuation::class, ::resumeDrawUpTo),
        resumer(RepeatWhileContinuation::class, ::resumeRepeatWhile),
        resumer(StormCopyTargetContinuation::class, ::resumeStormCopyTarget),
        resumer(StormCopyModalTargetContinuation::class, ::resumeStormCopyModalTarget),
        resumer(DistributeCountersContinuation::class, ::resumeDistributeCounters),
        resumer(AddDynamicManaContinuation::class, ::resumeAddDynamicMana),
        resumer(ReturnFromLinkedExileContinuation::class) { state, continuation, response, checkForMore ->
            resumeReturnFromLinkedExile(state, continuation, response, checkForMore)
        }
    )

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
        val result = services.effectExecutorRegistry.execute(currentState, drawEffect, drawContext).toExecutionResult()

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
                val context = continuation.effectContext
                val result = com.wingedsheep.engine.handlers.effects.composite.RepeatWhileExecutor.executeIteration(
                    state = state,
                    body = continuation.body,
                    repeatCondition = continuation.repeatCondition,
                    resolvedDeciderId = continuation.resolvedDeciderId,
                    context = context,
                    sourceName = continuation.sourceName,
                    effectExecutor = services.effectExecutorRegistry::execute,
                    priorEvents = emptyList()
                )

                if (result.isPaused) {
                    return result.toExecutionResult()
                }

                return checkForMore(result.state, result.events.toList())
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

        // Put the copy on the stack as a spell (707.12).
        val copyIndex = continuation.totalCopies - continuation.remainingCopies + 1
        val stackResult = services.stackResolver.putSpellCopy(
            state = currentState,
            sourceSpellId = continuation.sourceId,
            targets = selectedTargets,
            targetRequirements = continuation.spellTargetRequirements,
            copyIndex = copyIndex,
            copyTotal = continuation.totalCopies,
            controllerId = continuation.controllerId
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
            val legalTargets = services.targetFinder.findLegalTargets(
                currentState, requirement, continuation.controllerId, continuation.sourceId
            )
            legalTargetsMap[index] = legalTargets
        }

        // 707.7b: if no legal replacement exists for any remaining copy, still put
        // each copy on the stack inheriting the source's (illegal) targets so it
        // fizzles on resolution per 608.2b / 112.3b. The battlefield doesn't change
        // between copy creations, so legality is the same for all remaining copies.
        val hasNoLegalTargets = legalTargetsMap.any { (_, targets) -> targets.isEmpty() }
        if (hasNoLegalTargets) {
            var loopState = currentState
            val loopEvents = allEvents
            var copiesLeft = remainingAfterThis
            while (copiesLeft > 0) {
                val nextCopyIndex = continuation.totalCopies - copiesLeft + 1
                val res = services.stackResolver.putSpellCopy(
                    state = loopState,
                    sourceSpellId = continuation.sourceId,
                    copyIndex = nextCopyIndex,
                    copyTotal = continuation.totalCopies,
                    controllerId = continuation.controllerId
                )
                if (!res.isSuccess) return res
                loopState = res.newState
                loopEvents.addAll(res.events)
                copiesLeft--
            }
            return checkForMore(loopState, loopEvents)
        }

        val nextContinuation = StormCopyTargetContinuation(
            decisionId = decisionId,
            remainingCopies = remainingAfterThis,
            spellEffect = continuation.spellEffect,
            spellTargetRequirements = continuation.spellTargetRequirements,
            spellName = continuation.spellName,
            controllerId = continuation.controllerId,
            sourceId = continuation.sourceId,
            totalCopies = continuation.totalCopies
        )
        val targetReqInfos = continuation.spellTargetRequirements.mapIndexed { index, req ->
            TargetRequirementInfo(
                index = index,
                description = req.description
            )
        }

        val totalCopies = continuation.totalCopies
        val copyNumber = totalCopies - remainingAfterThis + 1
        val copyLabel = if (totalCopies > 1)
            "copy $copyNumber of $totalCopies of ${continuation.spellName}"
            else "copy of ${continuation.spellName}"
        val decision = ChooseTargetsDecision(
            id = decisionId,
            playerId = continuation.controllerId,
            prompt = "Choose new targets for $copyLabel",
            context = DecisionContext(
                phase = DecisionPhase.CASTING,
                sourceName = continuation.spellName,
                effectHint = "Copy of ${continuation.spellName}"
            ),
            targetRequirements = targetReqInfos,
            legalTargets = legalTargetsMap
        )

        currentState = currentState.withPendingDecision(decision)
        currentState = currentState.pushContinuation(nextContinuation)

        return ExecutionResult.paused(currentState, decision, allEvents)
    }

    private fun resumeStormCopyModalTarget(
        state: GameState,
        continuation: StormCopyModalTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected target selection response for Storm modal copy")
        }

        val selectedTargets = response.selectedTargets.entries
            .sortedBy { it.key }
            .flatMap { (_, ids) -> ids.map { entityId -> entityIdToChosenTarget(state, entityId) } }

        val updatedAccumulated = continuation.accumulatedOrdinalTargets + listOf(selectedTargets)
        val nextOrdinal = continuation.currentOrdinal + 1

        val result = com.wingedsheep.engine.handlers.effects.stack.StormCopyEffectExecutor.driveStormModalCopies(
            state = state,
            stackResolver = services.stackResolver,
            targetFinder = services.targetFinder,
            sourceId = continuation.sourceId,
            controllerId = continuation.controllerId,
            spellName = continuation.spellName,
            chosenModes = continuation.chosenModes,
            modeTargetRequirements = continuation.modeTargetRequirements,
            accumulatedOrdinalTargets = updatedAccumulated,
            currentOrdinal = nextOrdinal,
            remainingCopies = continuation.remainingCopies,
            totalCopies = continuation.totalCopies,
            priorEvents = emptyList()
        )

        if (result.isPaused) {
            return result
        }

        return checkForMore(result.state, result.events.toList())
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
                val modifiedAmount = ReplacementEffectUtils.applyCounterPlacementModifiers(
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
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected cards selected response for linked exile return")
        }

        val selectedCard = response.selectedCards.firstOrNull()
            ?: return checkForMore(state, emptyList())

        // Validate that the selected card is in the eligible list
        if (selectedCard !in continuation.eligibleCards) {
            return ExecutionResult.error(state, "Selected card is not in the eligible linked exile cards")
        }

        val result = com.wingedsheep.engine.handlers.effects.linkedexile.ReturnOneFromLinkedExileExecutor
            .returnCardToBattlefield(state, selectedCard, continuation.sourceId)

        return checkForMore(result.state, result.events)
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
            mapOf(continuation.firstColor to firstAmount, continuation.secondColor to secondAmount),
            continuation.restriction
        )

        return checkForMore(newState, emptyList())
    }
}
