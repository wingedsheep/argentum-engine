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
            val newContinuation = continuation.copy(
                currentPlayerId = nextPlayer,
                remainingPlayers = nextRemainingPlayers,
                chosenNumbers = newChosenNumbers
            )

            val decisionResult = decisionHandler.createNumberDecision(
                state = state,
                playerId = nextPlayer,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                prompt = prompt,
                minValue = 0,
                maxValue = 99,
                phase = DecisionPhase.RESOLUTION,
                answer = newContinuation,
            )

            return ExecutionResult.propagatePause(
                decisionResult.state,
                decisionResult.events
            )
        }

        // All players have chosen - resolve the bid
        return resolveSecretBid(state, continuation, newChosenNumbers, checkForMore)
    }

    /**
     * Resolve the secret bid: determine outcome groups and execute effects per matching bidder.
     * Each bidder's context is the resolving effect's own context (targets, pipeline values)
     * with controllerId = that bidder and xValue = bid amount, so effects can use
     * EffectTarget.Controller and DynamicAmount.XValue respectively.
     *
     * Every branch is queued as an [EffectContinuation] frame up front (first branch on top) and
     * popped just before it runs, so the branches still to run always sit beneath whatever the
     * running branch leaves on the stack. A branch that pauses for a decision returns that pause;
     * once the decision is answered, the continuation stack drains the remaining branches.
     */
    private fun resolveSecretBid(
        state: GameState,
        continuation: SecretBidContinuation,
        chosenNumbers: Map<EntityId, Int>,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val branches = secretBidBranches(continuation, chosenNumbers)

        var currentState = branches.asReversed().fold(state) { s, branch -> s.pushContinuation(branch) }
        val allEvents = mutableListOf<GameEvent>()

        for (branch in branches) {
            val (popped, stateWithoutBranch) = currentState.popContinuation()
            check(popped == branch) { "Secret bid branch frame is not on top of the continuation stack" }
            val result = services.effectExecutorRegistry
                .execute(stateWithoutBranch, branch.remainingEffects.single(), branch.effectContext)
                .toExecutionResult()
            if (result.error != null) return result
            if (result.pendingDecision != null) {
                return ExecutionResult.propagatePause(result.state, allEvents + result.events)
            }
            currentState = result.state
            allEvents.addAll(result.events)
        }

        return checkForMore(currentState, allEvents)
    }

    /** The per-bidder branches in execution order: highest, then lowest, then tied bidders. */
    private fun secretBidBranches(
        continuation: SecretBidContinuation,
        chosenNumbers: Map<EntityId, Int>
    ): List<EffectContinuation> {
        val nonZeroBids = chosenNumbers.filter { it.value > 0 }
        if (nonZeroBids.isEmpty()) return emptyList()
        val highestBid = nonZeroBids.values.max()
        val lowestBid = nonZeroBids.values.min()

        val groups = listOfNotNull(
            continuation.highestBidderEffect?.let { it to nonZeroBids.filter { bid -> bid.value == highestBid } },
            continuation.lowestBidderEffect?.let { it to nonZeroBids.filter { bid -> bid.value == lowestBid } },
            continuation.tiedBidderEffect?.takeIf { highestBid == lowestBid }?.let { it to nonZeroBids }
        )
        return groups.flatMap { (effect, bidders) ->
            bidders.map { (playerId, amount) ->
                EffectContinuation(
                    remainingEffects = listOf(effect),
                    effectContext = bidderContext(continuation, playerId, amount)
                )
            }
        }
    }

    private fun bidderContext(
        continuation: SecretBidContinuation,
        playerId: EntityId,
        bidAmount: Int
    ): EffectContext {
        val base = continuation.effectContext ?: EffectContext(
            resolvingTriggeredAbility = continuation.resolvingTriggeredAbility,
            sourceId = continuation.sourceId,
            controllerId = continuation.controllerId
        )
        return base.copy(
            objectReferences = continuation.objectReferences,
            effectControllerId = base.effectControllerId ?: base.controllerId,
            controllerId = playerId,
            xValue = bidAmount
        )
    }

}
