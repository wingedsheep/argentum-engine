package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.drawing.EachPlayerDiscardsOrLoseLifeExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone

class DiscardAndDrawContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    private val cycleCardHandler:
        com.wingedsheep.engine.handlers.actions.ability.CycleCardHandler by lazy {
            com.wingedsheep.engine.handlers.actions.ability.CycleCardHandler.create(services)
        }

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(HandSizeDiscardContinuation::class, ::resumeHandSizeDiscard),
        resumer(EachPlayerDiscardsOrLoseLifeContinuation::class, ::resumeEachPlayerDiscardsOrLoseLife),
        resumer(CycleCardChooseXContinuation::class, ::resumeCycleCardChooseX)
    )

    /**
     * Resume a cycling action once the player has announced X for an `{X}` cycling cost
     * (CR 107.3a) — Webstrike Elite's "Cycling {X}{G}{G}". Re-enter the handler with X bound; the
     * cost is then paid for that amount and the cycle proceeds normally. No follow-up decision —
     * paying the mana is automatic.
     */
    fun resumeCycleCardChooseX(
        state: GameState,
        continuation: CycleCardChooseXContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number response for cycling X choice")
        }
        val chosenX = response.number.coerceAtLeast(0)
        val result = cycleCardHandler.execute(state, continuation.action.copy(xValue = chosenX))
        return if (result.isPaused || result.error != null) result
        else checkForMore(result.newState, result.events)
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

        val result = ZoneTransitionService.discardCards(state, continuation.playerId, response.selectedCards)
        // The hand-size discard (CR 514.1) interrupted the cleanup step: performCleanupStep
        // early-returned to ask for this discard before performing the CR 514.2 turn-based
        // actions. Finish them now — otherwise marked damage (notably deathtouch damage that an
        // expiring "until end of turn" indestructible had suppressed) persists into the next turn
        // and kills the creature on the following turn's state-based-action check.
        val cleanedState = CleanupPhaseManager.applyCleanupTurnBasedActions(result.state, services.cardRegistry)
        return checkForMore(cleanedState, result.events)
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

}
