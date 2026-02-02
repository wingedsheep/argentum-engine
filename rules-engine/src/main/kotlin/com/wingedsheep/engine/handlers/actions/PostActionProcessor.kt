package com.wingedsheep.engine.handlers.actions

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Helper class that encapsulates common post-action processing patterns.
 *
 * After many actions complete, the engine needs to:
 * 1. Detect any triggered abilities
 * 2. Process those triggers (may pause for target selection)
 * 3. Check state-based actions
 * 4. Set priority appropriately
 *
 * This class extracts that repeated pattern for reuse across handlers.
 */
class PostActionProcessor(
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
    private val sbaChecker: StateBasedActionChecker
) {
    /**
     * Process triggers after an action and return the result.
     *
     * @param state The current game state after the action
     * @param events Events that occurred during the action
     * @param activePlayerId The player who should get priority after processing
     * @return ExecutionResult with triggers processed
     */
    fun processTriggersAndReturnPriority(
        state: GameState,
        events: List<GameEvent>,
        activePlayerId: EntityId
    ): ExecutionResult {
        val triggers = triggerDetector.detectTriggers(state, events)
        if (triggers.isEmpty()) {
            return ExecutionResult.success(state, events)
        }

        val triggerResult = triggerProcessor.processTriggers(state, triggers)

        if (triggerResult.isPaused) {
            return ExecutionResult.paused(
                triggerResult.state,
                triggerResult.pendingDecision!!,
                events + triggerResult.events
            )
        }

        return ExecutionResult.success(
            triggerResult.newState.withPriority(activePlayerId),
            events + triggerResult.events
        )
    }

    /**
     * Process triggers without setting priority (for actions that don't change priority).
     *
     * @param state The current game state after the action
     * @param events Events that occurred during the action
     * @return ExecutionResult with triggers processed
     */
    fun processTriggersOnly(
        state: GameState,
        events: List<GameEvent>
    ): ExecutionResult {
        val triggers = triggerDetector.detectTriggers(state, events)
        if (triggers.isEmpty()) {
            return ExecutionResult.success(state, events)
        }

        val triggerResult = triggerProcessor.processTriggers(state, triggers)

        if (triggerResult.isPaused) {
            return ExecutionResult.paused(
                triggerResult.state,
                triggerResult.pendingDecision!!,
                events + triggerResult.events
            )
        }

        return ExecutionResult.success(
            triggerResult.newState,
            events + triggerResult.events
        )
    }

    /**
     * Check state-based actions and process any resulting triggers.
     *
     * @param state The current game state
     * @param events Events that occurred before SBA check
     * @param activePlayerId The player who should get priority after processing
     * @return ExecutionResult with SBAs applied and triggers processed
     */
    fun checkSbasAndTriggers(
        state: GameState,
        events: List<GameEvent>,
        activePlayerId: EntityId
    ): ExecutionResult {
        val sbaResult = sbaChecker.checkAndApply(state)
        val combinedEvents = events + sbaResult.events

        // If game is over, don't give priority
        if (sbaResult.newState.gameOver) {
            return ExecutionResult.success(sbaResult.newState, combinedEvents)
        }

        // Detect and process triggers from the SBA events
        val triggers = triggerDetector.detectTriggers(sbaResult.newState, combinedEvents)
        if (triggers.isEmpty()) {
            return ExecutionResult.success(
                sbaResult.newState.withPriority(activePlayerId),
                combinedEvents
            )
        }

        val triggerResult = triggerProcessor.processTriggers(sbaResult.newState, triggers)

        if (triggerResult.isPaused) {
            return ExecutionResult.paused(
                triggerResult.state,
                triggerResult.pendingDecision!!,
                combinedEvents + triggerResult.events
            )
        }

        return ExecutionResult.success(
            triggerResult.newState.withPriority(activePlayerId),
            combinedEvents + triggerResult.events
        )
    }

    /**
     * Full post-resolution processing: SBAs + triggers + priority.
     * This is the pattern used after resolving a spell or ability.
     *
     * @param result The initial execution result from resolution
     * @param activePlayerId The player who should get priority
     * @return ExecutionResult with full post-processing applied
     */
    fun postResolutionProcessing(
        result: ExecutionResult,
        activePlayerId: EntityId
    ): ExecutionResult {
        if (!result.isSuccess) {
            return result
        }

        return checkSbasAndTriggers(result.newState, result.events, activePlayerId)
    }

    companion object {
        fun create(context: ActionContext): PostActionProcessor {
            return PostActionProcessor(
                context.triggerDetector,
                context.triggerProcessor,
                context.sbaChecker
            )
        }
    }
}
