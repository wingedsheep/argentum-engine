package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CostPaymentContinuation
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.mechanics.cost.CostPaymentContext
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.mechanics.cost.PaymentResult
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Resumer for [CostPaymentContinuation] — the single resume path for every [PayCost] variant paid
 * through [CostPaymentService].
 *
 * It reads the player's response (yes/no, card selection, or option pick depending on the cost
 * shape), decides paid vs. declined, delegates the actual mutation to
 * [CostPaymentService.performPayment], then runs the continuation's `onPaid` / `onDeclined` follow-up
 * and chains via `checkForMore` so any caller-pushed frame beneath resumes too.
 */
class CostPaymentContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    private val paymentService = CostPaymentService(services)

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(CostPaymentContinuation::class, ::resume)
    )

    fun resume(
        state: GameState,
        continuation: CostPaymentContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult = when (val cost = continuation.cost) {
        is PayCost.Choice -> resumeChoice(state, continuation, cost, response, checkForMore)
        // Resolved away before the frame is built; should never reach the resumer.
        is PayCost.OwnManaCost ->
            ExecutionResult.error(state, "OwnManaCost should have been resolved before payment")
        is PayCost.Atom -> when (val atom = cost.atom) {
            // Yes/no costs: mana, life, and random discard.
            is CostAtom.Mana, is CostAtom.PayLife ->
                resumeYesNo(state, continuation, cost, response, checkForMore)
            is CostAtom.Discard ->
                if (atom.random) resumeYesNo(state, continuation, cost, response, checkForMore)
                else resumeSelection(state, continuation, cost, response, checkForMore)
            // Selection costs.
            is CostAtom.ExileFrom, is CostAtom.RevealFromHand, is CostAtom.Sacrifice,
            is CostAtom.ReturnToHand, is CostAtom.TapPermanents ->
                resumeSelection(state, continuation, cost, response, checkForMore)
        }
    }

    private fun resumeYesNo(
        state: GameState,
        continuation: CostPaymentContinuation,
        cost: PayCost,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for cost payment")
        }
        if (!response.choice) return declined(state, continuation, checkForMore)

        val execution = paymentService.performPayment(state, continuation.payerId, cost, continuation.sourceId, emptyList())
        // A defensive payment failure (e.g. mana solve came up short) falls through to declined.
        return if (execution.success) paid(execution.state, execution.events, continuation, checkForMore)
        else declined(state, continuation, checkForMore)
    }

    private fun resumeSelection(
        state: GameState,
        continuation: CostPaymentContinuation,
        cost: PayCost,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card-selection response for cost payment")
        }
        if (response.selectedCards.size < paymentService.requiredCount(cost)) {
            return declined(state, continuation, checkForMore)
        }
        val execution = paymentService.performPayment(state, continuation.payerId, cost, continuation.sourceId, response.selectedCards)
        return if (execution.success) paid(execution.state, execution.events, continuation, checkForMore)
        else declined(state, continuation, checkForMore)
    }

    private fun resumeChoice(
        state: GameState,
        continuation: CostPaymentContinuation,
        cost: PayCost.Choice,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option-choice response for cost payment")
        }
        // The trailing option (index == options.size) is "Don't pay".
        if (response.optionIndex >= cost.options.size) {
            return declined(state, continuation, checkForMore)
        }
        // Re-enter the service with the chosen sub-cost, carrying the same follow-up. It pushes a
        // fresh CostPaymentContinuation and pauses again — handling a sub-cost that itself needs input.
        val chosen = cost.options[response.optionIndex]
        val result = paymentService.pay(state, continuation.payerId, chosen, continuation.sourceId, contextOf(continuation))
        return when (result) {
            is PaymentResult.Pending -> ExecutionResult.paused(result.state, result.pendingDecision, result.events)
            // canAfford was checked when building the option list, so a sub-cost should be payable;
            // treat any unexpected non-pending result as a decline so the punisher branch still runs.
            else -> declined(result.state, continuation, checkForMore)
        }
    }

    private fun paid(
        state: GameState,
        priorEvents: List<GameEvent>,
        continuation: CostPaymentContinuation,
        checkForMore: CheckForMore
    ): ExecutionResult = runFollowup(state, priorEvents, continuation.onPaid, continuation, checkForMore)

    private fun declined(
        state: GameState,
        continuation: CostPaymentContinuation,
        checkForMore: CheckForMore
    ): ExecutionResult = runFollowup(state, emptyList(), continuation.onDeclined, continuation, checkForMore)

    private fun runFollowup(
        state: GameState,
        priorEvents: List<GameEvent>,
        followup: Effect?,
        continuation: CostPaymentContinuation,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (followup == null) return checkForMore(state, priorEvents)
        val result = services.effectExecutorRegistry
            .execute(state, followup, effectContext(state, continuation))
            .toExecutionResult()
        val allEvents = priorEvents + result.events
        return if (result.isPaused) {
            ExecutionResult.paused(result.state, result.pendingDecision!!, allEvents)
        } else {
            checkForMore(result.state, allEvents)
        }
    }

    private fun effectContext(state: GameState, continuation: CostPaymentContinuation): EffectContext =
        EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.payerId,
            targets = continuation.targets,
            pipeline = PipelineState(
                namedTargets = continuation.namedTargets,
                storedCollections = continuation.storedCollections
            )
        )

    private fun contextOf(continuation: CostPaymentContinuation): CostPaymentContext =
        CostPaymentContext(
            onPaid = continuation.onPaid,
            onDeclined = continuation.onDeclined,
            targets = continuation.targets,
            namedTargets = continuation.namedTargets,
            storedCollections = continuation.storedCollections
        )
}
