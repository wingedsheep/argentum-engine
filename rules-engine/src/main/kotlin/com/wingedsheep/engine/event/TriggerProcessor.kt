package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Effect
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.MayPayManaEffect
import com.wingedsheep.sdk.targeting.TargetRequirement
import java.util.UUID

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

                // Push remaining triggers as a continuation so they are processed
                // after this trigger's target selection is resolved
                var stateWithContinuations = result.state
                if (remainingTriggers.isNotEmpty()) {
                    val pendingContinuation = PendingTriggersContinuation(
                        decisionId = "pending-triggers-${java.util.UUID.randomUUID()}",
                        remainingTriggers = remainingTriggers
                    )
                    // Push BELOW the TriggeredAbilityContinuation that was just pushed
                    // by inserting at the bottom of what was just added
                    val stack = stateWithContinuations.continuationStack
                    // The TriggeredAbilityContinuation is at the top; insert pending triggers below it
                    val newStack = stack.dropLast(1) + pendingContinuation + stack.last()
                    stateWithContinuations = stateWithContinuations.copy(continuationStack = newStack)
                }

                return ExecutionResult.paused(
                    stateWithContinuations,
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

        // If the effect is a MayPayManaEffect AND has targets, ask payment first, then targets.
        // This reverses the old flow where targets were chosen before the pay question.
        if (targetRequirement != null && ability.effect is MayPayManaEffect) {
            return processMayPayManaThenTargetTrigger(state, trigger, targetRequirement)
        }

        // If the effect is a MayEffect AND has targets, ask may first before target selection.
        // This gives the player a chance to decline before having to pick targets.
        if (targetRequirement != null && ability.effect is MayEffect) {
            return processMayThenTargetTrigger(state, trigger, targetRequirement)
        }

        // Check if this ability requires targets
        if (targetRequirement != null) {
            return processTargetedTrigger(state, trigger, targetRequirement)
        }

        // No targets required - put directly on stack
        return putTriggerOnStack(state, trigger, emptyList())
    }

    /**
     * Process a triggered ability that has both MayEffect and targets.
     *
     * Asks the player yes/no first. If they say yes, proceeds to target selection
     * via MayTriggerContinuation. If they say no, the trigger is skipped.
     *
     * Before asking, checks if legal targets exist — if not, the ability fizzles
     * without even asking the may question.
     */
    private fun processMayThenTargetTrigger(
        state: GameState,
        trigger: PendingTrigger,
        targetRequirement: TargetRequirement
    ): ExecutionResult {
        val ability = trigger.ability

        // Check if legal targets exist before asking the may question
        val legalTargets = targetFinder.findLegalTargets(
            state = state,
            requirement = targetRequirement,
            controllerId = trigger.controllerId,
            sourceId = trigger.sourceId
        )

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

        // Get the MayEffect description for the prompt
        val mayEffect = ability.effect as MayEffect
        val sourceName = trigger.sourceName

        // Create yes/no decision
        val decisionResult = decisionHandler.createYesNoDecision(
            state = state,
            playerId = trigger.controllerId,
            sourceId = trigger.sourceId,
            sourceName = sourceName,
            prompt = mayEffect.description,
            phase = DecisionPhase.RESOLUTION
        )

        if (!decisionResult.isPaused || decisionResult.pendingDecision == null) {
            return ExecutionResult.error(state, "Failed to create yes/no decision for may trigger")
        }

        // Create continuation to resume with target selection if player says yes
        val continuation = MayTriggerContinuation(
            decisionId = decisionResult.pendingDecision.id,
            trigger = trigger,
            targetRequirement = targetRequirement
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events.toList()
        )
    }

    /**
     * Process a triggered ability that has both MayPayManaEffect and targets.
     *
     * Asks "Pay {cost}?" first. If the player says yes, proceeds to mana source selection,
     * then target selection. If the player says no, the trigger is skipped entirely.
     * If the player can't pay, the trigger is skipped silently.
     *
     * Before asking, checks if legal targets exist — if not, the ability fizzles
     * without even asking the pay question.
     */
    private fun processMayPayManaThenTargetTrigger(
        state: GameState,
        trigger: PendingTrigger,
        targetRequirement: TargetRequirement
    ): ExecutionResult {
        val ability = trigger.ability
        val mayPayEffect = ability.effect as MayPayManaEffect
        val manaCost = mayPayEffect.cost

        // Check if the player can pay the mana cost
        val manaSolver = ManaSolver()
        if (!manaSolver.canPay(state, trigger.controllerId, manaCost)) {
            // Can't pay - skip silently
            return ExecutionResult.success(state)
        }

        // Check if legal targets exist before asking the pay question
        val legalTargets = targetFinder.findLegalTargets(
            state = state,
            requirement = targetRequirement,
            controllerId = trigger.controllerId,
            sourceId = trigger.sourceId
        )

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

        val sourceName = trigger.sourceName

        // Create yes/no decision: "Pay {cost}?"
        val decisionResult = decisionHandler.createYesNoDecision(
            state = state,
            playerId = trigger.controllerId,
            sourceId = trigger.sourceId,
            sourceName = sourceName,
            prompt = "Pay $manaCost?",
            yesText = "Pay $manaCost",
            noText = "Don't pay",
            phase = DecisionPhase.RESOLUTION
        )

        if (!decisionResult.isPaused || decisionResult.pendingDecision == null) {
            return ExecutionResult.error(state, "Failed to create yes/no decision for may pay mana trigger")
        }

        // Create continuation to resume with mana source selection if player says yes
        val continuation = MayPayManaTriggerContinuation(
            decisionId = decisionResult.pendingDecision.id,
            trigger = trigger,
            targetRequirement = targetRequirement,
            manaCost = manaCost
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events.toList()
        )
    }

    /**
     * Process a triggered ability that requires targets.
     *
     * Creates a target selection decision and continuation frame.
     * If there's exactly one legal target and the requirement is for exactly one target,
     * auto-selects that target without prompting the player.
     */
    internal fun processTargetedTrigger(
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
            // If the ability has an else effect (e.g., "If you don't, tap this creature"),
            // put it on the stack with the else effect instead of fizzling
            if (ability.elseEffect != null) {
                return putTriggerOnStack(state, trigger, emptyList(), ability.elseEffect)
            }
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

        // Auto-select player targets when there's exactly one legal target and requirement is for exactly one target.
        // This applies to TargetPlayer and TargetOpponent - in a 2-player game with TargetOpponent,
        // there's always exactly one choice so we skip the prompt for better UX.
        // We don't auto-select for creature/permanent targets because players may want to decline optional
        // abilities or need to confirm their targeting choice.
        val isPlayerTarget = targetRequirement is com.wingedsheep.sdk.targeting.TargetPlayer ||
                             targetRequirement is com.wingedsheep.sdk.targeting.TargetOpponent
        if (isPlayerTarget && legalTargets.size == 1 && targetRequirement.effectiveMinCount == 1 && targetRequirement.count == 1) {
            val autoSelectedTarget = legalTargets.first()
            val chosenTarget = createChosenTarget(state, autoSelectedTarget)
            return putTriggerOnStack(state, trigger, listOf(chosenTarget))
        }

        // Create target requirement info for the decision
        // If the ability is optional (e.g., "you may"), allow selecting 0 targets to decline
        val effectiveMinTargets = if (ability.optional) 0 else targetRequirement.effectiveMinCount
        val requirementInfo = TargetRequirementInfo(
            index = 0,
            description = targetRequirement.description,
            minTargets = effectiveMinTargets,
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
            description = ability.description,
            triggerDamageAmount = trigger.triggerContext.damageAmount,
            triggeringEntityId = trigger.triggerContext.triggeringEntityId,
            elseEffect = ability.elseEffect,
            targetRequirements = listOf(targetRequirement)
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
     *
     * @param effectOverride If provided, use this effect instead of the ability's main effect.
     *                       Used when the ability's else branch should execute (e.g., player
     *                       can't or didn't choose targets for an optional ability).
     */
    private fun putTriggerOnStack(
        state: GameState,
        trigger: PendingTrigger,
        targets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>,
        effectOverride: Effect? = null
    ): ExecutionResult {
        val ability = trigger.ability

        val abilityComponent = TriggeredAbilityOnStackComponent(
            sourceId = trigger.sourceId,
            sourceName = trigger.sourceName,
            controllerId = trigger.controllerId,
            effect = effectOverride ?: ability.effect,
            description = ability.description,
            triggerDamageAmount = trigger.triggerContext.damageAmount,
            triggeringEntityId = trigger.triggerContext.triggeringEntityId
        )

        return stackResolver.putTriggeredAbility(
            state, abilityComponent, targets,
            targetRequirements = listOfNotNull(ability.targetRequirement)
        )
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

    /**
     * Create a ChosenTarget from an EntityId based on what the entity is in the game state.
     *
     * @param state The current game state
     * @param targetId The entity ID of the target
     * @return The appropriate ChosenTarget type
     */
    private fun createChosenTarget(
        state: GameState,
        targetId: EntityId
    ): com.wingedsheep.engine.state.components.stack.ChosenTarget {
        // Check if it's a player
        if (state.turnOrder.contains(targetId)) {
            return com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(targetId)
        }

        // Check if it's on the battlefield (permanent)
        if (state.getBattlefield().contains(targetId)) {
            return com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(targetId)
        }

        // Check if it's on the stack (spell)
        if (state.stack.contains(targetId)) {
            return com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell(targetId)
        }

        // Otherwise, assume it's a card in a zone (graveyard, etc.)
        // Find which zone it's in
        for (playerId in state.turnOrder) {
            for (zoneType in listOf(
                com.wingedsheep.sdk.core.Zone.GRAVEYARD,
                com.wingedsheep.sdk.core.Zone.HAND,
                com.wingedsheep.sdk.core.Zone.LIBRARY,
                com.wingedsheep.sdk.core.Zone.EXILE
            )) {
                val zoneKey = com.wingedsheep.engine.state.ZoneKey(playerId, zoneType)
                if (state.getZone(zoneKey).contains(targetId)) {
                    return com.wingedsheep.engine.state.components.stack.ChosenTarget.Card(
                        cardId = targetId,
                        ownerId = playerId,
                        zone = zoneType
                    )
                }
            }
        }

        // Fallback to permanent (shouldn't happen if the target is valid)
        return com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(targetId)
    }
}
