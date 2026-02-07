package com.wingedsheep.engine.handlers.actions.decision

import com.wingedsheep.engine.core.DecisionSubmittedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.StepChangedEvent
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ContinuationHandler
import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.state.GameState
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

        return DecisionValidators.validate(pending, action.response)
    }

    override fun execute(state: GameState, action: SubmitDecision): ExecutionResult {
        val pending = state.pendingDecision
            ?: return ExecutionResult.error(state, "No pending decision")

        // Clear the pending decision
        val clearedState = state.clearPendingDecision()

        val submittedEvent = DecisionSubmittedEvent(pending.id, action.playerId)

        // Check if there's a continuation frame to process
        val hasContinuation = clearedState.peekContinuation() != null

        if (hasContinuation) {
            val result = continuationHandler.resume(clearedState, action.response)

            // Handle cleanup step completion
            if (result.isSuccess && !result.isPaused &&
                result.state.step == Step.CLEANUP &&
                result.state.pendingDecision == null
            ) {
                val cleanupAdvanceResult = turnManager.advanceStep(result.state)
                return ExecutionResult(
                    state = cleanupAdvanceResult.state,
                    events = listOf(submittedEvent) + result.events + cleanupAdvanceResult.events,
                    error = cleanupAdvanceResult.error,
                    pendingDecision = cleanupAdvanceResult.pendingDecision
                )
            }

            // Handle untap step completion (MAY_NOT_UNTAP decision resolved)
            if (result.isSuccess && !result.isPaused &&
                result.state.step == Step.UNTAP &&
                result.state.pendingDecision == null
            ) {
                val untapAdvanceResult = turnManager.advanceStep(result.state)
                return ExecutionResult(
                    state = untapAdvanceResult.state,
                    events = listOf(submittedEvent) + result.events + untapAdvanceResult.events,
                    error = untapAdvanceResult.error,
                    pendingDecision = untapAdvanceResult.pendingDecision
                )
            }

            // Check SBAs after continuation completes
            if (result.isSuccess && !result.isPaused) {
                val sbaResult = sbaChecker.checkAndApply(result.state)
                var combinedEvents = listOf(submittedEvent) + result.events + sbaResult.events

                if (sbaResult.newState.gameOver) {
                    return ExecutionResult.success(sbaResult.newState, combinedEvents)
                }

                // Process triggers
                val triggers = triggerDetector.detectTriggers(sbaResult.newState, combinedEvents)
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
                        triggerResult.newState.withPriority(state.activePlayerId),
                        combinedEvents
                    )
                }

                return ExecutionResult.success(
                    sbaResult.newState.withPriority(state.activePlayerId),
                    combinedEvents
                )
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

    companion object {
        fun create(context: ActionContext): SubmitDecisionHandler {
            return SubmitDecisionHandler(
                context.continuationHandler,
                context.turnManager,
                context.sbaChecker,
                context.triggerDetector,
                context.triggerProcessor
            )
        }
    }
}
