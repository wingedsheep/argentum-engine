package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TriggeredAbilityFiredEverComponent
import com.wingedsheep.engine.state.components.battlefield.TriggeredAbilityFiredThisTurnComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.abilityIdentityOf
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.dsl.LibraryPatterns
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.engine.handlers.effects.composite.asMayDecide
import com.wingedsheep.engine.handlers.effects.composite.asOptionalManaPayment
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.StoreNumberEffect
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

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
    private val cardRegistry: CardRegistry,
    private val stackResolver: StackResolver,
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
        // Rule 704.6 / 800.4a: once the game has ended (or a player has left), triggered
        // abilities don't resolve. In particular, dies/leaves-battlefield triggers from a
        // creature whose controller just lost must not pause the game asking that player
        // to choose targets — the ActionProcessor would refuse the resulting decision
        // (state.gameOver is true) and the session would deadlock.
        if (state.gameOver) {
            return ExecutionResult.success(state)
        }
        val liveTriggers = triggers.filterNot { trigger ->
            state.getEntity(trigger.controllerId)?.has<PlayerLostComponent>() == true
        }
        if (liveTriggers.isEmpty()) {
            return ExecutionResult.success(state)
        }

        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        var index = 0
        while (index < liveTriggers.size) {
            // Batch the may-question for a run of structurally identical optional ("you may …
            // target …") triggers (MTGO's auto-stack-identical-triggers affordance). A run of ≥ 2
            // is answered once with a BatchYesNoDecision instead of one yes/no per trigger; the
            // remainder of the list resumes (and re-batches) after the answer.
            val run = batchRunAt(currentState, liveTriggers, index)
            if (run != null) {
                val remainingTriggers = liveTriggers.drop(index + run.size)
                return raiseBatchMayDecision(currentState, run, remainingTriggers, allEvents)
            }

            val trigger = liveTriggers[index]
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
                val remainingTriggers = liveTriggers.drop(index + 1)

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
            index++
        }

        return ExecutionResult.success(currentState, allEvents)
    }

    /**
     * Stable key for "the same batchable may-question": same controller + same definition-scoped
     * [com.wingedsheep.sdk.scripting.AbilityIdentity]. Two such triggers share an identical "you may
     * …" prompt (the prompt is the ability's static effect description, identical per identity), so
     * one answer can cover both.
     */
    private data class BatchKey(
        val controllerId: EntityId,
        val abilityIdentity: com.wingedsheep.sdk.scripting.AbilityIdentity,
    )

    /**
     * The [BatchKey] for a trigger that would raise a put-on-stack may-question, or null if it is
     * not batchable. A trigger is batchable iff it is an optional ("may") trigger that *also* targets
     * (so the may-question is asked at put-on-stack time, the only point all simultaneous instances
     * are in hand before priority — see backlog §B.4), it has a definition-scoped ability identity
     * (synthesized sources like spell copies have none and are never grouped), and it would actually
     * raise the question rather than fizzle for lack of legal targets.
     */
    private fun batchKeyOf(state: GameState, trigger: PendingTrigger): BatchKey? {
        val ability = trigger.ability
        val targetRequirement = ability.targetRequirement ?: return null
        if (ability.effect.asMayDecide() == null) return null
        val identity = state.abilityIdentityOf(trigger.sourceId, ability.id) ?: return null
        // Mirror processMayThenTargetTrigger's fizzle guard: a trigger with no legal targets (for a
        // mandatory-target requirement) fizzles without asking, so it must not join a batch.
        val legalTargets = targetFinder.findLegalTargets(
            state = state,
            requirement = targetRequirement,
            controllerId = trigger.controllerId,
            sourceId = trigger.sourceId,
            triggeringEntityId = trigger.triggerContext.triggeringEntityId
        )
        if (legalTargets.isEmpty() && targetRequirement.effectiveMinCount > 0) return null
        return BatchKey(trigger.controllerId, identity)
    }

    /**
     * The maximal contiguous run of batchable triggers starting at [index] that all share one
     * [BatchKey], or null if fewer than two such triggers start there. Contiguous-only (matching the
     * stack's LIFO order); a later identical run is re-batched when the remainder resumes.
     */
    private fun batchRunAt(
        state: GameState,
        triggers: List<PendingTrigger>,
        index: Int
    ): List<PendingTrigger>? {
        val key = batchKeyOf(state, triggers[index]) ?: return null
        var end = index + 1
        while (end < triggers.size && batchKeyOf(state, triggers[end]) == key) {
            end++
        }
        return if (end - index >= 2) triggers.subList(index, end).toList() else null
    }

    /**
     * Raise one [BatchYesNoDecision] for a [run] of identical optional triggers, queueing
     * [remainingTriggers] (the triggers after the run) beneath it so they resume in order once the
     * batch is answered. The [BatchMayTriggerContinuation] carries the whole run; the resumer fans
     * the single answer back out (see [BatchMayTriggerContinuation]).
     */
    private fun raiseBatchMayDecision(
        state: GameState,
        run: List<PendingTrigger>,
        remainingTriggers: List<PendingTrigger>,
        priorEvents: List<GameEvent>
    ): ExecutionResult {
        val first = run.first()
        val ability = first.ability
        val decisionId = "batch-may-${java.util.UUID.randomUUID()}"
        val decision = BatchYesNoDecision(
            id = decisionId,
            playerId = first.controllerId,
            prompt = ability.effect.description,
            context = DecisionContext(
                sourceId = first.sourceId,
                sourceName = first.sourceName,
                phase = DecisionPhase.RESOLUTION,
                abilityIdentity = state.abilityIdentityOf(first.sourceId, ability.id)
            ),
            count = run.size
        )

        // Queue the triggers after the run first (deepest), then the batch frame on top, so the
        // batch resolves before the trailing triggers (APNAP order preserved).
        var stateWithContinuations = state.withPendingDecision(decision)
        if (remainingTriggers.isNotEmpty()) {
            stateWithContinuations = stateWithContinuations.pushContinuation(
                PendingTriggersContinuation(
                    decisionId = "pending-triggers-${java.util.UUID.randomUUID()}",
                    remainingTriggers = remainingTriggers
                )
            )
        }
        stateWithContinuations = stateWithContinuations.pushContinuation(
            BatchMayTriggerContinuation(decisionId = decisionId, triggers = run)
        )

        return ExecutionResult.paused(
            stateWithContinuations,
            decision,
            priorEvents + DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = first.controllerId,
                decisionType = "BATCH_YES_NO",
                prompt = decision.prompt
            )
        )
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
        var currentState = state

        // One-shot event-based delayed triggers ("when you next … this turn") are consumed
        // the moment they fire — remove the delayed trigger from state so a later matching
        // event the same turn doesn't fire it again.
        trigger.consumesDelayedTriggerId?.let { delayedId ->
            currentState = currentState.removeDelayedTriggers(setOf(delayedId))
        }

        // Mark once-per-turn triggers as fired so they don't trigger again this turn
        if (ability.oncePerTurn) {
            currentState = markTriggerFired(currentState, trigger.sourceId, ability.id)
        }
        // Mark "triggers only once" abilities as fired so they never trigger again while the
        // source stays on the battlefield (tracker persists across turns, unlike oncePerTurn).
        if (ability.triggersOnce) {
            currentState = markTriggerFiredEver(currentState, trigger.sourceId, ability.id)
        }

        val targetRequirement = ability.targetRequirement

        // If the effect is a MayPayManaEffect AND has targets, ask payment first, then targets.
        // This reverses the old flow where targets were chosen before the pay question.
        if (targetRequirement != null && ability.effect.asOptionalManaPayment() != null) {
            return processMayPayManaThenTargetTrigger(currentState, trigger, targetRequirement)
        }

        // If the effect is a bare "may" (lowered MayEffect) AND has targets, ask may first before
        // target selection. This gives the player a chance to decline before having to pick targets.
        if (targetRequirement != null && ability.effect.asMayDecide() != null) {
            return processMayThenTargetTrigger(currentState, trigger, targetRequirement)
        }

        // Check if this ability requires targets
        if (targetRequirement != null) {
            return processTargetedTrigger(currentState, trigger, targetRequirement)
        }

        // A no-target "you may X. If you don't, Y" (optional + elseEffect) has no target-selection
        // step to carry the decline through, so the targeted path's may/else handling never runs and
        // both fields were silently dropped (the latent bug that made Yawgmoth Demon do nothing).
        // Lower it into the unified GatedEffect(Gate.MayDecide) frame so GatedEffectExecutor owns the
        // resolution-time yes/no and runs `elseEffect` on decline. Feasibility derived from the
        // may-action lets an impossible "may" (e.g. "you may sacrifice an artifact" with no artifact)
        // fall straight to the else with no prompt — the no-target analogue of "no legal targets → else".
        val elseEffect = ability.elseEffect
        if (ability.optional && elseEffect != null) {
            val gated = GatedEffect(
                gate = Gate.MayDecide(feasibility = impliedMayFeasibility(ability.effect)),
                then = ability.effect,
                otherwise = elseEffect
            )
            return putTriggerOnStack(currentState, trigger, emptyList(), gated)
        }

        // No targets required - put directly on stack
        return putTriggerOnStack(currentState, trigger, emptyList())
    }

    /**
     * The feasibility a no-target "may" action implies, so a "you may [action]. If you don't, …"
     * trigger skips the prompt and runs its else branch when the action is impossible (the player
     * can't, so they "don't"). A [SacrificeEffect] is always controller-self and needs the
     * controller to control enough matching permanents; other actions (draw, gain life, add a
     * counter) are always feasible (`null` → always prompt). Extend as further
     * impossible-when-empty may-actions appear.
     */
    private fun impliedMayFeasibility(effect: Effect): FeasibilityCheck? = when (effect) {
        is SacrificeEffect -> FeasibilityCheck.ControlsPermanentMatching(effect.filter, effect.count)
        else -> null
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
            sourceId = trigger.sourceId,
            triggeringEntityId = trigger.triggerContext.triggeringEntityId
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

        // The gated "may" effect's own text is the prompt (GatedEffect.description renders
        // "You may …" for a Gate.MayDecide).
        val sourceName = trigger.sourceName
        val abilityIdentity = state.abilityIdentityOf(trigger.sourceId, ability.id)

        // Persistent auto-answer yield (backlog §C): a remembered yes/no for this ability resolves
        // the may-question without prompting. "Yes" still proceeds to per-instance target selection
        // (only the yes/no is batched, never the targeting — §C.6); "no" skips the trigger.
        abilityIdentity?.let { state.autoAnswerFor(trigger.controllerId, it) }?.let { auto ->
            val note = AbilityAutoAnsweredEvent(trigger.sourceId, sourceName, trigger.controllerId, auto)
            if (!auto) return ExecutionResult.success(state, listOf(note))
            val innerEffect = ability.effect.asMayDecide()!!.then
            val unwrappedTrigger = trigger.copy(ability = ability.copy(effect = innerEffect))
            val result = processTargetedTrigger(state, unwrappedTrigger, targetRequirement)
            return result.copy(events = listOf(note) + result.events)
        }

        // Create yes/no decision
        val decisionResult = decisionHandler.createYesNoDecision(
            state = state,
            playerId = trigger.controllerId,
            sourceId = trigger.sourceId,
            sourceName = sourceName,
            prompt = ability.effect.description,
            phase = DecisionPhase.RESOLUTION,
            abilityIdentity = abilityIdentity
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
        val manaCost = ability.effect.asOptionalManaPayment()!!.cost

        // Check if the player can pay the mana cost
        val manaSolver = ManaSolver(cardRegistry)
        if (!manaSolver.canPay(state, trigger.controllerId, manaCost)) {
            // Can't pay - skip silently
            return ExecutionResult.success(state)
        }

        // Check if legal targets exist before asking the pay question
        val legalTargets = targetFinder.findLegalTargets(
            state = state,
            requirement = targetRequirement,
            controllerId = trigger.controllerId,
            sourceId = trigger.sourceId,
            triggeringEntityId = trigger.triggerContext.triggeringEntityId
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
            phase = DecisionPhase.RESOLUTION,
            abilityIdentity = state.abilityIdentityOf(trigger.sourceId, ability.id)
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
        // Snapshot dynamicMaxCount on each requirement now (when the trigger is going on
        // the stack) so the resolved cap is what the player sees and the validator
        // enforces. CR 603.3c: X / target counts on triggered abilities are locked when
        // the ability triggers. Only TargetObject carries dynamicMaxCount today.
        val allRequirements = ability.allTargetRequirements.map { snapshotDynamicCount(state, trigger, it) }

        // Find legal targets for each requirement
        val allLegalTargets = mutableMapOf<Int, List<EntityId>>()
        for ((index, req) in allRequirements.withIndex()) {
            val legalTargets = targetFinder.findLegalTargets(
                state = state,
                requirement = req,
                controllerId = trigger.controllerId,
                sourceId = trigger.sourceId,
                triggeringEntityId = trigger.triggerContext.triggeringEntityId,
                // Carry the triggering player so "target … that player controls" filters
                // (ControllerPredicate.ControlledByReferencedPlayer over Player.TriggeringPlayer)
                // resolve at legality time — Fear of Burning Alive's delirium payoff.
                pipelineContext = com.wingedsheep.engine.handlers.PredicateContext(
                    controllerId = trigger.controllerId,
                    triggeringEntityId = trigger.triggerContext.triggeringEntityId,
                    triggeringPlayerId = trigger.triggerContext.triggeringPlayerId,
                ),
            )
            allLegalTargets[index] = legalTargets
        }

        // If no legal targets exist for any required requirement, the ability is not put on the stack
        // (Rule 603.3d). This applies regardless of whether the ability is optional ("you may").
        for ((index, req) in allRequirements.withIndex()) {
            val legalTargets = allLegalTargets[index] ?: emptyList()
            if (legalTargets.isEmpty() && req.effectiveMinCount > 0) {
                if (ability.elseEffect != null) {
                    return putTriggerOnStack(state, trigger, emptyList(), ability.elseEffect)
                }
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
        }

        // Auto-select player targets when there's exactly one legal target and requirement is for exactly one target.
        // Only applies for single-target abilities (not multi-target).
        if (allRequirements.size == 1) {
            val isPlayerTarget = targetRequirement is com.wingedsheep.sdk.scripting.targets.TargetPlayer ||
                                 targetRequirement is com.wingedsheep.sdk.scripting.targets.TargetOpponent
            val legalTargets = allLegalTargets[0] ?: emptyList()
            if (isPlayerTarget && legalTargets.size == 1 && targetRequirement.effectiveMinCount == 1 && targetRequirement.count == 1) {
                val autoSelectedTarget = legalTargets.first()
                val chosenTarget = createChosenTarget(state, autoSelectedTarget)
                return putTriggerOnStack(state, trigger, listOf(chosenTarget))
            }
        }

        // Create target requirement infos for the decision
        // If the ability is optional (e.g., "you may"), allow selecting 0 targets to decline
        val requirementInfos = allRequirements.mapIndexed { index, req ->
            val effectiveMinTargets = if (ability.optional) 0 else req.effectiveMinCount
            // "Any number of target ..." (unlimited) caps at however many legal targets exist,
            // mirroring the cast-time path (TargetEnumerationUtils). Using req.count (always 1
            // for an unlimited requirement) would wrongly clamp the decision to a single target.
            val maxTargets = if (req.unlimited) (allLegalTargets[index]?.size ?: 0) else req.count
            TargetRequirementInfo(
                index = index,
                description = req.description,
                minTargets = effectiveMinTargets,
                maxTargets = maxTargets,
                sameOwner = (req as? com.wingedsheep.sdk.scripting.targets.TargetObject)?.sameOwner == true
            )
        }

        // Create the target selection decision. The effect description becomes the
        // prominent hint banner so the player knows *what* they're targeting for
        // (e.g., "Put 1 -1/-1 counter on target creature") rather than just
        // seeing the generic "Choose target" label.
        // Resolve dynamic amounts so the player sees concrete values
        // (e.g., Gloom Ripper showing "+3/+0" instead of "+X/+0").
        val effectHint = try {
            val evaluator = DynamicAmountEvaluator()
            val context = EffectContext(
                sourceId = trigger.sourceId,
                controllerId = trigger.controllerId,
                triggeringEntityId = trigger.triggerContext.triggeringEntityId,
                triggeringPlayerId = trigger.triggerContext.triggeringPlayerId,
                triggerDamageAmount = trigger.triggerContext.damageAmount,
                triggerCounterCount = trigger.triggerContext.counterCount,
                triggerTotalCounterCount = trigger.triggerContext.totalCounterCount,
                triggerLastKnownCounters = trigger.triggerContext.lastKnownCounters,
                triggerLastKnownDamageDealtByPlayers =
                    trigger.triggerContext.lastKnownDamageDealtByPlayers,
                triggerLastKnownBlockingOrBlockedByIds =
                    trigger.triggerContext.lastKnownBlockingOrBlockedByIds,
                triggerLastKnownPower = trigger.triggerContext.lastKnownPower,
                triggerLastKnownToughness = trigger.triggerContext.lastKnownToughness,
                triggerModesChosenCount = trigger.triggerContext.modesChosenCount,
                triggerScryCount = trigger.triggerContext.scryCount,
                triggerExcessDamageAmount = trigger.triggerContext.excessDamageAmount,
                triggerRecipientToughness = trigger.triggerContext.recipientToughnessAtDamage,
                triggerManaSpentOnTriggeringSpell = trigger.triggerContext.manaSpentOnTriggeringSpell,
                triggerColorsSpentOnTriggeringSpell = trigger.triggerContext.colorsSpentOnTriggeringSpell,
                triggerManaValueOfTriggeringSpell = trigger.triggerContext.manaValueOfTriggeringSpell,
                triggerXValueOfTriggeringSpell = trigger.triggerContext.xValueOfTriggeringSpell
            )
            ability.effect.runtimeDescription { amount -> evaluator.evaluate(state, amount, context) }
        } catch (_: Exception) {
            ability.effect.description
        }

        val decisionResult = decisionHandler.createTargetDecision(
            state = state,
            playerId = trigger.controllerId,
            sourceId = trigger.sourceId,
            sourceName = trigger.sourceName,
            requirements = requirementInfos,
            legalTargets = allLegalTargets,
            effectHint = effectHint
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
            abilityIdentity = state.abilityIdentityOf(trigger.sourceId, ability.id),
            triggerDamageAmount = trigger.triggerContext.damageAmount,
            triggeringEntityId = trigger.triggerContext.triggeringEntityId,
            triggeringPlayerId = trigger.triggerContext.triggeringPlayerId,
            elseEffect = ability.elseEffect,
            targetRequirements = allRequirements,
            triggerCounterCount = trigger.triggerContext.counterCount,
            triggerTotalCounterCount = trigger.triggerContext.totalCounterCount,
            triggerLastKnownCounters = trigger.triggerContext.lastKnownCounters,
            triggerLastKnownDamageDealtByPlayers =
                trigger.triggerContext.lastKnownDamageDealtByPlayers,
            triggerLastKnownBlockingOrBlockedByIds =
                trigger.triggerContext.lastKnownBlockingOrBlockedByIds,
            lastKnownPower = trigger.triggerContext.lastKnownPower,
            lastKnownToughness = trigger.triggerContext.lastKnownToughness,
            triggerModesChosenCount = trigger.triggerContext.modesChosenCount,
            enchantedCreatureLastKnownPower = trigger.triggerContext.enchantedCreatureLastKnownPower,
            triggerScryCount = trigger.triggerContext.scryCount,
            triggerExcessDamageAmount = trigger.triggerContext.excessDamageAmount,
            triggerRecipientToughness = trigger.triggerContext.recipientToughnessAtDamage,
            triggerManaSpentOnTriggeringSpell = trigger.triggerContext.manaSpentOnTriggeringSpell,
            triggerColorsSpentOnTriggeringSpell = trigger.triggerContext.colorsSpentOnTriggeringSpell,
            triggerManaValueOfTriggeringSpell = trigger.triggerContext.manaValueOfTriggeringSpell,
            triggerXValueOfTriggeringSpell = trigger.triggerContext.xValueOfTriggeringSpell
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
            abilityIdentity = state.abilityIdentityOf(trigger.sourceId, ability.id),
            descriptionOverride = ability.descriptionOverride,
            triggerDamageAmount = trigger.triggerContext.damageAmount,
            triggeringEntityId = trigger.triggerContext.triggeringEntityId,
            triggeringPlayerId = trigger.triggerContext.triggeringPlayerId,
            xValue = trigger.triggerContext.xValue ?: computeXForDisplay(state, trigger),
            triggerCounterCount = trigger.triggerContext.counterCount,
            triggerTotalCounterCount = trigger.triggerContext.totalCounterCount,
            triggerLastKnownCounters = trigger.triggerContext.lastKnownCounters,
            triggerLastKnownDamageDealtByPlayers =
                trigger.triggerContext.lastKnownDamageDealtByPlayers,
            triggerLastKnownBlockingOrBlockedByIds =
                trigger.triggerContext.lastKnownBlockingOrBlockedByIds,
            targetingSourceEntityId = trigger.triggerContext.targetingSourceEntityId,
            lastKnownPower = trigger.triggerContext.lastKnownPower,
            lastKnownToughness = trigger.triggerContext.lastKnownToughness,
            triggerModesChosenCount = trigger.triggerContext.modesChosenCount,
            enchantedCreatureLastKnownPower = trigger.triggerContext.enchantedCreatureLastKnownPower,
            triggerScryCount = trigger.triggerContext.scryCount,
            triggerExcessDamageAmount = trigger.triggerContext.excessDamageAmount,
            triggerRecipientToughness = trigger.triggerContext.recipientToughnessAtDamage,
            triggerManaSpentOnTriggeringSpell = trigger.triggerContext.manaSpentOnTriggeringSpell,
            triggerColorsSpentOnTriggeringSpell = trigger.triggerContext.colorsSpentOnTriggeringSpell,
            triggerManaValueOfTriggeringSpell = trigger.triggerContext.manaValueOfTriggeringSpell,
            triggerXValueOfTriggeringSpell = trigger.triggerContext.xValueOfTriggeringSpell,
            capturedEntityIds = trigger.triggerContext.capturedEntityIds ?: emptyList(),
            sagaChapterInfo = trigger.sagaChapterInfo
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
        triggerDetector: TriggerDetector
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

    /**
     * Compute the X value for display on the stack for triggered abilities that use a variable
     * selection count. Handles both `ChooseUpTo` (e.g., Prismatic Undercurrents — up to X basic
     * lands) and `ChooseExactly` (e.g., Taster of Wares — reveal X cards from hand). When the
     * count is a [DynamicAmount.VariableReference], looks up the matching [StoreNumberEffect]
     * in the same effect tree to obtain the actual amount.
     */
    private fun computeXForDisplay(state: GameState, trigger: PendingTrigger): Int? {
        val rawAmount = findSelectionAmount(trigger.ability.effect) ?: return null
        if (rawAmount is DynamicAmount.Fixed) return null
        val resolvedAmount = if (rawAmount is DynamicAmount.VariableReference) {
            findStoreNumberAmount(trigger.ability.effect, rawAmount.variableName) ?: return null
        } else rawAmount
        val context = EffectContext(
            sourceId = trigger.sourceId,
            controllerId = trigger.controllerId,
        )
        return DynamicAmountEvaluator().evaluate(state, resolvedAmount, context)
    }

    /**
     * Recursively walk an effect tree looking for the first SelectFromCollectionEffect's
     * selection count (either `ChooseUpTo` or `ChooseExactly`).
     */
    private fun findSelectionAmount(effect: Effect): DynamicAmount? = when (effect) {
        is SelectFromCollectionEffect -> when (val sel = effect.selection) {
            is SelectionMode.ChooseUpTo -> sel.count
            is SelectionMode.ChooseExactly -> sel.count
            else -> null
        }
        is CompositeEffect -> effect.effects.firstNotNullOfOrNull { findSelectionAmount(it) }
        // Library macros (scry/surveil) are opaque nodes — expand to their pipeline before walking.
        else -> LibraryPatterns.expandMacro(effect)?.let { findSelectionAmount(it) }
    }

    /**
     * Find the [StoreNumberEffect] in the effect tree with the given [name] and return its
     * amount, or null if no match.
     */
    private fun findStoreNumberAmount(effect: Effect, name: String): DynamicAmount? = when (effect) {
        is StoreNumberEffect -> if (effect.name == name) effect.amount else null
        is CompositeEffect -> effect.effects.firstNotNullOfOrNull { findStoreNumberAmount(it, name) }
        else -> LibraryPatterns.expandMacro(effect)?.let { findStoreNumberAmount(it, name) }
    }

    /**
     * If the requirement carries a [TargetObject.dynamicMaxCount], evaluate it against
     * the trigger's controller/source and return a copy with `count` rewritten to the
     * resolved value (and `minCount` clamped to the new cap). When `dynamicMaxCount`
     * is set, the resolved value is authoritative — the SDK's static `count` is only
     * the no-dynamic-cap default. [TargetOther] is unwrapped, snapshotted, and
     * re-wrapped so "another target" wording stays intact.
     */
    private fun snapshotDynamicCount(
        state: GameState,
        trigger: PendingTrigger,
        requirement: TargetRequirement
    ): TargetRequirement = when (requirement) {
        is TargetObject -> {
            val dyn = requirement.dynamicMaxCount
            if (dyn == null) {
                requirement
            } else {
                val resolved = try {
                    val context = EffectContext(
                        sourceId = trigger.sourceId,
                        controllerId = trigger.controllerId,
                        triggeringEntityId = trigger.triggerContext.triggeringEntityId,
                        triggeringPlayerId = trigger.triggerContext.triggeringPlayerId
                    )
                    DynamicAmountEvaluator().evaluate(state, dyn, context)
                } catch (_: Exception) {
                    requirement.count
                }
                val newMax = resolved.coerceAtLeast(0)
                requirement.copy(
                    count = newMax,
                    minCount = requirement.minCount.coerceAtMost(newMax)
                )
            }
        }
        is TargetOther -> {
            val newBase = snapshotDynamicCount(state, trigger, requirement.baseRequirement)
            if (newBase !== requirement.baseRequirement) requirement.copy(baseRequirement = newBase) else requirement
        }
        else -> requirement
    }

    /**
     * Mark a once-per-turn triggered ability as fired on its source entity.
     */
    private fun markTriggerFired(state: GameState, sourceId: EntityId, abilityId: AbilityId): GameState {
        val entity = state.getEntity(sourceId) ?: return state
        val tracker = entity.get<TriggeredAbilityFiredThisTurnComponent>()
            ?: TriggeredAbilityFiredThisTurnComponent()
        val updated = tracker.withFired(abilityId)
        return state.updateEntity(sourceId) { it.with(updated) }
    }

    /**
     * Mark a "triggers only once" triggered ability as fired on its source entity. The tracker
     * persists for the permanent's lifetime (not cleared at end of turn).
     */
    private fun markTriggerFiredEver(state: GameState, sourceId: EntityId, abilityId: AbilityId): GameState {
        val entity = state.getEntity(sourceId) ?: return state
        val tracker = entity.get<TriggeredAbilityFiredEverComponent>()
            ?: TriggeredAbilityFiredEverComponent()
        val updated = tracker.withFired(abilityId)
        return state.updateEntity(sourceId) { it.with(updated) }
    }
}
