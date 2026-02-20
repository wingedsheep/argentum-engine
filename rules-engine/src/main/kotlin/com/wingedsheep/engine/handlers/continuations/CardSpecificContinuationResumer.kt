package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.drawing.ReadTheRunesExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

class CardSpecificContinuationResumer(
    private val ctx: ContinuationContext
) {

    fun resumeSecretBid(
        state: GameState,
        continuation: SecretBidContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number chosen response")
        }

        val chosenNumber = response.number
        val currentPlayerId = continuation.currentPlayerId

        val newChosenNumbers = continuation.chosenNumbers + (currentPlayerId to chosenNumber)

        // Check if there are more players
        if (continuation.remainingPlayers.isNotEmpty()) {
            val nextPlayer = continuation.remainingPlayers.first()
            val nextRemainingPlayers = continuation.remainingPlayers.drop(1)

            val prompt = "Secretly choose a number (you will lose that much life if you have the highest bid)"

            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createNumberDecision(
                state = state,
                playerId = nextPlayer,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                prompt = prompt,
                minValue = 0,
                maxValue = 99,
                phase = DecisionPhase.RESOLUTION
            )

            val newContinuation = continuation.copy(
                decisionId = decisionResult.pendingDecision!!.id,
                currentPlayerId = nextPlayer,
                remainingPlayers = nextRemainingPlayers,
                chosenNumbers = newChosenNumbers
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                decisionResult.events
            )
        }

        // All players have chosen - resolve the bid
        return resolveSecretBid(state, continuation, newChosenNumbers, checkForMore)
    }

    /**
     * Resolve the secret bid: find highest number, apply life loss and counters.
     */
    private fun resolveSecretBid(
        state: GameState,
        continuation: SecretBidContinuation,
        chosenNumbers: Map<EntityId, Int>,
        checkForMore: CheckForMore
    ): ExecutionResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        val highestBid = chosenNumbers.values.maxOrNull() ?: 0

        // Each player with the highest number loses that much life
        val highestBidders = chosenNumbers.filter { it.value == highestBid && it.value > 0 }

        for ((playerId, amount) in highestBidders) {
            val currentLife = currentState.getEntity(playerId)
                ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 0
            val newLife = currentLife - amount

            currentState = currentState.updateEntity(playerId) { container ->
                container.with(com.wingedsheep.engine.state.components.identity.LifeTotalComponent(newLife))
            }

            allEvents.add(LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.LIFE_LOSS))
        }

        // If the controller is one of the highest bidders, put counters on the source
        if (highestBidders.containsKey(continuation.controllerId) && continuation.sourceId != null) {
            val counterType = try {
                com.wingedsheep.sdk.core.CounterType.valueOf(
                    continuation.counterType.uppercase()
                        .replace(' ', '_')
                        .replace('+', 'P')
                        .replace('-', 'M')
                        .replace("/", "_")
                )
            } catch (e: IllegalArgumentException) {
                com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE
            }

            val current = currentState.getEntity(continuation.sourceId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                ?: com.wingedsheep.engine.state.components.battlefield.CountersComponent()

            currentState = currentState.updateEntity(continuation.sourceId) { container ->
                container.with(current.withAdded(counterType, continuation.counterCount))
            }

            val entityName = currentState.getEntity(continuation.sourceId)?.get<CardComponent>()?.name ?: ""
            allEvents.add(CountersAddedEvent(continuation.sourceId, continuation.counterType, continuation.counterCount, entityName))
        }

        return checkForMore(currentState, allEvents)
    }

    fun resumeReadTheRunes(
        state: GameState,
        continuation: ReadTheRunesContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for Read the Runes")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        return when (continuation.phase) {
            ReadTheRunesPhase.SACRIFICE_CHOICE -> {
                if (selectedCards.isNotEmpty()) {
                    // Player chose to sacrifice a permanent
                    val permanentId = selectedCards.first()
                    val result = ReadTheRunesExecutor.sacrificePermanent(state, playerId, permanentId)
                    val loopResult = ReadTheRunesExecutor.startChoiceLoop(
                        result.state, playerId, continuation.sourceId, continuation.sourceName,
                        continuation.remainingChoices - 1
                    )
                    if (loopResult.isPaused) {
                        ExecutionResult.paused(
                            loopResult.state,
                            loopResult.pendingDecision!!,
                            result.events + loopResult.events
                        )
                    } else {
                        checkForMore(loopResult.state, result.events + loopResult.events)
                    }
                } else {
                    // Player chose not to sacrifice - must discard a card
                    val hand = state.getZone(ZoneKey(playerId, Zone.HAND))
                    if (hand.isEmpty()) {
                        // No cards to discard - skip
                        val loopResult = ReadTheRunesExecutor.startChoiceLoop(
                            state, playerId, continuation.sourceId, continuation.sourceName,
                            continuation.remainingChoices - 1
                        )
                        if (loopResult.isPaused) {
                            return loopResult
                        }
                        checkForMore(loopResult.state, loopResult.events)
                    } else if (hand.size == 1) {
                        // Auto-discard only card
                        val result = ReadTheRunesExecutor.discardCard(state, playerId, hand.first())
                        val loopResult = ReadTheRunesExecutor.startChoiceLoop(
                            result.state, playerId, continuation.sourceId, continuation.sourceName,
                            continuation.remainingChoices - 1
                        )
                        if (loopResult.isPaused) {
                            ExecutionResult.paused(
                                loopResult.state,
                                loopResult.pendingDecision!!,
                                result.events + loopResult.events
                            )
                        } else {
                            checkForMore(loopResult.state, result.events + loopResult.events)
                        }
                    } else {
                        // Present discard choice
                        val decisionResult = DecisionHandler().createCardSelectionDecision(
                            state = state,
                            playerId = playerId,
                            sourceId = continuation.sourceId,
                            sourceName = continuation.sourceName,
                            prompt = "Choose a card to discard (${continuation.remainingChoices} remaining)",
                            options = hand,
                            minSelections = 1,
                            maxSelections = 1,
                            ordered = false,
                            phase = DecisionPhase.RESOLUTION
                        )

                        val newContinuation = ReadTheRunesContinuation(
                            decisionId = decisionResult.pendingDecision!!.id,
                            playerId = playerId,
                            sourceId = continuation.sourceId,
                            sourceName = continuation.sourceName,
                            remainingChoices = continuation.remainingChoices,
                            phase = ReadTheRunesPhase.DISCARD_CHOICE
                        )

                        val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

                        ExecutionResult.paused(
                            stateWithContinuation,
                            decisionResult.pendingDecision,
                            decisionResult.events
                        )
                    }
                }
            }
            ReadTheRunesPhase.DISCARD_CHOICE -> {
                if (selectedCards.isEmpty()) {
                    return ExecutionResult.error(state, "Must select a card to discard for Read the Runes")
                }
                val cardId = selectedCards.first()
                val result = ReadTheRunesExecutor.discardCard(state, playerId, cardId)
                val loopResult = ReadTheRunesExecutor.startChoiceLoop(
                    result.state, playerId, continuation.sourceId, continuation.sourceName,
                    continuation.remainingChoices - 1
                )
                if (loopResult.isPaused) {
                    ExecutionResult.paused(
                        loopResult.state,
                        loopResult.pendingDecision!!,
                        result.events + loopResult.events
                    )
                } else {
                    checkForMore(loopResult.state, result.events + loopResult.events)
                }
            }
        }
    }

}
