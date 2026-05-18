package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect

class CardSpecificContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(SecretBidContinuation::class, ::resumeSecretBid)
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

}
