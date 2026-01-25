package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.handlers.effects.drawing.EachPlayerDiscardsDrawsExecutor
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
            is SearchLibraryContinuation -> resumeSearchLibrary(stateAfterPop, continuation, response)
            is ReorderLibraryContinuation -> resumeReorderLibrary(stateAfterPop, continuation, response)
            is BlockerOrderContinuation -> resumeBlockerOrder(stateAfterPop, continuation, response)
            is SacrificeUnlessSacrificeContinuation -> resumeSacrificeUnlessSacrifice(stateAfterPop, continuation, response)
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
                    // Default to permanent (could be improved with more context)
                    else -> ChosenTarget.Permanent(entityId)
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

        // All attackers have been ordered - continue with game flow
        return checkForMoreContinuations(newState, events)
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
     * Resume after player selected permanents for "sacrifice unless you sacrifice" effect.
     *
     * If the player selected exactly the required count, sacrifice those permanents.
     * If the player selected 0 (or fewer than required), sacrifice the source instead.
     */
    private fun resumeSacrificeUnlessSacrifice(
        state: GameState,
        continuation: SacrificeUnlessSacrificeContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for sacrifice unless")
        }

        val playerId = continuation.playerId
        val sourceId = continuation.sourceId
        val sourceName = continuation.sourceName
        val selectedPermanents = response.selectedCards

        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

        // Check if the source is still on the battlefield
        if (sourceId !in state.getZone(battlefieldZone)) {
            // Source was already removed (e.g., by another effect) - nothing to do
            return checkForMoreContinuations(state, emptyList())
        }

        // If player selected exactly the required count, sacrifice those permanents
        if (selectedPermanents.size == continuation.requiredCount) {
            var newState = state
            val events = mutableListOf<GameEvent>()

            for (permanentId in selectedPermanents) {
                newState = newState.removeFromZone(battlefieldZone, permanentId)
                newState = newState.addToZone(graveyardZone, permanentId)

                val permanentName = newState.getEntity(permanentId)?.get<CardComponent>()?.name ?: "Unknown"
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

        // Player didn't select enough - sacrifice the source
        var newState = state.removeFromZone(battlefieldZone, sourceId)
        newState = newState.addToZone(graveyardZone, sourceId)

        val events = listOf(
            PermanentsSacrificedEvent(playerId, listOf(sourceId)),
            ZoneChangeEvent(
                entityId = sourceId,
                entityName = sourceName,
                fromZone = ZoneType.BATTLEFIELD,
                toZone = ZoneType.GRAVEYARD,
                ownerId = playerId
            )
        )

        return checkForMoreContinuations(newState, events)
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

            for (effect in nextContinuation.remainingEffects) {
                val result = effectExecutorRegistry.execute(currentState, effect, context)

                if (!result.isSuccess) {
                    return ExecutionResult(result.state, allEvents + result.events, result.error)
                }

                if (result.isPaused) {
                    // Push remaining effects as new continuation
                    val remainingIndex = nextContinuation.remainingEffects.indexOf(effect)
                    val stillRemaining = nextContinuation.remainingEffects.drop(remainingIndex + 1)

                    var finalState = result.state
                    if (stillRemaining.isNotEmpty()) {
                        val newContinuation = EffectContinuation(
                            decisionId = result.pendingDecision!!.id,
                            remainingEffects = stillRemaining,
                            sourceId = nextContinuation.sourceId,
                            controllerId = nextContinuation.controllerId,
                            opponentId = nextContinuation.opponentId,
                            xValue = nextContinuation.xValue
                        )
                        finalState = finalState.pushContinuation(newContinuation)
                    }

                    return ExecutionResult.paused(
                        finalState,
                        result.pendingDecision!!,
                        allEvents + result.events
                    )
                }

                currentState = result.state
                allEvents.addAll(result.events)
            }

            return ExecutionResult.success(currentState, allEvents)
        }

        return ExecutionResult.success(state, events)
    }
}
