package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.player.OpenLifeBidLogic
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect

class CardSpecificContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(SecretBidContinuation::class, ::resumeSecretBid),
        resumer(OpenLifeBidContinuation::class, ::resumeOpenLifeBid),
        resumer(ContestedRetargetContinuation::class, ::resumeContestedRetarget)
    )

    /**
     * Resume a chosen player's retargeting of a contested spell/ability (Psychic Battle's reveal
     * winner). Applies the chosen target for the current slot, then continues with the remaining slots.
     */
    fun resumeContestedRetarget(
        state: GameState,
        continuation: ContestedRetargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for contested retarget")
        }
        val current = continuation.originalTargets.getOrNull(continuation.currentSlot)
            ?: return checkForMore(state, emptyList())
        val selectedId = response.selectedCards.firstOrNull()
        val chosenTarget = if (selectedId == null) {
            current
        } else {
            com.wingedsheep.engine.handlers.effects.stack.ContestedRetargetLogic
                .rebuildTarget(state, selectedId, current)
        }

        val result = com.wingedsheep.engine.handlers.effects.stack.ContestedRetargetLogic.advance(
            state = state,
            stackObjectId = continuation.stackObjectId,
            chooserId = continuation.chooserId,
            ownerControllerId = continuation.ownerControllerId,
            perSlotRequirements = continuation.perSlotRequirements,
            originalTargets = continuation.originalTargets,
            newTargets = continuation.newTargets + chosenTarget,
            startSlot = continuation.currentSlot + 1,
            sourceId = continuation.sourceId
        )
        return if (result.pendingDecision != null) {
            result.toExecutionResult()
        } else {
            checkForMore(result.state, result.events)
        }
    }

    /**
     * Resume an open life-bid auction (Mages' Contest). On a "top" yes/no we either ask for
     * the bid amount or resolve (a pass ends the auction); on a bid amount we flip the high
     * bidder and ask the previous high bidder whether to top again.
     */
    fun resumeOpenLifeBid(
        state: GameState,
        continuation: OpenLifeBidContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val executeEffect = { s: GameState, e: Effect, c: EffectContext ->
            services.effectExecutorRegistry.execute(s, e, c)
        }

        return when (continuation.stage) {
            OpenLifeBidStage.AWAITING_TOP_DECISION -> {
                if (response !is YesNoResponse) {
                    return ExecutionResult.error(state, "Expected yes/no response for life bid")
                }
                if (!response.choice) {
                    // Pass — the high bid stands; resolve in favor of the current high bidder.
                    val result = OpenLifeBidLogic.resolve(
                        state, continuation.casterId, continuation.highBidder, continuation.highBid,
                        continuation.onWin, continuation.targets, continuation.sourceId, executeEffect
                    )
                    if (result.pendingDecision != null) result else checkForMore(result.state, result.events)
                } else {
                    OpenLifeBidLogic.askAmount(state, continuation)
                }
            }

            OpenLifeBidStage.AWAITING_BID_AMOUNT -> {
                if (response !is NumberChosenResponse) {
                    return ExecutionResult.error(state, "Expected number response for life bid")
                }
                val newBid = response.number.coerceAtLeast(continuation.highBid + 1)
                // The topping player becomes the high bidder; the previous high bidder is asked next.
                val result = OpenLifeBidLogic.advance(
                    state, continuation.casterId,
                    highBidder = continuation.bidderToAsk, highBid = newBid,
                    bidderToAsk = continuation.highBidder, onWin = continuation.onWin,
                    targets = continuation.targets, sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName, executeEffect = executeEffect
                )
                if (result.pendingDecision != null) result else checkForMore(result.state, result.events)
            }
        }
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
