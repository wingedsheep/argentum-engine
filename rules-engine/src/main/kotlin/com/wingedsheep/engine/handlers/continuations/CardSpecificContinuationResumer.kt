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
        resumer(SecretChoiceContinuation::class, ::resumeSecretChoice),
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
            sourceId = continuation.sourceId,
            targetFinder = services.targetFinder
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

        val context = (continuation.effectContext ?: EffectContext(
            sourceId = continuation.sourceId, controllerId = continuation.casterId, targets = continuation.targets
        )).copy(objectReferences = continuation.objectReferences)

        return when (continuation.stage) {
            OpenLifeBidStage.AWAITING_TOP_DECISION -> {
                if (response !is YesNoResponse) {
                    return ExecutionResult.error(state, "Expected yes/no response for life bid")
                }
                if (!response.choice) {
                    // Pass — the high bid stands; resolve in favor of the current high bidder.
                    val result = OpenLifeBidLogic.resolve(
                        state, continuation.casterId, continuation.highBidder, continuation.highBid,
                        continuation.onWin, continuation.targets, continuation.sourceId, executeEffect, context
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
                    sourceName = continuation.sourceName, executeEffect = executeEffect, context = context
                )
                if (result.pendingDecision != null) result else checkForMore(result.state, result.events)
            }
        }
    }

    fun resumeSecretChoice(
        state: GameState,
        continuation: SecretChoiceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val result = com.wingedsheep.engine.handlers.effects.player.SecretChoiceLogic.resume(
            state, continuation, response, services.predicateEvaluator.amounts
        )
        return if (result.error != null || result.pendingDecision != null) result
        else checkForMore(result.state, result.events)
    }
}
