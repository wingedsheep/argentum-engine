package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.SearchDestination

class LibraryAndZoneContinuationResumer(
    private val ctx: ContinuationContext
) {

    fun resumeReturnFromGraveyard(
        state: GameState,
        continuation: ReturnFromGraveyardContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for graveyard search")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Empty selection — no card returned
        if (selectedCards.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        val cardId = selectedCards.first()
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        // Validate card is still in graveyard
        if (cardId !in state.getZone(graveyardZone)) {
            return checkForMore(state, emptyList())
        }

        val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
        var newState = state.removeFromZone(graveyardZone, cardId)

        val toZone = when (continuation.destination) {
            SearchDestination.HAND -> {
                val handZone = ZoneKey(playerId, Zone.HAND)
                newState = newState.addToZone(handZone, cardId)
                Zone.HAND
            }
            SearchDestination.BATTLEFIELD -> {
                val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
                newState = newState.addToZone(battlefieldZone, cardId)
                newState = newState.updateEntity(cardId) { c ->
                    c.with(com.wingedsheep.engine.state.components.identity.ControllerComponent(playerId))
                        .with(com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent)
                }
                Zone.BATTLEFIELD
            }
            else -> return ExecutionResult.error(state, "Unsupported destination: ${continuation.destination}")
        }

        val events = listOf(
            ZoneChangeEvent(
                entityId = cardId,
                entityName = cardName,
                fromZone = Zone.GRAVEYARD,
                toZone = toZone,
                ownerId = playerId
            )
        )

        return checkForMore(newState, events)
    }

    /**
     * Resume after player ordered cards for a MoveCollection with ControllerChooses order.
     *
     * The response contains the card IDs in the new order (first = new top of library).
     * We remove the cards from their current zones and place them on top in the chosen order.
     */
    fun resumeMoveCollectionOrder(
        state: GameState,
        continuation: MoveCollectionOrderContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for MoveCollection order")
        }

        val orderedCards = response.orderedObjects
        val destPlayerId = continuation.destinationPlayerId
        val libraryZone = ZoneKey(destPlayerId, Zone.LIBRARY)

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Remove all cards from their current zones
        for (cardId in orderedCards) {
            val ownerId = newState.getEntity(cardId)?.get<OwnerComponent>()?.playerId ?: destPlayerId
            for (zone in Zone.entries) {
                val zoneKey = ZoneKey(ownerId, zone)
                if (cardId in newState.getZone(zoneKey)) {
                    newState = newState.removeFromZone(zoneKey, cardId)
                    break
                }
            }
        }

        // Place cards in library in the chosen order
        val currentLibrary = newState.getZone(libraryZone)
        newState = if (continuation.placement == com.wingedsheep.sdk.scripting.ZonePlacement.Bottom) {
            // Bottom: append ordered cards at the end
            newState.copy(
                zones = newState.zones + (libraryZone to currentLibrary + orderedCards)
            )
        } else {
            // Top (default): prepend ordered cards at the beginning
            newState.copy(
                zones = newState.zones + (libraryZone to orderedCards + currentLibrary)
            )
        }

        events.add(
            LibraryReorderedEvent(
                playerId = continuation.playerId,
                cardCount = orderedCards.size,
                source = continuation.sourceName
            )
        )

        return checkForMore(newState, events)
    }

    /**
     * Resume after player ordered cards to put on the bottom of their library.
     *
     * Same as resumeReorderLibrary but places cards on the BOTTOM of the library
     * instead of the top. Used for effects like Erratic Explosion.
     */
    fun resumePutOnBottomOfLibrary(
        state: GameState,
        continuation: PutOnBottomOfLibraryContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for library bottom reorder")
        }

        val playerId = continuation.playerId
        val orderedCards = response.orderedObjects
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)

        // Get current library
        val currentLibrary = state.getZone(libraryZone).toMutableList()

        // Remove the reordered cards from the library (they should already be removed by the executor,
        // but filter just in case)
        val cardsSet = orderedCards.toSet()
        val remainingLibrary = currentLibrary.filter { it !in cardsSet }

        // Place the cards on the BOTTOM in the player's chosen order
        val newLibrary = remainingLibrary + orderedCards

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

        return checkForMore(newState, events)
    }

    /**
     * Resume after player reorders revealed cards for Kaboom!'s "for each target" effect.
     *
     * Puts cards on bottom in chosen order, then continues processing remaining targets.
     */
    fun resumeKaboomReorder(
        state: GameState,
        continuation: KaboomReorderContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for Kaboom! library bottom reorder")
        }

        val playerId = continuation.playerId
        val orderedCards = response.orderedObjects
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)

        // Get current library and remove the reordered cards (they should already be removed)
        val currentLibrary = state.getZone(libraryZone).toMutableList()
        val cardsSet = orderedCards.toSet()
        val remainingLibrary = currentLibrary.filter { it !in cardsSet }

        // Place cards on the BOTTOM in the player's chosen order
        val newLibrary = remainingLibrary + orderedCards
        var newState = state.copy(
            zones = state.zones + (libraryZone to newLibrary)
        )

        val events = mutableListOf<GameEvent>(
            LibraryReorderedEvent(
                playerId = playerId,
                cardCount = orderedCards.size,
                source = continuation.sourceName
            )
        )

        // Continue processing remaining targets
        if (continuation.remainingTargetIds.isNotEmpty()) {
            val result = com.wingedsheep.engine.handlers.effects.library.RevealUntilNonlandDealDamageEachTargetExecutor.processTargets(
                newState, playerId, continuation.sourceId, continuation.remainingTargetIds
            )

            if (result.isPaused) {
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    events + result.events
                )
            }

            return ExecutionResult.success(result.state, events + result.events)
        }

        return checkForMore(newState, events)
    }

    fun resumePutFromHand(
        state: GameState,
        continuation: PutFromHandContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for put-from-hand")
        }

        // Player selected 0 cards — declined
        if (response.selectedCards.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        val cardId = response.selectedCards.first()
        val playerId = continuation.playerId
        val handZone = ZoneKey(playerId, Zone.HAND)

        // Verify card is still in hand
        if (cardId !in state.getZone(handZone)) {
            return checkForMore(state, emptyList())
        }

        var newState = state

        // Remove from hand
        newState = newState.removeFromZone(handZone, cardId)

        // Add to battlefield
        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, cardId)

        // Apply battlefield components
        val container = newState.getEntity(cardId)
        if (container != null) {
            var newContainer = container
                .with(com.wingedsheep.engine.state.components.identity.ControllerComponent(playerId))

            // Creatures enter with summoning sickness
            val cardComponent = container.get<CardComponent>()
            if (cardComponent?.typeLine?.isCreature == true) {
                newContainer = newContainer.with(
                    com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
                )
            }

            // Apply tapped status if entersTapped
            if (continuation.entersTapped) {
                newContainer = newContainer.with(TappedComponent)
            }

            newState = newState.copy(
                entities = newState.entities + (cardId to newContainer)
            )
        }

        // Emit zone change event
        val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
        val cardName = cardComponent?.name ?: "Unknown"
        val events = listOf(
            ZoneChangeEvent(
                entityId = cardId,
                entityName = cardName,
                fromZone = Zone.HAND,
                toZone = Zone.BATTLEFIELD,
                ownerId = playerId
            )
        )

        return checkForMore(newState, events)
    }

    fun resumeSelectFromCollection(
        state: GameState,
        continuation: SelectFromCollectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for SelectFromCollection")
        }

        val selectedSet = response.selectedCards.toSet()
        val selected = continuation.allCards.filter { it in selectedSet }
        val remainder = continuation.allCards.filter { it !in selectedSet }

        // Build the updated collections
        val updatedCollections = continuation.storedCollections.toMutableMap()
        updatedCollections[continuation.storeSelected] = selected
        if (continuation.storeRemainder != null) {
            updatedCollections[continuation.storeRemainder] = remainder
        }

        // Inject updated collections into the next EffectContinuation on the stack (if present)
        val nextFrame = state.peekContinuation()
        val newState = if (nextFrame is EffectContinuation) {
            val (_, stateAfterPop) = state.popContinuation()
            stateAfterPop.pushContinuation(
                nextFrame.copy(storedCollections = updatedCollections)
            )
        } else {
            state
        }

        return checkForMore(newState, emptyList())
    }
}
