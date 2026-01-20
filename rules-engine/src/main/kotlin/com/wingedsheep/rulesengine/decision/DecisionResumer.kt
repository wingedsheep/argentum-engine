package com.wingedsheep.rulesengine.decision

import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.TappedComponent
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.zone.ZoneType

/**
 * Result of resuming a decision.
 *
 * Similar to ExecutionResult but focused on decision resumption.
 * The decision has been processed and cleared from the state.
 */
data class DecisionResumeResult(
    val state: GameState,
    val events: List<EffectEvent> = emptyList()
)

/**
 * Resumes effect execution from a serialized [DecisionContext].
 *
 * When a player submits a decision response, this class pattern-matches on the
 * context type stored in [GameState.decisionContext] and executes the appropriate
 * completion logic.
 *
 * This enables stateless decision management where [GameState] is the **only**
 * thing required to resume the game. No lambda-based continuations needed.
 *
 * ## Usage
 *
 * ```kotlin
 * val resumer = DecisionResumer()
 * val response = CardsChoice(decisionId, selectedCardIds)
 * val result = resumer.resume(state, response)
 * // result.state has the decision cleared and effect completed
 * ```
 *
 * ## Thread Safety
 *
 * This class is stateless and thread-safe. All state is passed via parameters.
 */
class DecisionResumer {

    /**
     * Resume effect execution with the player's decision response.
     *
     * @param state The current game state (must have pendingDecision and decisionContext set)
     * @param response The player's response to the pending decision
     * @return The new game state with the effect completed and decision cleared
     * @throws IllegalStateException if there's no pending decision or context
     * @throws IllegalArgumentException if the response doesn't match the pending decision
     */
    fun resume(state: GameState, response: DecisionResponse): DecisionResumeResult {
        val context = state.decisionContext
            ?: error("Cannot resume: no decision context in game state")
        val decision = state.pendingDecision
            ?: error("Cannot resume: no pending decision in game state")

        // Validate that the response matches the pending decision
        require(response.decisionId == decision.decisionId) {
            "Response decision ID '${response.decisionId}' does not match pending decision ID '${decision.decisionId}'"
        }

        // Clear the decision before processing (prevents double-processing)
        val clearedState = state.clearPendingDecision()

        return when (context) {
            is SearchLibraryContext -> resumeSearchLibrary(clearedState, context, response)
            is DiscardCardsContext -> resumeDiscardCards(clearedState, context, response)
            is SacrificeUnlessContext -> resumeSacrificeUnless(clearedState, context, response)
            is ChooseTargetsContext -> resumeChooseTargets(clearedState, context, response)
            is ReturnFromGraveyardContext -> resumeReturnFromGraveyard(clearedState, context, response)
            is LookAtTopCardsContext -> resumeLookAtTopCards(clearedState, context, response)
            is ModeChoiceContext -> resumeModeChoice(clearedState, context, response)
            is CleanupDiscardContext -> resumeCleanupDiscard(clearedState, context, response)
        }
    }

    // =========================================================================
    // SearchLibraryContext Resumption
    // =========================================================================

    private fun resumeSearchLibrary(
        state: GameState,
        context: SearchLibraryContext,
        response: DecisionResponse
    ): DecisionResumeResult {
        val cardsResponse = response as? CardsChoice
            ?: error("Expected CardsChoice response for SearchLibraryContext, got ${response::class.simpleName}")

        // Validate selection against valid targets
        val selectedCards = cardsResponse.selectedCardIds
            .filter { it in context.validTargets }
            .take(context.maxCount)

        val playerId = context.searchedPlayerId
        val libraryZone = ZoneId(ZoneType.LIBRARY, playerId)
        var currentState = state
        val events = mutableListOf<EffectEvent>()

        events.add(EffectEvent.LibrarySearched(playerId, selectedCards.size, context.filterDescription))

        if (selectedCards.isEmpty()) {
            // No cards selected - just shuffle if needed
            if (context.shuffleAfter) {
                currentState = currentState.shuffleLibrary(playerId)
                events.add(EffectEvent.LibraryShuffled(playerId))
            }
            return DecisionResumeResult(currentState, events)
        }

        // Special handling for TOP_OF_LIBRARY with shuffle
        // Cards like Personal Tutor: "shuffle, then put that card on top"
        if (context.destination == SearchDestination.TOP_OF_LIBRARY && context.shuffleAfter) {
            // Remove all selected cards from library first
            for (cardId in selectedCards) {
                currentState = currentState.removeFromZone(cardId, libraryZone)
            }

            // Shuffle the library (without the selected cards)
            currentState = currentState.shuffleLibrary(playerId)
            events.add(EffectEvent.LibraryShuffled(playerId))

            // Put selected cards on top (reversed so first selected is on top)
            for (cardId in selectedCards.reversed()) {
                val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
                currentState = currentState.addToZoneAt(cardId, libraryZone, 0)
                events.add(EffectEvent.CardMovedToZone(cardId, cardName, "top of library"))
            }

            return DecisionResumeResult(currentState, events)
        }

        // Standard flow for other destinations
        for (cardId in selectedCards) {
            val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"

            // Remove from library
            currentState = currentState.removeFromZone(cardId, libraryZone)

            // Add to destination
            val (destZoneId, zoneName) = getDestinationZone(context.destination, playerId)
            currentState = if (context.destination == SearchDestination.TOP_OF_LIBRARY) {
                currentState.addToZoneAt(cardId, destZoneId, 0)
            } else {
                currentState.addToZone(cardId, destZoneId)
            }

            // Apply tapped if entering battlefield tapped
            if (context.entersTapped && context.destination == SearchDestination.BATTLEFIELD) {
                currentState = currentState.updateEntity(cardId) { it.with(TappedComponent) }
            }

            events.add(EffectEvent.CardMovedToZone(cardId, cardName, zoneName))
        }

        // Shuffle library if required (for non-TOP_OF_LIBRARY destinations)
        if (context.shuffleAfter) {
            currentState = currentState.shuffleLibrary(playerId)
            events.add(EffectEvent.LibraryShuffled(playerId))
        }

        return DecisionResumeResult(currentState, events)
    }

    private fun getDestinationZone(destination: SearchDestination, playerId: EntityId): Pair<ZoneId, String> {
        return when (destination) {
            SearchDestination.HAND -> ZoneId(ZoneType.HAND, playerId) to "hand"
            SearchDestination.BATTLEFIELD -> ZoneId.BATTLEFIELD to "battlefield"
            SearchDestination.GRAVEYARD -> ZoneId(ZoneType.GRAVEYARD, playerId) to "graveyard"
            SearchDestination.TOP_OF_LIBRARY -> ZoneId(ZoneType.LIBRARY, playerId) to "top of library"
        }
    }

    // =========================================================================
    // DiscardCardsContext Resumption
    // =========================================================================

    private fun resumeDiscardCards(
        state: GameState,
        context: DiscardCardsContext,
        response: DecisionResponse
    ): DecisionResumeResult {
        val cardsResponse = response as? CardsChoice
            ?: error("Expected CardsChoice response for DiscardCardsContext, got ${response::class.simpleName}")

        val selectedCards = cardsResponse.selectedCardIds
            .filter { it in context.validTargets }

        val playerId = context.discardingPlayerId
        val handZone = ZoneId(ZoneType.HAND, playerId)
        val graveyardZone = ZoneId(ZoneType.GRAVEYARD, playerId)
        var currentState = state
        val events = mutableListOf<EffectEvent>()

        for (cardId in selectedCards) {
            val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
            currentState = currentState
                .removeFromZone(cardId, handZone)
                .addToZone(cardId, graveyardZone)
            events.add(EffectEvent.CardDiscarded(playerId, cardId, cardName))
        }

        return DecisionResumeResult(currentState, events)
    }

    // =========================================================================
    // SacrificeUnlessContext Resumption
    // =========================================================================

    private fun resumeSacrificeUnless(
        state: GameState,
        context: SacrificeUnlessContext,
        response: DecisionResponse
    ): DecisionResumeResult {
        val sacrificeResponse = response as? SacrificeUnlessChoice
            ?: error("Expected SacrificeUnlessChoice response for SacrificeUnlessContext")

        var currentState = state
        val events = mutableListOf<EffectEvent>()

        if (sacrificeResponse.payCost) {
            // Player chose to pay the cost - sacrifice the cost targets
            val validSacrifices = sacrificeResponse.sacrificedPermanents
                .filter { it in context.validCostTargets }
                .take(context.requiredCount)

            for (permanentId in validSacrifices) {
                val name = currentState.getEntity(permanentId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
                currentState = currentState
                    .removeFromCurrentZone(permanentId)
                    .addToGraveyard(context.controllerId, permanentId)
                events.add(EffectEvent.PermanentSacrificed(permanentId, name, context.controllerId))
            }
        } else {
            // Player chose not to pay - sacrifice the main permanent
            currentState = currentState
                .removeFromCurrentZone(context.permanentToSacrifice)
                .addToGraveyard(context.controllerId, context.permanentToSacrifice)
            events.add(EffectEvent.PermanentSacrificed(
                context.permanentToSacrifice,
                context.permanentName,
                context.controllerId
            ))
        }

        return DecisionResumeResult(currentState, events)
    }

    // =========================================================================
    // ChooseTargetsContext Resumption
    // =========================================================================

    private fun resumeChooseTargets(
        state: GameState,
        context: ChooseTargetsContext,
        response: DecisionResponse
    ): DecisionResumeResult {
        // Target selection is typically handled by the spell casting flow
        // This is a placeholder for future target-based decision handling
        val targetsResponse = response as? TargetsChoice
            ?: error("Expected TargetsChoice response for ChooseTargetsContext")

        // For now, just return the cleared state
        // Full implementation would continue spell resolution with targets
        return DecisionResumeResult(state)
    }

    // =========================================================================
    // ReturnFromGraveyardContext Resumption
    // =========================================================================

    private fun resumeReturnFromGraveyard(
        state: GameState,
        context: ReturnFromGraveyardContext,
        response: DecisionResponse
    ): DecisionResumeResult {
        val cardsResponse = response as? CardsChoice
            ?: error("Expected CardsChoice response for ReturnFromGraveyardContext")

        val selectedCards = cardsResponse.selectedCardIds
            .filter { it in context.validTargets }
            .take(context.maxCount)

        val playerId = context.graveyardOwnerId
        val graveyardZone = ZoneId(ZoneType.GRAVEYARD, playerId)
        var currentState = state
        val events = mutableListOf<EffectEvent>()

        for (cardId in selectedCards) {
            val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
            currentState = currentState.removeFromZone(cardId, graveyardZone)

            val (destZoneId, zoneName) = getDestinationZone(context.destination, playerId)
            currentState = currentState.addToZone(cardId, destZoneId)
            events.add(EffectEvent.CardMovedToZone(cardId, cardName, zoneName))
        }

        return DecisionResumeResult(currentState, events)
    }

    // =========================================================================
    // LookAtTopCardsContext Resumption
    // =========================================================================

    private fun resumeLookAtTopCards(
        state: GameState,
        context: LookAtTopCardsContext,
        response: DecisionResponse
    ): DecisionResumeResult {
        val cardsResponse = response as? CardsChoice
            ?: error("Expected CardsChoice response for LookAtTopCardsContext")

        val keptCards = cardsResponse.selectedCardIds
            .filter { it in context.cardIds }
            .take(context.keepCount)

        val nonKeptCards = context.cardIds.filter { it !in keptCards }

        val playerId = context.libraryOwnerId
        val libraryZone = ZoneId(ZoneType.LIBRARY, playerId)
        var currentState = state
        val events = mutableListOf<EffectEvent>()

        // Put kept cards into hand
        val handZone = ZoneId(ZoneType.HAND, playerId)
        for (cardId in keptCards) {
            val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
            currentState = currentState
                .removeFromZone(cardId, libraryZone)
                .addToZone(cardId, handZone)
            events.add(EffectEvent.CardMovedToZone(cardId, cardName, "hand"))
        }

        // Put non-kept cards to their destination
        for (cardId in nonKeptCards) {
            currentState = currentState.removeFromZone(cardId, libraryZone)

            currentState = when (context.restDestination) {
                RestDestination.TOP_OF_LIBRARY -> currentState.addToZoneAt(cardId, libraryZone, 0)
                RestDestination.BOTTOM_OF_LIBRARY -> currentState.addToZoneBottom(cardId, libraryZone)
                RestDestination.GRAVEYARD -> {
                    val graveyardZone = ZoneId(ZoneType.GRAVEYARD, playerId)
                    currentState.addToZone(cardId, graveyardZone)
                }
            }
        }

        return DecisionResumeResult(currentState, events)
    }

    // =========================================================================
    // ModeChoiceContext Resumption
    // =========================================================================

    private fun resumeModeChoice(
        state: GameState,
        context: ModeChoiceContext,
        response: DecisionResponse
    ): DecisionResumeResult {
        val modeResponse = response as? ModeChoice
            ?: error("Expected ModeChoice response for ModeChoiceContext")

        // Mode choice affects how subsequent effects execute
        // This is a placeholder - full implementation would store chosen modes
        // and continue spell/ability resolution
        return DecisionResumeResult(state)
    }

    // =========================================================================
    // CleanupDiscardContext Resumption
    // =========================================================================

    private fun resumeCleanupDiscard(
        state: GameState,
        context: CleanupDiscardContext,
        response: DecisionResponse
    ): DecisionResumeResult {
        val cardsResponse = response as? CardsChoice
            ?: error("Expected CardsChoice response for CleanupDiscardContext")

        val selectedCards = cardsResponse.selectedCardIds
            .filter { it in context.cardsInHand }
            .take(context.discardCount)

        val playerId = context.discardingPlayerId
        val handZone = ZoneId(ZoneType.HAND, playerId)
        val graveyardZone = ZoneId(ZoneType.GRAVEYARD, playerId)
        var currentState = state
        val events = mutableListOf<EffectEvent>()

        for (cardId in selectedCards) {
            val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
            currentState = currentState
                .removeFromZone(cardId, handZone)
                .addToZone(cardId, graveyardZone)
            events.add(EffectEvent.CardDiscarded(playerId, cardId, cardName))
        }

        // Remove the pending cleanup discard for this player
        currentState = currentState.removePendingCleanupDiscardForPlayer(playerId)

        return DecisionResumeResult(currentState, events)
    }
}
