package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DependentTargetSelection
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DistributeCountersAmongTargetsEffect
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.engine.handlers.effects.composite.asMayDecide
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.targets.withCount

/**
 * Handles core effect and trigger resumption:
 * - EffectContinuation (composite effect pipelines)
 * - TriggeredAbilityContinuation (target selection for triggered abilities)
 * - ResolveSpellContinuation (no-op marker)
 * - MayAbilityContinuation (yes/no for may effects)
 * - MayTriggerContinuation (yes/no for may triggers with targets)
 */
class EffectAndTriggerContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices,
    private val effectRunner: EffectContinuationRunner
) : ContinuationResumerModule {
    private val amountEvaluator = services.dynamicAmountEvaluator

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(TriggeredAbilityContinuation::class, ::resumeTriggeredAbility),
        resumer(TriggerDamageDistributionContinuation::class, ::resumeTriggerDamageDistribution),
        resumer(ResolveSpellContinuation::class) { state, _, _, _ ->
            ExecutionResult.success(state)
        },
        resumer(MayAbilityContinuation::class, ::resumeMayAbility),
        resumer(GatedEffectContinuation::class, ::resumeGatedEffect),
        resumer(MayRevealCardFromHandContinuation::class, ::resumeMayRevealCardFromHand),
        resumer(BeholdContinuation::class, ::resumeBehold),
        resumer(MayTriggerContinuation::class, ::resumeMayTrigger),
        resumer(TriggerOpponentChooserContinuation::class, ::resumeTriggerOpponentChooser),
        resumer(BatchMayTriggerContinuation::class, ::resumeBatchMayTrigger)
    )



    private fun resumeTriggeredAbility(
        state: GameState,
        continuation: TriggeredAbilityContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected target selection response for triggered ability")
        }

        continuation.sequentialTargets?.let { prefix ->
            val requirements = continuation.targetRequirements
            val selected = response.selectedTargets[0]?.singleOrNull()
            // Declining an "up to one" slot ends the selection; `canStopAt` guarantees every
            // later slot is optional too, so no later target shifts into its position.
            if (selected == null && !DependentTargetSelection.canStopAt(requirements, prefix.size)) {
                return ExecutionResult.error(state, "Choose one target")
            }
            val chosen = if (selected == null) prefix else prefix + selected
            if (selected != null && chosen.size < requirements.size) {
                val pipeline = continuation.carriedPipeline
                val context = com.wingedsheep.engine.handlers.PredicateContext(
                    controllerId = continuation.controllerId,
                    sourceId = continuation.sourceId,
                    triggeringEntityId = continuation.triggerContext?.triggeringEntityId,
                    triggeringPlayerId = continuation.triggerContext?.triggeringPlayerId,
                    xValue = continuation.triggerContext?.xValue,
                    storedCollections = pipeline?.storedCollections ?: emptyMap(),
                    chosenValues = pipeline?.chosenValues ?: emptyMap(),
                    storedStringLists = pipeline?.storedStringLists ?: emptyMap(),
                    storedSubtypeGroups = pipeline?.storedSubtypeGroups ?: emptyMap(),
                )
                val legal = DependentTargetSelection.legalNext(state, requirements, chosen, context, targetFinder = services.targetFinder)
                return com.wingedsheep.engine.handlers.DecisionHandler().createTargetDecision(
                    state, continuation.controllerId, continuation.sourceId, continuation.sourceName,
                    requirements = listOf(TargetRequirementInfo(
                        index = 0,
                        description = requirements[chosen.size].description,
                        minTargets = if (DependentTargetSelection.canStopAt(requirements, chosen.size)) 0 else 1,
                        maxTargets = 1,
                    )),
                    legalTargets = mapOf(0 to legal),
                    effectHint = continuation.description,
                    answer = continuation.copy(sequentialTargets = chosen),
                )
            }
            return resumeTriggeredAbility(
                state, continuation.copy(sequentialTargets = null),
                response.copy(selectedTargets = chosen.mapIndexed { index, id -> index to listOf(id) }.toMap()),
                checkForMore,
            )
        }

        // Build the chosen-targets list in requirement-slot order, keeping it PARALLEL to the
        // requirements that actually received a target. A declined "up to one" slot (empty list)
        // drops out of BOTH lists together, so a later target never shifts forward into an earlier
        // requirement's position. Without this, exiling only a creature with Don & Leo, Problem
        // Solvers (declining the "up to one artifact" slot) validated the creature against the
        // artifact requirement at resolution and fizzled with "all targets invalid" (CR 608.2b).
        //
        // Each kept requirement is also narrowed (`withCount`) to the number of targets actually
        // chosen for its slot: the downstream index walks (StackResolver.getRequirementForTargetIndex,
        // EffectContext.buildNamedTargets) advance by `count`, so a partially filled "up to two"
        // slot left at its declared max would absorb the next slot's target into its own range and
        // validate it against the wrong filter.
        val orderedSlots = response.selectedTargets.entries.sortedBy { it.key }
        val selectedTargets = mutableListOf<ChosenTarget>()
        val alignedRequirements = mutableListOf<TargetRequirement>()
        for ((slotIndex, targetIds) in orderedSlots) {
            if (targetIds.isEmpty()) continue
            targetIds.forEach { entityId -> selectedTargets.add(entityIdToChosenTarget(state, entityId)) }
            continuation.targetRequirements.getOrNull(slotIndex)
                ?.let { alignedRequirements.add(it.withCount(targetIds.size)) }
        }

        // Zero-target resolution path. Two cases:
        //  - `elseEffect != null`: the ability has explicit "...; otherwise, X" wording —
        //    swap to that effect (Conditional/elseEffect pattern).
        //  - `elseEffect == null`: the player declined an "up to N" optional target. The
        //    ability still resolves with no targets; non-target portions of the effect
        //    (e.g. Samwise's "Then the Ring tempts you" sibling) MUST still execute.
        //    Fall through to the regular put-on-stack path with `selectedTargets = []`.
        if (selectedTargets.isEmpty() && continuation.elseEffect != null) {
            val elseComponent = TriggeredAbilityOnStackComponent(
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                sourceBattlefieldTimestamp = continuation.sourceBattlefieldTimestamp,
                objectReferences = continuation.objectReferences,
                controllerId = continuation.controllerId,
                effect = continuation.elseEffect,
                description = continuation.description,
                abilityIdentity = continuation.abilityIdentity,
                triggerContext = continuation.triggerContext,
                xValue = continuation.triggerContext?.xValue,
                carriedPipeline = continuation.carriedPipeline,
                interveningIf = continuation.interveningIf
            )
            val stackResult = services.stackResolver.putTriggeredAbility(state, elseComponent, emptyList())
            if (stackResult.outcome !is Outcome.Done) return stackResult
            return checkForMore(stackResult.newState, stackResult.events.toList())
        }

        // A divided effect with two or more targets announces its division now, as the ability goes
        // on the stack (CR 603.3d applies CR 601.2d to triggered abilities). A dynamic total (Ureni —
        // "X = lands you control") is evaluated here so the player divides the correct amount.
        if (selectedTargets.size > 1) {
            val division = announcedDivision(continuation.effect)
            if (division != null) {
                val amountContext = com.wingedsheep.engine.handlers.EffectContext(
                    sourceId = continuation.sourceId,
                    objectReferences = continuation.objectReferences,
                    controllerId = continuation.controllerId,
                )
                val (total, prompt, minPerTarget) = when (division) {
                    is DividedDamageEffect -> {
                        val total = division.dynamicTotal?.let { amountEvaluator.evaluate(state, it, amountContext) }
                            ?: division.totalDamage
                        Triple(total, "Divide $total damage among ${selectedTargets.size} targets", 1)
                    }
                    is DistributeCountersAmongTargetsEffect -> {
                        val total = amountEvaluator.evaluate(state, division.totalCounters, amountContext)
                        Triple(
                            total,
                            "Distribute $total ${division.counterType.printed} " +
                                "counter${if (total != 1) "s" else ""} among ${selectedTargets.size} targets",
                            division.minPerTarget
                        )
                    }
                    else -> error("announcedDivision returned ${division::class.simpleName}")
                }
                return createTriggerDistributionDecision(
                    state, continuation, selectedTargets, alignedRequirements, total, prompt, minPerTarget
                )
            }
        }

        val abilityComponent = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            sourceBattlefieldTimestamp = continuation.sourceBattlefieldTimestamp,
            objectReferences = continuation.objectReferences,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description,
            abilityIdentity = continuation.abilityIdentity,
            // The whole record survives the target-selection pause — including a batch trigger's
            // captured objects, so a payoff that says "from among them" still finds them
            // (CR 603.2c; Kaya, Spirits' Justice is a batch trigger that also targets).
            triggerContext = continuation.triggerContext,
            xValue = continuation.triggerContext?.xValue,
            carriedPipeline = continuation.carriedPipeline,
            interveningIf = continuation.interveningIf
        )

        val stackResult = services.stackResolver.putTriggeredAbility(
            state, abilityComponent, selectedTargets, alignedRequirements
        )

        if (stackResult.outcome !is Outcome.Done) {
            return stackResult
        }

        return checkForMore(stackResult.newState, stackResult.events.toList())
    }

    /**
     * The effect whose division a triggered ability announces as it goes on the stack: the ability's
     * whole effect, or the one divided step of a sequence ("distribute three +1/+1 counters …, then
     * you gain life …"). Null when there is none, or more than one to tell apart — those divide at
     * resolution instead.
     */
    private fun announcedDivision(effect: Effect): Effect? {
        fun isDivided(e: Effect) = e is DividedDamageEffect || e is DistributeCountersAmongTargetsEffect
        return when {
            isDivided(effect) -> effect
            effect is CompositeEffect -> effect.effects.filter(::isDivided).singleOrNull()
            else -> null
        }
    }

    /**
     * After targets are selected for a triggered ability with a divided effect (damage or
     * counters), pause to ask how to divide it among the chosen targets.
     */
    private fun createTriggerDistributionDecision(
        state: GameState,
        continuation: TriggeredAbilityContinuation,
        selectedTargets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>,
        alignedRequirements: List<TargetRequirement>,
        total: Int,
        prompt: String,
        minPerTarget: Int,
    ): ExecutionResult {
        val sourceName = continuation.sourceId.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: continuation.sourceName

        val targetEntityIds = selectedTargets.map { target ->
            when (target) {
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Player -> target.playerId
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> target.entityId
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Card -> target.cardId
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> target.spellEntityId
            }
        }
        val question = { decisionId: String -> DistributeDecision(
            id = decisionId,
            playerId = continuation.controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.CASTING
            ),
            totalAmount = total,
            targets = targetEntityIds,
            minPerTarget = minPerTarget
        ) }

        val distributionContinuation = TriggerDamageDistributionContinuation(
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            sourceBattlefieldTimestamp = continuation.sourceBattlefieldTimestamp,
            objectReferences = continuation.objectReferences,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description,
            abilityIdentity = continuation.abilityIdentity,
            triggerContext = continuation.triggerContext,
            selectedTargets = selectedTargets,
            targetRequirements = alignedRequirements,
            totalDamage = total,
            interveningIf = continuation.interveningIf
        )

        return state.suspendForDecision(question, distributionContinuation, emptyList())
    }

    /**
     * Resume after the player divides a triggered ability's damage or counters.
     * Put the ability on the stack with the distribution locked in.
     */
    private fun resumeTriggerDamageDistribution(
        state: GameState,
        continuation: TriggerDamageDistributionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is DistributionResponse) {
            return ExecutionResult.error(state, "Expected distribution response for triggered ability damage")
        }

        val abilityComponent = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            sourceBattlefieldTimestamp = continuation.sourceBattlefieldTimestamp,
            objectReferences = continuation.objectReferences,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description,
            abilityIdentity = continuation.abilityIdentity,
            triggerContext = continuation.triggerContext,
            xValue = continuation.triggerContext?.xValue,
            damageDistribution = response.distribution,
            interveningIf = continuation.interveningIf
        )

        val stackResult = services.stackResolver.putTriggeredAbility(
            state, abilityComponent, continuation.selectedTargets, continuation.targetRequirements
        )

        if (stackResult.outcome !is Outcome.Done) {
            return stackResult
        }

        return checkForMore(stackResult.newState, stackResult.events.toList())
    }

    /**
     * Resume a trigger after its controller picked which opponent chooses its "… of an opponent's
     * choice" target (Mausoleum Turnkey). Raised only with two or more opponents; with one, the
     * processor pins the decider without asking.
     *
     * The answer is pinned onto the trigger and target selection is re-entered, so the target
     * decision itself is built by the same `processTargetedTrigger` path a trigger with no chooser
     * takes — the pin is the only difference. Cancelling drops the trigger rather than silently
     * handing the choice back to the controller: nothing has been paid or moved, and the trigger
     * has not yet reached the stack.
     */
    private fun resumeTriggerOpponentChooser(
        state: GameState,
        continuation: TriggerOpponentChooserContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response is CancelDecisionResponse) {
            return checkForMore(state, emptyList())
        }
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option response for trigger opponent chooser")
        }
        val deciderId = continuation.opponentIds.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid opponent choice for trigger target")

        val result = services.triggerProcessor.processTargetedTrigger(
            state,
            continuation.trigger.copy(opponentTargetChooserId = deciderId),
            continuation.targetRequirement
        )

        if (result.outcome is Outcome.Paused || result.outcome !is Outcome.Done) return result
        return checkForMore(result.newState, result.events.toList())
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
        val innerEffect = trigger.ability.effect.asMayDecide()?.then
            ?: return ExecutionResult.error(state, "May trigger continuation resumed on a non-may effect")

        val unwrappedAbility = trigger.ability.copy(effect = innerEffect)
        val unwrappedTrigger = trigger.copy(ability = unwrappedAbility)

        val result = services.triggerProcessor.processTargetedTrigger(state, unwrappedTrigger, continuation.targetRequirement)

        if (result.outcome is Outcome.Paused) {
            return result
        }

        if (result.outcome !is Outcome.Done) {
            return result
        }

        return checkForMore(result.newState, result.events.toList())
    }

    /**
     * Resume a [BatchMayTriggerContinuation] after the controller answers the batched may-question.
     * Fans the single [BatchYesNoResponse] back out over the run (see the continuation's docs):
     *
     *  - apply-to-all + no  → drop the entire run.
     *  - apply-to-all + yes → unwrap the may-gate on every trigger and process them as ordinary
     *    targeted triggers (each picks its own target via the existing per-trigger machinery).
     *  - peel-off           → resolve the first trigger per [BatchYesNoResponse.choice] and re-run
     *    the rest (which re-batch if still ≥ 2), enabling "this one, then ask me about the rest".
     *
     * Triggers after the run already wait in a [PendingTriggersContinuation] beneath this frame, so
     * any path that ends in `checkForMore` resumes them in order.
     */
    private fun resumeBatchMayTrigger(
        state: GameState,
        continuation: BatchMayTriggerContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is BatchYesNoResponse) {
            return ExecutionResult.error(state, "Expected batch yes/no response for may trigger")
        }

        val run = continuation.triggers

        if (response.applyToAll) {
            if (!response.choice) {
                // No to all — the whole run declines; trailing triggers handled by the frame beneath.
                return checkForMore(state, emptyList())
            }
            // Yes to all — unwrap each may and let the standard pipeline target them one by one.
            val unwrapped = run.mapNotNull(::unwrapMayTrigger)
            val result = services.triggerProcessor.processTriggers(state, unwrapped)
            if (result.outcome is Outcome.Paused || result.outcome !is Outcome.Done) return result
            return checkForMore(result.newState, result.events.toList())
        }

        // Peel one instance off; queue the rest so they re-batch/ask after it resolves.
        val first = run.first()
        val rest = run.drop(1)
        var workingState = state
        if (rest.isNotEmpty()) {
            workingState = workingState.pushContinuation(
                PendingTriggersContinuation(
                    remainingTriggers = rest
                )
            )
        }

        if (!response.choice) {
            // No to this one — drop it; the rest (and trailing triggers) resume beneath.
            return checkForMore(workingState, emptyList())
        }

        val unwrapped = unwrapMayTrigger(first)
            ?: return ExecutionResult.error(state, "Batch may continuation resumed on a non-may trigger")
        val result = services.triggerProcessor.processTriggers(workingState, listOf(unwrapped))
        if (result.outcome is Outcome.Paused || result.outcome !is Outcome.Done) return result
        return checkForMore(result.newState, result.events.toList())
    }

    /**
     * Strip the bare "may" gate off a trigger, returning a copy whose effect is the inner payoff so
     * the standard targeted-trigger path handles it. Mirrors [resumeMayTrigger]. Null if the trigger
     * is not a lowered may (should not happen for a batched trigger).
     */
    private fun unwrapMayTrigger(
        trigger: com.wingedsheep.engine.event.PendingTrigger
    ): com.wingedsheep.engine.event.PendingTrigger? {
        val inner = trigger.ability.effect.asMayDecide()?.then ?: return null
        return trigger.copy(ability = trigger.ability.copy(effect = inner))
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

        val context = continuation.effectContext
        val effectToExecute = if (response.choice) {
            continuation.effectIfYes
        } else {
            continuation.effectIfNo
        }

        if (effectToExecute == null) {
            return checkForMore(state, emptyList())
        }

        val result = services.effectExecutorRegistry.execute(state, effectToExecute, context).toExecutionResult()

        if (result.outcome is Outcome.Paused) {
            return result
        }

        return checkForMore(result.state, result.events.toList())
    }

    /**
     * Resume a [GatedEffect] after its gate's yes/no decision. The canonical unwind:
     * on "yes", run [GatedEffectContinuation.then] — for [Gate.MayPay], pay the cost first
     * (a `stopOnError` composite so an unpayable cost aborts the payoff, mirroring the former
     * OptionalCost behavior); on "no", run [GatedEffectContinuation.otherwise]. The locked
     * targets travel in the continuation's [EffectContext], so a targeted `then` resolves
     * against its trigger-time target.
     */
    private fun resumeGatedEffect(
        state: GameState,
        continuation: GatedEffectContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for gated effect")
        }

        val effectToExecute: Effect? = if (response.choice) {
            when (val gate = continuation.gate) {
                is Gate.MayDecide -> continuation.then
                is Gate.MayPay ->
                    CompositeEffect(listOf(gate.cost, continuation.then), stopOnError = true)
                // WhenCondition, DoAction, MayPayX and OnceEachTurn never push this (yes/no)
                // continuation — the first and fourth resolve synchronously in the executor, the
                // second via the action-drain GatedActionContinuation, the third via the
                // number-chooser MayPayXContinuation — so these branches are unreachable, present
                // only for exhaustiveness.
                is Gate.WhenCondition -> continuation.then
                is Gate.DoAction -> continuation.then
                is Gate.MayPayX -> continuation.then
                is Gate.OnceEachTurn -> continuation.then
            }
        } else {
            continuation.otherwise
        }

        if (effectToExecute == null) {
            return checkForMore(state, emptyList())
        }

        val branchResult = services.effectExecutorRegistry
            .execute(state, effectToExecute, continuation.effectContext)
        val result = branchResult.toExecutionResult()

        if (result.outcome is Outcome.Paused) {
            return result
        }

        // The branch's pipeline storage belongs to the frame beneath, exactly as a drained composite's
        // does in [resumeEffect]. A gate sits *inside* a composite ("you may discard your hand. Draw X
        // cards, where X is the number of cards discarded this way" — Balin, Loremaster), so the
        // later siblings are the readers of whatever the `then` branch gathered. Dropping it here made
        // the same card work or not depending on whether the may-question happened to be asked: an
        // auto-answered or skipped gate runs `then` synchronously and keeps its storage, while a
        // prompted one lost it and the sibling read an unset variable as 0.
        val stateWithCollections = exposeCollectionsToNextFrame(
            result.state,
            continuation.effectContext.pipeline.storedCollections + branchResult.updatedCollections,
            continuation.effectContext.pipeline.storedNumbers + branchResult.updatedStoredNumbers,
            continuation.effectContext.pipeline.chosenValues + branchResult.updatedChosenValues,
        )

        return checkForMore(stateWithCollections, result.events.toList())
    }

    private fun resumeMayRevealCardFromHand(
        state: GameState,
        continuation: MayRevealCardFromHandContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for may-reveal-from-hand")
        }

        val chosenCardId = response.selectedCards.firstOrNull()

        if (chosenCardId == null) {
            // Player declined to reveal — fall through to the "otherwise" branch.
            val otherwise = continuation.otherwise
                ?: return checkForMore(state, emptyList())
            val result = services.effectExecutorRegistry
                .execute(state, otherwise, continuation.effectContext)
                .toExecutionResult()
            return if (result.outcome is Outcome.Paused) result
            else checkForMore(result.state, result.events.toList())
        }

        // Player picked a card — emit the public reveal. The reveal itself is the
        // entire payoff of the MayReveal atom; any rider effect lives in `otherwise`.
        val (revealedState, revealEvent) = com.wingedsheep.engine.handlers.effects.composite
            .MayRevealCardFromHandEffectExecutor.emitReveal(
                state, continuation.revealerId, chosenCardId, continuation.sourceName,
            )
        return checkForMore(revealedState, listOf(revealEvent))
    }

    private fun resumeBehold(
        state: GameState,
        continuation: BeholdContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for behold")
        }

        val chosenId = response.selectedCards.firstOrNull()
        if (chosenId == null) {
            // Player declined to behold — the "if you do" payoff doesn't run; the "if you don't"
            // rider (Theorist's Sanctum entering tapped) does.
            val otherwise = continuation.otherwise
                ?: return checkForMore(state, emptyList())
            val result = services.effectExecutorRegistry
                .execute(state, otherwise, continuation.effectContext)
                .toExecutionResult()
            return if (result.outcome is Outcome.Paused) result
            else checkForMore(result.state, result.events.toList())
        }

        // If the beheld object was a card in hand, reveal it publicly. Battlefield permanents
        // are chosen, not revealed.
        var currentState = state
        val events = mutableListOf<GameEvent>()
        if (chosenId in continuation.handOptionIds) {
            val (revealedState, revealEvent) = com.wingedsheep.engine.handlers.effects.composite
                .MayRevealCardFromHandEffectExecutor.emitReveal(
                    currentState, continuation.beholderId, chosenId, continuation.sourceName,
                )
            currentState = revealedState
            events += revealEvent
        }

        val ifBeheld = continuation.ifBeheld
            ?: return checkForMore(currentState, events)

        val result = services.effectExecutorRegistry
            .execute(currentState, ifBeheld, continuation.effectContext)
            .toExecutionResult()
        if (result.outcome is Outcome.Paused) return result
        return checkForMore(result.state, events + result.events.toList())
    }

}
