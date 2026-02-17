package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.drawing.ReadTheRunesExecutor
import com.wingedsheep.engine.handlers.effects.drawing.TradeSecretsExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

class CardSpecificContinuationResumer(
    private val ctx: ContinuationContext
) {

    /**
     * Resume after controller selected cards from opponent's library for Head Games.
     * Move selected cards to opponent's hand, then shuffle opponent's library.
     */
    fun resumeHeadGames(
        state: GameState,
        continuation: HeadGamesContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for Head Games")
        }

        val targetPlayerId = continuation.targetPlayerId
        val libraryZone = ZoneKey(targetPlayerId, Zone.LIBRARY)
        val handZone = ZoneKey(targetPlayerId, Zone.HAND)
        val selectedCards = response.selectedCards
        val events = mutableListOf<GameEvent>()

        var newState = state

        // Move selected cards from library to opponent's hand
        for (cardId in selectedCards) {
            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(handZone, cardId)

            val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardComponent?.name ?: "Unknown",
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.HAND,
                    ownerId = targetPlayerId
                )
            )
        }

        // Shuffle opponent's library
        val library = newState.getZone(libraryZone).shuffled()
        newState = newState.copy(zones = newState.zones + (libraryZone to library))
        events.add(LibraryShuffledEvent(targetPlayerId))

        return checkForMore(newState, events)
    }

    /**
     * Resume after controller selected cards from target player's library to exile.
     *
     * Moves selected cards to exile, then shuffles the target player's library.
     */
    fun resumeSearchTargetLibraryExile(
        state: GameState,
        continuation: SearchTargetLibraryExileContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for SearchTargetLibraryExile")
        }

        val targetPlayerId = continuation.targetPlayerId
        val libraryZone = ZoneKey(targetPlayerId, Zone.LIBRARY)
        val exileZone = ZoneKey(targetPlayerId, Zone.EXILE)
        val selectedCards = response.selectedCards
        val events = mutableListOf<GameEvent>()

        var newState = state

        // Move selected cards from library to exile
        for (cardId in selectedCards) {
            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(exileZone, cardId)

            val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardComponent?.name ?: "Unknown",
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.EXILE,
                    ownerId = targetPlayerId
                )
            )
        }

        // Shuffle target player's library
        val library = newState.getZone(libraryZone).shuffled()
        newState = newState.copy(zones = newState.zones + (libraryZone to library))
        events.add(LibraryShuffledEvent(targetPlayerId))

        return checkForMore(newState, events)
    }

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

            allEvents.add(CountersAddedEvent(continuation.sourceId, continuation.counterType, continuation.counterCount))
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

    /**
     * Resume Trade Secrets after a decision.
     *
     * Two phases:
     * - CONTROLLER_DRAWS: Controller chose how many cards to draw (0-4). Draw them, then ask opponent to repeat.
     * - OPPONENT_REPEATS: Opponent chose whether to repeat. If yes, opponent draws 2, then ask controller again.
     */
    fun resumeTradeSecrets(
        state: GameState,
        continuation: TradeSecretsContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        return when (continuation.phase) {
            TradeSecretsPhase.CONTROLLER_DRAWS -> {
                if (response !is NumberChosenResponse) {
                    return ExecutionResult.error(state, "Expected number chosen response for Trade Secrets")
                }

                val chosenCount = response.number
                val events = mutableListOf<GameEvent>()

                // Draw the chosen number of cards for the controller
                var currentState = state
                if (chosenCount > 0) {
                    val drawResult = TradeSecretsExecutor.drawCards(
                        currentState, continuation.controllerId, chosenCount
                    )
                    currentState = drawResult.state
                    events.addAll(drawResult.events)

                    if (!drawResult.isSuccess) {
                        return ExecutionResult(currentState, events, drawResult.error)
                    }
                }

                // Now ask opponent whether to repeat
                TradeSecretsExecutor.askOpponentToRepeat(
                    state = currentState,
                    controllerId = continuation.controllerId,
                    opponentId = continuation.opponentId,
                    sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName,
                    priorEvents = events
                )
            }

            TradeSecretsPhase.OPPONENT_REPEATS -> {
                if (response !is YesNoResponse) {
                    return ExecutionResult.error(state, "Expected yes/no response for Trade Secrets repeat")
                }

                if (!response.choice) {
                    // Opponent chose not to repeat - done
                    return checkForMore(state, emptyList())
                }

                // Opponent chose to repeat: draw 2 cards for opponent
                val drawResult = TradeSecretsExecutor.drawCards(
                    state, continuation.opponentId, 2
                )
                if (!drawResult.isSuccess) {
                    return drawResult
                }

                // Ask controller how many cards to draw again
                TradeSecretsExecutor.askControllerToDraw(
                    state = drawResult.state,
                    controllerId = continuation.controllerId,
                    opponentId = continuation.opponentId,
                    sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName,
                    priorEvents = drawResult.events.toList()
                )
            }
        }
    }
}
