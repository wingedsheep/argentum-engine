package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.drawing.EachOpponentDiscardsExecutor
import com.wingedsheep.engine.handlers.effects.drawing.EachPlayerDiscardsOrLoseLifeExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone

class DiscardAndDrawContinuationResumer(
    private val ctx: ContinuationContext
) {

    fun resumeDiscard(
        state: GameState,
        continuation: DiscardContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for discard")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Move selected cards from hand to graveyard
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

        // Controller draws for each card discarded (Syphon Mind)
        if (continuation.controllerDrawsPerDiscard > 0 && continuation.controllerId != null) {
            val drawCount = selectedCards.size * continuation.controllerDrawsPerDiscard
            val drawResult = EachOpponentDiscardsExecutor.drawCards(
                newState, continuation.controllerId, drawCount
            )
            newState = drawResult.state
            events.addAll(drawResult.events)
        }

        // Check if there are more continuations to process
        return checkForMore(newState, events)
    }

    fun resumeHandSizeDiscard(
        state: GameState,
        continuation: HandSizeDiscardContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for hand size discard")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Move selected cards from hand to graveyard
        var newState = state
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val discardNames = selectedCards.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
        val events = listOf(
            CardsDiscardedEvent(playerId, selectedCards, discardNames)
        )

        // Check if there are more continuations to process
        return checkForMore(newState, events)
    }

    fun resumeEachPlayerDiscardsOrLoseLife(
        state: GameState,
        continuation: EachPlayerDiscardsOrLoseLifeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response")
        }

        val selectedCards = response.selectedCards
        val currentPlayerId = continuation.currentPlayerId

        // Discard the selected card
        var newState = state
        val handZone = ZoneKey(currentPlayerId, Zone.HAND)
        val graveyardZone = ZoneKey(currentPlayerId, Zone.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val discardEvents: List<GameEvent> = if (selectedCards.isNotEmpty()) {
            val discardNames = selectedCards.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            listOf(CardsDiscardedEvent(currentPlayerId, selectedCards, discardNames))
        } else {
            emptyList()
        }

        // Check if the discarded card was a creature
        val discardedCreatureCard = selectedCards.any { cardId ->
            state.getEntity(cardId)?.get<CardComponent>()?.isCreature == true
        }

        val newDiscardedCreature = continuation.discardedCreature + (currentPlayerId to discardedCreatureCard)

        // Check if there are more players
        if (continuation.remainingPlayers.isNotEmpty()) {
            val nextPlayer = continuation.remainingPlayers.first()
            val nextRemainingPlayers = continuation.remainingPlayers.drop(1)

            val nextHandZone = ZoneKey(nextPlayer, Zone.HAND)
            val nextHand = newState.getZone(nextHandZone)

            // If next player has empty hand, skip them (didn't discard a creature)
            if (nextHand.isEmpty()) {
                val skippedDiscardedCreature = newDiscardedCreature + (nextPlayer to false)
                return continueEachPlayerDiscardsOrLoseLife(
                    newState,
                    continuation.copy(
                        remainingPlayers = nextRemainingPlayers,
                        discardedCreature = skippedDiscardedCreature
                    ),
                    discardEvents,
                    checkForMore
                )
            }

            // If next player has exactly 1 card, auto-discard
            if (nextHand.size == 1) {
                val cardId = nextHand.first()
                val isCreature = newState.getEntity(cardId)?.get<CardComponent>()?.isCreature == true
                val nextGraveyardZone = ZoneKey(nextPlayer, Zone.GRAVEYARD)
                newState = newState.removeFromZone(nextHandZone, cardId)
                newState = newState.addToZone(nextGraveyardZone, cardId)

                val autoDiscardCardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                val autoDiscardEvents = discardEvents + listOf(CardsDiscardedEvent(nextPlayer, listOf(cardId), listOf(autoDiscardCardName)))
                val autoDiscardedCreature = newDiscardedCreature + (nextPlayer to isCreature)

                return continueEachPlayerDiscardsOrLoseLife(
                    newState,
                    continuation.copy(
                        remainingPlayers = nextRemainingPlayers,
                        discardedCreature = autoDiscardedCreature
                    ),
                    autoDiscardEvents,
                    checkForMore
                )
            }

            // Create decision for next player
            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = nextPlayer,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                prompt = "Choose a card to discard",
                options = nextHand,
                minSelections = 1,
                maxSelections = 1,
                ordered = false,
                phase = DecisionPhase.RESOLUTION
            )

            val newContinuation = continuation.copy(
                decisionId = decisionResult.pendingDecision!!.id,
                currentPlayerId = nextPlayer,
                remainingPlayers = nextRemainingPlayers,
                discardedCreature = newDiscardedCreature
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                discardEvents + decisionResult.events
            )
        }

        // All players have discarded - apply life loss
        val lifeLossResult = EachPlayerDiscardsOrLoseLifeExecutor.applyLifeLoss(
            newState, newDiscardedCreature, continuation.lifeLoss
        )

        return ExecutionResult(
            lifeLossResult.state,
            discardEvents + lifeLossResult.events,
            lifeLossResult.error
        )
    }

    private fun continueEachPlayerDiscardsOrLoseLife(
        state: GameState,
        continuation: EachPlayerDiscardsOrLoseLifeContinuation,
        priorEvents: List<GameEvent>,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (continuation.remainingPlayers.isEmpty()) {
            // All done - apply life loss
            val lifeLossResult = EachPlayerDiscardsOrLoseLifeExecutor.applyLifeLoss(
                state, continuation.discardedCreature, continuation.lifeLoss
            )
            return ExecutionResult(
                lifeLossResult.state,
                priorEvents + lifeLossResult.events,
                lifeLossResult.error
            )
        }

        val nextPlayer = continuation.remainingPlayers.first()
        val nextRemainingPlayers = continuation.remainingPlayers.drop(1)

        val nextHandZone = ZoneKey(nextPlayer, Zone.HAND)
        val nextHand = state.getZone(nextHandZone)

        // If next player has empty hand, skip them
        if (nextHand.isEmpty()) {
            val skippedDiscardedCreature = continuation.discardedCreature + (nextPlayer to false)
            return continueEachPlayerDiscardsOrLoseLife(
                state,
                continuation.copy(
                    remainingPlayers = nextRemainingPlayers,
                    discardedCreature = skippedDiscardedCreature
                ),
                priorEvents,
                checkForMore
            )
        }

        // If next player has exactly 1 card, auto-discard
        if (nextHand.size == 1) {
            val cardId = nextHand.first()
            val isCreature = state.getEntity(cardId)?.get<CardComponent>()?.isCreature == true
            val graveyardZone = ZoneKey(nextPlayer, Zone.GRAVEYARD)
            var newState = state.removeFromZone(nextHandZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)

            val autoDiscardCardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
            val autoDiscardEvents = priorEvents + listOf(CardsDiscardedEvent(nextPlayer, listOf(cardId), listOf(autoDiscardCardName)))
            val autoDiscardedCreature = continuation.discardedCreature + (nextPlayer to isCreature)

            return continueEachPlayerDiscardsOrLoseLife(
                newState,
                continuation.copy(
                    remainingPlayers = nextRemainingPlayers,
                    discardedCreature = autoDiscardedCreature
                ),
                autoDiscardEvents,
                checkForMore
            )
        }

        // Create decision for next player
        val decisionHandler = DecisionHandler()
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = nextPlayer,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            prompt = "Choose a card to discard",
            options = nextHand,
            minSelections = 1,
            maxSelections = 1,
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

}
