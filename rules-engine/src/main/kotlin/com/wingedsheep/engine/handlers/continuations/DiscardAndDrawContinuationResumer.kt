package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.drawing.EachOpponentDiscardsExecutor
import com.wingedsheep.engine.handlers.effects.drawing.EachPlayerDiscardsOrLoseLifeExecutor
import com.wingedsheep.engine.handlers.effects.drawing.EachPlayerMayDrawExecutor
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

        val events = mutableListOf<GameEvent>(
            CardsDiscardedEvent(playerId, selectedCards)
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

        val events = listOf(
            CardsDiscardedEvent(playerId, selectedCards)
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
            listOf(CardsDiscardedEvent(currentPlayerId, selectedCards))
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

                val autoDiscardEvents = discardEvents + listOf(CardsDiscardedEvent(nextPlayer, listOf(cardId)))
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

            val autoDiscardEvents = priorEvents + listOf(CardsDiscardedEvent(nextPlayer, listOf(cardId)))
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

    fun resumeEachPlayerChoosesDraw(
        state: GameState,
        continuation: EachPlayerChoosesDrawContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
            val libraryZone = ZoneKey(nextPlayer, Zone.LIBRARY)
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

        return checkForMore(result.state, result.events.toList())
    }
}
