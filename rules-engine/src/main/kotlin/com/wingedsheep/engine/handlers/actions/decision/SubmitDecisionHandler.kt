package com.wingedsheep.engine.handlers.actions.decision

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ContinuationHandler
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Step
import kotlin.reflect.KClass

/**
 * Handler for the SubmitDecision action.
 *
 * Processes player responses to pending decisions, using the
 * continuation system to resume effect execution.
 */
class SubmitDecisionHandler(
    private val continuationHandler: ContinuationHandler,
    private val turnManager: TurnManager,
    private val sbaChecker: StateBasedActionChecker,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor
) : ActionHandler<SubmitDecision> {
    override val actionType: KClass<SubmitDecision> = SubmitDecision::class

    override fun validate(state: GameState, action: SubmitDecision): String? {
        val pending = state.pendingDecision
            ?: return "No pending decision to respond to"

        if (pending.playerId != action.playerId) {
            return "You are not the player who needs to make this decision"
        }

        if (pending.id != action.response.decisionId) {
            return "Decision ID mismatch: expected ${pending.id}, got ${action.response.decisionId}"
        }

        return DecisionValidators.validate(pending, action.response, state)
    }

    override fun execute(state: GameState, action: SubmitDecision): ExecutionResult {
        val pending = state.pendingDecision
            ?: return ExecutionResult.error(state, "No pending decision")

        // Clear the pending decision
        val clearedState = state.clearPendingDecision()

        val submittedEvent = DecisionSubmittedEvent(
            pending.id,
            action.playerId,
            description = buildDecisionDescription(state, pending, action.response)
        )

        // Check if there's a continuation frame to process
        val hasContinuation = clearedState.peekContinuation() != null

        if (hasContinuation) {
            // Snapshot the stack before resuming so deferred mid-resolution triggers (below) can be
            // placed beneath the frames this resume leaves in flight, not between them.
            val preResumeStack = clearedState.continuationStack
            val result = continuationHandler.resume(clearedState, action.response)

            // Handle cleanup step completion
            if (result.isSuccess && !result.isPaused &&
                result.state.step == Step.CLEANUP &&
                result.state.pendingDecision == null
            ) {
                val cleanupAdvanceResult = turnManager.advanceStep(result.state)
                return advanceWithTriggerDetection(
                    cleanupAdvanceResult,
                    listOf(submittedEvent) + result.events
                )
            }

            // Handle untap step completion. Two clients land here:
            //   1. A MAY_NOT_UNTAP decision (e.g., Frozen Solid / Static Orb) just resolved.
            //   2. The CR 103.6 leyline phase (LeylineContinuationResumer) just resolved its
            //      last yes/no decision. The engine starts a new game at Step.UNTAP, and the
            //      leyline phase deliberately leaves state.step == UNTAP with no pending
            //      decision so this branch advances into turn 1's actual UNTAP processing.
            // If you refactor either flow, make sure both still trigger this advance.
            if (result.isSuccess && !result.isPaused &&
                result.state.step == Step.UNTAP &&
                result.state.pendingDecision == null
            ) {
                val untapAdvanceResult = turnManager.advanceStep(result.state)
                return advanceWithTriggerDetection(
                    untapAdvanceResult,
                    listOf(submittedEvent) + result.events,
                    // The resumed untap choice emits the UntappedEvents (one per permanent that
                    // actually untapped). Those must be run through event-based trigger detection
                    // or "becomes untapped" triggers (Tawnos's Coffin) are lost — the non-paused
                    // untap path carries these forward to PassPriorityHandler's detection, so this
                    // paused-resume path has to do the same.
                    detectPrecedingEvents = true
                )
            }

            // Check SBAs after continuation completes
            if (result.isSuccess && !result.isPaused) {
                val sbaResult = sbaChecker.checkAndApply(result.state)

                // If SBA needs player input (e.g., legend rule), return paused
                if (sbaResult.isPaused) {
                    return ExecutionResult.paused(
                        sbaResult.state,
                        sbaResult.pendingDecision!!,
                        listOf(submittedEvent) + result.events + sbaResult.events
                    )
                }

                var combinedEvents = listOf(submittedEvent) + result.events + sbaResult.events

                if (sbaResult.newState.gameOver) {
                    return ExecutionResult.success(sbaResult.newState, combinedEvents)
                }

                // Process triggers. When the resumed chain re-entered an action handler
                // that already ran detection on its own emitted events (e.g.,
                // `CastSpellHandler` re-entered via `finalizeModalCast` after a cast-time
                // mode picker), skip those events here — re-detecting would double-queue
                // battlefield triggers like Riku of Many Paths.
                val eventsToDetect = if (result.triggersAlreadyProcessed) {
                    listOf(submittedEvent) + sbaResult.events
                } else {
                    combinedEvents
                }
                val triggers = triggerDetector.detectTriggers(sbaResult.newState, eventsToDetect)
                if (triggers.isNotEmpty()) {
                    val triggerResult = triggerProcessor.processTriggers(sbaResult.newState, triggers)

                    if (triggerResult.isPaused) {
                        return ExecutionResult.paused(
                            triggerResult.state,
                            triggerResult.pendingDecision!!,
                            combinedEvents + triggerResult.events
                        )
                    }

                    combinedEvents = combinedEvents + triggerResult.events
                    return ExecutionResult.success(
                        triggerResult.newState.withPriority(action.playerId),
                        combinedEvents
                    )
                }

                return ExecutionResult.success(
                    sbaResult.newState.withPriority(action.playerId),
                    combinedEvents
                )
            }

            // The chain paused (or errored). When paused, events emitted during the
            // chain (e.g., BecomesTargetEvent from putting a triggered ability on the
            // stack mid-chain) may fire additional triggers (e.g., Valiant) that the
            // success path's trigger-detection at line 108 would catch — but the paused
            // path returns before reaching it, so those triggers would be lost.
            // Detect them here and queue as a PendingTriggersContinuation BENEATH the frames
            // this resume left in flight, so they fire only after the whole in-flight resolution
            // finishes (CR 603.3 — triggers wait for the next time a player would receive
            // priority), not between two of its own steps. Mirrors
            // PassPriorityHandler.resolveTopOfStack's mid-resolution handling.
            if (result.isPaused && !result.triggersAlreadyProcessed) {
                val deferredTriggers = triggerDetector.detectTriggers(result.state, result.events)
                if (deferredTriggers.isNotEmpty()) {
                    val pending = PendingTriggersContinuation(
                        decisionId = "submit-deferred-triggers-${java.util.UUID.randomUUID()}",
                        remainingTriggers = deferredTriggers
                    )
                    // Frames untouched by this resume (the identity-equal bottom prefix) are outer
                    // resolutions the triggers must not jump ahead of; everything above is the
                    // in-flight resolution's remaining frames, which must drain first. Inserting
                    // pending just below the top frame (the old behavior) fired the trigger between,
                    // e.g., a ForEach player's keep decision and that same player's sacrifice —
                    // orphaning the sacrifice.
                    val postStack = result.state.continuationStack
                    var untouched = 0
                    while (untouched < preResumeStack.size && untouched < postStack.size &&
                        preResumeStack[untouched] === postStack[untouched]
                    ) untouched++
                    val newStack = postStack.subList(0, untouched) + pending +
                        postStack.subList(untouched, postStack.size)
                    return ExecutionResult.paused(
                        result.state.copy(continuationStack = newStack),
                        result.pendingDecision!!,
                        listOf(submittedEvent) + result.events
                    )
                }
            }

            // Prepend the submitted event
            return if (result.isSuccess || result.isPaused) {
                ExecutionResult(
                    state = result.state,
                    events = listOf(submittedEvent) + result.events,
                    error = result.error,
                    pendingDecision = result.pendingDecision
                )
            } else {
                result
            }
        }

        // No continuation - just return with cleared state
        return ExecutionResult.success(clearedState, listOf(submittedEvent))
    }

    /**
     * After advancing the game step, detect any triggers that should fire
     * (phase/step triggers, delayed triggers, event-based triggers).
     * Mirrors the trigger detection logic in PassPriorityHandler.
     */
    private fun advanceWithTriggerDetection(
        advanceResult: ExecutionResult,
        precedingEvents: List<GameEvent>,
        // When true, run event-based trigger detection over [precedingEvents] as well as the
        // step-advance events. The untap-resume path sets this so the UntappedEvents emitted by
        // the resolved untap choice fire "becomes untapped" triggers; other callers (cleanup)
        // keep the historical behaviour of detecting only on the advance events.
        detectPrecedingEvents: Boolean = false
    ): ExecutionResult {
        if (!advanceResult.isSuccess || advanceResult.events.isEmpty()) {
            return ExecutionResult(
                state = advanceResult.state,
                events = precedingEvents + advanceResult.events,
                error = advanceResult.error,
                pendingDecision = advanceResult.pendingDecision
            )
        }

        var currentState = advanceResult.newState
        val eventsForDetection = if (detectPrecedingEvents) {
            precedingEvents + advanceResult.events
        } else {
            advanceResult.events
        }
        val triggers = triggerDetector.detectTriggers(currentState, eventsForDetection).toMutableList()

        val stepChangedEvent = advanceResult.events.filterIsInstance<StepChangedEvent>().lastOrNull()
        if (stepChangedEvent != null) {
            val (delayedTriggers, consumedIds) = triggerDetector.detectDelayedTriggers(
                currentState, stepChangedEvent.newStep
            )
            if (consumedIds.isNotEmpty()) {
                currentState = currentState.removeDelayedTriggers(consumedIds)
            }
            triggers.addAll(delayedTriggers)

            val activePlayer = currentState.activePlayerId
            if (activePlayer != null) {
                val phaseStepTriggers = triggerDetector.detectPhaseStepTriggers(
                    currentState, stepChangedEvent.newStep, activePlayer
                )
                triggers.addAll(phaseStepTriggers)
            }
        }

        val allEvents = precedingEvents + advanceResult.events

        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(currentState, triggers)
            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    allEvents + triggerResult.events
                )
            }
            return ExecutionResult.success(
                triggerResult.newState.withPriority(currentState.activePlayerId),
                allEvents + triggerResult.events
            )
        }

        return ExecutionResult.success(currentState, allEvents)
    }

    /**
     * Build a human-readable description of the decision that was made.
     */
    private fun buildDecisionDescription(
        state: GameState,
        pending: PendingDecision,
        response: DecisionResponse
    ): String? {
        val sourceName = pending.context.sourceName
        val sourcePrefix = sourceName?.let { "($it) " } ?: ""

        return when {
            pending is YesNoDecision && response is YesNoResponse -> {
                val choice = if (response.choice) "Yes" else "No"
                "${sourcePrefix}Chose $choice"
            }

            pending is BatchYesNoDecision && response is BatchYesNoResponse -> {
                val choice = if (response.choice) "Yes" else "No"
                val scope = if (response.applyToAll) " to all ${pending.count}" else ""
                "${sourcePrefix}Chose $choice$scope"
            }

            pending is ChooseNumberDecision && response is NumberChosenResponse -> {
                "${sourcePrefix}Chose X = ${response.number}"
            }

            pending is ChooseColorDecision && response is ColorChosenResponse -> {
                "${sourcePrefix}Chose ${response.color.name.lowercase()}"
            }

            pending is ChooseModeDecision && response is ModesChosenResponse -> {
                val modeTexts = response.selectedModes.mapNotNull { idx ->
                    pending.modes.find { it.index == idx }?.text
                }
                if (modeTexts.isNotEmpty()) {
                    "${sourcePrefix}Chose mode: ${modeTexts.joinToString(", ")}"
                } else null
            }

            pending is ChooseOptionDecision && response is OptionChosenResponse -> {
                val optionText = pending.options.getOrNull(response.optionIndex)
                if (optionText != null) {
                    "${sourcePrefix}Chose $optionText"
                } else null
            }

            pending is ChooseTargetsDecision && response is TargetsResponse -> {
                val targetNames = response.selectedTargets.values.flatten().mapNotNull { targetId ->
                    state.getEntity(targetId)?.get<CardComponent>()?.name
                        ?: if (state.turnOrder.contains(targetId)) "player" else null
                }
                if (targetNames.isNotEmpty()) {
                    "${sourcePrefix}Targeting ${targetNames.joinToString(", ")}"
                } else null
            }

            pending is DistributeDecision && response is DistributionResponse -> {
                val parts = response.distribution.mapNotNull { (targetId, amount) ->
                    val name = state.getEntity(targetId)?.get<CardComponent>()?.name
                        ?: if (state.turnOrder.contains(targetId)) "player" else null
                    name?.let { "$amount to $it" }
                }
                if (parts.isNotEmpty()) {
                    "${sourcePrefix}Distributed: ${parts.joinToString(", ")}"
                } else null
            }

            // SelectCards, OrderObjects, SearchLibrary, etc. - these produce their own
            // events (ScryCompleted, PermanentsSacrificed, etc.) so no extra log needed
            else -> null
        }
    }

    companion object {
        fun create(services: EngineServices): SubmitDecisionHandler {
            return SubmitDecisionHandler(
                services.continuationHandler,
                services.turnManager,
                services.sbaChecker,
                services.triggerDetector,
                services.triggerProcessor
            )
        }
    }
}
