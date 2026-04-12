package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.drawing.ReadTheRunesExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect

class CardSpecificContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(SecretBidContinuation::class, ::resumeSecretBid),
        resumer(ReadTheRunesContinuation::class, ::resumeReadTheRunes)
    )

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
     * Resolve the secret bid: determine outcome groups and execute effects per matching bidder.
     * Each bidder's context sets controllerId = that bidder and xValue = bid amount,
     * so effects can use EffectTarget.Controller and DynamicAmount.XValue respectively.
     */
    private fun resolveSecretBid(
        state: GameState,
        continuation: SecretBidContinuation,
        chosenNumbers: Map<EntityId, Int>,
        checkForMore: CheckForMore
    ): ExecutionResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        val nonZeroBids = chosenNumbers.filter { it.value > 0 }

        if (nonZeroBids.isNotEmpty()) {
            val highestBid = nonZeroBids.values.max()
            val lowestBid = nonZeroBids.values.min()

            // Execute highestBidderEffect per player with the highest bid
            if (continuation.highestBidderEffect != null) {
                val highest = nonZeroBids.filter { it.value == highestBid }
                for ((playerId, amount) in highest) {
                    val result = executeForBidder(currentState, continuation, continuation.highestBidderEffect, playerId, amount)
                    if (result.error != null) return result
                    currentState = result.state
                    allEvents.addAll(result.events)
                }
            }

            // Execute lowestBidderEffect per player with the lowest non-zero bid
            if (continuation.lowestBidderEffect != null) {
                val lowest = nonZeroBids.filter { it.value == lowestBid }
                for ((playerId, amount) in lowest) {
                    val result = executeForBidder(currentState, continuation, continuation.lowestBidderEffect, playerId, amount)
                    if (result.error != null) return result
                    currentState = result.state
                    allEvents.addAll(result.events)
                }
            }

            // Execute tiedBidderEffect per player when all non-zero bids are equal
            if (continuation.tiedBidderEffect != null && highestBid == lowestBid) {
                for ((playerId, amount) in nonZeroBids) {
                    val result = executeForBidder(currentState, continuation, continuation.tiedBidderEffect, playerId, amount)
                    if (result.error != null) return result
                    currentState = result.state
                    allEvents.addAll(result.events)
                }
            }
        }

        return checkForMore(currentState, allEvents)
    }

    private fun executeForBidder(
        state: GameState,
        continuation: SecretBidContinuation,
        effect: Effect,
        playerId: EntityId,
        bidAmount: Int
    ): ExecutionResult {
        val context = com.wingedsheep.engine.handlers.EffectContext(
            sourceId = continuation.sourceId,
            controllerId = playerId,
            opponentId = state.turnOrder.firstOrNull { it != playerId },
            xValue = bidAmount
        )
        return services.effectExecutorRegistry.execute(state, effect, context).toExecutionResult()
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
                    val result = ReadTheRunesExecutor.sacrificePermanent(state, playerId, permanentId).toExecutionResult()
                    val loopResult = ReadTheRunesExecutor.startChoiceLoop(
                        result.state, playerId, continuation.sourceId, continuation.sourceName,
                        continuation.remainingChoices - 1
                    ).toExecutionResult()
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
                        ).toExecutionResult()
                        if (loopResult.isPaused) {
                            return loopResult
                        }
                        checkForMore(loopResult.state, loopResult.events)
                    } else if (hand.size == 1) {
                        // Auto-discard only card
                        val result = ReadTheRunesExecutor.discardCard(state, playerId, hand.first()).toExecutionResult()
                        val loopResult = ReadTheRunesExecutor.startChoiceLoop(
                            result.state, playerId, continuation.sourceId, continuation.sourceName,
                            continuation.remainingChoices - 1
                        ).toExecutionResult()
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
                val result = ReadTheRunesExecutor.discardCard(state, playerId, cardId).toExecutionResult()
                val loopResult = ReadTheRunesExecutor.startChoiceLoop(
                    result.state, playerId, continuation.sourceId, continuation.sourceName,
                    continuation.remainingChoices - 1
                ).toExecutionResult()
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
