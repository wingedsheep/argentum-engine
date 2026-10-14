package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.chain.ChainCopyExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

class ChainSpellContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule, AutoResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ChainCopyDecisionContinuation::class, ::resumeChainCopyDecision),
        resumer(ChainCopyCostContinuation::class, ::resumeChainCopyCost),
        resumer(ChainCopyTargetContinuation::class, ::resumeChainCopyTarget)
    )

    override fun autoResumers(): List<AutoResumer<*>> = listOf(
        autoResumer(ChainCopyAfterActionContinuation::class) { state, continuation, events, checkForMore ->
            offerChainCopy(
                state, events.toMutableList(),
                continuation.recipientPlayerId, continuation.effect, continuation.sourceId, checkForMore
            )
        }
    )

    // =========================================================================
    // 2. Yes/No Decision (do you want to copy?)
    // =========================================================================

    fun resumeChainCopyDecision(
        state: GameState,
        continuation: ChainCopyDecisionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for chain copy decision")
        }

        if (!response.choice) {
            return checkForMore(state, emptyList())
        }

        val effect = continuation.effect
        val controllerId = continuation.copyControllerId
        val copyCost = effect.copyCost

        // If there's no cost, go directly to target selection
        if (copyCost == null) {
            return presentTargetSelection(
                state, emptyList(), controllerId, effect, continuation.sourceId, checkForMore
            )
        }

        // Present cost payment based on PayCost type
        return when (val atom = (copyCost as? PayCost.Atom)?.atom) {
            is CostAtom.Sacrifice -> {
                val candidates = ChainCopyExecutor.findMatchingPermanents(state, controllerId, atom.filter, predicateEvaluator = services.predicateEvaluator)
                if (candidates.size < atom.count) {
                    return checkForMore(state, emptyList())
                }
                if (candidates.size == atom.count) {
                    // Auto-pay when exactly enough resources
                    return payCostAndPresentTargets(
                        state, controllerId, copyCost, candidates,
                        effect, continuation.sourceId, checkForMore
                    )
                }
                presentCostSelection(
                    state, controllerId, effect, continuation.sourceId, candidates,
                    "Choose a ${atom.filter.description} to sacrifice for the copy of ${spellNameOf(state, continuation.sourceId)}",
                    useTargetingUI = true
                )
            }
            is CostAtom.Discard -> {
                val handZone = ZoneKey(controllerId, Zone.HAND)
                val hand = state.getZone(handZone)
                if (hand.size < atom.count) {
                    return checkForMore(state, emptyList())
                }
                presentCostSelection(
                    state, controllerId, effect, continuation.sourceId, hand,
                    "Choose a card to discard for ${spellNameOf(state, continuation.sourceId)}",
                    useTargetingUI = false
                )
            }
            else -> {
                // Unsupported cost type — skip
                presentTargetSelection(
                    state, emptyList(), controllerId, effect, continuation.sourceId, checkForMore
                )
            }
        }
    }

    // =========================================================================
    // 3. Cost Payment (sacrifice / discard)
    // =========================================================================

    fun resumeChainCopyCost(
        state: GameState,
        continuation: ChainCopyCostContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for chain copy cost")
        }

        val selectedCards = response.selectedCards
        if (selectedCards.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        for (card in selectedCards) {
            if (card !in continuation.candidateOptions) {
                return ExecutionResult.error(state, "Invalid chain copy cost selection: $card")
            }
        }

        return payCostAndPresentTargets(
            state, continuation.copyControllerId, continuation.effect.copyCost!!, selectedCards,
            continuation.effect, continuation.sourceId, checkForMore
        )
    }

    // =========================================================================
    // 4. Target Selection (choose target for the copy)
    // =========================================================================

    fun resumeChainCopyTarget(
        state: GameState,
        continuation: ChainCopyTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for chain copy target")
        }

        val selectedTarget = response.selectedCards.firstOrNull()
            ?: return checkForMore(state, emptyList())

        if (selectedTarget !in continuation.candidateTargets) {
            return ExecutionResult.error(state, "Invalid chain copy target: $selectedTarget")
        }

        val effect = continuation.effect

        // Determine ChosenTarget type based on copy target requirement
        val chosenTarget = when (effect.copyTargetRequirement) {
            is TargetPlayer -> ChosenTarget.Player(selectedTarget)
            is AnyTarget -> {
                val entity = state.getEntity(selectedTarget)
                if (entity?.get<ControllerComponent>() != null) {
                    ChosenTarget.Permanent(selectedTarget)
                } else {
                    ChosenTarget.Player(selectedTarget)
                }
            }
            else -> ChosenTarget.Permanent(selectedTarget)
        }

        // Build the copy effect with BoundVariable target — update both the outer
        // ChainCopyEffect.target and the inner action's target so the copy resolves
        // against the new binding. When the card named its target, the copy's target is bound
        // under that same name, so every read of the handle inside the action — including a
        // player-typed one (`handle.asPlayer`) that [replaceActionTarget] can't rewrite — sees
        // the new target.
        val bindingName = (effect.target as? EffectTarget.BoundVariable)?.name ?: "chainTarget"
        val newTarget = EffectTarget.BoundVariable(bindingName)
        val updatedAction = replaceActionTarget(effect.action, newTarget)
        val copyEffect = effect.copy(target = newTarget, action = updatedAction)

        // Build the target requirement with the binding id
        val copyTargetReq = when (val req = effect.copyTargetRequirement) {
            is TargetObject -> req.copy(id = bindingName)
            is TargetPlayer -> req.copy(id = bindingName)
            is AnyTarget -> req.copy(id = bindingName)
            else -> req
        }

        // 700.2g: propagate chosen modes from the source spell if it was modal.
        // Targets are re-chosen via the chain target flow, so modeTargetsOrdered
        // is not inherited (the copy controller picks a new target above).
        val sourceSpell = continuation.sourceId?.let { state.getEntity(it)?.get<SpellOnStackComponent>() }
        val (effectiveState, sourceId) = if (continuation.sourceId != null) {
            state to continuation.sourceId
        } else {
            val (id, s) = state.newEntity()
            s to id
        }
        val ability = TriggeredAbilityOnStackComponent(
            sourceId = sourceId,
            objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(captured = true, origin = state.objectRef(sourceId), source = state.objectRef(sourceId)),
            sourceName = spellNameOf(state, continuation.sourceId),
            controllerId = continuation.copyControllerId,
            effect = copyEffect,
            description = "Copy of ${spellNameOf(state, continuation.sourceId)}",
            chosenModes = sourceSpell?.chosenModes ?: emptyList(),
            modeTargetRequirements = sourceSpell?.modeTargetRequirements ?: emptyMap()
        )

        val targets = listOf(chosenTarget)
        val targetRequirements = listOf(copyTargetReq)

        val putResult = services.stackResolver.putTriggeredAbility(effectiveState, ability, targets, targetRequirements)

        if (putResult.outcome !is Outcome.Done) {
            return putResult
        }

        return checkForMore(putResult.state, putResult.events.toList())
    }

    // =========================================================================
    // Shared helpers
    // =========================================================================

    /** The chain spell's printed name, read off its card — a copy keeps it, so it survives resolution. */
    private fun spellNameOf(state: GameState, sourceId: EntityId?): String =
        sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "this spell"

    private fun offerChainCopy(
        state: GameState,
        events: MutableList<GameEvent>,
        recipientPlayerId: EntityId,
        effect: ChainCopyEffect,
        sourceId: EntityId?,
        checkForMore: CheckForMore
    ): ExecutionResult {
        // Check cost prerequisites
        if (!ChainCopyExecutor.canPayCopyCost(state, recipientPlayerId, effect.copyCost, predicateEvaluator = services.predicateEvaluator)) {
            return checkForMore(state, events)
        }

        // Check if there are valid targets for a potential copy
        val legalTargets = services.targetFinder.findLegalTargets(
            state, effect.copyTargetRequirement, recipientPlayerId, sourceId
        )
        if (legalTargets.isEmpty()) {
            return checkForMore(state, events)
        }


        val copyCost = effect.copyCost
        val prompt = if (copyCost == null) {
            "Copy ${spellNameOf(state, sourceId)} and choose a new target?"
        } else {
            "${copyCost.description.replaceFirstChar { it.uppercase() }} to copy ${spellNameOf(state, sourceId)}?"
        }

        val (yesText, noText) = if (copyCost == null) {
            "Copy" to "Decline"
        } else {
            copyCost.description.replaceFirstChar { it.uppercase() } to "Decline"
        }

        val question = { decisionId: String -> YesNoDecision(
            id = decisionId,
            playerId = recipientPlayerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = spellNameOf(state, sourceId),
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = yesText,
            noText = noText
        ) }

        val copyContinuation = ChainCopyDecisionContinuation(
            effect = effect,
            copyControllerId = recipientPlayerId,
            sourceId = sourceId
        )

        return state.suspendForDecision(
            question = question,
            answer = copyContinuation,
            events = events,
        )
    }

    private fun presentCostSelection(
        state: GameState,
        controllerId: EntityId,
        effect: ChainCopyEffect,
        sourceId: EntityId?,
        options: List<EntityId>,
        prompt: String,
        useTargetingUI: Boolean
    ): ExecutionResult {
        val question = { decisionId: String -> SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = spellNameOf(state, sourceId),
                phase = DecisionPhase.RESOLUTION
            ),
            options = options,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = useTargetingUI
        ) }

        val costContinuation = ChainCopyCostContinuation(
            effect = effect,
            copyControllerId = controllerId,
            sourceId = sourceId,
            candidateOptions = options
        )

        return state.suspendForDecision(
            question = question,
            answer = costContinuation,
            events = emptyList(),
        )
    }

    private fun presentTargetSelection(
        state: GameState,
        priorEvents: List<GameEvent>,
        controllerId: EntityId,
        effect: ChainCopyEffect,
        sourceId: EntityId?,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val legalTargets = services.targetFinder.findLegalTargets(
            state, effect.copyTargetRequirement, controllerId, sourceId
        )

        if (legalTargets.isEmpty()) {
            return checkForMore(state, priorEvents)
        }

        val question = { decisionId: String -> SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a target for the copy of ${spellNameOf(state, sourceId)}",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = spellNameOf(state, sourceId),
                phase = DecisionPhase.RESOLUTION
            ),
            options = legalTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        ) }

        val targetContinuation = ChainCopyTargetContinuation(
            effect = effect,
            copyControllerId = controllerId,
            sourceId = sourceId,
            candidateTargets = legalTargets
        )

        return state.suspendForDecision(
            question = question,
            answer = targetContinuation,
            events = priorEvents,
        )
    }

    /**
     * Pay the selected cost resources, then present target selection for the copy.
     */
    private fun payCostAndPresentTargets(
        state: GameState,
        controllerId: EntityId,
        cost: PayCost,
        selectedCards: List<EntityId>,
        effect: ChainCopyEffect,
        sourceId: EntityId?,
        checkForMore: CheckForMore
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        when ((cost as? PayCost.Atom)?.atom) {
            is CostAtom.Sacrifice -> {
                // Shared sacrifice path — the full zone transition (LKI, cleanup, graveyard turn
                // history), as CostPaymentService does for every other sacrifice cost.
                if (selectedCards.isNotEmpty()) {
                    val names = selectedCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Unknown" }
                    events.add(PermanentsSacrificedEvent(controllerId, selectedCards, names))
                    newState = ZoneTransitionService.trackPermanentSacrifice(newState, selectedCards, controllerId)
                }
                for (cardId in selectedCards) {
                    val transition = services.zones.moveToZone(newState, cardId, Zone.GRAVEYARD)
                    newState = transition.state
                    events.addAll(transition.events)
                }
            }
            is CostAtom.Discard -> {
                // Shared discard path — a card-intrinsic discard replacement (madness,
                // CR 702.35a) applies to a card discarded to pay a chained spell's cost too.
                val discardResult = services.zones.discardCards(newState, controllerId, selectedCards)
                newState = discardResult.state
                events.addAll(discardResult.events)
            }
            else -> { /* Unsupported cost type — no-op */ }
        }

        return presentTargetSelection(
            newState, events, controllerId, effect, sourceId, checkForMore
        )
    }

    /**
     * Replace the target in a known inner action effect so the chain copy
     * resolves against the new target binding.
     *
     * CompositeEffect (discard pipeline) converts targets to Player enums at
     * construction time, so it doesn't need updating.
     */
    private fun replaceActionTarget(action: Effect, newTarget: EffectTarget): Effect {
        return when (action) {
            is MoveToZoneEffect -> action.copy(target = newTarget)
            is DealDamageEffect -> action.copy(target = newTarget)
            is PreventDamageEffect -> action.copy(target = newTarget)
            else -> action
        }
    }
}
