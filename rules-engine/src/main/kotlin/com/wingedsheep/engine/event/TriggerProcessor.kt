package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.targeting.TargetRequirement

/**
 * Processes triggered abilities by putting them on the stack.
 *
 * When a triggered ability fires, it needs to be placed on the stack. However,
 * if the ability requires targets (like Fire Imp's "deal 1 damage to any target"),
 * we must first ask the player to choose targets before the ability can go on the stack.
 *
 * This processor handles both cases:
 * - Targetless abilities: Put directly on the stack
 * - Targeted abilities: Pause for target selection, then put on stack via continuation
 */
class TriggerProcessor(
    private val stackResolver: StackResolver = StackResolver(),
    private val targetFinder: TargetFinder = TargetFinder(),
    private val decisionHandler: DecisionHandler = DecisionHandler()
) {

    /**
     * Process a list of pending triggers, placing them on the stack.
     *
     * Triggers are processed in APNAP order (active player first, then others).
     * If a trigger requires targets, execution pauses for player input.
     *
     * @param state The current game state
     * @param triggers List of pending triggers in APNAP order
     * @return ExecutionResult - may be paused if a trigger requires targets
     */
    fun processTriggers(state: GameState, triggers: List<PendingTrigger>): ExecutionResult {
        if (triggers.isEmpty()) {
            return ExecutionResult.success(state)
        }

        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for ((index, trigger) in triggers.withIndex()) {
            val result = processSingleTrigger(currentState, trigger)

            if (!result.isSuccess && !result.isPaused) {
                // Error occurred - return it
                return ExecutionResult(
                    state = result.state,
                    events = allEvents + result.events,
                    error = result.error
                )
            }

            if (result.isPaused) {
                // This trigger requires target selection
                // Store the remaining triggers to process after the decision
                val remainingTriggers = triggers.drop(index + 1)

                // If there are more triggers to process, we need to chain them
                // For now, we handle one at a time and rely on the game loop
                // to call processTriggers again after the decision is resolved
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            currentState = result.newState
            allEvents.addAll(result.events)
        }

        return ExecutionResult.success(currentState, allEvents)
    }

    /**
     * Process a single triggered ability.
     *
     * @param state The current game state
     * @param trigger The pending trigger to process
     * @return ExecutionResult - may be paused if trigger requires targets
     */
    private fun processSingleTrigger(state: GameState, trigger: PendingTrigger): ExecutionResult {
        val ability = trigger.ability
        val targetRequirement = ability.targetRequirement

        // Check if this ability requires targets
        if (targetRequirement != null) {
            return processTargetedTrigger(state, trigger, targetRequirement)
        }

        // No targets required - put directly on stack
        return putTriggerOnStack(state, trigger, emptyList())
    }

    /**
     * Process a triggered ability that requires targets.
     *
     * Creates a target selection decision and continuation frame.
     */
    private fun processTargetedTrigger(
        state: GameState,
        trigger: PendingTrigger,
        targetRequirement: TargetRequirement
    ): ExecutionResult {
        val ability = trigger.ability

        // Find legal targets for this requirement
        val legalTargets = targetFinder.findLegalTargets(
            state = state,
            requirement = targetRequirement,
            controllerId = trigger.controllerId,
            sourceId = trigger.sourceId
        )

        // If no legal targets exist and targets are required (not optional),
        // the ability is removed from the stack without resolving (Rule 603.3d)
        if (legalTargets.isEmpty() && targetRequirement.effectiveMinCount > 0) {
            // No legal targets - ability doesn't go on stack
            return ExecutionResult.success(
                state,
                listOf(
                    AbilityFizzledEvent(
                        trigger.sourceId,
                        ability.description,
                        "No legal targets available"
                    )
                )
            )
        }

        // Create target requirement info for the decision
        val requirementInfo = TargetRequirementInfo(
            index = 0,
            description = targetRequirement.description,
            minTargets = targetRequirement.effectiveMinCount,
            maxTargets = targetRequirement.count
        )

        // Create the target selection decision
        val decisionResult = decisionHandler.createTargetDecision(
            state = state,
            playerId = trigger.controllerId,
            sourceId = trigger.sourceId,
            sourceName = trigger.sourceName,
            requirements = listOf(requirementInfo),
            legalTargets = mapOf(0 to legalTargets)
        )

        if (!decisionResult.isPaused || decisionResult.pendingDecision == null) {
            return ExecutionResult.error(state, "Failed to create target decision")
        }

        // Create continuation frame to remember this trigger
        val continuation = TriggeredAbilityContinuation(
            decisionId = decisionResult.pendingDecision.id,
            sourceId = trigger.sourceId,
            sourceName = trigger.sourceName,
            controllerId = trigger.controllerId,
            effect = ability.effect,
            description = ability.description
        )

        // Push the continuation onto the stack
        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events.toList()
        )
    }

    /**
     * Put a triggered ability directly on the stack (no targets required).
     */
    private fun putTriggerOnStack(
        state: GameState,
        trigger: PendingTrigger,
        targets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>
    ): ExecutionResult {
        val ability = trigger.ability

        val abilityComponent = TriggeredAbilityOnStackComponent(
            sourceId = trigger.sourceId,
            sourceName = trigger.sourceName,
            controllerId = trigger.controllerId,
            effect = ability.effect,
            description = ability.description
        )

        return stackResolver.putTriggeredAbility(state, abilityComponent, targets)
    }

    /**
     * Convenience method to detect and process triggers in one call.
     *
     * @param state The current game state
     * @param events The events that may have caused triggers
     * @param triggerDetector The detector to use for finding triggers
     * @return ExecutionResult with triggers placed on stack (or paused for target selection)
     */
    fun detectAndProcess(
        state: GameState,
        events: List<GameEvent>,
        triggerDetector: TriggerDetector = TriggerDetector()
    ): ExecutionResult {
        val triggers = triggerDetector.detectTriggers(state, events)
        return processTriggers(state, triggers)
    }
}
