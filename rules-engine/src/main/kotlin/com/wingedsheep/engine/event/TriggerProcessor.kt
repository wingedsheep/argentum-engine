package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.modal.ChosenModeMemory
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TriggeredAbilityEffectAppliedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TriggeredAbilityFiredEverComponent
import com.wingedsheep.engine.state.components.battlefield.TriggeredAbilityFiredThisTurnComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.abilityIdentityOf
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.sdk.dsl.LibraryPatterns
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.isConsentGate
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor
import com.wingedsheep.engine.handlers.effects.composite.asMayDecide
import com.wingedsheep.engine.handlers.effects.composite.asOptionalManaPayment
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.StoreNumberEffect
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.scripting.targets.TargetChooser
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
        // An `effectOncePerTurn` ability is never batched: its whole point is picking *which* of the
        // simultaneous instances gets the turn's single action (which damaged creature's number to
        // mirror, which Villain connives). One shared yes/no would answer for all of them and take
        // that choice away. It also never reaches the put-on-stack may-question at all — the
        // lowering in `withEffectBudgetGate` moves consent to resolution time — but this guard reads
        // the *un-lowered* ability, so it is still load-bearing.
        if (ability.effectOncePerTurn) return null
        val identity = state.abilityIdentityOf(trigger.sourceId, ability.id) ?: return null
        // Mirror processMayThenTargetTrigger's fizzle guard: a trigger with no legal targets (for a
        // mandatory-target requirement) fizzles without asking, so it must not join a batch.
        val legalTargets = targetFinder.findLegalTargets(
            state = state,
            requirement = targetRequirement,
            controllerId = trigger.controllerId,
            sourceId = trigger.sourceId,
            triggeringEntityId = trigger.triggerContext.triggeringEntityId,
            // Carry the triggering player so a "target … that player controls" filter
            // (ControllerPredicate.ControlledByTriggeringPlayer / ControlledByReferencedPlayer over
            // Player.TriggeringPlayer) resolves identically here to the on-stack targeting path — a
            // trigger whose associated player rides on triggeringPlayerId must reach the same
            // legal-target verdict in this pre-check, or the may/pay question is wrongly skipped.
            pipelineContext = com.wingedsheep.engine.handlers.PredicateContext(
                controllerId = trigger.controllerId,
                triggeringEntityId = trigger.triggerContext.triggeringEntityId,
                triggeringPlayerId = trigger.triggerContext.triggeringPlayerId,
                // The X carried by the triggering event (an {X} cycling cost, a megamorph turn-up)
                // so an X-relative target filter — `manaValueEqualsX()` on Webstrike Elite's
                // "artifact or enchantment with mana value X" — finds targets at legality time.
                // Without it those predicates read an unbound X and match nothing.
                xValue = trigger.triggerContext.xValue,
                storedCollections = trigger.carriedPipeline?.storedCollections ?: emptyMap(),
                chosenValues = trigger.carriedPipeline?.chosenValues ?: emptyMap(),
                storedStringLists = trigger.carriedPipeline?.storedStringLists ?: emptyMap(),
                storedSubtypeGroups = trigger.carriedPipeline?.storedSubtypeGroups ?: emptyMap(),
            )
        )
        if (legalTargets.isEmpty() && targetRequirement.effectiveMinCount > 0) return null
        // Keyed on who is *asked*, not who controls the ability. A card whose "may" names someone
        // else (Farrel's Mantle's "its controller may") can produce two triggers with the same
        // controller and the same identity while the question belongs to two different players —
        // batching those would fan one player's answer onto the other's decision.
        return BatchKey(askedPlayerFor(state, trigger), identity)
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
            // Same player the BatchKey was built on, so the auto-answer store is keyed identically
            // on the batched and single paths.
            playerId = askedPlayerFor(state, first),
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
     * @param incomingTrigger The pending trigger to process, before any `effectOncePerTurn` lowering
     * @return ExecutionResult - may be paused if trigger requires targets
     */
    private fun processSingleTrigger(state: GameState, incomingTrigger: PendingTrigger): ExecutionResult {
        // "Do this only once each turn" (`effectOncePerTurn`). CR 603.2h: the ability "triggers only
        // if its source's controller has not yet taken the indicated action that turn". Once the
        // action has been taken this turn the ability simply does not trigger, so this instance is
        // dropped — silently, with no event: nothing went on the stack and nothing fizzled, and a
        // phantom ability in the log would be a lie. (Contrast `oncePerTurn`, the *trigger* cap,
        // which is spent by the first trigger whether or not the action happened.)
        //
        // Deliberately ahead of the `consumesDelayedTriggerId` removal and the `triggersOnce` mark
        // below: an ability that never triggers must not consume its one-shot delayed trigger nor
        // burn its lifetime fire. No shipped card combines those flags with this one.
        if (incomingTrigger.ability.effectOncePerTurn &&
            effectBudgetSpent(state, incomingTrigger.sourceId, incomingTrigger.ability.id)
        ) {
            return ExecutionResult.success(state, emptyList())
        }
        val trigger = if (incomingTrigger.ability.effectOncePerTurn) {
            incomingTrigger.copy(ability = withEffectBudgetGate(incomingTrigger.ability))
        } else {
            incomingTrigger
        }
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

        // No targets required — put directly on stack, with one derivation applied on the way.
        return putTriggerOnStack(
            currentState,
            trigger,
            emptyList(),
            ability.effect.withImpliedMayFeasibility()
        )
    }

    /**
     * Stamp the feasibility a no-target "you may [action]" implies onto its consent gate, so
     * "you may … If you don't, …" skips the prompt and runs its else branch when the action is
     * impossible — the player can't, so they "don't". The no-target analogue of
     * "no legal targets → else".
     *
     * **Derived here rather than stored on the card**, and that is the point. Nothing in the printed
     * text says it: "you may sacrifice an artifact" and "you may draw a card" are the same sentence
     * shape, and which one is unanswerable follows from the *effect*, not from the wording. A card
     * that spelled it would be recording a fact it does not know, and a second card written the
     * other way would then mean something different by accident. Only a gate with no feasibility of
     * its own is touched — a card that states one (Provisions Merchant) is saying something the
     * effect cannot imply, and it wins.
     *
     * A [SacrificeEffect] is always controller-self and needs the controller to control enough
     * matching permanents; other actions (draw, gain life, add a counter) are always feasible
     * (`null` → always prompt). Extend as further impossible-when-empty may-actions appear.
     */
    private fun Effect.withImpliedMayFeasibility(): Effect {
        val gated = this as? GatedEffect ?: return this
        val gate = gated.gate as? Gate.MayDecide ?: return this
        if (gate.feasibility != null) return this
        val implied = when (val action = gated.then) {
            is SacrificeEffect -> FeasibilityCheck.ControlsPermanentMatching(action.filter, action.count)
            else -> return this
        }
        return gated.copy(gate = gate.copy(feasibility = implied))
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
    /**
     * The player who answers a "you may" on a triggered ability: the ability's `decisionMaker` when
     * it names one, else its controller.
     *
     * Routed through the shared [TargetResolutionUtils.resolvePlayerTarget] rather than a local
     * `when`, so every [EffectTarget] player shape it already understands works here too and cannot
     * drift from the resolution-time answer [GatedEffectExecutor] gives. Anything it cannot resolve
     * falls back to the controller — what every card without a `decisionMaker` already gets.
     */
    private fun askedPlayerFor(state: GameState, trigger: PendingTrigger): EntityId {
        val chooser = trigger.ability.effect.asMayDecide()?.decisionMaker ?: return trigger.controllerId
        val context = EffectContext(
            sourceId = trigger.sourceId,
            controllerId = trigger.controllerId,
            triggeringEntityId = trigger.triggerContext.triggeringEntityId,
            triggeringPlayerId = trigger.triggerContext.triggeringPlayerId,
        )
        return TargetResolutionUtils.resolvePlayerTarget(chooser, context, state) ?: trigger.controllerId
    }

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
            triggeringEntityId = trigger.triggerContext.triggeringEntityId,
            // Carry the triggering player so a "target … that player controls" filter
            // (ControllerPredicate.ControlledByTriggeringPlayer / ControlledByReferencedPlayer over
            // Player.TriggeringPlayer) resolves identically here to the on-stack targeting path — a
            // trigger whose associated player rides on triggeringPlayerId must reach the same
            // legal-target verdict in this pre-check, or the may/pay question is wrongly skipped.
            pipelineContext = com.wingedsheep.engine.handlers.PredicateContext(
                controllerId = trigger.controllerId,
                triggeringEntityId = trigger.triggerContext.triggeringEntityId,
                triggeringPlayerId = trigger.triggerContext.triggeringPlayerId,
                // The X carried by the triggering event (an {X} cycling cost, a megamorph turn-up)
                // so an X-relative target filter — `manaValueEqualsX()` on Webstrike Elite's
                // "artifact or enchantment with mana value X" — finds targets at legality time.
                // Without it those predicates read an unbound X and match nothing.
                xValue = trigger.triggerContext.xValue,
                storedCollections = trigger.carriedPipeline?.storedCollections ?: emptyMap(),
                chosenValues = trigger.carriedPipeline?.chosenValues ?: emptyMap(),
                storedStringLists = trigger.carriedPipeline?.storedStringLists ?: emptyMap(),
                storedSubtypeGroups = trigger.carriedPipeline?.storedSubtypeGroups ?: emptyMap(),
            )
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

        // Who is asked. Normally the ability's controller, but a card can name someone else —
        // Farrel's Mantle's "its controller may", where "it" is the enchanted creature and the Aura
        // may sit on an opponent's permanent. This path asks the question before the effect runs,
        // so GatedEffectExecutor's own decisionMaker handling never gets the chance.
        val askedPlayerId = askedPlayerFor(state, trigger)

        // Persistent auto-answer yield (backlog §C): a remembered yes/no for this ability resolves
        // the may-question without prompting. "Yes" still proceeds to per-instance target selection
        // (only the yes/no is batched, never the targeting — §C.6); "no" skips the trigger.
        abilityIdentity?.let { state.autoAnswerFor(askedPlayerId, it) }?.let { auto ->
            val note = AbilityAutoAnsweredEvent(trigger.sourceId, sourceName, askedPlayerId, auto)
            if (!auto) return ExecutionResult.success(state, listOf(note))
            val innerEffect = ability.effect.asMayDecide()!!.then
            val unwrappedTrigger = trigger.copy(ability = ability.copy(effect = innerEffect))
            val result = processTargetedTrigger(state, unwrappedTrigger, targetRequirement)
            return result.copy(events = listOf(note) + result.events)
        }

        // Create yes/no decision.
        //
        // The card's own `description` wins over the generated effect text. A generated description
        // is assembled bottom-up from pipeline steps, so a composed effect reads like plumbing —
        // Safe Haven's upkeep trigger rendered as "You may sacrifice this creature. If you do, look
        // at cards exiled by this permanent. Put those cards onto the battlefield" instead of its
        // printed text. Whenever an author wrote the clause out, that is the prompt.
        val decisionResult = decisionHandler.createYesNoDecision(
            state = state,
            playerId = askedPlayerId,
            sourceId = trigger.sourceId,
            sourceName = sourceName,
            prompt = ability.descriptionOverride ?: ability.effect.description,
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
            triggeringEntityId = trigger.triggerContext.triggeringEntityId,
            // Carry the triggering player so a "target … that player controls" filter
            // (ControllerPredicate.ControlledByTriggeringPlayer / ControlledByReferencedPlayer over
            // Player.TriggeringPlayer) resolves identically here to the on-stack targeting path — a
            // trigger whose associated player rides on triggeringPlayerId must reach the same
            // legal-target verdict in this pre-check, or the may/pay question is wrongly skipped.
            pipelineContext = com.wingedsheep.engine.handlers.PredicateContext(
                controllerId = trigger.controllerId,
                triggeringEntityId = trigger.triggerContext.triggeringEntityId,
                triggeringPlayerId = trigger.triggerContext.triggeringPlayerId,
                // The X carried by the triggering event (an {X} cycling cost, a megamorph turn-up)
                // so an X-relative target filter — `manaValueEqualsX()` on Webstrike Elite's
                // "artifact or enchantment with mana value X" — finds targets at legality time.
                // Without it those predicates read an unbound X and match nothing.
                xValue = trigger.triggerContext.xValue,
                storedCollections = trigger.carriedPipeline?.storedCollections ?: emptyMap(),
                chosenValues = trigger.carriedPipeline?.chosenValues ?: emptyMap(),
                storedStringLists = trigger.carriedPipeline?.storedStringLists ?: emptyMap(),
                storedSubtypeGroups = trigger.carriedPipeline?.storedSubtypeGroups ?: emptyMap(),
            )
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
                    // See the note on the other findLegalTargets call sites: an X-relative target
                    // filter needs the triggering event's X bound to match anything.
                    xValue = trigger.triggerContext.xValue,
                    storedCollections = trigger.carriedPipeline?.storedCollections ?: emptyMap(),
                    chosenValues = trigger.carriedPipeline?.chosenValues ?: emptyMap(),
                    storedStringLists = trigger.carriedPipeline?.storedStringLists ?: emptyMap(),
                    storedSubtypeGroups = trigger.carriedPipeline?.storedSubtypeGroups ?: emptyMap(),
                ),
            )
            allLegalTargets[index] = legalTargets
        }

        // If no legal targets exist for any required requirement, the ability is not put on the stack
        // (Rule 603.3d). This applies regardless of whether the ability is optional ("you may").
        for ((index, req) in allRequirements.withIndex()) {
            val legalTargets = allLegalTargets[index] ?: emptyList()
            if (legalTargets.isEmpty() && req.effectiveMinCount > 0) {
                // The else branch is in one of two places, and both are the same printed clause.
                // A mandatory ability writes "…; otherwise, X" as `elseEffect`. A "you may … If you
                // don't, X" ability keeps its else inside the consent gate, because that is where
                // *declining* is decided — but a target it cannot choose is the other way of not
                // doing it (Entrails Feaster taps when no graveyard holds a creature card), so the
                // clause has to run from here too rather than be lost with the fizzled ability.
                val declineBranch = ability.elseEffect
                    ?: (ability.effect as? GatedEffect)?.takeIf { it.gate.isConsentGate }?.otherwise
                if (declineBranch != null) {
                    return putTriggerOnStack(state, trigger, emptyList(), declineBranch)
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
        //
        // Safe for a "you may" ability now that consent is a gate rather than a flag: either the
        // yes/no was already asked and answered before this ran (`processMayThenTargetTrigger`
        // unwraps the gate and calls back in), or the gate is still on the effect and will ask at
        // resolution. Neither reading is "there is only one choice, so don't prompt" applied to the
        // decline — which is what made this branch fail open while the flag existed, so that
        // "you may have target opponent discard a card" (Ebon Dragon) never asked in a two-player
        // game. An "up to one target player" requirement skips this via `effectiveMinCount == 0`.
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

        // Create target requirement infos for the decision.
        //
        // A slot's minimum is the *requirement's* — "up to one" allows zero, "target creature" does
        // not. A "you may" ability used to force every slot to zero here so that choosing nothing
        // was how you declined; that was the `optional` flag's second, unrelated meaning, and it was
        // wrong twice over. CR 603.3d chooses targets for a "you may" trigger like any other and
        // puts the choice at resolution, and an ability whose target is mandatory has to be removed
        // from the stack when there is no legal one (the loop above) rather than resolve targetless.
        // Consent is now a gate on the effect, answered on its own — either before this method runs
        // (`processMayThenTargetTrigger`) or as the ability resolves.
        val requirementInfos = allRequirements.mapIndexed { index, req ->
            // "Any number of target ..." (unlimited) caps at however many legal targets exist,
            // mirroring the cast-time path (TargetEnumerationUtils). Using req.count (always 1
            // for an unlimited requirement) would wrongly clamp the decision to a single target.
            val maxTargets = if (req.unlimited) (allLegalTargets[index]?.size ?: 0) else req.count
            TargetRequirementInfo(
                index = index,
                description = req.description,
                minTargets = req.effectiveMinCount,
                maxTargets = maxTargets,
                sameOwner = (req as? com.wingedsheep.sdk.scripting.targets.TargetObject)?.sameOwner == true,
                totalManaValueAtMost = resolveTotalManaValueAtMost(state, trigger, req),
                differentNames = (req as? com.wingedsheep.sdk.scripting.targets.TargetObject)?.differentNames == true,
                differentControllers =
                    (req as? com.wingedsheep.sdk.scripting.targets.TargetObject)?.differentControllers == true
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
            triggerLastKnownSubtypes = trigger.triggerContext.lastKnownSubtypes,
            triggerLastKnownCardTypes = trigger.triggerContext.lastKnownCardTypes,
                triggerLastKnownDamageDealtByPlayers =
                    trigger.triggerContext.lastKnownDamageDealtByPlayers,
                triggerLastKnownBlockingOrBlockedByIds =
                    trigger.triggerContext.lastKnownBlockingOrBlockedByIds,
                triggerLastKnownPower = trigger.triggerContext.lastKnownPower,
                triggerLastKnownToughness = trigger.triggerContext.lastKnownToughness,
                triggerDiedBatchTotalPower = trigger.triggerContext.diedBatchTotalPower,
                triggerModesChosenCount = trigger.triggerContext.modesChosenCount,
                triggerScryCount = trigger.triggerContext.scryCount,
                triggerDiscardCount = trigger.triggerContext.discardedCardCount,
                triggerDiscoverValue = trigger.triggerContext.discoverValue,
                triggerExcessDamageAmount = trigger.triggerContext.excessDamageAmount,
                triggerRecipientToughness = trigger.triggerContext.recipientToughnessAtDamage,
                triggerManaSpentOnTriggeringSpell = trigger.triggerContext.manaSpentOnTriggeringSpell,
                triggerColorsSpentOnTriggeringSpell = trigger.triggerContext.colorsSpentOnTriggeringSpell,
                triggerManaValueOfTriggeringSpell = trigger.triggerContext.manaValueOfTriggeringSpell,
                triggerXValueOfTriggeringSpell = trigger.triggerContext.xValueOfTriggeringSpell,
                pipeline = trigger.carriedPipeline ?: com.wingedsheep.engine.handlers.PipelineState.EMPTY
            )
            ability.effect.runtimeDescription { amount -> evaluator.evaluateForDisplay(state, amount, context) }
        } catch (_: Exception) {
            ability.effect.description
        }

        val decisionResult = decisionHandler.createTargetDecision(
            state = state,
            playerId = resolveTargetChooser(state, trigger, allRequirements),
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
            triggerLastKnownSubtypes = trigger.triggerContext.lastKnownSubtypes,
            triggerLastKnownCardTypes = trigger.triggerContext.lastKnownCardTypes,
            triggerLastKnownDamageDealtByPlayers =
                trigger.triggerContext.lastKnownDamageDealtByPlayers,
            triggerLastKnownBlockingOrBlockedByIds =
                trigger.triggerContext.lastKnownBlockingOrBlockedByIds,
            lastKnownPower = trigger.triggerContext.lastKnownPower,
            lastKnownToughness = trigger.triggerContext.lastKnownToughness,
            diedBatchTotalPower = trigger.triggerContext.diedBatchTotalPower,
            triggerModesChosenCount = trigger.triggerContext.modesChosenCount,
            enchantedCreatureLastKnownPower = trigger.triggerContext.enchantedCreatureLastKnownPower,
            triggerScryCount = trigger.triggerContext.scryCount,
            triggerDiscardCount = trigger.triggerContext.discardedCardCount,
            triggerDiscoverValue = trigger.triggerContext.discoverValue,
            triggerExcessDamageAmount = trigger.triggerContext.excessDamageAmount,
            triggerRecipientToughness = trigger.triggerContext.recipientToughnessAtDamage,
            triggerManaSpentOnTriggeringSpell = trigger.triggerContext.manaSpentOnTriggeringSpell,
            triggerColorsSpentOnTriggeringSpell = trigger.triggerContext.colorsSpentOnTriggeringSpell,
            triggerManaValueOfTriggeringSpell = trigger.triggerContext.manaValueOfTriggeringSpell,
            triggerXValueOfTriggeringSpell = trigger.triggerContext.xValueOfTriggeringSpell,
            xValue = trigger.triggerContext.xValue,
            carriedPipeline = trigger.carriedPipeline,
            capturedEntityIds = trigger.triggerContext.capturedEntityIds ?: emptyList(),
            interveningIf = ability.interveningIf
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
            granterId = trigger.granterId,
            descriptionOverride = ability.descriptionOverride,
            triggerDamageAmount = trigger.triggerContext.damageAmount,
            triggeringEntityId = trigger.triggerContext.triggeringEntityId,
            triggeringPlayerId = trigger.triggerContext.triggeringPlayerId,
            xValue = trigger.triggerContext.xValue ?: computeXForDisplay(state, trigger),
            triggerCounterCount = trigger.triggerContext.counterCount,
            triggerTotalCounterCount = trigger.triggerContext.totalCounterCount,
            triggerLastKnownCounters = trigger.triggerContext.lastKnownCounters,
            triggerLastKnownSubtypes = trigger.triggerContext.lastKnownSubtypes,
            triggerLastKnownCardTypes = trigger.triggerContext.lastKnownCardTypes,
            triggerLastKnownDamageDealtByPlayers =
                trigger.triggerContext.lastKnownDamageDealtByPlayers,
            triggerLastKnownBlockingOrBlockedByIds =
                trigger.triggerContext.lastKnownBlockingOrBlockedByIds,
            targetingSourceEntityId = trigger.triggerContext.targetingSourceEntityId,
            triggerUnattachedFromEntityId = trigger.triggerContext.unattachedFromEntityId,
            lastKnownPower = trigger.triggerContext.lastKnownPower,
            lastKnownToughness = trigger.triggerContext.lastKnownToughness,
            diedBatchTotalPower = trigger.triggerContext.diedBatchTotalPower,
            triggerModesChosenCount = trigger.triggerContext.modesChosenCount,
            enchantedCreatureLastKnownPower = trigger.triggerContext.enchantedCreatureLastKnownPower,
            triggerScryCount = trigger.triggerContext.scryCount,
            triggerDiscardCount = trigger.triggerContext.discardedCardCount,
            triggerDiscoverValue = trigger.triggerContext.discoverValue,
            triggerExcessDamageAmount = trigger.triggerContext.excessDamageAmount,
            triggerRecipientToughness = trigger.triggerContext.recipientToughnessAtDamage,
            triggerManaSpentOnTriggeringSpell = trigger.triggerContext.manaSpentOnTriggeringSpell,
            triggerColorsSpentOnTriggeringSpell = trigger.triggerContext.colorsSpentOnTriggeringSpell,
            triggerManaValueOfTriggeringSpell = trigger.triggerContext.manaValueOfTriggeringSpell,
            triggerXValueOfTriggeringSpell = trigger.triggerContext.xValueOfTriggeringSpell,
            capturedEntityIds = trigger.triggerContext.capturedEntityIds ?: emptyList(),
            sagaChapterInfo = trigger.sagaChapterInfo,
            carriedPipeline = trigger.carriedPipeline,
            // CR 603.4 — the intervening-"if" travels with the object so the resolver can check it
            // the second time. A `triggerRestriction` deliberately does not.
            interveningIf = ability.interveningIf
        )

        val causedByAttack = isAttackCausedTrigger(trigger)
        val outerRequirements = listOfNotNull(ability.targetRequirement)

        // CR 603.3c / 700.2b — a modal triggered ability's modes are announced as the ability is
        // put onto the stack, and CR 603.3d says its targets follow the same way (601.2c–d). Pause
        // here so both are locked in *before* the ability hits the stack: that is what lets an
        // opponent see the chosen mode while the ability is still responded-to, and only then do
        // the chosen targets actually *become* targets, which is what ward (CR 702.21) and
        // "becomes the target" triggers key on.
        //
        // Only a *top-level* ModalEffect is caught here. A modal nested inside another effect (a
        // gated effect, a reflexive trigger, a pipeline) isn't the ability's own mode question, so
        // it stays with the resolution-time picker in
        // [com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor].
        (abilityComponent.effect as? ModalEffect)?.let { modal ->
            return presentTriggerModalModeDecision(
                state = state,
                ability = abilityComponent,
                outerTargets = targets,
                outerTargetRequirements = outerRequirements,
                modal = modal,
                selectedModeIndices = emptyList(),
                availableIndices = null,
                causedByAttack = causedByAttack
            )
        }

        return stackResolver.putTriggeredAbility(
            state, abilityComponent, targets,
            targetRequirements = outerRequirements,
            causedByAttack = causedByAttack
        )
    }

    /**
     * How many modes this trigger's controller picks, and the minimum they must pick.
     *
     * A [ModalEffect.dynamicChooseCount] ("choose up to X") is evaluated here, once, against the
     * state the ability is going onto the stack in — CR 601.2c (reached via 603.3d) fixes the count
     * at that moment, so it can't drift as the picks are made. The floor drops to 0 because "up to"
     * always permits picking none; that mirrors the resolution-time evaluation in
     * [com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor], which still serves
     * modal *activated* abilities and nested modals.
     */
    private fun effectiveChooseCounts(
        state: GameState,
        ability: TriggeredAbilityOnStackComponent,
        modal: ModalEffect
    ): Pair<Int, Int> {
        val dynamic = modal.dynamicChooseCount
            ?: return modal.chooseCount to modal.minChooseCount
        val evaluated = DynamicAmountEvaluator().evaluate(
            state,
            dynamic,
            EffectContext.forTriggeredAbility(ability)
        )
        return evaluated.coerceIn(0, modal.modes.size) to 0
    }

    /**
     * CR 603.3c — "If no mode is chosen, the ability is removed from the stack." Reached when every
     * mode would be illegal, when a "choose up to X" cap evaluated to 0, and when the player
     * declined every optional pick.
     */
    private fun modalTriggerRemovedFromStack(
        state: GameState,
        ability: TriggeredAbilityOnStackComponent,
        reason: String
    ): ExecutionResult = ExecutionResult.success(
        state,
        listOf(AbilityFizzledEvent(ability.sourceId, ability.description, reason))
    )

    /**
     * Build the next mode-pick decision for a modal triggered ability going on the stack, or move
     * straight to per-mode target selection once enough modes are picked.
     *
     * CR 603.3c: a mode that would be illegal — no legal targets, say — can't be chosen, so
     * unselectable modes are filtered out of the offer. When no mode can be chosen and the minimum
     * hasn't been met, the ability is removed from the stack.
     */
    internal fun presentTriggerModalModeDecision(
        state: GameState,
        ability: TriggeredAbilityOnStackComponent,
        outerTargets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>,
        outerTargetRequirements: List<TargetRequirement>,
        modal: ModalEffect,
        selectedModeIndices: List<Int>,
        availableIndices: List<Int>?,
        causedByAttack: Boolean
    ): ExecutionResult {
        val (chooseCount, minChooseCount) = effectiveChooseCounts(state, ability, modal)

        // "Choose one that hasn't been chosen (this turn)" — the source's own memory narrows the
        // pool before the first pick. Later picks arrive with `availableIndices` already narrowed by
        // the resumer, so re-applying the memory is a no-op there.
        val remembered = ChosenModeMemory.excludedFor(state, ability.sourceId, modal)
        val candidateIndices = (availableIndices ?: modal.modes.indices.toList())
            .filter { it !in remembered }
        val offerIndices = candidateIndices.filter { index ->
            modeHasLegalTargets(state, ability, modal.modes[index])
        }

        if (offerIndices.isEmpty() || selectedModeIndices.size >= chooseCount) {
            if (offerIndices.isEmpty() && selectedModeIndices.size < minChooseCount) {
                // Every remaining mode is unselectable — no legal target, or already spent by
                // "…that hasn't been chosen (this turn)".
                return modalTriggerRemovedFromStack(
                    state, ability, "No legal mode could be chosen"
                )
            }
            return presentTriggerModalTargetDecision(
                state, ability, outerTargets, outerTargetRequirements,
                modal.modes, selectedModeIndices, emptyList(), currentOrdinal = 0, causedByAttack,
                recordChosenModesOnSource = modal.excludePreviouslyChosenModes,
                recordChosenModesThisTurn = modal.excludeModesChosenThisTurn
            )
        }

        val doneOffered = selectedModeIndices.size >= minChooseCount &&
            selectedModeIndices.size < chooseCount
        // Same decline label the resolution-time modal path uses, so clients (and tests) can
        // recognise "choose up to one"'s opt-out wherever the mode question is raised.
        val optionLabels = offerIndices.map { modal.modes[it].description } +
            (if (doneOffered) listOf(ModalEffectExecutor.DECLINE_MODE_LABEL) else emptyList())

        val decisionId = java.util.UUID.randomUUID().toString()
        val pickNumber = selectedModeIndices.size + 1
        val alreadyPicked = if (selectedModeIndices.isEmpty()) "" else {
            "\nAlready picked: ${selectedModeIndices.joinToString("; ") { modal.modes[it].description }}"
        }
        val basePrompt = "Choose a mode for ${ability.sourceName}"
        val prompt = if (chooseCount > 1) {
            "$basePrompt ($pickNumber of $chooseCount)$alreadyPicked"
        } else basePrompt

        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = ability.controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = ability.sourceId,
                sourceName = ability.sourceName,
                // Not a resolution-time question: the ability is on its way to the stack and this
                // pick is part of putting it there (CR 603.3c).
                phase = DecisionPhase.TRIGGER
            ),
            options = optionLabels
        )

        val continuation = TriggerModalModeSelectionContinuation(
            decisionId = decisionId,
            ability = ability,
            outerTargets = outerTargets,
            outerTargetRequirements = outerTargetRequirements,
            modes = modal.modes,
            // The effective counts, so a resolved `dynamicChooseCount` isn't re-evaluated per pick.
            chooseCount = chooseCount,
            minChooseCount = minChooseCount,
            allowRepeat = modal.allowRepeat,
            offeredIndices = offerIndices,
            availableIndices = candidateIndices,
            selectedModeIndices = selectedModeIndices,
            doneOptionOffered = doneOffered,
            causedByAttack = causedByAttack,
            recordChosenModesOnSource = modal.excludePreviouslyChosenModes,
            recordChosenModesThisTurn = modal.excludeModesChosenThisTurn
        )

        return ExecutionResult.paused(
            state.pushContinuation(continuation).withPendingDecision(decision),
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = ability.controllerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Collect targets for the chosen modes, one decision per targeting mode, then put the ability
     * on the stack. Modes with no requirements take an empty slot so [resolvedModeTargets] stays
     * aligned 1:1 with [chosenModeIndices]; a sole legal player target is auto-selected rather than
     * prompted for, matching the non-modal trigger path.
     */
    internal fun presentTriggerModalTargetDecision(
        state: GameState,
        ability: TriggeredAbilityOnStackComponent,
        outerTargets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>,
        outerTargetRequirements: List<TargetRequirement>,
        modes: List<com.wingedsheep.sdk.scripting.effects.Mode>,
        chosenModeIndices: List<Int>,
        resolvedModeTargets: List<List<com.wingedsheep.engine.state.components.stack.ChosenTarget>>,
        currentOrdinal: Int,
        causedByAttack: Boolean,
        recordChosenModesOnSource: Boolean,
        recordChosenModesThisTurn: Boolean
    ): ExecutionResult {
        var ordinal = currentOrdinal
        var targetsAccum = resolvedModeTargets

        while (ordinal < chosenModeIndices.size) {
            val mode = modes[chosenModeIndices[ordinal]]
            if (mode.targetRequirements.isEmpty()) {
                targetsAccum = targetsAccum + listOf(emptyList())
                ordinal++
                continue
            }

            val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
            val requirementInfos = mode.targetRequirements.mapIndexed { index, req ->
                legalTargetsMap[index] = findModeLegalTargets(state, ability, req)
                TargetRequirementInfo(
                    index = index,
                    description = req.description,
                    minTargets = req.effectiveMinCount,
                    maxTargets = req.count
                )
            }

            // Auto-select the lone legal player target instead of prompting (mirrors
            // processTargetedTrigger's single-player-target shortcut).
            val soleReq = mode.targetRequirements.singleOrNull()
            val soleLegal = legalTargetsMap[0].orEmpty()
            val isPlayerTarget = soleReq is com.wingedsheep.sdk.scripting.targets.TargetPlayer ||
                soleReq is com.wingedsheep.sdk.scripting.targets.TargetOpponent
            if (isPlayerTarget && soleLegal.size == 1 && soleReq.count == 1) {
                targetsAccum = targetsAccum + listOf(listOf(createChosenTarget(state, soleLegal.first())))
                ordinal++
                continue
            }

            val decisionId = java.util.UUID.randomUUID().toString()
            val pickNumber = ordinal + 1
            val prompt = if (chosenModeIndices.size > 1) {
                "Choose targets for ${ability.sourceName} — ${mode.description} ($pickNumber of ${chosenModeIndices.size})"
            } else {
                "Choose targets for ${ability.sourceName} — ${mode.description}"
            }
            val decision = ChooseTargetsDecision(
                id = decisionId,
                playerId = ability.controllerId,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = ability.sourceId,
                    sourceName = ability.sourceName,
                    // Part of putting the ability on the stack, not of resolving it (CR 603.3d).
                    phase = DecisionPhase.TRIGGER,
                    effectHint = mode.description
                ),
                targetRequirements = requirementInfos,
                legalTargets = legalTargetsMap
            )

            val continuation = TriggerModalTargetSelectionContinuation(
                decisionId = decisionId,
                ability = ability,
                outerTargets = outerTargets,
                outerTargetRequirements = outerTargetRequirements,
                modes = modes,
                chosenModeIndices = chosenModeIndices,
                resolvedModeTargets = targetsAccum,
                currentOrdinal = ordinal,
                causedByAttack = causedByAttack,
                recordChosenModesOnSource = recordChosenModesOnSource,
                recordChosenModesThisTurn = recordChosenModesThisTurn
            )

            return ExecutionResult.paused(
                state.pushContinuation(continuation).withPendingDecision(decision),
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = ability.controllerId,
                        decisionType = "CHOOSE_TARGETS",
                        prompt = decision.prompt
                    )
                )
            )
        }

        return finalizeModalTrigger(
            state, ability, outerTargets, outerTargetRequirements,
            modes, chosenModeIndices, targetsAccum, causedByAttack,
            recordChosenModesOnSource, recordChosenModesThisTurn
        )
    }

    /**
     * Put the modal trigger on the stack with its modes and per-mode targets baked in.
     *
     * The per-mode targets join the flat `targets` list so [StackResolver.putTriggeredAbility]
     * emits a `BecomesTargetEvent` for each — the whole point of choosing them here — and so
     * CR 608.2b re-validation runs against them on resolution.
     */
    private fun finalizeModalTrigger(
        state: GameState,
        ability: TriggeredAbilityOnStackComponent,
        outerTargets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>,
        outerTargetRequirements: List<TargetRequirement>,
        modes: List<com.wingedsheep.sdk.scripting.effects.Mode>,
        chosenModeIndices: List<Int>,
        resolvedModeTargets: List<List<com.wingedsheep.engine.state.components.stack.ChosenTarget>>,
        causedByAttack: Boolean,
        recordChosenModesOnSource: Boolean,
        recordChosenModesThisTurn: Boolean
    ): ExecutionResult {
        if (chosenModeIndices.isEmpty()) {
            // CR 603.3c — no mode was chosen, so the ability never reaches the stack. Either every
            // mode was illegal, a "choose up to X" cap evaluated to 0, or the player declined all
            // of an optional set of picks.
            return modalTriggerRemovedFromStack(state, ability, "No mode was chosen")
        }

        // "…that hasn't been chosen (this turn)": commit the picks to the source's memory now that
        // they're final, so the next trigger of this same object offers what's left.
        val stateWithMemory = chosenModeIndices.fold(state) { acc, modeIndex ->
            ChosenModeMemory.record(
                acc, ability.sourceId, modeIndex,
                ever = recordChosenModesOnSource,
                thisTurn = recordChosenModesThisTurn
            )
        }

        val component = ability.copy(
            chosenModes = chosenModeIndices,
            modeTargetsOrdered = resolvedModeTargets,
            modeTargetRequirements = chosenModeIndices.associateWith { modes[it].targetRequirements }
        )
        val flatTargets = outerTargets + resolvedModeTargets.flatten()
        val flatRequirements = outerTargetRequirements +
            chosenModeIndices.flatMap { modes[it].targetRequirements }

        return stackResolver.putTriggeredAbility(
            stateWithMemory, component, flatTargets,
            targetRequirements = flatRequirements,
            causedByAttack = causedByAttack
        )
    }

    /** Legal targets for one of a mode's requirements, in the trigger's own targeting context. */
    private fun findModeLegalTargets(
        state: GameState,
        ability: TriggeredAbilityOnStackComponent,
        requirement: TargetRequirement
    ): List<EntityId> = targetFinder.findLegalTargets(
        state = state,
        requirement = requirement,
        controllerId = ability.controllerId,
        sourceId = ability.sourceId,
        triggeringEntityId = ability.triggeringEntityId,
        pipelineContext = com.wingedsheep.engine.handlers.PredicateContext(
            controllerId = ability.controllerId,
            triggeringEntityId = ability.triggeringEntityId,
            triggeringPlayerId = ability.triggeringPlayerId,
            // Same reason as the pending-trigger call sites: an X-relative target filter must see
            // the X the ability went on the stack with, or it re-checks as having no legal targets.
            xValue = ability.xValue,
            storedCollections = ability.carriedPipeline?.storedCollections ?: emptyMap(),
            chosenValues = ability.carriedPipeline?.chosenValues ?: emptyMap(),
            storedStringLists = ability.carriedPipeline?.storedStringLists ?: emptyMap(),
            storedSubtypeGroups = ability.carriedPipeline?.storedSubtypeGroups ?: emptyMap(),
        ),
    )

    /** True when every mandatory target requirement of [mode] has at least one legal target. */
    private fun modeHasLegalTargets(
        state: GameState,
        ability: TriggeredAbilityOnStackComponent,
        mode: com.wingedsheep.sdk.scripting.effects.Mode
    ): Boolean = mode.targetRequirements.all { req ->
        req.effectiveMinCount == 0 || findModeLegalTargets(state, ability, req).isNotEmpty()
    }

    /**
     * True when [trigger] is a creature's own "whenever this creature attacks" ability — a
     * SELF-bound per-attacker [com.wingedsheep.sdk.scripting.EventPattern.AttackEvent]. Used to
     * stamp `causedByAttack` on the emitted `AbilityTriggeredEvent` so Firebender Ascension's
     * "a creature you control attacking causes a triggered ability of that creature to trigger"
     * meta-trigger fires only for genuine attack triggers, not other in-combat triggers (deals
     * damage, dies, etc.). The SELF binding is what ties the ability to "that [attacking] creature":
     * an anthem-style ANY-bound "whenever a creature you control attacks" ability lives on a
     * different permanent and is deliberately excluded.
     */
    private fun isAttackCausedTrigger(trigger: PendingTrigger): Boolean =
        trigger.ability.trigger is com.wingedsheep.sdk.scripting.EventPattern.AttackEvent &&
            trigger.ability.binding == com.wingedsheep.sdk.scripting.TriggerBinding.SELF

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
     * Which player is asked to pick this trigger's targets.
     *
     * The ability's controller, unless a requirement carries a [TargetChooser] naming somebody
     * else — "that player … of their choice" (Quicksilver Fountain), "its controller chooses target
     * permanent …" (Confusion in the Ranks). Per [TargetChooser] the chooser is orthogonal to
     * legality: the legal-target sets above were built relative to `trigger.controllerId` and stay
     * that way, because these are still the controller's targets (CR 115). Only who answers the
     * decision changes.
     *
     * A chooser that resolves to nobody falls back to the controller rather than dropping the
     * trigger: a target was already found legal, so somebody has to pick it.
     *
     * Choosers are read from the *whole* requirement list and must agree — no printed card splits
     * one trigger's targets between two deciders, and honoring only the first requirement's chooser
     * would silently hand the rest to the wrong player. [TargetChooser.Opponent] is deliberately
     * not handled here: it needs the controller to first pick *which* opponent decides, which is
     * the activated-ability path's `pauseForOpponentTargetChooser`, and `CardLinter` already
     * refuses it on a triggered ability.
     */
    private fun resolveTargetChooser(
        state: GameState,
        trigger: PendingTrigger,
        requirements: List<TargetRequirement>
    ): EntityId {
        val controller = trigger.controllerId
        val choosers = requirements.map { it.chooser }.distinct()
        val chooser = choosers.singleOrNull() ?: return controller
        return when (chooser) {
            TargetChooser.Controller, TargetChooser.Opponent -> controller
            TargetChooser.TriggeringPlayer ->
                trigger.triggerContext.triggeringPlayerId
                    ?: trigger.triggerContext.triggeringEntityId
                    ?: controller
            TargetChooser.ControllerOfTriggeringEntity ->
                trigger.triggerContext.triggeringEntityId
                    ?.let { state.projectedState.getController(it) }
                    ?: controller
        }
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
                        triggeringPlayerId = trigger.triggerContext.triggeringPlayerId,
                        xValue = trigger.triggerContext.xValue,
                        triggerDamageAmount = trigger.triggerContext.damageAmount,
                        triggerCounterCount = trigger.triggerContext.counterCount,
                        triggerTotalCounterCount = trigger.triggerContext.totalCounterCount,
                        triggerLastKnownCounters = trigger.triggerContext.lastKnownCounters,
            triggerLastKnownSubtypes = trigger.triggerContext.lastKnownSubtypes,
            triggerLastKnownCardTypes = trigger.triggerContext.lastKnownCardTypes,
                        triggerLastKnownDamageDealtByPlayers = trigger.triggerContext.lastKnownDamageDealtByPlayers,
                        triggerLastKnownBlockingOrBlockedByIds = trigger.triggerContext.lastKnownBlockingOrBlockedByIds,
                        triggerLastKnownPower = trigger.triggerContext.lastKnownPower,
                        triggerLastKnownToughness = trigger.triggerContext.lastKnownToughness,
                        triggerDiedBatchTotalPower = trigger.triggerContext.diedBatchTotalPower,
                        triggerModesChosenCount = trigger.triggerContext.modesChosenCount,
                        triggerManaSpentOnTriggeringSpell = trigger.triggerContext.manaSpentOnTriggeringSpell,
                        triggerColorsSpentOnTriggeringSpell = trigger.triggerContext.colorsSpentOnTriggeringSpell,
                        triggerManaValueOfTriggeringSpell = trigger.triggerContext.manaValueOfTriggeringSpell,
                        triggerXValueOfTriggeringSpell = trigger.triggerContext.xValueOfTriggeringSpell,
                        // The dynamic cap may read trigger-context properties — e.g. Elrond,
                        // Master of Healing's "up to X target creatures, where X is the number of
                        // cards looked at while scrying" (ContextPropertyKey.TRIGGER_SCRY_COUNT).
                        // Without this the cap resolves to 0 and the player can pick no targets.
                        triggerScryCount = trigger.triggerContext.scryCount,
                        triggerDiscardCount = trigger.triggerContext.discardedCardCount,
                        triggerDiscoverValue = trigger.triggerContext.discoverValue,
                        triggerExcessDamageAmount = trigger.triggerContext.excessDamageAmount,
                        triggerRecipientToughness = trigger.triggerContext.recipientToughnessAtDamage,
                        // A reflexive trigger's dynamic cap may read what its action half stashed
                        // (e.g. `VariableReference("discarded_count")`, Amass's army reference).
                        pipeline = trigger.carriedPipeline ?: com.wingedsheep.engine.handlers.PipelineState.EMPTY,
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
     * Resolve a [TargetObject.totalManaValueAtMost] aggregate cap ("...with total mana value X or
     * less") to a concrete integer at decision-build time — e.g. Fire Lord Sozin's cap reflecting
     * the X just paid, or a reflexive trigger's action-half payment (CR 603.12) via
     * [PendingTrigger.carriedPipeline]. `null` when the requirement carries no such cap.
     */
    private fun resolveTotalManaValueAtMost(
        state: GameState,
        trigger: PendingTrigger,
        requirement: TargetRequirement
    ): Int? {
        val dyn = (requirement as? TargetObject)?.totalManaValueAtMost ?: return null
        return try {
            val context = EffectContext(
                sourceId = trigger.sourceId,
                controllerId = trigger.controllerId,
                triggeringEntityId = trigger.triggerContext.triggeringEntityId,
                triggeringPlayerId = trigger.triggerContext.triggeringPlayerId,
                xValue = trigger.triggerContext.xValue,
                triggerDamageAmount = trigger.triggerContext.damageAmount,
                triggerCounterCount = trigger.triggerContext.counterCount,
                triggerTotalCounterCount = trigger.triggerContext.totalCounterCount,
                triggerLastKnownCounters = trigger.triggerContext.lastKnownCounters,
            triggerLastKnownSubtypes = trigger.triggerContext.lastKnownSubtypes,
            triggerLastKnownCardTypes = trigger.triggerContext.lastKnownCardTypes,
                triggerLastKnownDamageDealtByPlayers = trigger.triggerContext.lastKnownDamageDealtByPlayers,
                triggerLastKnownBlockingOrBlockedByIds = trigger.triggerContext.lastKnownBlockingOrBlockedByIds,
                triggerLastKnownPower = trigger.triggerContext.lastKnownPower,
                triggerLastKnownToughness = trigger.triggerContext.lastKnownToughness,
                triggerDiedBatchTotalPower = trigger.triggerContext.diedBatchTotalPower,
                triggerModesChosenCount = trigger.triggerContext.modesChosenCount,
                triggerManaSpentOnTriggeringSpell = trigger.triggerContext.manaSpentOnTriggeringSpell,
                triggerColorsSpentOnTriggeringSpell = trigger.triggerContext.colorsSpentOnTriggeringSpell,
                triggerManaValueOfTriggeringSpell = trigger.triggerContext.manaValueOfTriggeringSpell,
                triggerXValueOfTriggeringSpell = trigger.triggerContext.xValueOfTriggeringSpell,
                triggerScryCount = trigger.triggerContext.scryCount,
                triggerDiscardCount = trigger.triggerContext.discardedCardCount,
                triggerDiscoverValue = trigger.triggerContext.discoverValue,
                triggerExcessDamageAmount = trigger.triggerContext.excessDamageAmount,
                triggerRecipientToughness = trigger.triggerContext.recipientToughnessAtDamage,
                pipeline = trigger.carriedPipeline ?: com.wingedsheep.engine.handlers.PipelineState.EMPTY,
            )
            DynamicAmountEvaluator().evaluate(state, dyn, context).coerceAtLeast(0)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Has this source's controller already taken [abilityId]'s "Do this only once each turn" action
     * this turn (CR 603.2h)? Read-only mirror of the stamp `GatedEffectExecutor` writes when the
     * spending budget gate passes.
     */
    private fun effectBudgetSpent(state: GameState, sourceId: EntityId, abilityId: AbilityId): Boolean =
        state.getEntity(sourceId)
            ?.get<TriggeredAbilityEffectAppliedThisTurnComponent>()
            ?.hasApplied(abilityId) == true

    /**
     * Lower `TriggeredAbility.effectOncePerTurn` into [Gate.OnceEachTurn] budget gates around the
     * effect that actually runs (CR 603.2h — see the flag's KDoc).
     *
     * Placement is the whole point. When the ability's effect already owns a consent gate (a
     * `Gate.MayDecide` / `MayPay` / `MayPayX` — "**you may** have it connive", "**you may** have
     * She-Hulk deal that much damage"), the lowering emits a *sandwich*:
     *
     * ```
     * OnceEachTurn(spend = false)   ← has the action already been taken? if so, resolve silently
     *   └ MayDecide / MayPay / …    ← the printed "you may", asked only when it can still matter
     *       └ OnceEachTurn()        ← taking the action spends the turn's single use
     *           └ the real effect
     * ```
     *
     * The inner, spending gate is why declining costs nothing: the player can decline instance after
     * instance and still take the action on the one they want. The outer, read-only gate is why an
     * instance whose turn has already been used up "does nothing as it resolves" (Nykthos Paragon /
     * Riveteers Ascendancy rulings) instead of raising a yes/no whose answer cannot matter — the
     * trap of accepting three She-Hulk prompts in one multi-block and getting one mirror.
     *
     * A consequence worth stating: with a gate on the outside, `asMayDecide()` no longer matches at
     * the top of the effect, so `processSingleTrigger` routes a *targeted* capped trigger through
     * `processTargetedTrigger` rather than `processMayThenTargetTrigger`. Consent therefore moves
     * from put-on-stack time to resolution time, which is where CR puts it anyway: targets are
     * chosen on announcement (CR 603.3d) and the "you may" is chosen as the ability resolves (the
     * Legolas, Counter of Kills ruling says so in as many words). Without that move the three
     * prompts of a multi-block would all be answered before any instance resolved, so the outer gate
     * would have nothing to suppress.
     *
     * A mandatory capped ability has no consent gate and no prompt to suppress, so a single
     * spending gate simply wraps the effect.
     */
    private fun withEffectBudgetGate(ability: TriggeredAbility): TriggeredAbility =
        ability.copy(effect = loweredEffectBudget(ability.effect, ability.id))

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

    /**
     * The pure half of the `effectOncePerTurn` lowering. Lives on the companion so the card-registry
     * guard ([consentGateIsMisplaced]) can be run over every shipped card at build time from a test,
     * rather than only being exercised when a capped ability happens to trigger in a game.
     */
    companion object {

        /**
         * Wrap [effect] in the [Gate.OnceEachTurn] budget gates for `effectOncePerTurn` — the
         * sandwich documented on `withEffectBudgetGate`.
         *
         * Throws when the ability has a consent gate the lowering cannot reach. That combination is
         * a silent rules bug if it ships: the budget gate lands *outside* the "you may", so
         * declining would spend the turn's single use — exactly the defect `effectOncePerTurn`
         * exists to fix, and invisible at the table because the prompt looks identical. The
         * condition depends only on the card definition, never on game state, so
         * `EffectOncePerTurnLoweringTest`'s sweep over the whole registry is what guarantees this
         * can never fire mid-game.
         */
        internal fun loweredEffectBudget(effect: Effect, abilityId: AbilityId): Effect {
            val spending = withSpendingGate(effect, abilityId)
            require(spending != null || !containsConsentGate(effect)) {
                "effectOncePerTurn ability $abilityId has a 'you may' the budget lowering can't " +
                    "reach — it must be at the top of the effect or the tail of a CompositeEffect, " +
                    "or declining would spend the turn's use (CR 603.2h). Effect: $effect"
            }
            return if (spending != null) {
                GatedEffect(gate = Gate.OnceEachTurn(abilityId, spend = false), then = spending)
            } else {
                GatedEffect(gate = Gate.OnceEachTurn(abilityId), then = effect)
            }
        }

        /**
         * True when [effect] carries a consent gate that [withSpendingGate] would *not* find — a
         * "you may" buried mid-composite or under some other wrapper. The card-registry guard.
         */
        internal fun consentGateIsMisplaced(effect: Effect): Boolean =
            withSpendingGate(effect, AbilityId("probe")) == null && containsConsentGate(effect)

        /**
         * Push the *spending* budget gate inside [effect]'s consent gate, or return null when there
         * is no consent gate to put it inside (a mandatory ability — the caller then wraps the whole
         * effect in a spending gate instead).
         *
         * The consent gate is not always at the top. Planetarium of Wan Shi Tong reads "look at the
         * top card of your library. You may cast that card without paying its mana cost. Do this
         * only once each turn", which is `Composite(look, May(cast))` — and its ruling is explicit
         * that it is the *casting* that spends the turn ("once you choose to cast the top card of
         * your library, the ability won't trigger again that turn"), not the looking. So the search
         * descends the **tail** of a [CompositeEffect]: the payoff of a "do X, then you may Y"
         * instruction is its last step, and that is the step the rider is attached to.
         */
        private fun withSpendingGate(effect: Effect, abilityId: AbilityId): Effect? = when (effect) {
            is GatedEffect ->
                if (effect.gate.isConsentGate) {
                    effect.copy(then = GatedEffect(gate = Gate.OnceEachTurn(abilityId), then = effect.then))
                } else {
                    null
                }

            is CompositeEffect ->
                effect.effects.lastOrNull()
                    ?.let { withSpendingGate(it, abilityId) }
                    ?.let { effect.copy(effects = effect.effects.dropLast(1) + it) }

            else -> null
        }

        /**
         * Any consent gate anywhere in the gate/composite spine of [effect], however deeply nested —
         * including inside a `MayPay` cost or a `DoAction` action, which are effects in their own
         * right. Deliberately broader than [withSpendingGate]: the gap between the two is precisely
         * the set of shapes whose budget would end up in the wrong place.
         */
        private fun containsConsentGate(effect: Effect): Boolean = when (effect) {
            is GatedEffect ->
                effect.gate.isConsentGate ||
                    containsConsentGate(effect.then) ||
                    effect.otherwise?.let { containsConsentGate(it) } == true ||
                    when (val gate = effect.gate) {
                        is Gate.MayPay -> containsConsentGate(gate.cost)
                        is Gate.DoAction -> containsConsentGate(gate.action)
                        else -> false
                    }

            is CompositeEffect -> effect.effects.any { containsConsentGate(it) }

            else -> false
        }
    }
}
