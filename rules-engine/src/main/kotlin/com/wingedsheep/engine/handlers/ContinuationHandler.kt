package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.handlers.effects.drawing.EachPlayerDiscardsDrawsExecutor
import com.wingedsheep.engine.handlers.effects.drawing.EachPlayerMayDrawExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.SearchDestination

/**
 * Handles resumption of execution after a player decision.
 *
 * When the engine pauses for player input, it pushes a ContinuationFrame
 * onto the state's continuation stack. When the player submits their decision,
 * this handler pops the frame and resumes execution based on the frame type.
 */
class ContinuationHandler(
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val stackResolver: StackResolver = StackResolver()
) {

    /**
     * Resume execution after a decision is submitted.
     *
     * @param state The game state with the pending decision already cleared
     * @param response The player's decision response
     * @return The result of resuming execution
     */
    fun resume(state: GameState, response: DecisionResponse): ExecutionResult {
        val (continuation, stateAfterPop) = state.popContinuation()

        if (continuation == null) {
            // No continuation frame - this shouldn't happen if used correctly
            return ExecutionResult.success(state)
        }

        // Verify the decision ID matches
        if (continuation.decisionId != response.decisionId) {
            return ExecutionResult.error(
                state,
                "Decision ID mismatch: expected ${continuation.decisionId}, got ${response.decisionId}"
            )
        }

        return when (continuation) {
            is DiscardContinuation -> resumeDiscard(stateAfterPop, continuation, response)
            is ScryContinuation -> resumeScry(stateAfterPop, continuation, response)
            is EffectContinuation -> resumeEffect(stateAfterPop, continuation, response)
            is TriggeredAbilityContinuation -> resumeTriggeredAbility(stateAfterPop, continuation, response)
            is DamageAssignmentContinuation -> resumeDamageAssignment(stateAfterPop, continuation, response)
            is ResolveSpellContinuation -> resumeSpellResolution(stateAfterPop, continuation, response)
            is SacrificeContinuation -> resumeSacrifice(stateAfterPop, continuation, response)
            is MayAbilityContinuation -> resumeMayAbility(stateAfterPop, continuation, response)
            is HandSizeDiscardContinuation -> resumeHandSizeDiscard(stateAfterPop, continuation, response)
            is EachPlayerSelectsThenDrawsContinuation -> resumeEachPlayerSelectsThenDraws(stateAfterPop, continuation, response)
            is ReturnFromGraveyardContinuation -> resumeReturnFromGraveyard(stateAfterPop, continuation, response)
            is SearchLibraryContinuation -> resumeSearchLibrary(stateAfterPop, continuation, response)
            is ReorderLibraryContinuation -> resumeReorderLibrary(stateAfterPop, continuation, response)
            is BlockerOrderContinuation -> resumeBlockerOrder(stateAfterPop, continuation, response)
            is SearchLibraryToTopContinuation -> resumeSearchLibraryToTop(stateAfterPop, continuation, response)
            is EachPlayerChoosesDrawContinuation -> resumeEachPlayerChoosesDraw(stateAfterPop, continuation, response)
            is LookAtOpponentLibraryContinuation -> resumeLookAtOpponentLibrary(stateAfterPop, continuation, response)
            is ReorderOpponentLibraryContinuation -> resumeReorderOpponentLibrary(stateAfterPop, continuation, response)
            is PayOrSufferContinuation -> resumePayOrSuffer(stateAfterPop, continuation, response)
            is DistributeDamageContinuation -> resumeDistributeDamage(stateAfterPop, continuation, response)
            is LookAtTopCardsContinuation -> resumeLookAtTopCards(stateAfterPop, continuation, response)
        }
    }

    /**
     * Resume after player selected cards to discard.
     */
    private fun resumeDiscard(
        state: GameState,
        continuation: DiscardContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for discard")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Move selected cards from hand to graveyard
        var newState = state
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val events = listOf(
            CardsDiscardedEvent(playerId, selectedCards)
        )

        // Check if there are more continuations to process
        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player ordered cards for scry.
     */
    private fun resumeScry(
        state: GameState,
        continuation: ScryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is PilesSplitResponse) {
            return ExecutionResult.error(state, "Expected pile split response for scry")
        }

        val playerId = continuation.playerId

        // Pile 0 = top of library, Pile 1 = bottom of library
        val topCards = response.piles.getOrElse(0) { emptyList() }
        val bottomCards = response.piles.getOrElse(1) { emptyList() }

        var newState = state
        val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)

        // Remove all scried cards from library first
        val allScried = topCards + bottomCards
        for (cardId in allScried) {
            newState = newState.removeFromZone(libraryZone, cardId)
        }

        // Get current library
        val currentLibrary = newState.getZone(libraryZone).toMutableList()

        // Add top cards to top of library (in order)
        val newLibrary = topCards + currentLibrary + bottomCards

        // Update zone with new order
        newState = newState.copy(
            zones = newState.zones + (libraryZone to newLibrary)
        )

        val events = listOf(
            ScryCompletedEvent(playerId, topCards.size, bottomCards.size)
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume a composite effect with remaining effects.
     */
    private fun resumeEffect(
        state: GameState,
        continuation: EffectContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        val context = continuation.toEffectContext()
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for ((index, effect) in continuation.remainingEffects.withIndex()) {
            val stillRemaining = continuation.remainingEffects.drop(index + 1)

            // Pre-push EffectContinuation for remaining effects BEFORE executing.
            // This ensures that if the sub-effect pushes its own continuation,
            // that continuation ends up on TOP (to be processed first when the response comes).
            val stateForExecution = if (stillRemaining.isNotEmpty()) {
                val remainingContinuation = EffectContinuation(
                    decisionId = "pending", // Will be found by checkForMoreContinuations
                    remainingEffects = stillRemaining,
                    sourceId = continuation.sourceId,
                    controllerId = continuation.controllerId,
                    opponentId = continuation.opponentId,
                    xValue = continuation.xValue
                )
                currentState.pushContinuation(remainingContinuation)
            } else {
                currentState
            }

            val result = effectExecutorRegistry.execute(stateForExecution, effect, context)

            if (!result.isSuccess) {
                return result
            }

            if (result.isPaused) {
                // Sub-effect paused. Its continuation is on top.
                // Our pre-pushed EffectContinuation is underneath, ready to be
                // processed by checkForMoreContinuations after the sub-effect resolves.
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            // Effect succeeded - pop the pre-pushed continuation (it wasn't needed)
            currentState = if (stillRemaining.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)
        }

        return checkForMoreContinuations(currentState, allEvents)
    }

    /**
     * Resume placing a triggered ability on the stack after target selection.
     *
     * The player has selected targets for a triggered ability. Now we can
     * actually put the ability on the stack with those targets.
     */
    private fun resumeTriggeredAbility(
        state: GameState,
        continuation: TriggeredAbilityContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected target selection response for triggered ability")
        }

        // Convert the selected targets into ChosenTarget objects
        // The response contains a map of requirement index -> selected entity IDs
        // For most triggered abilities, there's a single requirement at index 0
        val selectedTargets = response.selectedTargets.flatMap { (_, targetIds) ->
            targetIds.map { entityId ->
                // Determine what kind of target this is based on the entity
                val container = state.getEntity(entityId)
                when {
                    // Check if it's a player
                    entityId in state.turnOrder -> ChosenTarget.Player(entityId)
                    // Check if it's on the battlefield (permanent)
                    entityId in state.getBattlefield() -> ChosenTarget.Permanent(entityId)
                    // Check if it's on the stack (spell)
                    entityId in state.stack -> ChosenTarget.Spell(entityId)
                    // Check if it's in a graveyard (card)
                    else -> {
                        // Look through all graveyards to find which player owns this card
                        val graveyardOwner = state.turnOrder.find { playerId ->
                            val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)
                            entityId in state.getZone(graveyardZone)
                        }
                        if (graveyardOwner != null) {
                            ChosenTarget.Card(entityId, graveyardOwner, ZoneType.GRAVEYARD)
                        } else {
                            // Default to permanent (fallback for unknown cases)
                            ChosenTarget.Permanent(entityId)
                        }
                    }
                }
            }
        }

        // Create the triggered ability component to put on stack
        val abilityComponent = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description
        )

        // Put the ability on the stack with the selected targets
        val stackResult = stackResolver.putTriggeredAbility(state, abilityComponent, selectedTargets)

        if (!stackResult.isSuccess) {
            return stackResult
        }

        // After putting the ability on stack, check for more continuations
        return checkForMoreContinuations(stackResult.newState, stackResult.events.toList())
    }

    /**
     * Resume combat damage assignment.
     */
    private fun resumeDamageAssignment(
        state: GameState,
        continuation: DamageAssignmentContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is DamageAssignmentResponse) {
            return ExecutionResult.error(state, "Expected damage assignment response")
        }

        // TODO: Apply the damage assignments from the response
        // This would need to work with the CombatManager

        return ExecutionResult.success(state)
    }

    /**
     * Resume spell resolution after target/mode selection.
     */
    private fun resumeSpellResolution(
        state: GameState,
        continuation: ResolveSpellContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        // TODO: Implement spell resolution resumption
        // This would store targets/modes and continue resolution
        return ExecutionResult.success(state)
    }

    /**
     * Resume after player selected a card from their graveyard.
     */
    private fun resumeReturnFromGraveyard(
        state: GameState,
        continuation: ReturnFromGraveyardContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for graveyard search")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Empty selection — no card returned
        if (selectedCards.isEmpty()) {
            return checkForMoreContinuations(state, emptyList())
        }

        val cardId = selectedCards.first()
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

        // Validate card is still in graveyard
        if (cardId !in state.getZone(graveyardZone)) {
            return checkForMoreContinuations(state, emptyList())
        }

        val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
        var newState = state.removeFromZone(graveyardZone, cardId)

        val toZone = when (continuation.destination) {
            SearchDestination.HAND -> {
                val handZone = ZoneKey(playerId, ZoneType.HAND)
                newState = newState.addToZone(handZone, cardId)
                ZoneType.HAND
            }
            SearchDestination.BATTLEFIELD -> {
                val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
                newState = newState.addToZone(battlefieldZone, cardId)
                newState = newState.updateEntity(cardId) { c ->
                    c.with(com.wingedsheep.engine.state.components.identity.ControllerComponent(playerId))
                        .with(com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent)
                }
                ZoneType.BATTLEFIELD
            }
            else -> return ExecutionResult.error(state, "Unsupported destination: ${continuation.destination}")
        }

        val events = listOf(
            ZoneChangeEvent(
                entityId = cardId,
                entityName = cardName,
                fromZone = ZoneType.GRAVEYARD,
                toZone = toZone,
                ownerId = playerId
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player selected cards from library search.
     *
     * Handles:
     * 1. "Fail to find" (0 cards selected) - just shuffle if configured
     * 2. Move selected cards from library to destination zone
     * 3. Apply tapped status if entering battlefield tapped
     * 4. Emit reveal events if configured
     * 5. Shuffle library if configured
     */
    private fun resumeSearchLibrary(
        state: GameState,
        continuation: SearchLibraryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for library search")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards
        val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)
        val events = mutableListOf<GameEvent>()

        var newState = state

        // Handle "fail to find" case - just shuffle and return
        if (selectedCards.isEmpty()) {
            if (continuation.shuffleAfter) {
                val library = newState.getZone(libraryZone).shuffled()
                newState = newState.copy(zones = newState.zones + (libraryZone to library))
                events.add(LibraryShuffledEvent(playerId))
            }
            return checkForMoreContinuations(newState, events)
        }

        // Remove selected cards from library
        for (cardId in selectedCards) {
            newState = newState.removeFromZone(libraryZone, cardId)
        }

        // Move cards to destination zone
        val destinationZone = when (continuation.destination) {
            SearchDestination.HAND -> ZoneKey(playerId, ZoneType.HAND)
            SearchDestination.BATTLEFIELD -> ZoneKey(playerId, ZoneType.BATTLEFIELD)
            SearchDestination.GRAVEYARD -> ZoneKey(playerId, ZoneType.GRAVEYARD)
            SearchDestination.TOP_OF_LIBRARY -> libraryZone
        }

        for (cardId in selectedCards) {
            // For TOP_OF_LIBRARY, add to the front
            if (continuation.destination == SearchDestination.TOP_OF_LIBRARY) {
                val currentLibrary = newState.getZone(libraryZone)
                newState = newState.copy(
                    zones = newState.zones + (libraryZone to listOf(cardId) + currentLibrary)
                )
            } else {
                newState = newState.addToZone(destinationZone, cardId)
            }

            // Apply tapped status for battlefield with entersTapped
            if (continuation.destination == SearchDestination.BATTLEFIELD && continuation.entersTapped) {
                val container = newState.getEntity(cardId)
                if (container != null) {
                    val newContainer = container.with(TappedComponent)
                    newState = newState.copy(
                        entities = newState.entities + (cardId to newContainer)
                    )
                }
            }

            // Emit zone change event
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = ZoneType.LIBRARY,
                    toZone = when (continuation.destination) {
                        SearchDestination.HAND -> ZoneType.HAND
                        SearchDestination.BATTLEFIELD -> ZoneType.BATTLEFIELD
                        SearchDestination.GRAVEYARD -> ZoneType.GRAVEYARD
                        SearchDestination.TOP_OF_LIBRARY -> ZoneType.LIBRARY
                    },
                    ownerId = playerId
                )
            )
        }

        // Reveal cards if configured (could emit a CardsRevealedEvent in the future)
        // For now, the zone change events serve as implicit reveal

        // Shuffle library after search if configured
        if (continuation.shuffleAfter) {
            val library = newState.getZone(libraryZone).shuffled()
            newState = newState.copy(zones = newState.zones + (libraryZone to library))
            events.add(LibraryShuffledEvent(playerId))
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player reordered cards on top of their library.
     *
     * The response contains the card IDs in the new order (first = new top of library).
     * We replace the top N cards in the library with this new order.
     */
    private fun resumeReorderLibrary(
        state: GameState,
        continuation: ReorderLibraryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for library reorder")
        }

        val playerId = continuation.playerId
        val orderedCards = response.orderedObjects
        val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)

        // Get current library
        val currentLibrary = state.getZone(libraryZone).toMutableList()

        // Remove the reordered cards from the library (they were at the top)
        val cardsSet = orderedCards.toSet()
        val remainingLibrary = currentLibrary.filter { it !in cardsSet }

        // Place the cards back on top in the new order
        val newLibrary = orderedCards + remainingLibrary

        // Update the library zone
        val newState = state.copy(
            zones = state.zones + (libraryZone to newLibrary)
        )

        val events = listOf(
            LibraryReorderedEvent(
                playerId = playerId,
                cardCount = orderedCards.size,
                source = continuation.sourceName
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after attacking player declared damage assignment order for blockers.
     *
     * Per MTG CR 509.2, the attacking player must order blockers for each attacker
     * blocked by 2+ creatures. This determines the order damage is assigned.
     */
    private fun resumeBlockerOrder(
        state: GameState,
        continuation: BlockerOrderContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for blocker ordering")
        }

        // Add DamageAssignmentOrderComponent to the attacker
        var newState = state.updateEntity(continuation.attackerId) { container ->
            container.with(
                com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent(
                    response.orderedObjects
                )
            )
        }

        val events = mutableListOf<GameEvent>(
            BlockerOrderDeclaredEvent(continuation.attackerId, response.orderedObjects)
        )

        // Check if there are more attackers that need ordering
        if (continuation.remainingAttackers.isNotEmpty()) {
            val nextAttacker = continuation.remainingAttackers.first()
            val nextRemaining = continuation.remainingAttackers.drop(1)

            // Create decision for next attacker
            val attackerContainer = newState.getEntity(nextAttacker)!!
            val attackerCard = attackerContainer.get<CardComponent>()!!
            val blockedComponent = attackerContainer.get<com.wingedsheep.engine.state.components.combat.BlockedComponent>()!!
            val blockerIds = blockedComponent.blockerIds

            // Build card info for blockers
            val cardInfo = blockerIds.associateWith { blockerId ->
                val blockerCard = newState.getEntity(blockerId)?.get<CardComponent>()
                SearchCardInfo(
                    name = blockerCard?.name ?: "Unknown",
                    manaCost = blockerCard?.manaCost?.toString() ?: "",
                    typeLine = blockerCard?.typeLine?.toString() ?: ""
                )
            }

            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = OrderObjectsDecision(
                id = decisionId,
                playerId = continuation.attackingPlayerId,
                prompt = "Order damage assignment for ${attackerCard.name}",
                context = DecisionContext(
                    sourceId = nextAttacker,
                    sourceName = attackerCard.name,
                    phase = DecisionPhase.COMBAT
                ),
                objects = blockerIds,
                cardInfo = cardInfo
            )

            val nextContinuation = BlockerOrderContinuation(
                decisionId = decisionId,
                attackingPlayerId = continuation.attackingPlayerId,
                attackerId = nextAttacker,
                attackerName = attackerCard.name,
                remainingAttackers = nextRemaining
            )

            newState = newState
                .withPendingDecision(decision)
                .pushContinuation(nextContinuation)

            events.add(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = continuation.attackingPlayerId,
                    decisionType = "ORDER_BLOCKERS",
                    prompt = decision.prompt
                )
            )

            return ExecutionResult.paused(newState, decision, events)
        }

        // All attackers have been ordered - give priority back to the active player
        // Per MTG CR 509.4, after blockers are declared (and ordered), players get priority
        // in the declare blockers step before combat damage happens
        val activePlayer = newState.activePlayerId
        val stateWithPriority = if (activePlayer != null) {
            newState.withPriority(activePlayer)
        } else {
            newState
        }
        return checkForMoreContinuations(stateWithPriority, events)
    }

    /**
     * Resume after player selected cards to sacrifice.
     */
    private fun resumeSacrifice(
        state: GameState,
        continuation: SacrificeContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for sacrifice")
        }

        val playerId = continuation.playerId
        val selectedPermanents = response.selectedCards

        // Move selected permanents from battlefield to graveyard
        var newState = state
        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

        for (permanentId in selectedPermanents) {
            newState = newState.removeFromZone(battlefieldZone, permanentId)
            newState = newState.addToZone(graveyardZone, permanentId)
        }

        val events = listOf(
            PermanentsSacrificedEvent(playerId, selectedPermanents)
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player selected cards to discard for hand size during cleanup.
     */
    private fun resumeHandSizeDiscard(
        state: GameState,
        continuation: HandSizeDiscardContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for hand size discard")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Move selected cards from hand to graveyard
        var newState = state
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val events = listOf(
            CardsDiscardedEvent(playerId, selectedCards)
        )

        // Check if there are more continuations to process
        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player made a yes/no choice.
     */
    private fun resumeMayAbility(
        state: GameState,
        continuation: MayAbilityContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may ability")
        }

        val context = continuation.toEffectContext()
        val effectToExecute = if (response.choice) {
            continuation.effectIfYes
        } else {
            continuation.effectIfNo
        }

        if (effectToExecute == null) {
            // No effect to execute (player said no and there's no alternative)
            return checkForMoreContinuations(state, emptyList())
        }

        val result = effectExecutorRegistry.execute(state, effectToExecute, context)

        if (result.isPaused) {
            // The effect needs another decision
            return result
        }

        return checkForMoreContinuations(result.state, result.events.toList())
    }

    /**
     * Resume after a player selected cards for "each player selects, then draws" effects.
     * Handles effects like Flux where each player discards, then draws that many.
     */
    private fun resumeEachPlayerSelectsThenDraws(
        state: GameState,
        continuation: EachPlayerSelectsThenDrawsContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response")
        }

        val selectedCards = response.selectedCards

        // Get current player from the continuation
        val currentPlayerId = continuation.currentPlayerId

        // Discard the selected cards
        var newState = state
        val handZone = ZoneKey(currentPlayerId, ZoneType.HAND)
        val graveyardZone = ZoneKey(currentPlayerId, ZoneType.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val discardEvents = if (selectedCards.isNotEmpty()) {
            listOf(CardsDiscardedEvent(currentPlayerId, selectedCards))
        } else {
            emptyList()
        }

        // Update draw amounts with this player's count
        val newDrawAmounts = continuation.drawAmounts + (currentPlayerId to selectedCards.size)

        // Check if there are more players
        if (continuation.remainingPlayers.isNotEmpty()) {
            // Ask next player
            val nextPlayer = continuation.remainingPlayers.first()
            val nextRemainingPlayers = continuation.remainingPlayers.drop(1)

            val nextHandZone = ZoneKey(nextPlayer, ZoneType.HAND)
            val nextHand = newState.getZone(nextHandZone)

            // Determine selection bounds for next player
            val minSelection = continuation.minSelection
            val maxSelection = (continuation.maxSelection ?: nextHand.size).coerceAtMost(nextHand.size)

            // If next player has empty hand or must select 0, skip them
            if (nextHand.isEmpty() || (minSelection == 0 && maxSelection == 0)) {
                // Skip this player, add 0 to their draw amount
                val skippedDrawAmounts = newDrawAmounts + (nextPlayer to 0)

                // Recursively check remaining players
                return continueEachPlayerSelection(
                    newState,
                    continuation.copy(
                        remainingPlayers = nextRemainingPlayers,
                        drawAmounts = skippedDrawAmounts
                    ),
                    discardEvents
                )
            }

            // Create decision for next player
            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = nextPlayer,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                prompt = continuation.selectionPrompt,
                options = nextHand,
                minSelections = minSelection,
                maxSelections = maxSelection,
                ordered = false,
                phase = DecisionPhase.RESOLUTION
            )

            // Push updated continuation
            val newContinuation = continuation.copy(
                decisionId = decisionResult.pendingDecision!!.id,
                currentPlayerId = nextPlayer,
                remainingPlayers = nextRemainingPlayers,
                drawAmounts = newDrawAmounts
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                discardEvents + decisionResult.events
            )
        }

        // All players have selected - execute draws
        val drawResult = EachPlayerDiscardsDrawsExecutor.executeDraws(
            newState,
            newDrawAmounts,
            continuation.controllerBonusDraw,
            continuation.controllerId
        )

        return ExecutionResult(
            drawResult.state,
            discardEvents + drawResult.events,
            drawResult.error
        )
    }

    /**
     * Continue processing remaining players for each-player selection effects.
     * Handles skipping players with empty hands.
     */
    private fun continueEachPlayerSelection(
        state: GameState,
        continuation: EachPlayerSelectsThenDrawsContinuation,
        priorEvents: List<GameEvent>
    ): ExecutionResult {
        if (continuation.remainingPlayers.isEmpty()) {
            // All done - execute draws
            val drawResult = EachPlayerDiscardsDrawsExecutor.executeDraws(
                state,
                continuation.drawAmounts,
                continuation.controllerBonusDraw,
                continuation.controllerId
            )
            return ExecutionResult(
                drawResult.state,
                priorEvents + drawResult.events,
                drawResult.error
            )
        }

        val nextPlayer = continuation.remainingPlayers.first()
        val nextRemainingPlayers = continuation.remainingPlayers.drop(1)

        val nextHandZone = ZoneKey(nextPlayer, ZoneType.HAND)
        val nextHand = state.getZone(nextHandZone)

        val minSelection = continuation.minSelection
        val maxSelection = (continuation.maxSelection ?: nextHand.size).coerceAtMost(nextHand.size)

        // If next player has empty hand or must select 0, skip them
        if (nextHand.isEmpty() || (minSelection == 0 && maxSelection == 0)) {
            val skippedDrawAmounts = continuation.drawAmounts + (nextPlayer to 0)
            return continueEachPlayerSelection(
                state,
                continuation.copy(
                    remainingPlayers = nextRemainingPlayers,
                    drawAmounts = skippedDrawAmounts
                ),
                priorEvents
            )
        }

        // Create decision for next player
        val decisionHandler = DecisionHandler()
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = nextPlayer,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            prompt = continuation.selectionPrompt,
            options = nextHand,
            minSelections = minSelection,
            maxSelections = maxSelection,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        val newContinuation = continuation.copy(
            decisionId = decisionResult.pendingDecision!!.id,
            currentPlayerId = nextPlayer,
            remainingPlayers = nextRemainingPlayers
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            priorEvents + decisionResult.events
        )
    }

    /**
     * Resume after player selected a card from library for "search and put on top" effect.
     *
     * This handles the tutor pattern where:
     * 1. Remove selected card from library
     * 2. Shuffle library FIRST
     * 3. Put selected card on top of library
     *
     * This ensures the tutored card stays on top after the shuffle, which is the
     * correct behavior for cards like Cruel Tutor and Personal Tutor.
     */
    private fun resumeSearchLibraryToTop(
        state: GameState,
        continuation: SearchLibraryToTopContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for library search")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards
        val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)
        val events = mutableListOf<GameEvent>()

        var newState = state

        // Handle "fail to find" case - just shuffle and return
        if (selectedCards.isEmpty()) {
            val library = newState.getZone(libraryZone).shuffled()
            newState = newState.copy(zones = newState.zones + (libraryZone to library))
            events.add(LibraryShuffledEvent(playerId))
            return checkForMoreContinuations(newState, events)
        }

        // Get the selected card (there should be exactly 1)
        val selectedCard = selectedCards.first()

        // Step 1: Remove selected card from library
        newState = newState.removeFromZone(libraryZone, selectedCard)

        // Step 2: Shuffle library FIRST (without the selected card)
        val shuffledLibrary = newState.getZone(libraryZone).shuffled()
        newState = newState.copy(zones = newState.zones + (libraryZone to shuffledLibrary))
        events.add(LibraryShuffledEvent(playerId))

        // Step 3: Put selected card on TOP of library (after shuffle)
        val libraryWithCardOnTop = listOf(selectedCard) + newState.getZone(libraryZone)
        newState = newState.copy(zones = newState.zones + (libraryZone to libraryWithCardOnTop))

        // Emit zone change event (card moved within library to top)
        val cardName = newState.getEntity(selectedCard)?.get<CardComponent>()?.name ?: "Unknown"
        events.add(
            ZoneChangeEvent(
                entityId = selectedCard,
                entityName = cardName,
                fromZone = ZoneType.LIBRARY,
                toZone = ZoneType.LIBRARY,
                ownerId = playerId
            )
        )

        // Reveal the chosen card to all players if configured
        if (continuation.reveal) {
            events.add(
                CardsRevealedEvent(
                    revealingPlayerId = playerId,
                    cardIds = listOf(selectedCard),
                    cardNames = listOf(cardName),
                    source = continuation.sourceName
                )
            )
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after a player chose how many cards to draw for "each player may draw" effects.
     *
     * For effects like Temporary Truce where each player chooses 0-N cards to draw
     * and gains life for each card not drawn.
     */
    private fun resumeEachPlayerChoosesDraw(
        state: GameState,
        continuation: EachPlayerChoosesDrawContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number chosen response")
        }

        val chosenCount = response.number
        val currentPlayerId = continuation.currentPlayerId

        // Calculate life gain for this player
        val cardsNotDrawn = continuation.maxCards - chosenCount
        val lifeGain = cardsNotDrawn * continuation.lifePerCardNotDrawn

        // Update draw amounts and life gain amounts with this player's choice
        val newDrawAmounts = continuation.drawAmounts + (currentPlayerId to chosenCount)
        val newLifeGainAmounts = continuation.lifeGainAmounts + (currentPlayerId to lifeGain)

        // Check if there are more players
        if (continuation.remainingPlayers.isNotEmpty()) {
            // Ask next player
            val nextPlayer = continuation.remainingPlayers.first()
            val nextRemainingPlayers = continuation.remainingPlayers.drop(1)

            // Calculate actual max for next player (can't draw more than library size)
            val libraryZone = ZoneKey(nextPlayer, ZoneType.LIBRARY)
            val librarySize = state.getZone(libraryZone).size
            val actualMax = continuation.maxCards.coerceAtMost(librarySize)

            val prompt = if (continuation.lifePerCardNotDrawn > 0) {
                "Choose how many cards to draw (0-$actualMax). Gain ${continuation.lifePerCardNotDrawn} life for each card not drawn."
            } else {
                "Choose how many cards to draw (0-$actualMax)"
            }

            // Create decision for next player
            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createNumberDecision(
                state = state,
                playerId = nextPlayer,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                prompt = prompt,
                minValue = 0,
                maxValue = actualMax,
                phase = DecisionPhase.RESOLUTION
            )

            // Push updated continuation
            val newContinuation = continuation.copy(
                decisionId = decisionResult.pendingDecision!!.id,
                currentPlayerId = nextPlayer,
                remainingPlayers = nextRemainingPlayers,
                drawAmounts = newDrawAmounts,
                lifeGainAmounts = newLifeGainAmounts
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                decisionResult.events
            )
        }

        // All players have chosen - execute draws and life gains
        val result = EachPlayerMayDrawExecutor.executeDrawsAndLifeGains(
            state,
            newDrawAmounts,
            newLifeGainAmounts
        )

        return checkForMoreContinuations(result.state, result.events.toList())
    }

    /**
     * Resume after player selected cards to put in opponent's graveyard.
     *
     * This is the first step of LookAtOpponentLibraryEffect:
     * 1. Move selected cards to opponent's graveyard
     * 2. If there are remaining cards, ask player to reorder them
     * 3. Put remaining cards back on top of opponent's library in the chosen order
     */
    private fun resumeLookAtOpponentLibrary(
        state: GameState,
        continuation: LookAtOpponentLibraryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for look at opponent library")
        }

        val selectedForGraveyard = response.selectedCards
        val opponentId = continuation.opponentId
        val playerId = continuation.playerId

        // Validate selection count
        if (selectedForGraveyard.size != continuation.toGraveyard) {
            return ExecutionResult.error(
                state,
                "Expected ${continuation.toGraveyard} cards for graveyard, got ${selectedForGraveyard.size}"
            )
        }

        val libraryZone = ZoneKey(opponentId, ZoneType.LIBRARY)
        val graveyardZone = ZoneKey(opponentId, ZoneType.GRAVEYARD)
        val events = mutableListOf<GameEvent>()

        var newState = state

        // Move selected cards from library to graveyard
        for (cardId in selectedForGraveyard) {
            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)

            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = ZoneType.LIBRARY,
                    toZone = ZoneType.GRAVEYARD,
                    ownerId = opponentId
                )
            )
        }

        // Calculate remaining cards that need to be reordered
        val remainingCards = continuation.allCards.filter { it !in selectedForGraveyard }

        // If only 0 or 1 cards remain, no reordering needed
        if (remainingCards.size <= 1) {
            // Cards are already in the correct position at the top of the library
            // (they haven't been moved, just the graveyard cards were removed)
            return checkForMoreContinuations(newState, events)
        }

        // Need to reorder remaining cards - create a ReorderLibraryDecision
        val cardInfoMap = remainingCards.associateWith { cardId ->
            val container = newState.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null
            )
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = ReorderLibraryDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Put the remaining ${remainingCards.size} cards on top of opponent's library in any order.",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            cards = remainingCards,
            cardInfo = cardInfoMap
        )

        val reorderContinuation = ReorderOpponentLibraryContinuation(
            decisionId = decisionId,
            playerId = playerId,
            opponentId = opponentId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName
        )

        newState = newState
            .withPendingDecision(decision)
            .pushContinuation(reorderContinuation)

        events.add(
            DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = playerId,
                decisionType = "REORDER_LIBRARY",
                prompt = decision.prompt
            )
        )

        return ExecutionResult.paused(newState, decision, events)
    }

    /**
     * Resume after player reordered the remaining cards for opponent's library.
     *
     * This is the second step of LookAtOpponentLibraryEffect:
     * Put the cards back on top of opponent's library in the chosen order.
     */
    private fun resumeReorderOpponentLibrary(
        state: GameState,
        continuation: ReorderOpponentLibraryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for reorder opponent library")
        }

        val opponentId = continuation.opponentId
        val orderedCards = response.orderedObjects
        val libraryZone = ZoneKey(opponentId, ZoneType.LIBRARY)

        // Get current library
        val currentLibrary = state.getZone(libraryZone).toMutableList()

        // Remove the reordered cards from the library (they should be at the top)
        val cardsSet = orderedCards.toSet()
        val remainingLibrary = currentLibrary.filter { it !in cardsSet }

        // Place the cards back on top in the new order
        val newLibrary = orderedCards + remainingLibrary

        // Update the library zone
        val newState = state.copy(
            zones = state.zones + (libraryZone to newLibrary)
        )

        val events = listOf(
            LibraryReorderedEvent(
                playerId = opponentId,
                cardCount = orderedCards.size,
                source = continuation.sourceName
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player made a choice for a generic pay or suffer effect.
     *
     * Handles both card selection responses (for discard/sacrifice costs)
     * and yes/no responses (for random discard and pay life costs).
     */
    private fun resumePayOrSuffer(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        val playerId = continuation.playerId
        val sourceId = continuation.sourceId
        val sourceName = continuation.sourceName

        return when (continuation.costType) {
            PayOrSufferCostType.DISCARD -> {
                if (continuation.random) {
                    resumePayOrSufferRandomDiscard(state, continuation, response)
                } else {
                    resumePayOrSufferDiscard(state, continuation, response)
                }
            }
            PayOrSufferCostType.SACRIFICE -> resumePayOrSufferSacrifice(state, continuation, response)
            PayOrSufferCostType.PAY_LIFE -> resumePayOrSufferPayLife(state, continuation, response)
        }
    }

    /**
     * Handle discard cost selection for pay or suffer.
     */
    private fun resumePayOrSufferDiscard(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for pay or suffer discard")
        }

        val playerId = continuation.playerId
        val sourceId = continuation.sourceId
        val selectedCards = response.selectedCards

        // If player didn't select enough cards, execute the suffer effect
        if (selectedCards.size < continuation.requiredCount) {
            return executePayOrSufferConsequence(state, continuation)
        }

        // Player paid the cost - discard the selected cards
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (cardId in selectedCards) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = ZoneType.HAND,
                    toZone = ZoneType.GRAVEYARD,
                    ownerId = playerId
                )
            )
        }

        events.add(0, CardsDiscardedEvent(playerId, selectedCards))
        return checkForMoreContinuations(newState, events)
    }

    /**
     * Handle random discard yes/no choice for pay or suffer.
     */
    private fun resumePayOrSufferRandomDiscard(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay or suffer random discard")
        }

        if (!response.choice) {
            // Player declined - execute suffer effect
            return executePayOrSufferConsequence(state, continuation)
        }

        // Player chose to pay - execute random discard
        val result = com.wingedsheep.engine.handlers.effects.removal.PayOrSufferExecutor.executeRandomDiscard(
            state,
            continuation.playerId,
            continuation.filter,
            continuation.requiredCount
        )
        return checkForMoreContinuations(result.state, result.events.toList())
    }

    /**
     * Handle sacrifice cost selection for pay or suffer.
     */
    private fun resumePayOrSufferSacrifice(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for pay or suffer sacrifice")
        }

        val playerId = continuation.playerId
        val selectedPermanents = response.selectedCards

        // If player didn't select enough permanents, execute the suffer effect
        if (selectedPermanents.size < continuation.requiredCount) {
            return executePayOrSufferConsequence(state, continuation)
        }

        // Player paid the cost - sacrifice the selected permanents
        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (permanentId in selectedPermanents) {
            val permanentName = newState.getEntity(permanentId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(battlefieldZone, permanentId)
            newState = newState.addToZone(graveyardZone, permanentId)
            events.add(
                ZoneChangeEvent(
                    entityId = permanentId,
                    entityName = permanentName,
                    fromZone = ZoneType.BATTLEFIELD,
                    toZone = ZoneType.GRAVEYARD,
                    ownerId = playerId
                )
            )
        }

        events.add(0, PermanentsSacrificedEvent(playerId, selectedPermanents))
        return checkForMoreContinuations(newState, events)
    }

    /**
     * Handle pay life yes/no choice for pay or suffer.
     */
    private fun resumePayOrSufferPayLife(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay or suffer pay life")
        }

        if (!response.choice) {
            // Player declined - execute suffer effect
            return executePayOrSufferConsequence(state, continuation)
        }

        // Player chose to pay life
        val playerId = continuation.playerId
        val lifeToPay = continuation.requiredCount
        val playerContainer = state.getEntity(playerId)
        val currentLife = playerContainer?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 0
        val newLife = currentLife - lifeToPay

        val newState = state.updateEntity(playerId) {
            it.with(com.wingedsheep.engine.state.components.identity.LifeTotalComponent(newLife))
        }

        val events = listOf(
            LifeChangedEvent(
                playerId = playerId,
                oldLife = currentLife,
                newLife = newLife,
                reason = LifeChangeReason.PAYMENT
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player distributed damage among targets.
     */
    private fun resumeDistributeDamage(
        state: GameState,
        continuation: DistributeDamageContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is DistributionResponse) {
            return ExecutionResult.error(state, "Expected distribution response for divided damage")
        }

        val distribution = response.distribution
        val events = mutableListOf<GameEvent>()
        var newState = state

        // Deal damage to each target according to the distribution
        for ((targetId, damageAmount) in distribution) {
            if (damageAmount > 0) {
                val result = EffectExecutorUtils.dealDamageToTarget(
                    newState,
                    targetId,
                    damageAmount,
                    continuation.sourceId
                )

                if (!result.isSuccess) {
                    return ExecutionResult(newState, events, result.error)
                }

                newState = result.state
                events.addAll(result.events)
            }
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player selected cards to keep from looking at top cards of library.
     *
     * The player has selected which cards to put into their hand. The remaining
     * cards from the looked-at set go to graveyard (if restToGraveyard is true)
     * or stay on top of the library (if false).
     */
    private fun resumeLookAtTopCards(
        state: GameState,
        continuation: LookAtTopCardsContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for look at top cards")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards.toSet()
        val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)
        val events = mutableListOf<GameEvent>()

        var newState = state

        // Process all looked-at cards
        for (cardId in continuation.allCards) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"

            // Remove from library first
            newState = newState.removeFromZone(libraryZone, cardId)

            if (cardId in selectedCards) {
                // Selected cards go to hand
                newState = newState.addToZone(handZone, cardId)
                events.add(
                    ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardName,
                        fromZone = ZoneType.LIBRARY,
                        toZone = ZoneType.HAND,
                        ownerId = playerId
                    )
                )
            } else {
                // Non-selected cards go to graveyard or stay on top of library
                if (continuation.restToGraveyard) {
                    newState = newState.addToZone(graveyardZone, cardId)
                    events.add(
                        ZoneChangeEvent(
                            entityId = cardId,
                            entityName = cardName,
                            fromZone = ZoneType.LIBRARY,
                            toZone = ZoneType.GRAVEYARD,
                            ownerId = playerId
                        )
                    )
                } else {
                    // Put back on top of library
                    val currentLibrary = newState.getZone(libraryZone)
                    newState = newState.copy(
                        zones = newState.zones + (libraryZone to listOf(cardId) + currentLibrary)
                    )
                }
            }
        }

        // Add a cards drawn event for the selected cards
        if (selectedCards.isNotEmpty()) {
            events.add(0, CardsDrawnEvent(playerId, selectedCards.size, selectedCards.toList()))
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Execute the suffer effect for pay or suffer.
     */
    private fun executePayOrSufferConsequence(
        state: GameState,
        continuation: PayOrSufferContinuation
    ): ExecutionResult {
        val sourceId = continuation.sourceId
        val playerId = continuation.playerId
        val sufferEffect = continuation.sufferEffect

        // Create context for executing the suffer effect
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = playerId,
            opponentId = null
        )

        // Execute the suffer effect using the registry
        val result = effectExecutorRegistry.execute(state, sufferEffect, context)

        return if (result.isPaused) {
            result
        } else {
            checkForMoreContinuations(result.state, result.events.toList())
        }
    }

    /**
     * Check if there are more continuation frames to process.
     * If there's an EffectContinuation waiting, process it.
     */
    private fun checkForMoreContinuations(
        state: GameState,
        events: List<GameEvent>
    ): ExecutionResult {
        val nextContinuation = state.peekContinuation()

        if (nextContinuation is EffectContinuation && nextContinuation.remainingEffects.isNotEmpty()) {
            // Pop and process the effect continuation
            val (_, stateAfterPop) = state.popContinuation()
            val context = nextContinuation.toEffectContext()
            var currentState = stateAfterPop
            val allEvents = events.toMutableList()

            for ((index, effect) in nextContinuation.remainingEffects.withIndex()) {
                val stillRemaining = nextContinuation.remainingEffects.drop(index + 1)

                // Pre-push EffectContinuation for remaining effects BEFORE executing.
                // This ensures that if the sub-effect pushes its own continuation,
                // that continuation ends up on TOP (to be processed first when the response comes).
                val stateForExecution = if (stillRemaining.isNotEmpty()) {
                    val remainingContinuation = EffectContinuation(
                        decisionId = "pending", // Will be found by checkForMoreContinuations
                        remainingEffects = stillRemaining,
                        sourceId = nextContinuation.sourceId,
                        controllerId = nextContinuation.controllerId,
                        opponentId = nextContinuation.opponentId,
                        xValue = nextContinuation.xValue
                    )
                    currentState.pushContinuation(remainingContinuation)
                } else {
                    currentState
                }

                val result = effectExecutorRegistry.execute(stateForExecution, effect, context)

                if (!result.isSuccess) {
                    return ExecutionResult(result.state, allEvents + result.events, result.error)
                }

                if (result.isPaused) {
                    // Sub-effect paused. Its continuation is on top.
                    // Our pre-pushed EffectContinuation is underneath, ready to be
                    // processed by checkForMoreContinuations after the sub-effect resolves.
                    return ExecutionResult.paused(
                        result.state,
                        result.pendingDecision!!,
                        allEvents + result.events
                    )
                }

                // Effect succeeded - pop our pre-pushed continuation (it wasn't needed)
                currentState = if (stillRemaining.isNotEmpty()) {
                    val (_, stateWithoutCont) = result.state.popContinuation()
                    stateWithoutCont
                } else {
                    result.state
                }
                allEvents.addAll(result.events)
            }

            return ExecutionResult.success(currentState, allEvents)
        }

        return ExecutionResult.success(state, events)
    }
}
