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
import com.wingedsheep.sdk.scripting.effects.BounceAndChainCopyEffect
import com.wingedsheep.sdk.scripting.effects.DamageAndChainCopyEffect
import com.wingedsheep.sdk.scripting.effects.DestroyAndChainCopyEffect
import com.wingedsheep.sdk.scripting.effects.DiscardAndChainCopyEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageAndChainCopyEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

class ChainSpellContinuationResumer(
    private val ctx: ContinuationContext
) {

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

        val requirement = com.wingedsheep.sdk.scripting.targets.TargetPermanent(filter = continuation.targetFilter)
        val legalTargets = ctx.targetFinder.findLegalTargets(
            state, requirement, continuation.targetControllerId, continuation.sourceId
        )

        if (legalTargets.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = continuation.targetControllerId,
            prompt = "Choose a target for the copy of ${continuation.spellName}",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = legalTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val targetContinuation = ChainCopyTargetContinuation(
            decisionId = decisionId,
            copyControllerId = continuation.targetControllerId,
            targetFilter = continuation.targetFilter,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId,
            candidateTargets = legalTargets
        )

        val newState = state.withPendingDecision(decision).pushContinuation(targetContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.targetControllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

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

        val ability = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId ?: EntityId.generate(),
            sourceName = continuation.spellName,
            controllerId = continuation.copyControllerId,
            effect = DestroyAndChainCopyEffect(
                target = EffectTarget.ContextTarget(0),
                targetFilter = continuation.targetFilter,
                spellName = continuation.spellName
            ),
            description = "Copy of ${continuation.spellName}"
        )

        val targets = listOf(ChosenTarget.Permanent(selectedTarget))
        val targetRequirements = listOf(
            com.wingedsheep.sdk.scripting.targets.TargetPermanent(filter = continuation.targetFilter)
        )

        val putResult = ctx.stackResolver.putTriggeredAbility(state, ability, targets, targetRequirements)

        if (!putResult.isSuccess) {
            return putResult
        }

        return checkForMore(putResult.state, putResult.events.toList())
    }

    fun resumeBounceChainCopyDecision(
        state: GameState,
        continuation: BounceChainCopyDecisionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for bounce chain copy decision")
        }

        if (!response.choice) {
            return checkForMore(state, emptyList())
        }

        val controllerLands = findControllerLands(state, continuation.targetControllerId)

        if (controllerLands.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        if (controllerLands.size == 1) {
            return sacrificeLandAndPresentTargets(
                state, continuation.targetControllerId, controllerLands.first(),
                continuation.targetFilter, continuation.spellName, continuation.sourceId,
                checkForMore
            )
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = continuation.targetControllerId,
            prompt = "Choose a land to sacrifice for the copy of ${continuation.spellName}",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = controllerLands,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val landContinuation = BounceChainCopyLandContinuation(
            decisionId = decisionId,
            copyControllerId = continuation.targetControllerId,
            targetFilter = continuation.targetFilter,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId,
            candidateLands = controllerLands
        )

        val newState = state.withPendingDecision(decision).pushContinuation(landContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.targetControllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    fun resumeBounceChainCopyLand(
        state: GameState,
        continuation: BounceChainCopyLandContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for bounce chain land sacrifice")
        }

        val selectedLand = response.selectedCards.firstOrNull()
            ?: return checkForMore(state, emptyList())

        if (selectedLand !in continuation.candidateLands) {
            return ExecutionResult.error(state, "Invalid land for bounce chain sacrifice: $selectedLand")
        }

        return sacrificeLandAndPresentTargets(
            state, continuation.copyControllerId, selectedLand,
            continuation.targetFilter, continuation.spellName, continuation.sourceId,
            checkForMore
        )
    }

    fun resumeBounceChainCopyTarget(
        state: GameState,
        continuation: BounceChainCopyTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for bounce chain copy target")
        }

        val selectedTarget = response.selectedCards.firstOrNull()
            ?: return checkForMore(state, emptyList())

        if (selectedTarget !in continuation.candidateTargets) {
            return ExecutionResult.error(state, "Invalid bounce chain copy target: $selectedTarget")
        }

        val ability = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId ?: EntityId.generate(),
            sourceName = continuation.spellName,
            controllerId = continuation.copyControllerId,
            effect = BounceAndChainCopyEffect(
                target = EffectTarget.ContextTarget(0),
                targetFilter = continuation.targetFilter,
                spellName = continuation.spellName
            ),
            description = "Copy of ${continuation.spellName}"
        )

        val targets = listOf(ChosenTarget.Permanent(selectedTarget))
        val targetRequirements = listOf(
            com.wingedsheep.sdk.scripting.targets.TargetPermanent(filter = continuation.targetFilter)
        )

        val putResult = ctx.stackResolver.putTriggeredAbility(state, ability, targets, targetRequirements)

        if (!putResult.isSuccess) {
            return putResult
        }

        return checkForMore(putResult.state, putResult.events.toList())
    }

    fun resumeDiscardForChain(
        state: GameState,
        continuation: DiscardForChainContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for discard chain")
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

        val requirement = com.wingedsheep.sdk.scripting.targets.TargetPlayer()
        val legalTargets = ctx.targetFinder.findLegalTargets(
            newState, requirement, playerId, continuation.sourceId
        )

        if (legalTargets.isEmpty()) {
            return checkForMore(newState, events)
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Copy ${continuation.spellName} and choose a new target?",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Copy",
            noText = "Decline"
        )

        val copyContinuation = DiscardChainCopyDecisionContinuation(
            decisionId = decisionId,
            targetPlayerId = playerId,
            count = continuation.count,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId
        )

        newState = newState.withPendingDecision(decision).pushContinuation(copyContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            events + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }

    fun resumeDiscardChainCopyDecision(
        state: GameState,
        continuation: DiscardChainCopyDecisionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for discard chain copy decision")
        }

        if (!response.choice) {
            return checkForMore(state, emptyList())
        }

        val requirement = com.wingedsheep.sdk.scripting.targets.TargetPlayer()
        val legalTargets = ctx.targetFinder.findLegalTargets(
            state, requirement, continuation.targetPlayerId, continuation.sourceId
        )

        if (legalTargets.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = continuation.targetPlayerId,
            prompt = "Choose a target player for the copy of ${continuation.spellName}",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = legalTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val targetContinuation = DiscardChainCopyTargetContinuation(
            decisionId = decisionId,
            copyControllerId = continuation.targetPlayerId,
            count = continuation.count,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId,
            candidateTargets = legalTargets
        )

        val newState = state.withPendingDecision(decision).pushContinuation(targetContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.targetPlayerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    fun resumeDiscardChainCopyTarget(
        state: GameState,
        continuation: DiscardChainCopyTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for discard chain copy target")
        }

        val selectedTarget = response.selectedCards.firstOrNull()
            ?: return checkForMore(state, emptyList())

        if (selectedTarget !in continuation.candidateTargets) {
            return ExecutionResult.error(state, "Invalid discard chain copy target: $selectedTarget")
        }

        val ability = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId ?: EntityId.generate(),
            sourceName = continuation.spellName,
            controllerId = continuation.copyControllerId,
            effect = DiscardAndChainCopyEffect(
                count = continuation.count,
                target = EffectTarget.ContextTarget(0),
                spellName = continuation.spellName
            ),
            description = "Copy of ${continuation.spellName}"
        )

        val targets = listOf(ChosenTarget.Player(selectedTarget))
        val targetRequirements = listOf(
            com.wingedsheep.sdk.scripting.targets.TargetPlayer()
        )

        val putResult = ctx.stackResolver.putTriggeredAbility(state, ability, targets, targetRequirements)

        if (!putResult.isSuccess) {
            return putResult
        }

        return checkForMore(putResult.state, putResult.events.toList())
    }

    fun resumeDamageChainCopyDecision(
        state: GameState,
        continuation: DamageChainCopyDecisionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for damage chain copy decision")
        }

        if (!response.choice) {
            return checkForMore(state, emptyList())
        }

        val handZone = ZoneKey(continuation.affectedPlayerId, Zone.HAND)
        val hand = state.getZone(handZone)

        if (hand.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        val sourceName = continuation.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: continuation.spellName

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = continuation.affectedPlayerId,
            prompt = "Choose a card to discard for ${continuation.spellName}",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = hand,
            minSelections = 1,
            maxSelections = 1
        )

        val discardContinuation = DamageChainDiscardContinuation(
            decisionId = decisionId,
            affectedPlayerId = continuation.affectedPlayerId,
            amount = continuation.amount,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId
        )

        val newState = state.withPendingDecision(decision).pushContinuation(discardContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.affectedPlayerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    fun resumeDamageChainDiscard(
        state: GameState,
        continuation: DamageChainDiscardContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for damage chain discard")
        }

        val selectedCard = response.selectedCards.firstOrNull()
            ?: return checkForMore(state, emptyList())

        val playerId = continuation.affectedPlayerId
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        var newState = state.removeFromZone(handZone, selectedCard)
        newState = newState.addToZone(graveyardZone, selectedCard)

        val selectedCardName = state.getEntity(selectedCard)?.get<CardComponent>()?.name ?: "Card"
        val events = mutableListOf<GameEvent>(
            CardsDiscardedEvent(playerId, listOf(selectedCard), listOf(selectedCardName))
        )

        val requirement = com.wingedsheep.sdk.scripting.targets.AnyTarget()
        val legalTargets = ctx.targetFinder.findLegalTargets(
            newState, requirement, playerId, continuation.sourceId
        )

        if (legalTargets.isEmpty()) {
            return checkForMore(newState, events)
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Choose a target for the copy of ${continuation.spellName}",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = legalTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val targetContinuation = DamageChainCopyTargetContinuation(
            decisionId = decisionId,
            copyControllerId = playerId,
            amount = continuation.amount,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId,
            candidateTargets = legalTargets
        )

        newState = newState.withPendingDecision(decision).pushContinuation(targetContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            events + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    fun resumeDamageChainCopyTarget(
        state: GameState,
        continuation: DamageChainCopyTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for damage chain copy target")
        }

        val selectedTarget = response.selectedCards.firstOrNull()
            ?: return checkForMore(state, emptyList())

        if (selectedTarget !in continuation.candidateTargets) {
            return ExecutionResult.error(state, "Invalid damage chain copy target: $selectedTarget")
        }

        val entity = state.getEntity(selectedTarget)
        val chosenTarget = if (entity?.get<ControllerComponent>() != null) {
            ChosenTarget.Permanent(selectedTarget)
        } else {
            ChosenTarget.Player(selectedTarget)
        }

        val ability = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId ?: EntityId.generate(),
            sourceName = continuation.spellName,
            controllerId = continuation.copyControllerId,
            effect = DamageAndChainCopyEffect(
                amount = continuation.amount,
                target = EffectTarget.ContextTarget(0),
                spellName = continuation.spellName
            ),
            description = "Copy of ${continuation.spellName}"
        )

        val targets = listOf(chosenTarget)
        val targetRequirements = listOf(
            com.wingedsheep.sdk.scripting.targets.AnyTarget()
        )

        val putResult = ctx.stackResolver.putTriggeredAbility(state, ability, targets, targetRequirements)

        if (!putResult.isSuccess) {
            return putResult
        }

        return checkForMore(putResult.state, putResult.events.toList())
    }

    fun resumePreventDamageChainCopyDecision(
        state: GameState,
        continuation: PreventDamageChainCopyDecisionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for prevent damage chain copy decision")
        }

        if (!response.choice) {
            return checkForMore(state, emptyList())
        }

        val controllerLands = findControllerLands(state, continuation.targetControllerId)

        if (controllerLands.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        if (controllerLands.size == 1) {
            return sacrificeLandAndPresentPreventDamageChainTargets(
                state, continuation.targetControllerId, controllerLands.first(),
                continuation.targetFilter, continuation.spellName, continuation.sourceId,
                checkForMore
            )
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = continuation.targetControllerId,
            prompt = "Choose a land to sacrifice for the copy of ${continuation.spellName}",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = controllerLands,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val landContinuation = PreventDamageChainCopyLandContinuation(
            decisionId = decisionId,
            copyControllerId = continuation.targetControllerId,
            targetFilter = continuation.targetFilter,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId,
            candidateLands = controllerLands
        )

        val newState = state.withPendingDecision(decision).pushContinuation(landContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.targetControllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    fun resumePreventDamageChainCopyLand(
        state: GameState,
        continuation: PreventDamageChainCopyLandContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for prevent damage chain land sacrifice")
        }

        val selectedLand = response.selectedCards.firstOrNull()
            ?: return checkForMore(state, emptyList())

        if (selectedLand !in continuation.candidateLands) {
            return ExecutionResult.error(state, "Invalid land for prevent damage chain sacrifice: $selectedLand")
        }

        return sacrificeLandAndPresentPreventDamageChainTargets(
            state, continuation.copyControllerId, selectedLand,
            continuation.targetFilter, continuation.spellName, continuation.sourceId,
            checkForMore
        )
    }

    fun resumePreventDamageChainCopyTarget(
        state: GameState,
        continuation: PreventDamageChainCopyTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for prevent damage chain copy target")
        }

        val selectedTarget = response.selectedCards.firstOrNull()
            ?: return checkForMore(state, emptyList())

        if (selectedTarget !in continuation.candidateTargets) {
            return ExecutionResult.error(state, "Invalid prevent damage chain copy target: $selectedTarget")
        }

        val ability = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId ?: EntityId.generate(),
            sourceName = continuation.spellName,
            controllerId = continuation.copyControllerId,
            effect = PreventDamageAndChainCopyEffect(
                target = EffectTarget.ContextTarget(0),
                targetFilter = continuation.targetFilter,
                spellName = continuation.spellName
            ),
            description = "Copy of ${continuation.spellName}"
        )

        val targets = listOf(ChosenTarget.Permanent(selectedTarget))
        val targetRequirements = listOf(
            com.wingedsheep.sdk.scripting.targets.TargetPermanent(filter = continuation.targetFilter)
        )

        val putResult = ctx.stackResolver.putTriggeredAbility(state, ability, targets, targetRequirements)

        if (!putResult.isSuccess) {
            return putResult
        }

        return checkForMore(putResult.state, putResult.events.toList())
    }

    private fun sacrificeLandAndPresentTargets(
        state: GameState,
        controllerId: EntityId,
        landId: EntityId,
        targetFilter: TargetFilter,
        spellName: String,
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

        val requirement = com.wingedsheep.sdk.scripting.targets.TargetPermanent(filter = targetFilter)
        val legalTargets = ctx.targetFinder.findLegalTargets(newState, requirement, controllerId, sourceId)

        if (legalTargets.isEmpty()) {
            return checkForMore(newState, sacrificeEvents)
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a target for the copy of $spellName",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = legalTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val targetContinuation = BounceChainCopyTargetContinuation(
            decisionId = decisionId,
            copyControllerId = controllerId,
            targetFilter = targetFilter,
            spellName = spellName,
            sourceId = sourceId,
            candidateTargets = legalTargets
        )

        newState = newState.withPendingDecision(decision).pushContinuation(targetContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            sacrificeEvents + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    private fun sacrificeLandAndPresentPreventDamageChainTargets(
        state: GameState,
        controllerId: EntityId,
        landId: EntityId,
        targetFilter: TargetFilter,
        spellName: String,
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

        val requirement = com.wingedsheep.sdk.scripting.targets.TargetPermanent(filter = targetFilter)
        val legalTargets = ctx.targetFinder.findLegalTargets(newState, requirement, controllerId, sourceId)

        if (legalTargets.isEmpty()) {
            return checkForMore(newState, sacrificeEvents)
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a target for the copy of $spellName",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = legalTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val targetContinuation = PreventDamageChainCopyTargetContinuation(
            decisionId = decisionId,
            copyControllerId = controllerId,
            targetFilter = targetFilter,
            spellName = spellName,
            sourceId = sourceId,
            candidateTargets = legalTargets
        )

        newState = newState.withPendingDecision(decision).pushContinuation(targetContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            sacrificeEvents + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    fun findControllerLands(state: GameState, controllerId: EntityId): List<EntityId> {
        val projected = StateProjector().project(state)
        val controlledPermanents = projected.getBattlefieldControlledBy(controllerId)
        val predicateEvaluator = PredicateEvaluator()
        val context = PredicateContext(controllerId = controllerId)
        return controlledPermanents.filter { permanentId ->
            predicateEvaluator.matches(state, permanentId, GameObjectFilter.Land, context)
        }
    }
}
