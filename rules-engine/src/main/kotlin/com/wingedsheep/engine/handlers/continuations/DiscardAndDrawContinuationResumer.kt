package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.ConniveEffectHandler
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.drawing.EachPlayerDiscardsOrLoseLifeExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone

class DiscardAndDrawContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(HandSizeDiscardContinuation::class, ::resumeHandSizeDiscard),
        resumer(EachPlayerDiscardsOrLoseLifeContinuation::class, ::resumeEachPlayerDiscardsOrLoseLife),
        resumer(ConniveContinuation::class, ::resumeConnive)
    )

    fun resumeHandSizeDiscard(
        state: GameState,
        continuation: HandSizeDiscardContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for hand size discard")
        }

        val result = ZoneTransitionService.discardCards(state, continuation.playerId, response.selectedCards)
        return checkForMore(result.state, result.events)
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

        // Snapshot creature-ness before the cards leave the battlefield-adjacent zone —
        // by the time discardCards finishes, CardComponent on the entity may not be the
        // right lookup anchor for downstream queries.
        val discardedCreatureCard = selectedCards.any { cardId ->
            state.getEntity(cardId)?.get<CardComponent>()?.isCreature == true
        }

        val discardResult = ZoneTransitionService.discardCards(state, currentPlayerId, selectedCards)
        val newState = discardResult.state
        val discardEvents: List<GameEvent> = discardResult.events

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
                val autoResult = ZoneTransitionService.discardCard(newState, nextPlayer, cardId)
                val autoDiscardedCreature = newDiscardedCreature + (nextPlayer to isCreature)

                return continueEachPlayerDiscardsOrLoseLife(
                    autoResult.state,
                    continuation.copy(
                        remainingPlayers = nextRemainingPlayers,
                        discardedCreature = autoDiscardedCreature
                    ),
                    discardEvents + autoResult.events,
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
            val autoResult = ZoneTransitionService.discardCard(state, nextPlayer, cardId)
            val autoDiscardedCreature = continuation.discardedCreature + (nextPlayer to isCreature)

            return continueEachPlayerDiscardsOrLoseLife(
                autoResult.state,
                continuation.copy(
                    remainingPlayers = nextRemainingPlayers,
                    discardedCreature = autoDiscardedCreature
                ),
                priorEvents + autoResult.events,
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

    fun resumeConnive(
        state: GameState,
        continuation: ConniveContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for Connive")
        }

        val selectedCards = response.selectedCards
        val controllerId = continuation.controllerId
        var newState = state
        val events = mutableListOf<GameEvent>()

        var discardedNonland = false
        for (cardId in selectedCards) {
            val isNonland = state.getEntity(cardId)?.get<CardComponent>()?.isLand == false
            val discardResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                .discardCard(newState, controllerId, cardId)
            newState = discardResult.state
            events.addAll(discardResult.events)
            if (isNonland) discardedNonland = true
        }

        if (discardedNonland) {
            val (counterState, counterEvents) = ConniveEffectHandler.addPlusOnePlusOne(newState, continuation.targetCreatureId)
            newState = counterState
            events.addAll(counterEvents)
        }

        return checkForMore(newState, events)
    }

}
