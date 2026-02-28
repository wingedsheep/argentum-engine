package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ChainCopyCost
import com.wingedsheep.sdk.scripting.effects.ChainCopyEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

class ChainSpellContinuationResumer(
    private val ctx: ContinuationContext
) {

    // =========================================================================
    // 1. Primary Discard (Chain of Smog — card selection before chain offer)
    // =========================================================================

    fun resumeChainCopyPrimaryDiscard(
        state: GameState,
        continuation: ChainCopyPrimaryDiscardContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for chain primary discard")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        var newState = state
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val discardNames = selectedCards.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
        val events = mutableListOf<GameEvent>(
            CardsDiscardedEvent(playerId, selectedCards, discardNames)
        )

        // Now offer the chain copy
        return offerChainCopy(newState, events, playerId, continuation.effect, continuation.sourceId, checkForMore)
    }

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

        // If there's a cost, present cost payment
        return when (effect.copyCost) {
            is ChainCopyCost.NoCost -> presentTargetSelection(
                state, emptyList(), controllerId, effect, continuation.sourceId, checkForMore
            )
            is ChainCopyCost.SacrificeALand -> {
                val controllerLands = findControllerLands(state, controllerId)
                if (controllerLands.isEmpty()) {
                    return checkForMore(state, emptyList())
                }
                if (controllerLands.size == 1) {
                    // Auto-sacrifice the only land
                    return sacrificeLandAndPresentTargets(
                        state, controllerId, controllerLands.first(),
                        effect, continuation.sourceId, checkForMore
                    )
                }
                presentCostSelection(state, controllerId, effect, continuation.sourceId, controllerLands,
                    "Choose a land to sacrifice for the copy of ${effect.spellName}")
            }
            is ChainCopyCost.DiscardACard -> {
                val handZone = ZoneKey(controllerId, Zone.HAND)
                val hand = state.getZone(handZone)
                if (hand.isEmpty()) {
                    return checkForMore(state, emptyList())
                }
                presentCostSelection(state, controllerId, effect, continuation.sourceId, hand,
                    "Choose a card to discard for ${effect.spellName}")
            }
        }
    }

    // =========================================================================
    // 3. Cost Payment (sacrifice land / discard card)
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

        val selectedCard = response.selectedCards.firstOrNull()
            ?: return checkForMore(state, emptyList())

        if (selectedCard !in continuation.candidateOptions) {
            return ExecutionResult.error(state, "Invalid chain copy cost selection: $selectedCard")
        }

        return when (continuation.effect.copyCost) {
            is ChainCopyCost.SacrificeALand -> {
                sacrificeLandAndPresentTargets(
                    state, continuation.copyControllerId, selectedCard,
                    continuation.effect, continuation.sourceId, checkForMore
                )
            }
            is ChainCopyCost.DiscardACard -> {
                val playerId = continuation.copyControllerId
                val handZone = ZoneKey(playerId, Zone.HAND)
                val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

                var newState = state.removeFromZone(handZone, selectedCard)
                newState = newState.addToZone(graveyardZone, selectedCard)

                val selectedCardName = state.getEntity(selectedCard)?.get<CardComponent>()?.name ?: "Card"
                val events = mutableListOf<GameEvent>(
                    CardsDiscardedEvent(playerId, listOf(selectedCard), listOf(selectedCardName))
                )

                presentTargetSelection(
                    newState, events, playerId, continuation.effect, continuation.sourceId, checkForMore
                )
            }
            is ChainCopyCost.NoCost -> {
                // Should not reach here, but handle gracefully
                presentTargetSelection(
                    state, emptyList(), continuation.copyControllerId,
                    continuation.effect, continuation.sourceId, checkForMore
                )
            }
        }
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

        // Build the copy effect with BoundVariable target
        val copyEffect = effect.copy(target = EffectTarget.BoundVariable("chainTarget"))

        // Build the target requirement with the binding id
        val copyTargetReq = when (val req = effect.copyTargetRequirement) {
            is TargetObject -> req.copy(id = "chainTarget")
            is TargetPlayer -> req.copy(id = "chainTarget")
            is AnyTarget -> req.copy(id = "chainTarget")
            else -> req
        }

        val ability = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId ?: EntityId.generate(),
            sourceName = effect.spellName,
            controllerId = continuation.copyControllerId,
            effect = copyEffect,
            description = "Copy of ${effect.spellName}"
        )

        val targets = listOf(chosenTarget)
        val targetRequirements = listOf(copyTargetReq)

        val putResult = ctx.stackResolver.putTriggeredAbility(state, ability, targets, targetRequirements)

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
        // Check if there are valid targets for a potential copy
        val legalTargets = ctx.targetFinder.findLegalTargets(
            state, effect.copyTargetRequirement, recipientPlayerId, sourceId
        )
        if (legalTargets.isEmpty()) {
            return checkForMore(state, events)
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = YesNoDecision(
            id = decisionId,
            playerId = recipientPlayerId,
            prompt = "Copy ${effect.spellName} and choose a new target?",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = effect.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Copy",
            noText = "Decline"
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
        prompt: String
    ): ExecutionResult {
        val decisionId = java.util.UUID.randomUUID().toString()
        val useTargetingUI = effect.copyCost is ChainCopyCost.SacrificeALand
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
        val legalTargets = ctx.targetFinder.findLegalTargets(
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

    private fun sacrificeLandAndPresentTargets(
        state: GameState,
        controllerId: EntityId,
        landId: EntityId,
        effect: ChainCopyEffect,
        sourceId: EntityId?,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val container = state.getEntity(landId) ?: return checkForMore(state, emptyList())
        val cardComponent = container.get<CardComponent>()
            ?: return checkForMore(state, emptyList())

        val currentZone = state.zones.entries.find { (_, cards) -> landId in cards }?.key
            ?: return checkForMore(state, emptyList())

        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: controllerId
        val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

        var newState = state.removeFromZone(currentZone, landId)
        newState = newState.addToZone(graveyardZone, landId)

        val sacrificeEvents = mutableListOf<GameEvent>(
            PermanentsSacrificedEvent(controllerId, listOf(landId), listOf(cardComponent.name)),
            ZoneChangeEvent(
                entityId = landId,
                entityName = cardComponent.name,
                fromZone = Zone.BATTLEFIELD,
                toZone = Zone.GRAVEYARD,
                ownerId = ownerId
            )
        )

        return presentTargetSelection(
            newState, sacrificeEvents, controllerId, effect, sourceId, checkForMore
        )
    }

    fun findControllerLands(state: GameState, controllerId: EntityId): List<EntityId> {
        val projected = StateProjector().project(state)
        val controlledPermanents = projected.getBattlefieldControlledBy(controllerId)
        val predicateEvaluator = PredicateEvaluator()
        val context = PredicateContext(controllerId = controllerId)
        return controlledPermanents.filter { permanentId ->
            predicateEvaluator.matchesWithProjection(state, projected, permanentId, GameObjectFilter.Land, context)
        }
    }
}
