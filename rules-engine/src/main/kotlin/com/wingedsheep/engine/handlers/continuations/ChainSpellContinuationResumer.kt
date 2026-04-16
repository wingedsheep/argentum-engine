package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.chain.ChainCopyExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
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
        return when (copyCost) {
            is PayCost.Sacrifice -> {
                val candidates = ChainCopyExecutor.findMatchingPermanents(state, controllerId, copyCost.filter)
                if (candidates.size < copyCost.count) {
                    return checkForMore(state, emptyList())
                }
                if (candidates.size == copyCost.count) {
                    // Auto-pay when exactly enough resources
                    return payCostAndPresentTargets(
                        state, controllerId, copyCost, candidates,
                        effect, continuation.sourceId, checkForMore
                    )
                }
                presentCostSelection(
                    state, controllerId, effect, continuation.sourceId, candidates,
                    "Choose a ${copyCost.filter.description} to sacrifice for the copy of ${effect.spellName}",
                    useTargetingUI = true
                )
            }
            is PayCost.Discard -> {
                val handZone = ZoneKey(controllerId, Zone.HAND)
                val hand = state.getZone(handZone)
                if (hand.size < copyCost.count) {
                    return checkForMore(state, emptyList())
                }
                presentCostSelection(
                    state, controllerId, effect, continuation.sourceId, hand,
                    "Choose a card to discard for ${effect.spellName}",
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
        // against the new binding.
        val newTarget = EffectTarget.BoundVariable("chainTarget")
        val updatedAction = replaceActionTarget(effect.action, newTarget)
        val copyEffect = effect.copy(target = newTarget, action = updatedAction)

        // Build the target requirement with the binding id
        val copyTargetReq = when (val req = effect.copyTargetRequirement) {
            is TargetObject -> req.copy(id = "chainTarget")
            is TargetPlayer -> req.copy(id = "chainTarget")
            is AnyTarget -> req.copy(id = "chainTarget")
            else -> req
        }

        // 700.2g: propagate chosen modes from the source spell if it was modal.
        // Targets are re-chosen via the chain target flow, so modeTargetsOrdered
        // is not inherited (the copy controller picks a new target above).
        val sourceSpell = continuation.sourceId?.let { state.getEntity(it)?.get<SpellOnStackComponent>() }
        val ability = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId ?: EntityId.generate(),
            sourceName = effect.spellName,
            controllerId = continuation.copyControllerId,
            effect = copyEffect,
            description = "Copy of ${effect.spellName}",
            chosenModes = sourceSpell?.chosenModes ?: emptyList(),
            modeTargetRequirements = sourceSpell?.modeTargetRequirements ?: emptyMap()
        )

        val targets = listOf(chosenTarget)
        val targetRequirements = listOf(copyTargetReq)

        val putResult = services.stackResolver.putTriggeredAbility(state, ability, targets, targetRequirements)

        if (!putResult.isSuccess) {
            return putResult
        }

        return checkForMore(putResult.state, putResult.events.toList())
    }

    // =========================================================================
    // Shared helpers
    // =========================================================================

    private fun offerChainCopy(
        state: GameState,
        events: MutableList<GameEvent>,
        recipientPlayerId: EntityId,
        effect: ChainCopyEffect,
        sourceId: EntityId?,
        checkForMore: CheckForMore
    ): ExecutionResult {
        // Check cost prerequisites
        if (!ChainCopyExecutor.canPayCopyCost(state, recipientPlayerId, effect.copyCost)) {
            return checkForMore(state, events)
        }

        // Check if there are valid targets for a potential copy
        val legalTargets = services.targetFinder.findLegalTargets(
            state, effect.copyTargetRequirement, recipientPlayerId, sourceId
        )
        if (legalTargets.isEmpty()) {
            return checkForMore(state, events)
        }

        val decisionId = java.util.UUID.randomUUID().toString()

        val copyCost = effect.copyCost
        val prompt = if (copyCost == null) {
            "Copy ${effect.spellName} and choose a new target?"
        } else {
            "${copyCost.description.replaceFirstChar { it.uppercase() }} to copy ${effect.spellName}?"
        }

        val (yesText, noText) = if (copyCost == null) {
            "Copy" to "Decline"
        } else {
            copyCost.description.replaceFirstChar { it.uppercase() } to "Decline"
        }

        val decision = YesNoDecision(
            id = decisionId,
            playerId = recipientPlayerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = effect.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = yesText,
            noText = noText
        )

        val copyContinuation = ChainCopyDecisionContinuation(
            decisionId = decisionId,
            effect = effect,
            copyControllerId = recipientPlayerId,
            sourceId = sourceId
        )

        val newState = state.withPendingDecision(decision).pushContinuation(copyContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            events + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = recipientPlayerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
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
        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = effect.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = options,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = useTargetingUI
        )

        val costContinuation = ChainCopyCostContinuation(
            decisionId = decisionId,
            effect = effect,
            copyControllerId = controllerId,
            sourceId = sourceId,
            candidateOptions = options
        )

        val newState = state.withPendingDecision(decision).pushContinuation(costContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
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

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a target for the copy of ${effect.spellName}",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = effect.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = legalTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val targetContinuation = ChainCopyTargetContinuation(
            decisionId = decisionId,
            effect = effect,
            copyControllerId = controllerId,
            sourceId = sourceId,
            candidateTargets = legalTargets
        )

        val newState = state.withPendingDecision(decision).pushContinuation(targetContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            priorEvents + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
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

        when (cost) {
            is PayCost.Sacrifice -> {
                for (cardId in selectedCards) {
                    val container = newState.getEntity(cardId) ?: continue
                    val cardComponent = container.get<CardComponent>() ?: continue
                    val currentZone = newState.zones.entries.find { (_, cards) -> cardId in cards }?.key
                        ?: continue
                    val ownerId = container.get<OwnerComponent>()?.playerId
                        ?: cardComponent.ownerId
                        ?: controllerId
                    val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

                    newState = newState.removeFromZone(currentZone, cardId)
                    newState = newState.addToZone(graveyardZone, cardId)

                    events.add(PermanentsSacrificedEvent(controllerId, listOf(cardId), listOf(cardComponent.name)))
                    events.add(ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardComponent.name,
                        fromZone = Zone.BATTLEFIELD,
                        toZone = Zone.GRAVEYARD,
                        ownerId = ownerId
                    ))
                }
            }
            is PayCost.Discard -> {
                val handZone = ZoneKey(controllerId, Zone.HAND)
                val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)

                for (cardId in selectedCards) {
                    newState = newState.removeFromZone(handZone, cardId)
                    newState = newState.addToZone(graveyardZone, cardId)
                }

                val cardNames = selectedCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
                events.add(CardsDiscardedEvent(controllerId, selectedCards, cardNames))
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
